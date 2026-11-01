package com.alorma.caliu.domain

import com.alorma.caliu.data.fake.FakeThermostatRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScheduleTest {
  private val schedule = FakeThermostatRepository.DefaultSchedule

  @Test
  fun `el tram actiu inclou l'inici i exclou el final`() {
    assertEquals(0, schedule.activeSlotIndex(MinuteOfDay.Midnight))
    assertEquals(1, schedule.activeSlotIndex(MinuteOfDay.of(6, 30)))
    assertEquals(2, schedule.activeSlotIndex(MinuteOfDay.of(12, 47)))
    assertEquals(2, schedule.activeSlotIndex(MinuteOfDay.of(13, 59)))
    assertEquals(3, schedule.activeSlotIndex(MinuteOfDay.of(14)))
    assertEquals(5, schedule.activeSlotIndex(MinuteOfDay.of(23, 59)))
  }

  @Test
  fun `sense programa no hi ha tram actiu`() {
    assertNull(emptyList<ScheduleSlot>().activeSlotIndex(MinuteOfDay.of(12)))
  }

  @Test
  fun `les hores es formaten amb dues xifres i el final del dia és 24 00`() {
    assertEquals("06:30", MinuteOfDay.of(6, 30).toString())
    assertEquals("24:00", MinuteOfDay.EndOfDay.toString())
  }

  @Test(expected = IllegalArgumentException::class)
  fun `no hi ha minuts fora del dia`() {
    MinuteOfDay(MinuteOfDay.MINUTES_PER_DAY + 1)
  }
}
