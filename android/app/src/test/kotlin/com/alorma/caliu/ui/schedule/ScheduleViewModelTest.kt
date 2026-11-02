package com.alorma.caliu.ui.schedule

import com.alorma.caliu.data.fake.FakeThermostatRepository
import com.alorma.caliu.domain.MinuteOfDay
import com.alorma.caliu.domain.ProgramChanges
import com.alorma.caliu.domain.ProgramEntry
import com.alorma.caliu.domain.ThermostatException
import com.alorma.caliu.domain.WeekProgram
import com.alorma.caliu.testing.MainDispatcherRule
import com.alorma.caliu.testing.MutableClock
import java.time.DayOfWeek
import java.time.Instant
import kotlin.time.Duration
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ScheduleViewModelTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  // Dilluns 2026-11-02
  private val clock = MutableClock(Instant.parse("2026-11-02T12:47:00Z"))
  private val fake = FakeThermostatRepository(clock = clock, latency = Duration.ZERO)
  private val programChanges = ProgramChanges()
  private val viewModel by lazy { ScheduleViewModel(fake, clock, programChanges) }

  private fun test(block: suspend TestScope.() -> Unit) =
    runTest(mainDispatcherRule.dispatcher) {
      viewModel.uiState
      advanceUntilIdle()
      block()
    }

  private val editing get() = viewModel.uiState.value as ScheduleUiState.Editing

  private suspend fun TestScope.events(action: () -> Unit): List<ScheduleEvent> {
    val events = mutableListOf<ScheduleEvent>()
    val job = launch { viewModel.events.collect { events += it } }
    advanceUntilIdle()
    action()
    advanceUntilIdle()
    job.cancel()
    return events
  }

  @Test
  fun `obre amb avui seleccionat, els 7 dies i cap canvi`() =
    test {
      assertEquals(DayOfWeek.MONDAY, editing.selectedDay)
      assertEquals(DayOfWeek.MONDAY, editing.today)
      assertEquals(7, editing.days.size)
      assertEquals(FakeThermostatRepository.DefaultDay, editing.entries)
      assertFalse(editing.hasChanges)
    }

  @Test
  fun `−+ canvia la temperatura a passos de 0,5 i marca el dia com a modificat`() =
    test {
      viewModel.stepTemperature(index = 1, steps = 1)
      assertEquals(21.5, editing.entries[1].setpoint, 0.0)
      assertEquals(setOf(DayOfWeek.MONDAY), editing.modifiedDays)

      // Tornar al valor de partida treu la marca
      viewModel.stepTemperature(index = 1, steps = -1)
      assertFalse(editing.hasChanges)
    }

  @Test
  fun `la temperatura no surt de min i max`() =
    test {
      // Els límits per defecte del simulat són 5 i 35
      repeat(100) { viewModel.stepTemperature(index = 0, steps = 1) }
      assertEquals(35.0, editing.entries[0].setpoint, 0.0)
      assertFalse(editing.canIncrease(0))
      repeat(100) { viewModel.stepTemperature(index = 0, steps = -1) }
      assertEquals(5.0, editing.entries[0].setpoint, 0.0)
      assertFalse(editing.canDecrease(0))
    }

  @Test
  fun `l'hora d'un tram ha d'anar entre la de l'anterior i la del següent`() =
    test {
      // Trams: 00:00, 06:30, 08:00, 14:00, 18:00, 22:30
      assertEquals(MinuteOfDay.of(0, 1).value..MinuteOfDay.of(7, 59).value, editing.startRange(1))
      assertEquals(0..MinuteOfDay.of(6, 29).value, editing.startRange(0))
      assertEquals(MinuteOfDay.of(18, 1).value..MinuteOfDay.of(23, 59).value, editing.startRange(5))

      viewModel.setStart(1, MinuteOfDay.of(7))
      assertEquals(MinuteOfDay.of(7), editing.entries[1].start)

      // Fora de l'interval no fa res
      viewModel.setStart(1, MinuteOfDay.of(8))
      viewModel.setStart(1, MinuteOfDay.Midnight)
      assertEquals(MinuteOfDay.of(7), editing.entries[1].start)
    }

  @Test
  fun `el primer tram es pot moure i la matinada surt del dia anterior`() =
    test {
      viewModel.setStart(0, MinuteOfDay.of(5))
      assertEquals(MinuteOfDay.of(5), editing.entries[0].start)
      // De 00:00 a 05:00, l'últim tram de diumenge (22:30, 17,0 °C)
      val first = editing.program.scheduleFor(DayOfWeek.MONDAY).first()
      assertEquals(MinuteOfDay.of(5), first.end)
      assertEquals(17.0, first.setpoint, 0.0)
    }

  @Test
  fun `copiar un dia el posa als dies triats i avisa de quants`() =
    test {
      viewModel.stepTemperature(index = 1, steps = 1)
      val events = events { viewModel.copyTo(setOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY, DayOfWeek.MONDAY)) }
      assertEquals(listOf(ScheduleEvent.Copied(2)), events)
      assertEquals(editing.entries, editing.days[DayOfWeek.TUESDAY])
      assertEquals(editing.entries, editing.days[DayOfWeek.FRIDAY])
      assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.FRIDAY), editing.modifiedDays)
    }

  @Test
  fun `desar envia només els dies modificats i torna enrere`() =
    test {
      viewModel.stepTemperature(index = 1, steps = 1)
      viewModel.copyTo(setOf(DayOfWeek.WEDNESDAY))
      val saved = mutableListOf<Unit>()
      val notified = launch { programChanges.saved.first().also { saved += it } }
      val events = events { viewModel.save() }
      // Abans hi ha el Copied, que s'ha quedat al canal
      assertEquals(ScheduleEvent.Saved, events.last())
      assertFalse(events.any { it is ScheduleEvent.SaveFailed })
      assertFalse(editing.hasChanges)
      assertFalse(editing.saving)
      assertEquals(editing.entries, fake.weekProgram()[DayOfWeek.WEDNESDAY])
      assertEquals(FakeThermostatRepository.DefaultDay, fake.weekProgram()[DayOfWeek.TUESDAY])
      assertEquals(1, saved.size)
      notified.cancel()
    }

  @Test
  fun `si un dia falla, la resta es desa i el que falla queda marcat`() =
    test {
      fake.failingDays = setOf(DayOfWeek.THURSDAY)
      viewModel.stepTemperature(index = 2, steps = -1)
      viewModel.copyTo(setOf(DayOfWeek.THURSDAY, DayOfWeek.SATURDAY))
      val events = events { viewModel.save() }
      assertEquals(ScheduleEvent.SaveFailed(DayOfWeek.THURSDAY), events.last())
      assertFalse(ScheduleEvent.Saved in events)
      assertEquals(setOf(DayOfWeek.THURSDAY), editing.modifiedDays)
      assertEquals(editing.entries, fake.weekProgram()[DayOfWeek.SATURDAY])
      assertEquals(FakeThermostatRepository.DefaultDay, fake.weekProgram()[DayOfWeek.THURSDAY])
    }

  @Test
  fun `sense canvis, desar només torna enrere`() =
    test {
      assertEquals(listOf(ScheduleEvent.Saved), events { viewModel.save() })
    }

  @Test
  fun `seleccionar un altre dia no canvia res`() =
    test {
      viewModel.selectDay(DayOfWeek.SUNDAY)
      assertEquals(DayOfWeek.SUNDAY, editing.selectedDay)
      assertFalse(editing.hasChanges)
    }

  @Test
  fun `sense connexió mostra l'error i es pot tornar a provar`() =
    runTest(mainDispatcherRule.dispatcher) {
      fake.failure = ThermostatException.Offline()
      viewModel.uiState
      advanceUntilIdle()
      assertEquals(ScheduleUiState.Error(message = null), viewModel.uiState.value)

      fake.failure = null
      viewModel.load()
      advanceUntilIdle()
      assertTrue(viewModel.uiState.value is ScheduleUiState.Editing)
    }

  @Test
  fun `si algun dia no té 6 trams no es deixa editar`() =
    runTest(mainDispatcherRule.dispatcher) {
      val week =
        WeekProgram(
          DayOfWeek.entries.associateWith {
            if (it ==
              DayOfWeek.FRIDAY
            ) {
              listOf(ProgramEntry(MinuteOfDay.of(7), 21.0))
            } else {
              FakeThermostatRepository.DefaultDay
            }
          },
        )
      val viewModel = ScheduleViewModel(FakeThermostatRepository(clock, Duration.ZERO, program = week), clock)
      advanceUntilIdle()
      assertEquals(ScheduleUiState.Unreadable, viewModel.uiState.value)
    }
}
