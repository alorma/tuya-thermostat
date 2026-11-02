package com.alorma.caliu.data.tuya

import com.alorma.caliu.data.fake.FakeThermostatRepository
import com.alorma.caliu.domain.MinuteOfDay
import com.alorma.caliu.domain.ProgramEntry
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Test

class WeekProgramEncoderTest {
  private val real = "AQYPANwNAADNDwAA3BEAAOYVAADcFy0AzQ=="

  @Test
  fun `torna a codificar exactament el valor real del termòstat`() {
    assertEquals(real, WeekProgramEncoder.encode(1, WeekProgramDecoder.decode(real)))
  }

  @Test
  fun `el primer byte és el dia`() {
    assertEquals(
      "BwYPANwNAADNDwAA3BEAAOYVAADcFy0AzQ==",
      WeekProgramEncoder.encode(7, WeekProgramDecoder.decode(real)),
    )
  }

  @Test
  fun `anada i tornada amb el programa de les captures`() {
    val encoded = WeekProgramEncoder.encode(3, FakeThermostatRepository.DefaultDay)
    assertEquals(FakeThermostatRepository.DefaultDay, WeekProgramDecoder.decode(encoded))
  }

  @Test
  fun `sempre fa 25 bytes, sense byte de recompte`() {
    listOf(FakeThermostatRepository.DefaultDay, FakeThermostatRepository.WeekendDay).forEach { entries ->
      val bytes = Base64.getDecoder().decode(WeekProgramEncoder.encode(1, entries))
      assertEquals(WeekProgramEncoder.SIZE, bytes.size)
      // El segon byte ja és l'hora del primer tram
      assertEquals(entries.first().start.hour, bytes[1].toInt())
    }
  }

  @Test
  fun `les temperatures van ×10 en uint16 big-endian`() {
    val entries = FakeThermostatRepository.DefaultDay.mapIndexed { i, e -> if (i == 0) e.copy(setpoint = 30.0) else e }
    val bytes = Base64.getDecoder().decode(WeekProgramEncoder.encode(1, entries))
    // 300 = 0x012C
    assertEquals(0x01, bytes[3].toInt())
    assertEquals(0x2C, bytes[4].toInt())
  }

  @Test(expected = IllegalArgumentException::class)
  fun `no codifica menys de 6 trams`() {
    WeekProgramEncoder.encode(1, FakeThermostatRepository.DefaultDay.take(5))
  }

  @Test(expected = IllegalArgumentException::class)
  fun `no codifica trams desordenats`() {
    WeekProgramEncoder.encode(1, FakeThermostatRepository.DefaultDay.reversed())
  }

  @Test(expected = IllegalArgumentException::class)
  fun `no codifica dos trams a la mateixa hora`() {
    val entries = FakeThermostatRepository.DefaultDay.toMutableList()
    entries[2] = ProgramEntry(MinuteOfDay.of(6, 30), 20.0)
    WeekProgramEncoder.encode(1, entries)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `el dia ha de ser de l'1 al 7`() {
    WeekProgramEncoder.encode(0, FakeThermostatRepository.DefaultDay)
  }
}
