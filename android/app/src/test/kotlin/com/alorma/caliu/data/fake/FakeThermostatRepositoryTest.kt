package com.alorma.caliu.data.fake

import com.alorma.caliu.domain.MinuteOfDay
import com.alorma.caliu.domain.Mode
import com.alorma.caliu.domain.ThermostatException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.time.Duration
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeThermostatRepositoryTest {
  private var now = Instant.parse("2026-11-02T12:47:00Z")
  private val clock =
    object : Clock() {
      override fun getZone() = ZoneOffset.UTC

      override fun withZone(zone: java.time.ZoneId?) = this

      override fun instant() = now
    }

  private fun repository(initialTemp: Double = 19.3) =
    FakeThermostatRepository(clock = clock, latency = Duration.ZERO, initialTemp = initialTemp)

  @Test
  fun `en automàtic segueix el tram actiu`() =
    runTest {
      val state = repository().fetch()
      assertEquals(Mode.AUTO, state.mode)
      assertEquals(20.0, state.setpoint, 0.0)
      assertTrue(state.heating)
      assertFalse(state.paused)
    }

  @Test
  fun `canviar la consigna en automàtic crea un canvi temporal fins al final del tram`() =
    runTest {
      val repository = repository()
      repository.setSetpoint(22.0)
      val state = repository.fetch()
      assertEquals(22.0, state.setpoint, 0.0)
      assertEquals(MinuteOfDay.of(14), state.override?.until)
    }

  @Test
  fun `el canvi temporal caduca quan comença el tram següent`() =
    runTest {
      val repository = repository()
      repository.setSetpoint(22.0)
      now = Instant.parse("2026-11-02T14:00:00Z")
      val state = repository.fetch()
      assertNull(state.override)
      assertEquals(19.0, state.setpoint, 0.0)
    }

  @Test
  fun `tornar a automàtic descarta el canvi temporal`() =
    runTest {
      val repository = repository()
      repository.setSetpoint(22.0)
      repository.setMode(Mode.AUTO)
      assertNull(repository.fetch().override)
    }

  @Test
  fun `en manual fa servir la consigna manual`() =
    runTest {
      val repository = repository(initialTemp = 22.0)
      repository.setMode(Mode.MANUAL)
      repository.setSetpoint(21.5)
      val state = repository.fetch()
      assertEquals(21.5, state.setpoint, 0.0)
      assertFalse(state.heating)
      assertNull(state.override)
    }

  @Test
  fun `apagat no dona temperatura ni escalfa`() =
    runTest {
      val repository = repository()
      repository.setPower(false)
      val state = repository.fetch()
      assertFalse(state.power)
      assertNull(state.currentTemp)
      assertFalse(state.heating)
    }

  @Test
  fun `escalfant la temperatura puja a cada lectura`() =
    runTest {
      val repository = repository()
      val first = repository.fetch().currentTemp!!
      val second = repository.fetch().currentTemp!!
      assertTrue(second > first)
    }

  @Test(expected = ThermostatException.Offline::class)
  fun `amb una fallada configurada totes les crides fallen`() =
    runTest {
      val repository = repository()
      repository.failure = ThermostatException.Offline()
      repository.fetch()
    }
}
