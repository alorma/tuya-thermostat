package com.alorma.caliu.domain

import com.alorma.caliu.data.fake.FakeThermostatRepository
import com.alorma.caliu.testing.InMemoryPauseStore
import com.alorma.caliu.testing.MutableClock
import com.alorma.caliu.testing.RecordingRepository
import java.time.Instant
import kotlin.time.Duration
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThermostatControllerTest {
  // 12:47: tram de 08:00 a 14:00 a 20,0 °C
  private val clock = MutableClock(Instant.parse("2026-11-02T12:47:00Z"))
  private val fake = FakeThermostatRepository(clock = clock, latency = Duration.ZERO, initialTemp = 19.3)
  private val repository = RecordingRepository(fake)
  private val pauseStore = InMemoryPauseStore()
  private val controller = ThermostatController(repository, pauseStore)

  @Test
  fun `la consigna de la pausa és mig grau o més per sota, en passos de 0,5 i mai sota el mínim`() {
    assertEquals(18.0, ThermostatController.pausedSetpoint(19.3, 5.0), 0.0)
    assertEquals(19.0, ThermostatController.pausedSetpoint(20.4, 5.0), 0.0)
    assertEquals(19.0, ThermostatController.pausedSetpoint(20.0, 5.0), 0.0)
    assertEquals(5.0, ThermostatController.pausedSetpoint(5.4, 5.0), 0.0)
  }

  @Test
  fun `pausar en automàtic passa a manual amb la consigna baixa i la UI veu el mode desat`() =
    runTest {
      controller.pause(controller.state())

      assertEquals(listOf("mode=MANUAL", "setpoint=18.0"), repository.commands)
      val state = controller.state()
      assertTrue(state.paused)
      assertEquals(Mode.AUTO, state.mode)
      assertEquals(20.0, state.setpoint, 0.0)
    }

  @Test
  fun `activar des d'automàtic torna al programa`() =
    runTest {
      controller.pause(controller.state())
      repository.commands.clear()

      controller.activate(controller.state())

      assertEquals(listOf("mode=AUTO"), repository.commands)
      assertNull(pauseStore.pause)
      assertFalse(controller.state().paused)
    }

  @Test
  fun `activar des de manual restaura la consigna`() =
    runTest {
      fake.setMode(Mode.MANUAL)
      fake.setSetpoint(21.5)
      controller.pause(controller.state())
      repository.commands.clear()

      controller.activate(controller.state())

      assertEquals(listOf("setpoint=21.5"), repository.commands)
      assertEquals(21.5, controller.state().setpoint, 0.0)
    }

  @Test
  fun `apagar en pausa primer en surt`() =
    runTest {
      controller.pause(controller.state())
      repository.commands.clear()

      controller.turnOff()

      assertEquals(listOf("mode=AUTO", "power=false"), repository.commands)
      assertNull(pauseStore.pause)
    }

  @Test
  fun `pausar apagat l'encén i llegeix la temperatura`() =
    runTest {
      fake.setPower(false)

      controller.pause(controller.state())

      assertEquals(listOf("power=true", "mode=MANUAL", "setpoint=18.0"), repository.commands)
      assertTrue(controller.state().paused)
    }

  @Test
  fun `si algú canvia la consigna des de fora, la pausa s'abandona`() =
    runTest {
      controller.pause(controller.state())
      fake.setSetpoint(22.0)

      val state = controller.state()

      assertFalse(state.paused)
      assertNull(pauseStore.pause)
      assertEquals(Mode.MANUAL, state.mode)
    }

  @Test
  fun `canviar el mode en pausa només canvia el mode desat`() =
    runTest {
      controller.pause(controller.state())
      repository.commands.clear()

      controller.setMode(controller.state(), Mode.MANUAL)

      assertTrue(repository.commands.isEmpty())
      assertEquals(Mode.MANUAL, pauseStore.pause?.mode)
    }

  @Test
  fun `si la pausa falla al dispositiu, no queda desada`() =
    runTest {
      val state = controller.state()
      fake.failure = ThermostatException.Offline()

      runCatching { controller.pause(state) }

      assertNull(pauseStore.pause)
    }

  @Test
  fun `la rajola alterna la pausa`() =
    runTest {
      controller.togglePause()
      assertTrue(controller.state().paused)
      controller.togglePause()
      assertFalse(controller.state().paused)
    }
}
