package com.alorma.caliu.ui.home

import com.alorma.caliu.designsystem.theme.Mood
import com.alorma.caliu.domain.MinuteOfDay
import com.alorma.caliu.domain.ThermostatState
import com.alorma.caliu.domain.mood
import java.time.LocalTime

sealed interface HomeUiState {
  data object Loading : HomeUiState

  /**
   * L'estat del termòstat. [now] decideix el tram actiu del programa i [updatedAt] és l'hora local de [state]
   * `.lastUpdated`; es calculen fora de la UI perquè les previews i els tests siguin deterministes.
   */
  data class Content(
    val state: ThermostatState,
    val now: MinuteOfDay,
    val updatedAt: LocalTime,
    val refreshing: Boolean = false,
  ) : HomeUiState

  /** [message] ja és el text per a l'usuari. [openConsole] mostra el botó per anar a iot.tuya.com. */
  data class ApiError(
    val message: String,
    val openConsole: Boolean,
  ) : HomeUiState

  data object Offline : HomeUiState

  data object MissingConfig : HomeUiState
}

/** Errors, càrrega i configuració fan servir la paleta neutra. */
val HomeUiState.mood: Mood
  get() = (this as? HomeUiState.Content)?.state?.mood() ?: Mood.OFF
