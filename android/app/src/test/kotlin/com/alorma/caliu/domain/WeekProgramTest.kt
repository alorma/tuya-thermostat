package com.alorma.caliu.domain

import com.alorma.caliu.data.fake.FakeThermostatRepository
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeekProgramTest {
  private val weekday = FakeThermostatRepository.DefaultDay
  private val weekend = FakeThermostatRepository.WeekendDay
  private val week = FakeThermostatRepository.DebugWeek

  @Test
  fun `si el primer tram és a les 00 00 no s'hi afegeix res`() {
    assertEquals(FakeThermostatRepository.DefaultSchedule, daySchedule(today = weekday, previous = weekend))
    assertEquals(6, FakeThermostatRepository.DefaultSchedule.size)
  }

  @Test
  fun `la matinada pren l'últim tram del dia anterior, no el d'avui`() {
    // Dissabte: el primer tram és a les 08:30; de 00:00 a 08:30 mana l'últim de divendres (22:30, 17,0 °C), no
    // l'últim de dissabte (23:30, 18,0 °C)
    val saturday = week.scheduleFor(DayOfWeek.SATURDAY)
    assertEquals(ScheduleSlot(MinuteOfDay.Midnight, MinuteOfDay.of(8, 30), 17.0), saturday.first())
    assertEquals(ScheduleSlot(MinuteOfDay.of(23, 30), MinuteOfDay.EndOfDay, 18.0), saturday.last())
    assertEquals(7, saturday.size)

    // Diumenge: la matinada continua el dissabte a la nit
    assertEquals(18.0, week.scheduleFor(DayOfWeek.SUNDAY).first().setpoint, 0.0)
  }

  @Test
  fun `dilluns agafa la matinada del diumenge`() {
    val program =
      WeekProgram(
        DayOfWeek.entries.associateWith {
          if (it == DayOfWeek.SUNDAY) weekend else weekend.map { e -> e.copy(setpoint = 20.0) }
        },
      )
    assertEquals(18.0, program.scheduleFor(DayOfWeek.MONDAY).first().setpoint, 0.0)
  }

  @Test
  fun `els trams van seguits de 00 00 a 24 00`() {
    DayOfWeek.entries.forEach { day ->
      val slots = week.scheduleFor(day)
      assertEquals(MinuteOfDay.Midnight, slots.first().start)
      assertEquals(MinuteOfDay.EndOfDay, slots.last().end)
      slots.zipWithNext().forEach { (a, b) -> assertEquals(a.end, b.start) }
    }
  }

  @Test
  fun `sense dia anterior, la matinada pren l'últim tram del mateix dia`() {
    assertEquals(18.0, daySchedule(today = weekend, previous = emptyList()).first().setpoint, 0.0)
  }

  @Test
  fun `un dia sense trams no té programa`() {
    assertTrue(daySchedule(today = emptyList(), previous = weekday).isEmpty())
  }
}
