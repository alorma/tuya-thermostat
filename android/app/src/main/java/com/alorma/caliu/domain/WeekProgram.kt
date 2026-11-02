package com.alorma.caliu.domain

import java.time.DayOfWeek

/** Un tram del programa tal com el desa el dispositiu: comença a [start] i dura fins al tram següent. */
data class ProgramEntry(
  val start: MinuteOfDay,
  val setpoint: Double,
) {
  init {
    require(start.value < MinuteOfDay.MINUTES_PER_DAY) { "Un tram no pot començar a les 24:00" }
  }
}

/**
 * El programa setmanal: els trams de cada dia, en l'ordre del dispositiu (`week_program_13_1` … `_7`). El dispositiu
 * desa cada dia per separat i sempre amb [SLOTS_PER_DAY] trams.
 *
 * Un dia no té cap tram fix a les 00:00: de mitjanit al primer tram mana l'últim tram del dia anterior.
 */
data class WeekProgram(
  val days: Map<DayOfWeek, List<ProgramEntry>>,
) {
  operator fun get(day: DayOfWeek): List<ProgramEntry> = days[day].orEmpty()

  /** Els trams que manen durant [day], de 00:00 a 24:00. */
  fun scheduleFor(day: DayOfWeek): List<ScheduleSlot> = daySchedule(today = get(day), previous = get(day.minus(1)))

  companion object {
    const val SLOTS_PER_DAY = 6
  }
}

/**
 * Els trams d'un dia de 00:00 a 24:00, sense forats: cada tram dura fins a l'inici del següent i l'últim fins a les
 * 24:00. Si el primer no comença a les 00:00, s'hi afegeix el tram de matinada amb la consigna de l'últim tram de
 * [previous] (el que continua del dia anterior) o, si el dia anterior no en té, de l'últim de [today].
 */
fun daySchedule(
  today: List<ProgramEntry>,
  previous: List<ProgramEntry>,
): List<ScheduleSlot> {
  val entries = today.sortedBy { it.start.value }.distinctBy { it.start }
  if (entries.isEmpty()) return emptyList()
  val overnight = previous.maxByOrNull { it.start.value } ?: entries.last()
  val withMidnight =
    if (entries.first().start == MinuteOfDay.Midnight) {
      entries
    } else {
      listOf(ProgramEntry(MinuteOfDay.Midnight, overnight.setpoint)) + entries
    }
  return withMidnight.mapIndexed { index, entry ->
    val end = withMidnight.getOrNull(index + 1)?.start ?: MinuteOfDay.EndOfDay
    ScheduleSlot(entry.start, end, entry.setpoint)
  }
}
