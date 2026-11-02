package com.alorma.caliu.ui.settings

import com.alorma.caliu.data.settings.CredentialsStore
import com.alorma.caliu.data.settings.ThemeMode
import com.alorma.caliu.data.settings.ThemePreferences
import com.alorma.caliu.data.tuya.ConnectionTester
import com.alorma.caliu.data.tuya.TuyaConfig
import com.alorma.caliu.data.tuya.TuyaRegion
import com.alorma.caliu.domain.ThermostatException
import com.alorma.caliu.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  private val stored = TuyaConfig("id", "secret", TuyaRegion.CN, "device")
  private val store = InMemoryCredentialsStore(stored)
  private var testResult: ThermostatException? = null
  private val tested = mutableListOf<TuyaConfig>()
  private var gate: CompletableDeferred<Unit>? = null
  private val tester =
    ConnectionTester { config ->
      tested += config
      gate?.await()
      testResult
    }

  private val themePreferences = InMemoryThemePreferences()
  private var themeChanges = 0

  private fun viewModel() = SettingsViewModel(store, tester, themePreferences, onThemeChanged = { themeChanges++ })

  @Test
  fun `el tema per defecte és el del sistema i es desa en triar-lo`() =
    runTest(mainDispatcherRule.dispatcher) {
      val viewModel = viewModel()
      advanceUntilIdle()
      assertEquals(ThemeMode.SYSTEM, viewModel.uiState.value.themeMode)

      viewModel.setThemeMode(ThemeMode.LIGHT)
      advanceUntilIdle()

      assertEquals(ThemeMode.LIGHT, themePreferences.themeMode.first())
      assertEquals(ThemeMode.LIGHT, viewModel.uiState.value.themeMode)
      assertEquals(1, themeChanges)
    }

  @Test
  fun `omple el formulari amb el que hi ha desat`() =
    runTest(mainDispatcherRule.dispatcher) {
      val viewModel = viewModel()
      advanceUntilIdle()

      assertEquals(stored, viewModel.form.config(viewModel.uiState.value.region))
    }

  @Test
  fun `prova les credencials del formulari sense desar-les`() =
    runTest(mainDispatcherRule.dispatcher) {
      val viewModel = viewModel()
      advanceUntilIdle()
      viewModel.setRegion(TuyaRegion.EU)
      gate = CompletableDeferred()

      viewModel.testConnection()
      advanceUntilIdle()
      assertEquals(ConnectionTestState.Testing, viewModel.uiState.value.test)

      gate?.complete(Unit)
      advanceUntilIdle()
      assertEquals(ConnectionTestState.Success, viewModel.uiState.value.test)
      assertEquals(listOf(stored.copy(region = TuyaRegion.EU)), tested)
      assertEquals(stored, store.state.value)
    }

  @Test
  fun `un error de Tuya es mostra amb la causa`() =
    runTest(mainDispatcherRule.dispatcher) {
      testResult = ThermostatException.Api("Credencials no vàlides")
      val viewModel = viewModel()
      advanceUntilIdle()

      viewModel.testConnection()
      advanceUntilIdle()

      assertEquals(ConnectionTestState.Failure("Credencials no vàlides"), viewModel.uiState.value.test)
    }

  @Test
  fun `sense internet la causa és nul·la`() =
    runTest(mainDispatcherRule.dispatcher) {
      testResult = ThermostatException.Offline()
      val viewModel = viewModel()
      advanceUntilIdle()

      viewModel.testConnection()
      advanceUntilIdle()

      assertEquals(ConnectionTestState.Failure(null), viewModel.uiState.value.test)
    }

  @Test
  fun `amb credencials incompletes no es prova res`() =
    runTest(mainDispatcherRule.dispatcher) {
      val viewModel = viewModel()
      advanceUntilIdle()
      viewModel.form.accessSecret.edit { replace(0, length, "") }

      viewModel.testConnection()
      advanceUntilIdle()

      assertEquals(ConnectionTestState.Idle, viewModel.uiState.value.test)
      assertEquals(emptyList<TuyaConfig>(), tested)
    }

  @Test
  fun `desa sense espais i avisa per tornar enrere`() =
    runTest(mainDispatcherRule.dispatcher) {
      store.state.value = null
      val viewModel = viewModel()
      advanceUntilIdle()
      viewModel.form.accessId.edit { append("  new-id ") }
      viewModel.form.accessSecret.edit { append("new-secret") }
      viewModel.form.deviceId.edit { append("new-device\n") }
      viewModel.setRegion(TuyaRegion.IN)

      viewModel.save()
      advanceUntilIdle()

      assertEquals(TuyaConfig("new-id", "new-secret", TuyaRegion.IN, "new-device"), store.state.value)
      assertEquals(SettingsEvent.Saved, viewModel.events.first())
    }

  @Test
  fun `sense res desat el formulari queda buit i en EU`() =
    runTest(mainDispatcherRule.dispatcher) {
      store.state.value = null
      val viewModel = viewModel()
      advanceUntilIdle()

      assertEquals(TuyaRegion.EU, viewModel.uiState.value.region)
      assertNull(viewModel.form.config(TuyaRegion.EU).takeIf { it.isComplete })
    }
}

private class InMemoryCredentialsStore(
  initial: TuyaConfig?,
) : CredentialsStore {
  val state = MutableStateFlow(initial)
  private val _saved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
  override val credentials: Flow<TuyaConfig?> = state
  override val saved: Flow<Unit> = _saved

  override suspend fun save(config: TuyaConfig) {
    state.value = config
    _saved.emit(Unit)
  }
}

private class InMemoryThemePreferences : ThemePreferences {
  override val themeMode = MutableStateFlow(ThemeMode.SYSTEM)

  override suspend fun setThemeMode(mode: ThemeMode) {
    themeMode.value = mode
  }
}
