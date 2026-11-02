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
    // week_program_13_1 escrit al dispositiu el 2026-11-02 i validat a l'app Smart Life: 6 trams, el primer a les 06:15
    val slots = WeekProgramDecoder.decode("AQYPANwNAADNDwAA3BEAAOYVAADcFy0AzQ==")
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
  fun `no hi ha byte de recompte després del dia`() {
    // Valor corrupte que vam escriure el 2026-11-01 amb un byte de recompte (06) de més: l'app Smart Life el mostrava
    // com 06:06 | 384,0 °C. Amb el format correcte, el primer tram surt a les 06:06.
    val slots = WeekProgramDecoder.decode("AQYGDwDcDQAAzQ8AANwRAADmFQAA3BctAM0=")
    assertEquals(MinuteOfDay.of(6, 6), slots.first { it.start != MinuteOfDay.Midnight }.start)
  }

  @Test
  fun `el primer byte és el dia de la setmana`() {
    // Els 7 dies del dispositiu tenen els mateixos trams i només canvia el primer byte
    assertEquals(
      WeekProgramDecoder.decode("AQYPANwNAADNDwAA3BEAAOYVAADcFy0AzQ=="),
      WeekProgramDecoder.decode("BwYPANwNAADNDwAA3BEAAOYVAADcFy0AzQ=="),
    )
  }

  @Test
  fun `decodifica els 6 trams del programa de les captures`() {
    // [1, (0:00 17,0) (6:30 21,0) (8:00 20,0) (14:00 19,0) (18:00 21,0) (22:30 17,0)]
    val slots = WeekProgramDecoder.decode("AQAAAKoGHgDSCAAAyA4AAL4SAADSFh4Aqg==")
    assertEquals(FakeThermostatRepository.DefaultSchedule, slots)
  }

  @Test
  fun `si no comença a mitjanit, el primer tram continua el de l'últim`() {
    // [1, (6:30 21,0) (22:30 17,0)]
    val slots = WeekProgramDecoder.decode("AQYeANIWHgCq")
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
    // [1, (22:30 17,0) (6:30 21,0)]
    assertEquals(WeekProgramDecoder.decode("AQYeANIWHgCq"), WeekProgramDecoder.decode("ARYeAKoGHgDS"))
  }

  @Test
  fun `un valor buit, truncat o que no és base64 dona un programa buit o parcial`() {
    assertTrue(WeekProgramDecoder.decode("").isEmpty())
    assertTrue(WeekProgramDecoder.decode("no és base64!").isEmpty())
    // Un tram sencer (0:00 17,0) i un de truncat a mig camí: només compta el sencer
    assertEquals(1, WeekProgramDecoder.decode("AQAAAKoGHg==").size)
  }

  @Test
  fun `el programa de cada dia té el seu codi, de dilluns (1) a diumenge (7)`() {
    assertEquals("week_program_13_1", ThermostatDpMap.weekProgram(DayOfWeek.MONDAY))
    assertEquals("week_program_13_7", ThermostatDpMap.weekProgram(DayOfWeek.SUNDAY))
  }
}
