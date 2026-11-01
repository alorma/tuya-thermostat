package com.alorma.caliu.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.caliu.domain.MinuteOfDay
import com.alorma.caliu.domain.Mode
import com.alorma.caliu.domain.Override
import com.alorma.caliu.domain.ThermostatController
import com.alorma.caliu.domain.ThermostatException
import com.alorma.caliu.domain.ThermostatState
import com.alorma.caliu.domain.activeSlotIndex
import com.alorma.caliu.domain.targetAt
import java.time.Clock
import java.time.LocalTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Esdeveniments puntuals per a la UI. */
sealed interface HomeEvent {
  /** Una ordre ha fallat i s'ha revertit: Snackbar «No s'ha pogut aplicar el canvi». */
  data object CommandFailed : HomeEvent
}

/**
 * Pantalla principal (`disseny/docs/01-especificacio.md` § Ordres i sincronització):
 * - refresc cada 60 s amb l'app en primer pla ([refreshPeriodically]) i en estirar ([refresh]);
 * - ordres optimistes: la UI canvia a l'instant i, si l'ordre falla, es reverteix i s'emet [HomeEvent.CommandFailed];
 * - −/+ s'agrupen 800 ms abans d'enviar-se, sempre dins `[min, max]`;
 * - mentre hi ha ordres pendents, els refrescos no trepitgen l'estat optimista; quan acaben, es torna a llegir al cap
 *   de 2 s per agafar el que hagi canviat el dispositiu (escalfa o no, el mode temporal…).
 */
class HomeViewModel(
  private val controller: ThermostatController,
  private val clock: Clock,
  private val setpointDebounce: Duration = SETPOINT_DEBOUNCE,
  private val syncDelay: Duration = SYNC_DELAY,
) : ViewModel() {
  private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
  val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

  private val _events = Channel<HomeEvent>(Channel.BUFFERED)
  val events: Flow<HomeEvent> = _events.receiveAsFlow()

  // Les ordres van al dispositiu d'una en una i en ordre
  private val commandMutex = Mutex()
  private var commandsInFlight = 0
  private var loadJob: Job? = null
  private var syncJob: Job? = null

  // −/+: l'estat d'abans de la ràfega de tocs (per revertir) i l'enviament diferit
  private var setpointBase: ThermostatState? = null
  private var setpointJob: Job? = null

  private val busy: Boolean get() = commandsInFlight > 0 || setpointBase != null

  /** Refresca ara i després cada 60 s. Es crida amb l'app en primer pla i es cancel·la en sortir-ne. */
  suspend fun refreshPeriodically() {
    while (true) {
      load(pull = false)
      delay(REFRESH_INTERVAL)
    }
  }

  /** Pull-to-refresh i «Torna-ho a provar». */
  fun refresh() = load(pull = true)

  fun activate() =
    command(
      optimistic = { it.copy(power = true, paused = false) },
      action = { controller.activate(it) },
    )

  fun pause() =
    command(
      optimistic = { it.copy(power = true, paused = true, heating = false) },
      action = { controller.pause(it) },
    )

  fun turnOff() =
    command(
      optimistic = { it.copy(power = false, paused = false, heating = false) },
      action = { controller.turnOff() },
    )

  fun setMode(mode: Mode) {
    val state = content()?.state ?: return
    if (!state.power || state.mode == mode) return
    command(
      optimistic = {
        if (it.paused) {
          it.copy(mode = mode)
        } else {
          val switched = it.copy(mode = mode, override = null)
          switched.copy(setpoint = switched.targetAt(now()))
        }
      },
      action = { controller.setMode(it, mode) },
    )
  }

  /** «Tornar al programa»: descarta el canvi temporal. */
  fun backToSchedule() {
    val state = content()?.state ?: return
    if (state.override == null) return
    command(
      optimistic = {
        val back = it.copy(override = null)
        back.copy(setpoint = back.targetAt(now()))
      },
      action = { controller.setMode(it, Mode.AUTO) },
    )
  }

  /** Temperatures ràpides: s'envien de seguida. */
  fun setPreset(celsius: Double) {
    val state = content()?.state ?: return
    if (!state.power || state.paused) return
    val target = celsius.coerceIn(state.minSetpoint, state.maxSetpoint)
    command(
      optimistic = { withTarget(it, target) },
      action = { controller.setSetpoint(target) },
    )
  }

  fun increase() = step(+ThermostatState.SETPOINT_STEP)

  fun decrease() = step(-ThermostatState.SETPOINT_STEP)

  private fun step(delta: Double) {
    val content = content() ?: return
    val state = content.state
    if (!state.power || state.paused) return
    val current = state.targetAt(content.now)
    val target = (current + delta).coerceIn(state.minSetpoint, state.maxSetpoint)
    if (target == current) return

    if (setpointBase == null) setpointBase = state
    _uiState.value = content.copy(state = withTarget(state, target))

    setpointJob?.cancel()
    setpointJob =
      viewModelScope.launch {
        delay(setpointDebounce)
        val before = setpointBase ?: return@launch
        setpointBase = null
        send(before) { controller.setSetpoint(target) }
      }
  }

  /** En manual canvia la consigna; en automàtic crea (o mou) el canvi temporal fins al final del tram. */
  private fun withTarget(
    state: ThermostatState,
    target: Double,
  ): ThermostatState =
    when (state.mode) {
      Mode.MANUAL -> state.copy(setpoint = target)
      Mode.AUTO -> {
        val now = now()
        val until = state.schedule.activeSlotIndex(now)?.let { state.schedule[it].end } ?: MinuteOfDay.EndOfDay
        state.copy(setpoint = target, override = Override(until = until, setpoint = target))
      }
    }

  private fun command(
    optimistic: (ThermostatState) -> ThermostatState,
    action: suspend (ThermostatState) -> Unit,
  ) {
    val content = content() ?: return
    val before = content.state
    _uiState.value = content.copy(state = optimistic(before))
    viewModelScope.launch { send(before) { action(before) } }
  }

  private suspend fun send(
    before: ThermostatState,
    action: suspend () -> Unit,
  ) {
    commandsInFlight++
    try {
      commandMutex.withLock { action() }
      scheduleSync()
    } catch (e: ThermostatException) {
      (_uiState.value as? HomeUiState.Content)?.let { _uiState.value = it.copy(state = before) }
      _events.send(HomeEvent.CommandFailed)
    } finally {
      commandsInFlight--
    }
  }

  private fun scheduleSync() {
    syncJob?.cancel()
    syncJob =
      viewModelScope.launch {
        delay(syncDelay)
        load(pull = false)
      }
  }

  private fun load(pull: Boolean) {
    if (loadJob?.isActive == true) return
    when (val current = _uiState.value) {
      is HomeUiState.Content -> if (pull) _uiState.value = current.copy(refreshing = true)
      HomeUiState.Loading -> Unit
      // Torna-ho a provar des d'un error: es torna a l'skeleton
      else -> if (pull) _uiState.value = HomeUiState.Loading
    }
    loadJob =
      viewModelScope.launch {
        val result =
          try {
            val state = controller.state()
            HomeUiState.Content(
              state = state,
              now = now(),
              updatedAt = LocalTime.ofInstant(state.lastUpdated, clock.zone),
            )
          } catch (e: ThermostatException) {
            e.toUiState()
          }
        val current = _uiState.value
        _uiState.value =
          when {
            // Amb ordres pendents no es trepitja l'estat optimista; ja es tornarà a llegir quan acabin
            busy && current is HomeUiState.Content -> current.copy(refreshing = false, now = now())
            else -> result
          }
      }
  }

  private fun content(): HomeUiState.Content? = _uiState.value as? HomeUiState.Content

  private fun now(): MinuteOfDay = MinuteOfDay.of(LocalTime.now(clock))

  companion object {
    val REFRESH_INTERVAL = 60.seconds
    val SETPOINT_DEBOUNCE = 800.milliseconds
    val SYNC_DELAY = 2.seconds
  }
}

private fun ThermostatException.toUiState(): HomeUiState =
  when (this) {
    is ThermostatException.Api -> HomeUiState.ApiError(message, openConsole)
    is ThermostatException.Offline -> HomeUiState.Offline
    is ThermostatException.MissingConfig -> HomeUiState.MissingConfig
  }
