package com.alorma.caliu.ui.settings

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.caliu.data.settings.CredentialsStore
import com.alorma.caliu.data.settings.ThemeMode
import com.alorma.caliu.data.settings.ThemePreferences
import com.alorma.caliu.data.tuya.ConnectionTester
import com.alorma.caliu.data.tuya.TuyaConfig
import com.alorma.caliu.data.tuya.TuyaRegion
import com.alorma.caliu.domain.ThermostatException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Els camps de text del formulari. Són `TextFieldState` perquè `SecureTextField` només funciona així. */
@Stable
class SettingsForm(
  accessId: String = "",
  accessSecret: String = "",
  deviceId: String = "",
) {
  val accessId = TextFieldState(accessId)
  val accessSecret = TextFieldState(accessSecret)
  val deviceId = TextFieldState(deviceId)

  fun config(region: TuyaRegion): TuyaConfig =
    TuyaConfig(
      accessId = accessId.text.toString().trim(),
      accessSecret = accessSecret.text.toString().trim(),
      region = region,
      deviceId = deviceId.text.toString().trim(),
    )

  fun fill(config: TuyaConfig) {
    accessId.setTextAndPlaceCursorAtEnd(config.accessId)
    accessSecret.setTextAndPlaceCursorAtEnd(config.accessSecret)
    deviceId.setTextAndPlaceCursorAtEnd(config.deviceId)
  }
}

data class SettingsUiState(
  val region: TuyaRegion = TuyaRegion.EU,
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val test: ConnectionTestState = ConnectionTestState.Idle,
  val saving: Boolean = false,
)

/** El resultat de «Provar connexió» (captures `13` i `14`). */
sealed interface ConnectionTestState {
  data object Idle : ConnectionTestState

  data object Testing : ConnectionTestState

  data object Success : ConnectionTestState

  /** [message] és la causa per a l'usuari; si és `null`, no hi ha connexió a internet. */
  data class Failure(
    val message: String?,
  ) : ConnectionTestState
}

sealed interface SettingsEvent {
  /** S'ha desat: es torna a la pantalla principal, que es refresca sola. */
  data object Saved : SettingsEvent
}

/**
 * Configuració (`disseny/docs/01-especificacio.md` § Configuració): carrega les credencials desades, les prova contra
 * Tuya sense desar-les i les desa xifrades.
 */
class SettingsViewModel(
  private val store: CredentialsStore,
  private val tester: ConnectionTester,
  private val themePreferences: ThemePreferences,
) : ViewModel() {
  val form = SettingsForm()

  private val _uiState = MutableStateFlow(SettingsUiState())
  val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

  private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
  val events: Flow<SettingsEvent> = _events.receiveAsFlow()

  private var testJob: Job? = null

  init {
    viewModelScope.launch {
      store.credentials.first()?.let { stored ->
        form.fill(stored)
        _uiState.update { it.copy(region = stored.region) }
      }
    }
    viewModelScope.launch {
      themePreferences.themeMode.collect { mode -> _uiState.update { it.copy(themeMode = mode) } }
    }
  }

  /** El tema s'aplica de seguida, sense esperar «Desa»: no és una credencial. */
  fun setThemeMode(mode: ThemeMode) {
    viewModelScope.launch {
      themePreferences.setThemeMode(mode)
    }
  }

  fun setRegion(region: TuyaRegion) {
    _uiState.update { it.copy(region = region) }
  }

  fun testConnection() {
    val config = form.config(_uiState.value.region)
    if (!config.isComplete) return
    testJob?.cancel()
    _uiState.update { it.copy(test = ConnectionTestState.Testing) }
    testJob =
      viewModelScope.launch {
        val result =
          when (val error = tester.test(config)) {
            null -> ConnectionTestState.Success
            is ThermostatException.Offline -> ConnectionTestState.Failure(message = null)
            else -> ConnectionTestState.Failure(message = error.message)
          }
        _uiState.update { it.copy(test = result) }
      }
  }

  fun save() {
    if (_uiState.value.saving) return
    _uiState.update { it.copy(saving = true) }
    viewModelScope.launch {
      store.save(form.config(_uiState.value.region))
      _events.send(SettingsEvent.Saved)
    }
  }
}
