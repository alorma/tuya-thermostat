package com.alorma.caliu.data.fake

import com.alorma.caliu.data.tuya.WeekProgramEncoder
import com.alorma.caliu.domain.MinuteOfDay
import com.alorma.caliu.domain.Mode
import com.alorma.caliu.domain.Override
import com.alorma.caliu.domain.ProgramEntry
import com.alorma.caliu.domain.ScheduleSlot
import com.alorma.caliu.domain.ThermostatException
import com.alorma.caliu.domain.ThermostatRepository
import com.alorma.caliu.domain.ThermostatState
import com.alorma.caliu.domain.WeekProgram
import com.alorma.caliu.domain.activeSlotIndex
import com.alorma.caliu.domain.daySchedule
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
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
 * Es comporta com el dispositiu real: en automàtic segueix el programa setmanal (de 00:00 al primer tram, l'últim tram
 * del dia anterior), tocar la consigna en automàtic crea un canvi
 * temporal fins al final del tram, i el canvi temporal s'acaba sol quan comença el tram següent. A cada [fetch] la
 * temperatura s'acosta una mica a la consigna, perquè es vegi passar d'«Escalfant» a «En repòs».
 *
 * [failure], si no és nul, fa fallar totes les crides: serveix per veure els estats d'error.
 */
class FakeThermostatRepository(
  private val clock: Clock = Clock.systemDefaultZone(),
  private val latency: Duration = DEFAULT_LATENCY,
  program: WeekProgram = DefaultWeek,
  initialTemp: Double = 19.3,
) : ThermostatRepository {
  @Volatile
  var failure: ThermostatException? = null

  /** Dies on [setDayProgram] falla, per veure l'error de desar un dia concret. */
  @Volatile
  var failingDays: Set<DayOfWeek> = emptySet()

  private var program = program

  private val mutex = Mutex()
  private var power = true
  private var mode = Mode.AUTO
  private var manualSetpoint = 21.0
  private var currentTemp = initialTemp

  // Canvi temporal i l'inici del tram on es va fer, per saber quan caduca
  private var override: Pair<MinuteOfDay, Double>? = null

  override suspend fun fetch(): ThermostatState =
    call {
      val schedule = schedule()
      val active = activeSlot(schedule)
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
        Mode.AUTO -> override = activeSlot(schedule())?.let { it.start to celsius }
      }
    }

  override suspend fun setMode(mode: Mode) =
    call {
      this.mode = mode
      override = null
    }

  override suspend fun weekProgram(): WeekProgram = call { program }

  override suspend fun setDayProgram(
    day: DayOfWeek,
    entries: List<ProgramEntry>,
  ) {
    // Com el real: un programa mal format no arriba mai al dispositiu
    WeekProgramEncoder.encode(day.value, entries)
    call {
      if (day in failingDays) throw ThermostatException.Api("El termòstat ha rebutjat el programa")
      program = WeekProgram(program.days + (day to entries))
    }
  }

  private suspend fun <T> call(block: () -> T): T {
    delay(latency)
    failure?.let { throw it }
    return mutex.withLock { block() }
  }

  private fun schedule(): List<ScheduleSlot> = program.scheduleFor(LocalDate.now(clock).dayOfWeek)

  private fun activeSlot(schedule: List<ScheduleSlot>): ScheduleSlot? {
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

    /** Els 6 trams del programa de les captures del handoff. */
    val DefaultDay: List<ProgramEntry> =
      listOf(
        ProgramEntry(MinuteOfDay.of(0), 17.0),
        ProgramEntry(MinuteOfDay.of(6, 30), 21.0),
        ProgramEntry(MinuteOfDay.of(8), 20.0),
        ProgramEntry(MinuteOfDay.of(14), 19.0),
        ProgramEntry(MinuteOfDay.of(18), 21.0),
        ProgramEntry(MinuteOfDay.of(22, 30), 17.0),
      )

    /** El cap de setmana es lleva més tard i va a dormir més tard, perquè es noti el tram de matinada del dilluns. */
    val WeekendDay: List<ProgramEntry> =
      listOf(
        ProgramEntry(MinuteOfDay.of(8, 30), 21.0),
        ProgramEntry(MinuteOfDay.of(11), 20.5),
        ProgramEntry(MinuteOfDay.of(14), 20.0),
        ProgramEntry(MinuteOfDay.of(17), 21.0),
        ProgramEntry(MinuteOfDay.of(20), 21.5),
        ProgramEntry(MinuteOfDay.of(23, 30), 18.0),
      )

    /** El programa de les captures: els 7 dies iguals. */
    val DefaultWeek: WeekProgram = WeekProgram(DayOfWeek.entries.associateWith { DefaultDay })

    /** El programa de l'app de debug: feiners com les captures i el cap de setmana diferent. */
    val DebugWeek: WeekProgram =
      WeekProgram(
        DayOfWeek.entries.associateWith {
          if (it == DayOfWeek.SATURDAY || it == DayOfWeek.SUNDAY) WeekendDay else DefaultDay
        },
      )

    /** El programa d'avui de les captures, de 00:00 a 24:00. */
    val DefaultSchedule: List<ScheduleSlot> = daySchedule(today = DefaultDay, previous = DefaultDay)
  }
}
