package com.alorma.caliu.ui.schedule

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.caliu.R
import com.alorma.caliu.designsystem.R as DsR
import com.alorma.caliu.designsystem.components.CaliuLargeTopBar
import com.alorma.caliu.designsystem.components.CaliuLoadingIndicator
import com.alorma.caliu.designsystem.components.CopyDayToggle
import com.alorma.caliu.designsystem.components.CopyDaysButton
import com.alorma.caliu.designsystem.components.DaySelector
import com.alorma.caliu.designsystem.components.DayTimeline
import com.alorma.caliu.designsystem.components.ProgramCard
import com.alorma.caliu.designsystem.components.ProgramSlotRow
import com.alorma.caliu.designsystem.components.Section
import com.alorma.caliu.designsystem.components.StatePanel
import com.alorma.caliu.designsystem.components.StatePanelButton
import com.alorma.caliu.designsystem.components.StatePanelButtonKind
import com.alorma.caliu.designsystem.components.TimelineSegment
import com.alorma.caliu.designsystem.theme.CaliuSpacing
import com.alorma.caliu.designsystem.theme.CaliuTheme
import com.alorma.caliu.designsystem.theme.CaliuType
import com.alorma.caliu.designsystem.theme.Mood
import com.alorma.caliu.domain.MinuteOfDay
import com.alorma.caliu.ui.format.formatTemperature
import java.time.DayOfWeek
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/** Accions del programa setmanal. */
@Immutable
data class ScheduleActions(
  val onBack: () -> Unit = {},
  val onSave: () -> Unit = {},
  val onRetry: () -> Unit = {},
  val onSelectDay: (DayOfWeek) -> Unit = {},
  val onStep: (index: Int, steps: Int) -> Unit = { _, _ -> },
  val onEditTime: (index: Int) -> Unit = {},
  val onCopy: () -> Unit = {},
)

@Composable
fun ScheduleScreen(
  onBack: () -> Unit,
  onSaved: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: ScheduleViewModel = koinViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val dayNames = stringArrayResource(R.array.weekday_in_sentence)
  val resources = LocalResources.current

  var editingSlot by rememberSaveable { mutableStateOf<Int?>(null) }
  var copying by rememberSaveable { mutableStateOf(false) }
  var confirmingDiscard by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(viewModel) {
    viewModel.events.collect { event ->
      when (event) {
        is ScheduleEvent.Copied ->
          snackbarHostState.showSnackbar(
            resources.getQuantityString(R.plurals.schedule_copied, event.count, event.count),
          )
        ScheduleEvent.Saved -> onSaved()
        is ScheduleEvent.SaveFailed ->
          snackbarHostState.showSnackbar(
            resources.getString(R.string.schedule_save_failed, dayNames[event.day.ordinal]),
          )
      }
    }
  }

  val editing = uiState as? ScheduleUiState.Editing
  val hasChanges = editing?.hasChanges == true
  val back = { if (hasChanges) confirmingDiscard = true else onBack() }
  BackHandler(enabled = hasChanges) { confirmingDiscard = true }

  ScheduleContent(
    uiState = uiState,
    actions =
      ScheduleActions(
        onBack = back,
        onSave = viewModel::save,
        onRetry = viewModel::load,
        onSelectDay = viewModel::selectDay,
        onStep = viewModel::stepTemperature,
        onEditTime = { editingSlot = it },
        onCopy = { copying = true },
      ),
    modifier = modifier,
    snackbarHostState = snackbarHostState,
  )

  CaliuTheme(mood = Mood.OFF) {
    if (editing != null) {
      editingSlot?.takeIf { it in editing.entries.indices }?.let { index ->
        StartTimeDialog(
          initial = editing.entries[index].start,
          allowed = editing.startRange(index),
          onConfirm = {
            viewModel.setStart(index, it)
            editingSlot = null
          },
          onDismiss = { editingSlot = null },
        )
      }
      if (copying) {
        CopyDaysSheet(
          source = editing.selectedDay,
          onCopy = {
            viewModel.copyTo(it)
            copying = false
          },
          onDismiss = { copying = false },
        )
      }
    }
    if (confirmingDiscard) {
      DiscardDialog(
        onDiscard = {
          confirmingDiscard = false
          onBack()
        },
        onKeepEditing = { confirmingDiscard = false },
      )
    }
  }
}

/** Programa setmanal sense estat, per a les previews i els tests de captures. Sempre amb la paleta neutra. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleContent(
  uiState: ScheduleUiState,
  actions: ScheduleActions,
  modifier: Modifier = Modifier,
  snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
  CaliuTheme(mood = Mood.OFF) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val editing = uiState as? ScheduleUiState.Editing
    Scaffold(
      modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
      containerColor = MaterialTheme.colorScheme.background,
      snackbarHost = { SnackbarHost(snackbarHostState) },
      topBar = {
        CaliuLargeTopBar(
          title = stringResource(R.string.schedule_title),
          backLabel = stringResource(R.string.config_back),
          onBack = actions.onBack,
          actionLabel = stringResource(R.string.schedule_save),
          onAction = actions.onSave,
          actionEnabled = editing != null && !editing.saving,
          actionLoading = editing?.saving == true,
          scrollBehavior = scrollBehavior,
        )
      },
    ) { padding ->
      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = CaliuSpacing.screen)
            .padding(bottom = CaliuSpacing.screen)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(CaliuSpacing.l),
      ) {
        when (uiState) {
          ScheduleUiState.Loading ->
            CaliuLoadingIndicator(Modifier.align(Alignment.CenterHorizontally).padding(CaliuSpacing.xl))
          is ScheduleUiState.Error -> ErrorPanel(uiState, actions)
          ScheduleUiState.Unreadable ->
            StatePanel(
              icon = DsR.drawable.ic_error_filled,
              title = stringResource(R.string.schedule_unreadable_title),
              body = stringResource(R.string.schedule_unreadable_body),
              isError = true,
            )
          is ScheduleUiState.Editing -> Editor(uiState, actions)
        }
      }
    }
  }
}

@Composable
private fun ErrorPanel(
  uiState: ScheduleUiState.Error,
  actions: ScheduleActions,
) {
  val offline = uiState.message == null
  StatePanel(
    icon = if (offline) DsR.drawable.ic_wifi_off else DsR.drawable.ic_error_filled,
    title = stringResource(if (offline) R.string.error_offline_title else R.string.error_api_title),
    body = uiState.message ?: stringResource(R.string.error_offline_body),
    isError = !offline,
  ) {
    StatePanelButton(
      text = stringResource(R.string.error_retry),
      onClick = actions.onRetry,
      icon = DsR.drawable.ic_refresh,
      kind = StatePanelButtonKind.Tonal,
    )
  }
}

@Composable
private fun ColumnScope.Editor(
  state: ScheduleUiState.Editing,
  actions: ScheduleActions,
) {
  val shortNames = stringArrayResource(R.array.weekday_short)
  val longNames = stringArrayResource(R.array.weekday_long)
  val locale = LocalConfiguration.current.locales[0]
  val day = state.selectedDay
  DaySelector(
    labels = shortNames.toList(),
    descriptions = longNames.toList(),
    selectedIndex = day.ordinal,
    todayIndex = state.today.ordinal,
    modifiedIndices = state.modifiedDays.mapTo(mutableSetOf()) { it.ordinal },
    onSelect = { actions.onSelectDay(DayOfWeek.entries[it]) },
  )
  Section(longNames[day.ordinal]) {
    DayTimeline(
      segments =
        state.program.scheduleFor(day).map {
          TimelineSegment(start = it.start.value, end = it.end.value, level = timelineLevel(it.setpoint))
        },
      description = stringResource(R.string.schedule_timeline_a11y, longNames[day.ordinal]),
    )
  }
  val slotsDescription = stringResource(R.string.schedule_slots_a11y)
  ProgramCard(modifier = Modifier.semantics { contentDescription = slotsDescription }) {
    state.entries.forEachIndexed { index, entry ->
      val time = entry.start.toString()
      ProgramSlotRow(
        time = time,
        timeDescription = stringResource(R.string.schedule_start_time_a11y, time),
        onTimeClick = { actions.onEditTime(index) },
        value = stringResource(R.string.temp_value, formatTemperature(entry.setpoint, locale)),
        onDecrease = { actions.onStep(index, -1) },
        onIncrease = { actions.onStep(index, 1) },
        decreaseLabel = stringResource(R.string.schedule_temp_decrease),
        increaseLabel = stringResource(R.string.schedule_temp_increase),
        enabled = !state.saving,
        canDecrease = state.canDecrease(index),
        canIncrease = state.canIncrease(index),
      )
    }
  }
  CopyDaysButton(
    text = stringResource(R.string.schedule_copy),
    onClick = actions.onCopy,
    enabled = !state.saving,
  )
}

/**
 * Nivell de color de la barra de 24 h segons la temperatura, a trams fixos perquè un mateix valor tingui el mateix
 * color tots els dies: fins a 18 °C, el neutre; i de 19 °C a 21,5 °C o més, de `heat-hero` cap a `heat`.
 */
internal fun timelineLevel(celsius: Double): Int =
  when {
    celsius < 18.5 -> 0
    celsius < 19.5 -> 1
    celsius < 20.5 -> 2
    celsius < 21.5 -> 3
    else -> 4
  }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartTimeDialog(
  initial: MinuteOfDay,
  allowed: IntRange,
  onConfirm: (MinuteOfDay) -> Unit,
  onDismiss: () -> Unit,
) {
  val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
  val chosen = MinuteOfDay.of(state.hour, state.minute)
  TimePickerDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.schedule_start_time)) },
    confirmButton = {
      // Fora de l'interval que deixen els trams veïns, D'acord queda desactivat
      TextButton(onClick = { onConfirm(chosen) }, enabled = chosen.value in allowed) {
        Text(stringResource(R.string.action_ok), style = CaliuType.button)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.action_cancel), style = CaliuType.button)
      }
    },
  ) {
    TimePicker(state = state)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CopyDaysSheet(
  source: DayOfWeek,
  onCopy: (Set<DayOfWeek>) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val scope = rememberCoroutineScope()
  var selected by remember { mutableStateOf(emptySet<DayOfWeek>()) }

  fun close(then: () -> Unit) {
    scope.launch { sheetState.hide() }.invokeOnCompletion { then() }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
  ) {
    CopyDaysSheetContent(
      source = source,
      selected = selected,
      onSelectedChange = { selected = it },
      onCancel = { close(onDismiss) },
      onConfirm = { close { onCopy(selected) } },
    )
  }
}

/** El contingut del full «Copia el … a», a part perquè les previews el puguin pintar sense el full. */
@Composable
internal fun CopyDaysSheetContent(
  source: DayOfWeek,
  selected: Set<DayOfWeek>,
  onSelectedChange: (Set<DayOfWeek>) -> Unit,
  onCancel: () -> Unit,
  onConfirm: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val shortNames = stringArrayResource(R.array.weekday_short)
  val longNames = stringArrayResource(R.array.weekday_long)
  val dayNames = stringArrayResource(R.array.weekday_in_sentence)
  val targets = selected - source

  fun pick(days: Set<DayOfWeek>) = onSelectedChange(days - source)

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(horizontal = CaliuSpacing.l)
        .padding(bottom = CaliuSpacing.l),
    verticalArrangement = Arrangement.spacedBy(CaliuSpacing.l),
  ) {
    Text(
      text = stringResource(R.string.schedule_copy_title, dayNames[source.ordinal]),
      style = CaliuType.stateTitle,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      DayOfWeek.entries.forEach { day ->
        CopyDayToggle(
          label = shortNames[day.ordinal],
          description = longNames[day.ordinal],
          checked = day in targets,
          onCheckedChange = { checked -> onSelectedChange(if (checked) targets + day else targets - day) },
          enabled = day != source,
        )
      }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(CaliuSpacing.s)) {
      AssistChip(
        onClick = { pick(Weekdays) },
        label = { Text(stringResource(R.string.schedule_copy_weekdays), style = CaliuType.button) },
      )
      AssistChip(
        onClick = { pick(Weekend) },
        label = { Text(stringResource(R.string.schedule_copy_weekend), style = CaliuType.button) },
      )
      AssistChip(
        onClick = { pick(DayOfWeek.entries.toSet()) },
        label = { Text(stringResource(R.string.schedule_copy_all), style = CaliuType.button) },
      )
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(CaliuSpacing.s, Alignment.End),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      TextButton(onClick = onCancel) {
        Text(stringResource(R.string.action_cancel), style = CaliuType.button)
      }
      Button(onClick = onConfirm, enabled = targets.isNotEmpty()) {
        Text(
          text =
            if (targets.isEmpty()) {
              stringResource(R.string.schedule_copy_none)
            } else {
              pluralStringResource(R.plurals.schedule_copy_action, targets.size, targets.size)
            },
          style = CaliuType.button,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}

private val Weekdays = DayOfWeek.entries.take(5).toSet()
private val Weekend = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

@Composable
private fun DiscardDialog(
  onDiscard: () -> Unit,
  onKeepEditing: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onKeepEditing,
    title = { Text(stringResource(R.string.schedule_discard_title)) },
    confirmButton = {
      TextButton(onClick = onDiscard) {
        Text(stringResource(R.string.schedule_discard), style = CaliuType.button)
      }
    },
    dismissButton = {
      TextButton(onClick = onKeepEditing) {
        Text(stringResource(R.string.schedule_keep_editing), style = CaliuType.button)
      }
    },
  )
}
