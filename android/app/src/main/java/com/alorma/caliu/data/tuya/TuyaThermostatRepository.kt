package com.alorma.caliu.data.tuya

import com.alorma.caliu.domain.MinuteOfDay
import com.alorma.caliu.domain.Mode
import com.alorma.caliu.domain.ProgramEntry
import com.alorma.caliu.domain.ThermostatRepository
import com.alorma.caliu.domain.ThermostatState
import com.alorma.caliu.domain.WeekProgram
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * El termòstat real per Tuya Cloud. Ajunta l'estat bàsic de v1 (`/status`) amb el mode, `work_state` i el programa
 * d'avui i d'ahir de v2 (`shadow/properties`), que v1 no dona. El programa setmanal també es llegeix i s'escriu per v2.
 * El mapatge a [ThermostatState] és a [TuyaStateMapper].
 */
class TuyaThermostatRepository(
  private val api: TuyaApi,
  private val errors: TuyaErrors,
  private val clock: Clock,
) : ThermostatRepository {
  override suspend fun fetch(): ThermostatState =
    guard {
      val today = LocalDate.now(clock).dayOfWeek
      val todayCode = ThermostatDpMap.weekProgram(today)
      // El tram de matinada surt de l'últim tram d'ahir
      val previousCode = ThermostatDpMap.weekProgram(today.minus(1))
      coroutineScope {
        val status = async { api.status() }
        val properties =
          async {
            api.properties(listOf(ThermostatDpMap.MODE, ThermostatDpMap.WORK_STATE, todayCode, previousCode))
          }
        TuyaStateMapper.map(
          status = status.await(),
          properties = properties.await(),
          todayCode = todayCode,
          previousCode = previousCode,
          now = MinuteOfDay.of(LocalTime.now(clock)),
          lastUpdated = clock.instant(),
        )
      }
    }

  override suspend fun setPower(on: Boolean) = guard { api.sendCommand(ThermostatDpMap.SWITCH, JsonPrimitive(on)) }

  override suspend fun setSetpoint(celsius: Double) =
    guard { api.sendCommand(ThermostatDpMap.TEMP_SET, JsonPrimitive(ThermostatDpMap.toRaw(celsius))) }

  // El mode només es pot escriure per v2: a /commands només s'accepta `auto`
  override suspend fun setMode(mode: Mode) =
    guard {
      val value =
        when (mode) {
          Mode.AUTO -> ThermostatDpMap.MODE_AUTO
          Mode.MANUAL -> ThermostatDpMap.MODE_MANUAL
        }
      api.issueProperties(buildJsonObject { put(ThermostatDpMap.MODE, value) })
    }

  override suspend fun weekProgram(): WeekProgram =
    guard {
      val codes = DayOfWeek.entries.associateWith(ThermostatDpMap::weekProgram)
      val properties = api.properties(codes.values.toList())
      WeekProgram(
        codes.mapValues { (_, code) ->
          properties[code]
            ?.jsonPrimitive
            ?.contentOrNull
            ?.let(WeekProgramDecoder::decode)
            .orEmpty()
        },
      )
    }

  // Com el mode, el programa només es pot escriure per v2. Cada dia és un DP a part: s'escriu només el que ha canviat
  override suspend fun setDayProgram(
    day: DayOfWeek,
    entries: List<ProgramEntry>,
  ) {
    // Fora del guard: un programa mal format és un error de programació i no s'ha d'enviar mai al dispositiu
    val value = WeekProgramEncoder.encode(day.value, entries)
    guard { api.issueProperties(buildJsonObject { put(ThermostatDpMap.weekProgram(day), value) }) }
  }

  private suspend fun <T> guard(block: suspend () -> T): T =
    try {
      block()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      throw errors.translate(e)
    }
}
