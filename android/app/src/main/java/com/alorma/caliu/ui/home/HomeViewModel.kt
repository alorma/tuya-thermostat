package com.alorma.caliu.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.caliu.domain.MinuteOfDay
import com.alorma.caliu.domain.ThermostatException
import com.alorma.caliu.domain.ThermostatRepository
import java.time.Clock
import java.time.LocalTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Pantalla principal. De moment només llegeix l'estat (en entrar, en estirar per refrescar i en tornar-ho a provar);
 * les ordres, el refresc periòdic i la pausa arriben a la fase 4.
 */
class HomeViewModel(
  private val repository: ThermostatRepository,
  private val clock: Clock,
) : ViewModel() {
  private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
  val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

  private var loadJob: Job? = null

  init {
    load()
  }

  fun refresh() = load()

  private fun load() {
    if (loadJob?.isActive == true) return
    _uiState.update { if (it is HomeUiState.Content) it.copy(refreshing = true) else HomeUiState.Loading }
    loadJob =
      viewModelScope.launch {
        _uiState.value =
          try {
            val state = repository.fetch()
            HomeUiState.Content(
              state = state,
              now = MinuteOfDay.of(LocalTime.now(clock)),
              updatedAt = LocalTime.ofInstant(state.lastUpdated, clock.zone),
            )
          } catch (e: ThermostatException) {
            e.toUiState()
          }
      }
  }
}

private fun ThermostatException.toUiState(): HomeUiState =
  when (this) {
    is ThermostatException.Api -> HomeUiState.ApiError(message, openConsole)
    is ThermostatException.Offline -> HomeUiState.Offline
    is ThermostatException.MissingConfig -> HomeUiState.MissingConfig
  }
