package com.alorma.caliu.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.caliu.domain.MinuteOfDay
import com.alorma.caliu.domain.ProgramChanges
import com.alorma.caliu.domain.ProgramEntry
import com.alorma.caliu.domain.ThermostatException
import com.alorma.caliu.domain.ThermostatRepository
import com.alorma.caliu.domain.ThermostatState
import com.alorma.caliu.domain.WeekProgram
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ScheduleUiState {
  data object Loading : ScheduleUiState

  /** No s'ha pogut llegir el programa. [message] és la causa; `null` vol dir sense connexió. */
  data class Error(
    val message: String?,
  ) : ScheduleUiState

  /** Algun dia no té els 6 trams del format: no se sap què s'escriuria, així que no es deixa editar. */
  data object Unreadable : ScheduleUiState

  /**
   * La còpia de treball dels 7 dies ([days]) i el que hi ha al dispositiu ([saved]). Un dia és modificat si són
   * diferents; en desar-lo bé, [saved] passa a ser igual que [days].
   */
  data class Editing(
    val days: Map<DayOfWeek, List<ProgramEntry>>,
    val saved: Map<DayOfWeek, List<ProgramEntry>>,
    val selectedDay: DayOfWeek,
    val today: DayOfWeek,
    val minSetpoint: Double,
    val maxSetpoint: Double,
    val saving: Boolean = false,
  ) : ScheduleUiState {
    val modifiedDays: Set<DayOfWeek> get() = DayOfWeek.entries.filterTo(mutableSetOf()) { days[it] != saved[it] }
    val hasChanges: Boolean get() = modifiedDays.isNotEmpty()
    val entries: List<ProgramEntry> get() = days.getValue(selectedDay)

    /** La setmana tal com s'està editant, per pintar la barra de 24 h (amb el tram de matinada del dia anterior). */
    val program: WeekProgram get() = WeekProgram(days)

    /** Les hores que pot tenir el tram [index]: entre la del tram anterior i la del següent, sense tocar-les. */
    fun startRange(index: Int): IntRange {
      val first = entries.getOrNull(index - 1)?.let { it.start.value + 1 } ?: 0
      val last = entries.getOrNull(index + 1)?.let { it.start.value - 1 } ?: (MinuteOfDay.MINUTES_PER_DAY - 1)
      return first..last
    }

    fun canDecrease(index: Int): Boolean = entries[index].setpoint > minSetpoint

    fun canIncrease(index: Int): Boolean = entries[index].setpoint < maxSetpoint
  }
}

sealed interface ScheduleEvent {
  data class Copied(
    val count: Int,
  ) : ScheduleEvent

  /** S'han desat tots els dies modificats: es torna a la pantalla principal. */
  data object Saved : ScheduleEvent

  /** No s'ha pogut desar [day]: es queda marcat com a modificat. */
  data class SaveFailed(
    val day: DayOfWeek,
  ) : ScheduleEvent
}

/**
 * Programa setmanal (`disseny/docs/01-especificacio.md` § Programa setmanal): edita una còpia dels 7 dies i, en desar,
 * envia només els dies modificats, una ordre per dia.
 *
 * Escriu el programa del termòstat real: cada dia que s'envia té sempre 6 trams en ordre d'hora, i l'encoder ho torna
 * a comprovar abans d'enviar res.
 */
class ScheduleViewModel(
  private val repository: ThermostatRepository,
  private val clock: Clock,
  private val programChanges: ProgramChanges = ProgramChanges(),
) : ViewModel() {
  private val _uiState = MutableStateFlow<ScheduleUiState>(ScheduleUiState.Loading)
  val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

  private val _events = Channel<ScheduleEvent>(Channel.BUFFERED)
  val events: Flow<ScheduleEvent> = _events.receiveAsFlow()

  init {
    load()
  }

  fun load() {
    _uiState.value = ScheduleUiState.Loading
    viewModelScope.launch {
      _uiState.value =
        try {
          coroutineScope {
            val program = async { repository.weekProgram() }
            val state = async { repository.fetch() }
            editing(program.await(), state.await())
          }
        } catch (e: CancellationException) {
          throw e
        } catch (e: ThermostatException.Offline) {
          ScheduleUiState.Error(message = null)
        } catch (e: ThermostatException) {
          ScheduleUiState.Error(message = e.message.orEmpty())
        }
    }
  }

  private fun editing(
    program: WeekProgram,
    state: ThermostatState,
  ): ScheduleUiState {
    val days = DayOfWeek.entries.associateWith { program[it] }
    // El dispositiu sempre en té 6; si no, no se sap què s'escriuria i millor no tocar res
    if (days.values.any { it.size != WeekProgram.SLOTS_PER_DAY }) return ScheduleUiState.Unreadable
    val today = LocalDate.now(clock).dayOfWeek
    return ScheduleUiState.Editing(
      days = days,
      saved = days,
      selectedDay = today,
      today = today,
      minSetpoint = state.minSetpoint,
      maxSetpoint = state.maxSetpoint,
    )
  }

  fun selectDay(day: DayOfWeek) {
    _uiState.update { (it as? ScheduleUiState.Editing)?.copy(selectedDay = day) ?: it }
  }

  /** −/+ de la temperatura del tram [index] del dia seleccionat, a passos de 0,5 °C dins `[min, max]`. */
  fun stepTemperature(
    index: Int,
    steps: Int,
  ) = edit {
    val entry = entries[index]
    val setpoint = (entry.setpoint + steps * ThermostatState.SETPOINT_STEP).coerceIn(minSetpoint, maxSetpoint)
    withEntries(entries.toMutableList().apply { set(index, entry.copy(setpoint = setpoint)) })
  }

  /** Canvia l'hora d'inici del tram [index]. Fora de l'interval que permeten els trams veïns no fa res. */
  fun setStart(
    index: Int,
    start: MinuteOfDay,
  ) = edit {
    if (start.value !in startRange(index)) return@edit this
    withEntries(entries.toMutableList().apply { set(index, get(index).copy(start = start)) })
  }

  /** Copia el dia seleccionat a [targets]. */
  fun copyTo(targets: Set<DayOfWeek>) {
    val state = _uiState.value as? ScheduleUiState.Editing ?: return
    val destinations = targets - state.selectedDay
    if (destinations.isEmpty() || state.saving) return
    _uiState.value = state.copy(days = state.days + destinations.associateWith { state.entries })
    _events.trySend(ScheduleEvent.Copied(destinations.size))
  }

  /** Desa els dies modificats, d'un en un i de dilluns a diumenge. El que falla es queda marcat com a modificat. */
  fun save() {
    val state = _uiState.value as? ScheduleUiState.Editing ?: return
    if (state.saving) return
    if (!state.hasChanges) {
      _events.trySend(ScheduleEvent.Saved)
      return
    }
    _uiState.value = state.copy(saving = true)
    viewModelScope.launch {
      val failed = mutableListOf<DayOfWeek>()
      var savedAny = false
      for (day in DayOfWeek.entries.filter { it in state.modifiedDays }) {
        val entries = state.days.getValue(day)
        try {
          repository.setDayProgram(day, entries)
          savedAny = true
          _uiState.update { (it as? ScheduleUiState.Editing)?.copy(saved = it.saved + (day to entries)) ?: it }
        } catch (e: CancellationException) {
          throw e
        } catch (e: ThermostatException) {
          failed += day
        }
      }
      _uiState.update { (it as? ScheduleUiState.Editing)?.copy(saving = false) ?: it }
      if (savedAny) programChanges.notifySaved()
      if (failed.isEmpty()) {
        _events.send(ScheduleEvent.Saved)
      } else {
        failed.forEach { _events.send(ScheduleEvent.SaveFailed(it)) }
      }
    }
  }

  private fun ScheduleUiState.Editing.withEntries(entries: List<ProgramEntry>) =
    copy(days = days + (selectedDay to entries))

  // Mentre es desa, la còpia de treball no es toca: el que s'envia ha de ser el que es veu
  private inline fun edit(block: ScheduleUiState.Editing.() -> ScheduleUiState.Editing) {
    _uiState.update { state ->
      if (state is ScheduleUiState.Editing && !state.saving) state.block() else state
    }
  }
}
