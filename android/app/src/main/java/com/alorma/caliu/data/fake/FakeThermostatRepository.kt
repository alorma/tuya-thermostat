package com.alorma.caliu.data.fake

import com.alorma.caliu.domain.MinuteOfDay
import com.alorma.caliu.domain.Mode
import com.alorma.caliu.domain.Override
import com.alorma.caliu.domain.ScheduleSlot
import com.alorma.caliu.domain.ThermostatException
import com.alorma.caliu.domain.ThermostatRepository
import com.alorma.caliu.domain.ThermostatState
import com.alorma.caliu.domain.activeSlotIndex
import java.time.Clock
import java.time.LocalTime
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Termòstat simulat en memòria. No fa cap crida de xarxa, així que es pot provar tota l'app sense tocar la calefacció.
 *
 * Es comporta com el dispositiu real: en automàtic segueix [schedule], tocar la consigna en automàtic crea un canvi
 * temporal fins al final del tram, i el canvi temporal s'acaba sol quan comença el tram següent. A cada [fetch] la
 * temperatura s'acosta una mica a la consigna, perquè es vegi passar d'«Escalfant» a «En repòs».
 *
 * [failure], si no és nul, fa fallar totes les crides: serveix per veure els estats d'error.
 */
class FakeThermostatRepository(
  private val clock: Clock = Clock.systemDefaultZone(),
  private val latency: Duration = DEFAULT_LATENCY,
  private val schedule: List<ScheduleSlot> = DefaultSchedule,
  initialTemp: Double = 19.3,
) : ThermostatRepository {
  @Volatile
  var failure: ThermostatException? = null

  private val mutex = Mutex()
  private var power = true
  private var mode = Mode.AUTO
  private var manualSetpoint = 21.0
  private var currentTemp = initialTemp

  // Canvi temporal i l'inici del tram on es va fer, per saber quan caduca
  private var override: Pair<MinuteOfDay, Double>? = null

  override suspend fun fetch(): ThermostatState =
    call {
      val active = activeSlot()
      expireOverride(active)
      val target = target(active)
      val heating = power && currentTemp < target
      currentTemp = simulateTemp(heating)
      ThermostatState(
        power = power,
        paused = false,
        mode = mode,
        currentTemp = currentTemp.takeIf { power },
        setpoint = target,
        heating = heating,
        override = override?.let { (_, setpoint) -> active?.let { Override(until = it.end, setpoint = setpoint) } },
        schedule = schedule,
        battery = BATTERY,
        lastUpdated = clock.instant(),
      )
    }

  override suspend fun setPower(on: Boolean) = call { power = on }

  override suspend fun setSetpoint(celsius: Double) =
    call {
      when (mode) {
        Mode.MANUAL -> manualSetpoint = celsius
        Mode.AUTO -> override = activeSlot()?.let { it.start to celsius }
      }
    }

  override suspend fun setMode(mode: Mode) =
    call {
      this.mode = mode
      override = null
    }

  private suspend fun <T> call(block: () -> T): T {
    delay(latency)
    failure?.let { throw it }
    return mutex.withLock { block() }
  }

  private fun activeSlot(): ScheduleSlot? {
    val now = MinuteOfDay.of(LocalTime.now(clock))
    return schedule.activeSlotIndex(now)?.let(schedule::get)
  }

  private fun expireOverride(active: ScheduleSlot?) {
    if (mode != Mode.AUTO || override?.first != active?.start) override = null
  }

  private fun target(active: ScheduleSlot?): Double =
    when (mode) {
      Mode.MANUAL -> manualSetpoint
      Mode.AUTO -> override?.second ?: active?.setpoint ?: manualSetpoint
    }

  // Escalfa 0,1 °C per lectura; si no escalfa, es refreda la meitat de ràpid fins a la temperatura ambient
  private fun simulateTemp(heating: Boolean): Double {
    val next =
      when {
        heating -> currentTemp + HEAT_STEP
        currentTemp > AMBIENT -> currentTemp - COOL_STEP
        else -> currentTemp
      }
    return (next * 100).roundToInt() / 100.0
  }

  companion object {
    val DEFAULT_LATENCY = 700.milliseconds
    private const val BATTERY = 87
    private const val HEAT_STEP = 0.1
    private const val COOL_STEP = 0.05
    private const val AMBIENT = 17.0

    /** El programa de les captures del handoff. */
    val DefaultSchedule: List<ScheduleSlot> =
      listOf(
        ScheduleSlot(MinuteOfDay.of(0), MinuteOfDay.of(6, 30), 17.0),
        ScheduleSlot(MinuteOfDay.of(6, 30), MinuteOfDay.of(8), 21.0),
        ScheduleSlot(MinuteOfDay.of(8), MinuteOfDay.of(14), 20.0),
        ScheduleSlot(MinuteOfDay.of(14), MinuteOfDay.of(18), 19.0),
        ScheduleSlot(MinuteOfDay.of(18), MinuteOfDay.of(22, 30), 21.0),
        ScheduleSlot(MinuteOfDay.of(22, 30), MinuteOfDay.EndOfDay, 17.0),
      )
  }
}
