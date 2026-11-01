package com.alorma.caliu.data.tuya

import com.alorma.caliu.domain.MinuteOfDay
import com.alorma.caliu.domain.Mode
import com.alorma.caliu.domain.ThermostatRepository
import com.alorma.caliu.domain.ThermostatState
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * El termòstat real per Tuya Cloud. Ajunta l'estat bàsic de v1 (`/status`) amb el mode, `work_state` i el programa
 * d'avui de v2 (`shadow/properties`), que v1 no dona.
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
      val programCode = ThermostatDpMap.weekProgram(today)
      coroutineScope {
        val status = async { api.status() }
        val properties =
          async { api.properties(listOf(ThermostatDpMap.MODE, ThermostatDpMap.WORK_STATE, programCode)) }
        TuyaStateMapper.map(
          status = status.await(),
          properties = properties.await(),
          programCode = programCode,
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

  private suspend fun <T> guard(block: suspend () -> T): T =
    try {
      block()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      throw errors.translate(e)
    }
}
