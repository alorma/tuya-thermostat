package com.alorma.caliu.ui.home

import com.alorma.caliu.data.fake.FakeThermostatRepository
import com.alorma.caliu.domain.MinuteOfDay
import com.alorma.caliu.domain.Mode
import com.alorma.caliu.domain.ThermostatController
import com.alorma.caliu.domain.ThermostatException
import com.alorma.caliu.testing.InMemoryPauseStore
import com.alorma.caliu.testing.MainDispatcherRule
import com.alorma.caliu.testing.MutableClock
import com.alorma.caliu.testing.RecordingRepository
import java.time.Instant
import kotlin.time.Duration
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  // 12:47: tram de 08:00 a 14:00 a 20,0 °C; 19,3 °C a casa, així que escalfa
  private val clock = MutableClock(Instant.parse("2026-11-02T12:47:00Z"))
  private val fake = FakeThermostatRepository(clock = clock, latency = Duration.ZERO, initialTemp = 19.3)
  private val repository = RecordingRepository(fake)

  // Lazy: el ViewModel s'ha de crear quan MainDispatcherRule ja ha posat el dispatcher principal
  private val viewModel by lazy { HomeViewModel(ThermostatController(repository, InMemoryPauseStore()), clock) }

  private fun test(block: suspend TestScope.() -> Unit) =
    runTest(mainDispatcherRule.dispatcher) {
      viewModel.refresh()
      advanceUntilIdle()
      block()
    }

  private val content get() = viewModel.uiState.value as HomeUiState.Content

  @Test
  fun `carrega l'estat del termòstat`() =
    test {
      assertEquals(Mode.AUTO, content.state.mode)
      assertEquals(20.0, content.state.setpoint, 0.0)
      assertTrue(content.state.heating)
      assertEquals(MinuteOfDay.of(12, 47), content.now)
      assertFalse(content.refreshing)
    }

  @Test
  fun `els tocs ràpids de + s'envien una sola vegada després de 800 ms`() =
    test {
      viewModel.increase()
      viewModel.increase()
      viewModel.increase()
      runCurrent()

      // La UI ja ho mostra, però encara no s'ha enviat res
      assertEquals(21.5, content.state.setpoint, 0.0)
      assertEquals(MinuteOfDay.of(14), content.state.override?.until)
      assertTrue(repository.commands.isEmpty())

      advanceTimeBy(HomeViewModel.SETPOINT_DEBOUNCE.inWholeMilliseconds + 1)
      assertEquals(listOf("setpoint=21.5"), repository.commands)
    }

  @Test
  fun `un refresc mentre hi ha tocs pendents no trepitja la consigna`() =
    test {
      viewModel.increase()
      viewModel.refresh()
      runCurrent()

      assertEquals(20.5, content.state.setpoint, 0.0)
    }

  @Test
  fun `no es passa del màxim`() =
    test {
      viewModel.setMode(Mode.MANUAL)
      advanceUntilIdle()
      viewModel.setPreset(35.0)
      advanceUntilIdle()
      repository.commands.clear()

      viewModel.increase()
      advanceUntilIdle()

      assertEquals(35.0, content.state.setpoint, 0.0)
      assertTrue(repository.commands.isEmpty())
    }

  @Test
  fun `si una ordre falla, es reverteix i s'avisa`() =
    test {
      fake.failure = ThermostatException.Offline()

      viewModel.turnOff()
      runCurrent()
      advanceUntilIdle()

      assertTrue(content.state.power)
      assertEquals(HomeEvent.CommandFailed, viewModel.events.first())
    }

  @Test
  fun `pausar i activar`() =
    test {
      viewModel.pause()
      runCurrent()
      assertTrue(content.state.paused)
      advanceUntilIdle()
      assertTrue(content.state.paused)
      assertEquals(Mode.AUTO, content.state.mode)

      viewModel.activate()
      advanceUntilIdle()
      assertFalse(content.state.paused)
      assertEquals(listOf("mode=MANUAL", "setpoint=18.0", "mode=AUTO"), repository.commands)
    }

  @Test
  fun `tornar al programa descarta el canvi temporal`() =
    test {
      viewModel.increase()
      advanceUntilIdle()
      assertEquals(20.5, content.state.override?.setpoint)

      viewModel.backToSchedule()
      runCurrent()
      assertNull(content.state.override)
      assertEquals(20.0, content.state.setpoint, 0.0)
      advanceUntilIdle()
      assertEquals(listOf("setpoint=20.5", "mode=AUTO"), repository.commands)
    }

  @Test
  fun `apagat no es pot canviar la consigna`() =
    test {
      viewModel.turnOff()
      advanceUntilIdle()
      repository.commands.clear()

      viewModel.increase()
      viewModel.setPreset(21.0)
      advanceUntilIdle()

      assertTrue(repository.commands.isEmpty())
    }

  @Test
  fun `tornar-ho a provar des d'un error passa per la càrrega`() =
    runTest(mainDispatcherRule.dispatcher) {
      fake.failure = ThermostatException.Offline()
      viewModel.refresh()
      advanceUntilIdle()
      assertEquals(HomeUiState.Offline, viewModel.uiState.value)

      fake.failure = null
      viewModel.refresh()
      assertEquals(HomeUiState.Loading, viewModel.uiState.value)
      advanceUntilIdle()
      assertTrue(viewModel.uiState.value is HomeUiState.Content)
    }

  @Test
  fun `quan es desen credencials noves es torna a llegir, encara que hi hagués un error`() =
    runTest(mainDispatcherRule.dispatcher) {
      val saved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
      fake.failure = ThermostatException.MissingConfig()
      val viewModel =
        HomeViewModel(ThermostatController(repository, InMemoryPauseStore()), clock, configSaved = saved)
      viewModel.refresh()
      advanceUntilIdle()
      assertEquals(HomeUiState.MissingConfig, viewModel.uiState.value)

      fake.failure = null
      saved.emit(Unit)
      advanceUntilIdle()

      assertTrue(viewModel.uiState.value is HomeUiState.Content)
    }
}
