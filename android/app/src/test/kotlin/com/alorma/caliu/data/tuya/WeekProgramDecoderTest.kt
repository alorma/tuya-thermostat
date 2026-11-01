package com.alorma.caliu.data.tuya

import com.alorma.caliu.data.fake.FakeThermostatRepository
import com.alorma.caliu.domain.MinuteOfDay
import com.alorma.caliu.domain.ScheduleSlot
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeekProgramDecoderTest {
  @Test
  fun `decodifica el programa real del termòstat`() {
    // week_program_13_1 llegit del dispositiu el 2026-11-01: 6 trams i el primer a les 06:15
    val slots = WeekProgramDecoder.decode("AQYGDwDcDQAAzQ8AANwRAADmFQAA3BctAM0=")
    assertEquals(
      listOf(
        ScheduleSlot(MinuteOfDay.Midnight, MinuteOfDay.of(6, 15), 20.5),
        ScheduleSlot(MinuteOfDay.of(6, 15), MinuteOfDay.of(13), 22.0),
        ScheduleSlot(MinuteOfDay.of(13), MinuteOfDay.of(15), 20.5),
        ScheduleSlot(MinuteOfDay.of(15), MinuteOfDay.of(17), 22.0),
        ScheduleSlot(MinuteOfDay.of(17), MinuteOfDay.of(21), 23.0),
        ScheduleSlot(MinuteOfDay.of(21), MinuteOfDay.of(23, 45), 22.0),
        ScheduleSlot(MinuteOfDay.of(23, 45), MinuteOfDay.EndOfDay, 20.5),
      ),
      slots,
    )
  }

  @Test
  fun `el primer byte és el dia de la setmana`() {
    // Els 7 dies del dispositiu tenen els mateixos trams i només canvia el primer byte
    assertEquals(
      WeekProgramDecoder.decode("AQYGDwDcDQAAzQ8AANwRAADmFQAA3BctAM0="),
      WeekProgramDecoder.decode("BwYGDwDcDQAAzQ8AANwRAADmFQAA3BctAM0="),
    )
  }

  @Test
  fun `decodifica els 6 trams del programa de les captures`() {
    // [1, 6, (0:00 17,0) (6:30 21,0) (8:00 20,0) (14:00 19,0) (18:00 21,0) (22:30 17,0)]
    val slots = WeekProgramDecoder.decode("AQYAAACqBh4A0ggAAMgOAAC+EgAA0hYeAKo=")
    assertEquals(FakeThermostatRepository.DefaultSchedule, slots)
  }

  @Test
  fun `si no comença a mitjanit, el primer tram continua el de l'últim`() {
    // [1, 2, (6:30 21,0) (22:30 17,0)]
    val slots = WeekProgramDecoder.decode("AQIGHgDSFh4Aqg==")
    assertEquals(
      listOf(
        ScheduleSlot(MinuteOfDay.Midnight, MinuteOfDay.of(6, 30), 17.0),
        ScheduleSlot(MinuteOfDay.of(6, 30), MinuteOfDay.of(22, 30), 21.0),
        ScheduleSlot(MinuteOfDay.of(22, 30), MinuteOfDay.EndOfDay, 17.0),
      ),
      slots,
    )
  }

  @Test
  fun `ordena els trams per hora`() {
    // [1, 2, (22:30 17,0) (6:30 21,0)]
    assertEquals(WeekProgramDecoder.decode("AQIGHgDSFh4Aqg=="), WeekProgramDecoder.decode("AQIWHgCqBh4A0g=="))
  }

  @Test
  fun `un valor buit, truncat o que no és base64 dona un programa buit o parcial`() {
    assertTrue(WeekProgramDecoder.decode("").isEmpty())
    assertTrue(WeekProgramDecoder.decode("no és base64!").isEmpty())
    // Diu 6 trams però només en porta 1
    assertEquals(1, WeekProgramDecoder.decode("AQYAAACq").size)
  }

  @Test
  fun `el programa de cada dia té el seu codi, de dilluns (1) a diumenge (7)`() {
    assertEquals("week_program_13_1", ThermostatDpMap.weekProgram(DayOfWeek.MONDAY))
    assertEquals("week_program_13_7", ThermostatDpMap.weekProgram(DayOfWeek.SUNDAY))
  }
}
