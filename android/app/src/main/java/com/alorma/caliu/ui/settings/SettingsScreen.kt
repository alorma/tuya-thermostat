package com.alorma.caliu.ui.settings

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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.caliu.R
import com.alorma.caliu.data.tuya.TuyaRegion
import com.alorma.caliu.designsystem.components.CaliuLargeTopBar
import com.alorma.caliu.designsystem.components.ConnectionResultCard
import com.alorma.caliu.designsystem.components.ConnectionTestButton
import com.alorma.caliu.designsystem.components.FormCard
import com.alorma.caliu.designsystem.components.FormCardPosition
import com.alorma.caliu.designsystem.components.FormGroup
import com.alorma.caliu.designsystem.components.FormSecretField
import com.alorma.caliu.designsystem.components.FormTextField
import com.alorma.caliu.designsystem.components.RegionSelector
import com.alorma.caliu.designsystem.theme.CaliuSpacing
import com.alorma.caliu.designsystem.theme.CaliuTheme
import com.alorma.caliu.designsystem.theme.Mood
import org.koin.compose.viewmodel.koinViewModel

/** Accions de Configuració. */
@Immutable
data class SettingsActions(
  val onBack: () -> Unit = {},
  val onSave: () -> Unit = {},
  val onRegionChange: (TuyaRegion) -> Unit = {},
  val onTestConnection: () -> Unit = {},
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
        onTestConnection = viewModel::testConnection,
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
      }
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
      RegionSelector(
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
