package com.alorma.caliu.ui.settings

import android.app.StatusBarManager
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.caliu.R
import com.alorma.caliu.data.settings.ThemeMode
import com.alorma.caliu.data.tuya.TuyaRegion
import com.alorma.caliu.designsystem.R as DsR
import com.alorma.caliu.designsystem.components.CaliuLargeTopBar
import com.alorma.caliu.designsystem.components.ChoiceSelector
import com.alorma.caliu.designsystem.components.ConnectionResultCard
import com.alorma.caliu.designsystem.components.ConnectionTestButton
import com.alorma.caliu.designsystem.components.FormCard
import com.alorma.caliu.designsystem.components.FormCardPosition
import com.alorma.caliu.designsystem.components.FormGroup
import com.alorma.caliu.designsystem.components.FormSecretField
import com.alorma.caliu.designsystem.components.FormTextField
import com.alorma.caliu.designsystem.theme.CaliuSpacing
import com.alorma.caliu.designsystem.theme.CaliuTheme
import com.alorma.caliu.designsystem.theme.CaliuType
import com.alorma.caliu.designsystem.theme.Mood
import com.alorma.caliu.tile.ThermostatTileService
import org.koin.compose.viewmodel.koinViewModel

/** Accions de Configuració. */
@Immutable
data class SettingsActions(
  val onBack: () -> Unit = {},
  val onSave: () -> Unit = {},
  val onRegionChange: (TuyaRegion) -> Unit = {},
  val onThemeModeChange: (ThemeMode) -> Unit = {},
  val onTestConnection: () -> Unit = {},
  // Nul·la si el sistema no permet demanar-ho (abans d'Android 13): llavors no es mostra el botó
  val onAddTile: (() -> Unit)? = null,
)

@Composable
fun SettingsScreen(
  onBack: () -> Unit,
  onSaved: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: SettingsViewModel = koinViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  // Les credencials de Tuya no són un compte: en sortir, que Android no ofereixi desar-les al gestor de contrasenyes
  val autofillManager = LocalAutofillManager.current
  DisposableEffect(autofillManager) { onDispose { autofillManager?.cancel() } }
  LaunchedEffect(viewModel) {
    viewModel.events.collect { event ->
      when (event) {
        SettingsEvent.Saved -> onSaved()
      }
    }
  }
  SettingsContent(
    form = viewModel.form,
    uiState = uiState,
    actions =
      SettingsActions(
        onBack = onBack,
        onSave = viewModel::save,
        onRegionChange = viewModel::setRegion,
        onThemeModeChange = viewModel::setThemeMode,
        onTestConnection = viewModel::testConnection,
        onAddTile = rememberAddTileAction(),
      ),
    modifier = modifier,
  )
}

/** Configuració sense estat, per a les previews i els tests de captures. Sempre amb la paleta neutra. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
  form: SettingsForm,
  uiState: SettingsUiState,
  actions: SettingsActions,
  modifier: Modifier = Modifier,
  initialSecretVisible: Boolean = false,
) {
  CaliuTheme(mood = Mood.OFF) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
      modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
      containerColor = MaterialTheme.colorScheme.background,
      topBar = {
        CaliuLargeTopBar(
          title = stringResource(R.string.config_title),
          backLabel = stringResource(R.string.config_back),
          onBack = actions.onBack,
          actionLabel = stringResource(R.string.config_save),
          onAction = actions.onSave,
          actionEnabled = !uiState.saving,
          scrollBehavior = scrollBehavior,
        )
      },
    ) { padding ->
      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .padding(padding)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = CaliuSpacing.screen)
            .padding(bottom = CaliuSpacing.screen)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(CaliuSpacing.l),
      ) {
        CredentialsForm(form, uiState, actions, initialSecretVisible)
        ConnectionTest(form, uiState, actions)
        ThemeCard(uiState, actions)
        actions.onAddTile?.let { onAddTile ->
          TextButton(onClick = onAddTile, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(R.string.config_add_tile), style = CaliuType.button)
          }
        }
      }
    }
  }
}

@Composable
private fun ThemeCard(
  uiState: SettingsUiState,
  actions: SettingsActions,
) {
  FormCard(label = stringResource(R.string.config_theme), position = FormCardPosition.Single) {
    ChoiceSelector(
      options = ThemeOptions.map { stringResource(it.second) },
      selectedIndex = ThemeOptions.indexOfFirst { it.first == uiState.themeMode },
      onSelect = { actions.onThemeModeChange(ThemeOptions[it].first) },
    )
  }
}

private val ThemeOptions =
  listOf(
    ThemeMode.LIGHT to R.string.theme_light,
    ThemeMode.DARK to R.string.theme_dark,
    ThemeMode.SYSTEM to R.string.theme_system,
  )

/** Demana al sistema que afegeixi la rajola al panell de configuració ràpida (Android 13+). */
@Composable
private fun rememberAddTileAction(): (() -> Unit)? {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
  val context = LocalContext.current
  val label = stringResource(R.string.tile_label)
  return remember(context, label) {
    {
      context.getSystemService(StatusBarManager::class.java)?.requestAddTileService(
        ComponentName(context, ThermostatTileService::class.java),
        label,
        Icon.createWithResource(context, DsR.drawable.ic_local_fire_department_filled),
        context.mainExecutor,
      ) { }
    }
  }
}

@Composable
private fun CredentialsForm(
  form: SettingsForm,
  uiState: SettingsUiState,
  actions: SettingsActions,
  initialSecretVisible: Boolean,
) {
  FormGroup {
    FormTextField(
      state = form.accessId,
      label = stringResource(R.string.config_access_id),
      position = FormCardPosition.First,
    )
    FormSecretField(
      state = form.accessSecret,
      label = stringResource(R.string.config_access_secret),
      showLabel = stringResource(R.string.config_show_secret),
      hideLabel = stringResource(R.string.config_hide_secret),
      position = FormCardPosition.Middle,
      initiallyVisible = initialSecretVisible,
    )
    FormCard(label = stringResource(R.string.config_region), position = FormCardPosition.Middle) {
      ChoiceSelector(
        options = TuyaRegion.entries.map { it.name },
        selectedIndex = uiState.region.ordinal,
        onSelect = { actions.onRegionChange(TuyaRegion.entries[it]) },
      )
    }
    FormTextField(
      state = form.deviceId,
      label = stringResource(R.string.config_device_id),
      position = FormCardPosition.Last,
      imeAction = ImeAction.Done,
    )
  }
}

@Composable
private fun ConnectionTest(
  form: SettingsForm,
  uiState: SettingsUiState,
  actions: SettingsActions,
) {
  val test = uiState.test
  val testing = test == ConnectionTestState.Testing
  Column(verticalArrangement = Arrangement.spacedBy(CaliuSpacing.section)) {
    ConnectionTestButton(
      text = stringResource(if (testing) R.string.config_testing else R.string.config_test),
      testing = testing,
      onClick = actions.onTestConnection,
      // Sense totes les credencials no hi ha res a provar
      enabled = form.config(uiState.region).isComplete,
    )
    when (test) {
      ConnectionTestState.Success ->
        ConnectionResultCard(success = true, title = stringResource(R.string.config_test_ok))
      is ConnectionTestState.Failure ->
        ConnectionResultCard(
          success = false,
          title = stringResource(R.string.config_test_error),
          detail = test.message ?: stringResource(R.string.error_offline_body),
        )
      ConnectionTestState.Idle, ConnectionTestState.Testing -> Unit
    }
  }
}
