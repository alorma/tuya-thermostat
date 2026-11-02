package com.alorma.caliu.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.alorma.caliu.R
import com.alorma.caliu.designsystem.R as DsR
import com.alorma.caliu.designsystem.components.CaliuPullToRefreshBox
import com.alorma.caliu.designsystem.components.HeroTemperature
import com.alorma.caliu.designsystem.components.HomeFooter
import com.alorma.caliu.designsystem.components.HomeSkeleton
import com.alorma.caliu.designsystem.components.ModeSelector
import com.alorma.caliu.designsystem.components.QuickTemperatures
import com.alorma.caliu.designsystem.components.ScheduleRow
import com.alorma.caliu.designsystem.components.Section
import com.alorma.caliu.designsystem.components.SetpointControl
import com.alorma.caliu.designsystem.components.StatePanel
import com.alorma.caliu.designsystem.components.StatePanelButton
import com.alorma.caliu.designsystem.components.StatePanelButtonKind
import com.alorma.caliu.designsystem.components.StatePanelStyle
import com.alorma.caliu.designsystem.components.StatusChip
import com.alorma.caliu.designsystem.components.StatusChipSkeleton
import com.alorma.caliu.designsystem.components.StatusMenuItem
import com.alorma.caliu.designsystem.components.TemporaryBanner
import com.alorma.caliu.designsystem.components.TodaySchedule
import com.alorma.caliu.designsystem.theme.CaliuSpacing
import com.alorma.caliu.designsystem.theme.CaliuTheme
import com.alorma.caliu.designsystem.theme.Mood
import com.alorma.caliu.domain.Mode
import com.alorma.caliu.domain.ThermostatState
import com.alorma.caliu.domain.activeSlotIndex
import com.alorma.caliu.domain.mood
import com.alorma.caliu.ui.format.formatTemperature
import com.alorma.caliu.ui.format.presetDegrees
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.koin.compose.viewmodel.koinViewModel

private const val OFF_ALPHA = 0.38f
private const val TUYA_CONSOLE_URL = "https://iot.tuya.com"
private val Presets = listOf(20.0, 21.0, 23.0)
private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm")

/** Accions de la pantalla principal. */
@Immutable
data class HomeActions(
  val onRefresh: () -> Unit = {},
  val onOpenSettings: () -> Unit = {},
  val onOpenConsole: () -> Unit = {},
  val onActivate: () -> Unit = {},
  val onPause: () -> Unit = {},
  val onTurnOff: () -> Unit = {},
  val onModeChange: (Mode) -> Unit = {},
  val onDecrease: () -> Unit = {},
  val onIncrease: () -> Unit = {},
  val onPreset: (Double) -> Unit = {},
  val onBackToSchedule: () -> Unit = {},
  val onEditSchedule: () -> Unit = {},
)

@Composable
fun HomeScreen(
  onOpenSettings: () -> Unit,
  onEditSchedule: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: HomeViewModel = koinViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val uriHandler = LocalUriHandler.current
  val snackbarHostState = remember { SnackbarHostState() }
  val commandFailed = stringResource(R.string.command_failed)
  val lifecycleOwner = LocalLifecycleOwner.current

  // Refresc periòdic només amb l'app en primer pla
  LaunchedEffect(viewModel, lifecycleOwner) {
    lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.refreshPeriodically() }
  }
  LaunchedEffect(viewModel) {
    viewModel.events.collect { event ->
      when (event) {
        HomeEvent.CommandFailed -> snackbarHostState.showSnackbar(commandFailed)
      }
    }
  }

  HomeContent(
    uiState = uiState,
    actions =
      HomeActions(
        onRefresh = viewModel::refresh,
        onOpenSettings = onOpenSettings,
        onOpenConsole = { uriHandler.openUri(TUYA_CONSOLE_URL) },
        onActivate = viewModel::activate,
        onPause = viewModel::pause,
        onTurnOff = viewModel::turnOff,
        onModeChange = viewModel::setMode,
        onDecrease = viewModel::decrease,
        onIncrease = viewModel::increase,
        onPreset = viewModel::setPreset,
        onBackToSchedule = viewModel::backToSchedule,
        onEditSchedule = onEditSchedule,
      ),
    modifier = modifier,
    snackbarHostState = snackbarHostState,
  )
}

/** Pantalla principal sense estat, per a les previews i els tests de captures. Aplica la paleta del mood. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
  uiState: HomeUiState,
  actions: HomeActions,
  modifier: Modifier = Modifier,
  snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
  initialMenuExpanded: Boolean = false,
) {
  CaliuTheme(mood = uiState.mood) {
    Scaffold(
      modifier = modifier,
      containerColor = MaterialTheme.colorScheme.background,
      snackbarHost = { SnackbarHost(snackbarHostState) },
      topBar = {
        TopAppBar(
          title = {
            when (uiState) {
              is HomeUiState.Content -> HomeStatusChip(uiState.state, actions, initialMenuExpanded)
              HomeUiState.Loading -> StatusChipSkeleton()
              else -> Unit
            }
          },
          actions = {
            IconButton(onClick = actions.onOpenSettings) {
              Icon(
                painter = painterResource(DsR.drawable.ic_settings_filled),
                contentDescription = stringResource(R.string.settings),
              )
            }
          },
          expandedHeight = 80.dp,
        )
      },
      bottomBar = {
        if (uiState is HomeUiState.Content) HomeFooterFor(uiState)
      },
    ) { padding ->
      val contentModifier =
        Modifier
          .fillMaxSize()
          .padding(padding)
      when (uiState) {
        HomeUiState.Loading ->
          HomeSkeleton(contentModifier.padding(horizontal = CaliuSpacing.screen))
        is HomeUiState.Content ->
          CaliuPullToRefreshBox(
            isRefreshing = uiState.refreshing,
            onRefresh = actions.onRefresh,
            modifier = contentModifier,
          ) {
            ThermostatControls(uiState, actions)
          }
        is HomeUiState.ApiError -> ErrorState(contentModifier) { ApiErrorPanel(uiState, actions) }
        HomeUiState.Offline -> ErrorState(contentModifier) { OfflinePanel(actions) }
        HomeUiState.MissingConfig -> ErrorState(contentModifier) { MissingConfigPanel(actions) }
      }
    }
  }
}

@Composable
private fun HomeStatusChip(
  state: ThermostatState,
  actions: HomeActions,
  initialMenuExpanded: Boolean,
) {
  var expanded by rememberSaveable { mutableStateOf(initialMenuExpanded) }
  val (labelRes, icon) =
    when (state.mood()) {
      Mood.HEAT -> R.string.status_heating to DsR.drawable.ic_local_fire_department_filled
      Mood.CALM -> R.string.status_idle to DsR.drawable.ic_pause_filled
      Mood.PAUSE -> R.string.status_paused to DsR.drawable.ic_pause_circle_filled
      Mood.OFF -> R.string.status_off to DsR.drawable.ic_power_settings_new
    }
  val label = stringResource(labelRes)
  StatusChip(
    label = label,
    icon = icon,
    contentDescription = stringResource(R.string.status_change_a11y, label),
    expanded = expanded,
    onExpandedChange = { expanded = it },
    primaryItems =
      listOf(
        StatusMenuItem(
          label = stringResource(R.string.action_activate),
          icon = DsR.drawable.ic_play_arrow_filled,
          selected = state.power && !state.paused,
          onClick = actions.onActivate,
        ),
        StatusMenuItem(
          label = stringResource(R.string.action_pause),
          icon = DsR.drawable.ic_pause_filled,
          selected = state.power && state.paused,
          onClick = actions.onPause,
        ),
      ),
    secondaryItems =
      listOf(
        StatusMenuItem(
          label = stringResource(R.string.action_turn_off),
          icon = DsR.drawable.ic_power_settings_new,
          selected = !state.power,
          onClick = actions.onTurnOff,
        ),
      ),
  )
}

@Composable
private fun ThermostatControls(
  uiState: HomeUiState.Content,
  actions: HomeActions,
) {
  val state = uiState.state
  val locale = currentLocale()
  val temp: @Composable (Double) -> String = { stringResourceTemp(it, locale) }

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = CaliuSpacing.screen)
        .padding(bottom = CaliuSpacing.screen),
    verticalArrangement = Arrangement.spacedBy(CaliuSpacing.section),
  ) {
    val current = state.currentTemp?.takeIf { state.power }
    val currentText = current?.let { formatTemperature(it, locale) } ?: stringResource(R.string.temp_unknown)
    Section(stringResource(R.string.section_current_temp)) {
      HeroTemperature(
        value = currentText,
        contentDescription = stringResource(R.string.temp_value, currentText),
      )
    }

    // Apagat: tot el que hi ha sota la temperatura queda desactivat i al 38 %
    val enabled = state.power
    Column(
      modifier = Modifier.alpha(if (enabled) 1f else OFF_ALPHA),
      verticalArrangement = Arrangement.spacedBy(CaliuSpacing.section),
    ) {
      Section(stringResource(R.string.section_mode)) {
        ModeSelector(
          options = listOf(stringResource(R.string.mode_auto), stringResource(R.string.mode_manual)),
          selectedIndex = state.mode.ordinal,
          onSelect = { actions.onModeChange(Mode.entries[it]) },
          enabled = enabled,
        )
      }
      when (state.mode) {
        Mode.AUTO -> AutoControls(uiState, actions, enabled, temp)
        Mode.MANUAL -> ManualControls(state, actions, enabled, temp)
      }
    }
  }
}

@Composable
private fun AutoControls(
  uiState: HomeUiState.Content,
  actions: HomeActions,
  enabled: Boolean,
  temp: @Composable (Double) -> String,
) {
  val state = uiState.state
  val active = state.schedule.activeSlotIndex(uiState.now)
  val activeSetpoint = active?.let { state.override?.setpoint ?: state.schedule[it].setpoint }
  val override = state.override
  if (override != null && !state.paused) {
    TemporaryBanner(
      text = stringResource(R.string.temporary_until, override.until.toString()),
      actionLabel = stringResource(R.string.temporary_back),
      onAction = actions.onBackToSchedule,
      enabled = enabled,
    )
  }
  // «Edita» no depèn de l'encesa ni de la pausa: el programa es pot preparar igualment
  Section(
    label = stringResource(R.string.section_schedule),
    actionLabel = stringResource(R.string.schedule_edit),
    onAction = actions.onEditSchedule,
  ) {
    TodaySchedule(
      rows =
        state.schedule.mapIndexed { index, slot ->
          ScheduleRow(
            range = stringResource(R.string.schedule_range, slot.start.toString(), slot.end.toString()),
            value = temp(if (index == active && activeSetpoint != null) activeSetpoint else slot.setpoint),
          )
        },
      activeIndex = active,
      onDecrease = actions.onDecrease,
      onIncrease = actions.onIncrease,
      decreaseLabel = stringResource(R.string.setpoint_decrease),
      increaseLabel = stringResource(R.string.setpoint_increase),
      enabled = enabled && !state.paused,
      canDecrease = activeSetpoint != null && activeSetpoint > state.minSetpoint,
      canIncrease = activeSetpoint != null && activeSetpoint < state.maxSetpoint,
    )
  }
}

@Composable
private fun ManualControls(
  state: ThermostatState,
  actions: HomeActions,
  enabled: Boolean,
  temp: @Composable (Double) -> String,
) {
  Section(stringResource(R.string.section_setpoint)) {
    SetpointControl(
      value = temp(state.setpoint),
      onDecrease = actions.onDecrease,
      onIncrease = actions.onIncrease,
      decreaseLabel = stringResource(R.string.setpoint_decrease),
      increaseLabel = stringResource(R.string.setpoint_increase),
      enabled = enabled,
      canDecrease = state.setpoint > state.minSetpoint,
      canIncrease = state.setpoint < state.maxSetpoint,
      pending = state.paused,
    )
  }
  // En pausa les dreceres queden desactivades i atenuades, com la consigna que es recuperarà
  Section(
    label = stringResource(R.string.section_presets),
    modifier = Modifier.alpha(if (state.paused && enabled) OFF_ALPHA else 1f),
  ) {
    QuickTemperatures(
      options = Presets.map { stringResource(R.string.temp_preset, presetDegrees(it)) },
      selectedIndex = Presets.indexOf(state.setpoint).takeIf { it >= 0 },
      onSelect = { actions.onPreset(Presets[it]) },
      enabled = enabled && !state.paused,
    )
  }
}

@Composable
private fun HomeFooterFor(uiState: HomeUiState.Content) {
  val battery = uiState.state.battery
  HomeFooter(
    modifier = Modifier.navigationBarsPadding(),
    battery = battery,
    batteryText = battery?.let { stringResource(R.string.battery_percent, it) },
    updatedText =
      if (uiState.refreshing) {
        stringResource(R.string.updating)
      } else {
        stringResource(R.string.updated_at, TimeFormat.format(uiState.updatedAt))
      },
  )
}

@Composable
private fun ErrorState(
  modifier: Modifier,
  content: @Composable () -> Unit,
) {
  Box(
    modifier = modifier.padding(CaliuSpacing.screen),
    contentAlignment = Alignment.Center,
  ) {
    content()
  }
}

@Composable
private fun ApiErrorPanel(
  uiState: HomeUiState.ApiError,
  actions: HomeActions,
) {
  StatePanel(
    icon = DsR.drawable.ic_error_filled,
    title = stringResource(R.string.error_api_title),
    body = uiState.message,
    isError = true,
  ) {
    if (uiState.openConsole) {
      StatePanelButton(
        text = stringResource(R.string.error_api_open_console),
        onClick = actions.onOpenConsole,
        icon = DsR.drawable.ic_open_in_new,
      )
    }
    StatePanelButton(
      text = stringResource(R.string.error_retry),
      onClick = actions.onRefresh,
      kind = if (uiState.openConsole) StatePanelButtonKind.Text else StatePanelButtonKind.Tonal,
      icon = if (uiState.openConsole) null else DsR.drawable.ic_refresh,
    )
  }
}

@Composable
private fun OfflinePanel(actions: HomeActions) {
  StatePanel(
    icon = DsR.drawable.ic_wifi_off,
    title = stringResource(R.string.error_offline_title),
    body = stringResource(R.string.error_offline_body),
  ) {
    StatePanelButton(
      text = stringResource(R.string.error_retry),
      onClick = actions.onRefresh,
      icon = DsR.drawable.ic_refresh,
      kind = StatePanelButtonKind.Tonal,
    )
  }
}

@Composable
private fun MissingConfigPanel(actions: HomeActions) {
  StatePanel(
    icon = DsR.drawable.ic_key_filled,
    title = stringResource(R.string.empty_config_title),
    body = stringResource(R.string.empty_config_body),
    style = StatePanelStyle.Centered,
  ) {
    StatePanelButton(
      text = stringResource(R.string.empty_config_action),
      onClick = actions.onOpenSettings,
      icon = DsR.drawable.ic_settings_filled,
      fillWidth = false,
    )
  }
}

@Composable
private fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

@Composable
private fun stringResourceTemp(
  celsius: Double,
  locale: Locale,
): String = stringResource(R.string.temp_value, formatTemperature(celsius, locale))
