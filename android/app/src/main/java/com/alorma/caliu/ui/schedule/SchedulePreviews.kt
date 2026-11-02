package com.alorma.caliu.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alorma.caliu.R
import com.alorma.caliu.data.fake.FakeThermostatRepository
import com.alorma.caliu.designsystem.theme.CaliuSpacing
import com.alorma.caliu.designsystem.theme.CaliuTheme
import com.alorma.caliu.designsystem.theme.CaliuType
import com.alorma.caliu.designsystem.theme.Mood
import com.alorma.caliu.domain.MinuteOfDay
import com.alorma.caliu.domain.ProgramEntry
import com.alorma.caliu.ui.home.HomePreviews
import java.time.DayOfWeek

private const val SCRIM_ALPHA = 0.32f

/** Estats fixos de les captures `19-programa-*`: dilluns, que també és avui, amb el programa del handoff. */
object SchedulePreviewData {
  private val week = DayOfWeek.entries.associateWith { FakeThermostatRepository.DefaultDay }

  val Edit =
    ScheduleUiState.Editing(
      days = week,
      saved = week,
      selectedDay = DayOfWeek.MONDAY,
      today = DayOfWeek.MONDAY,
      minSetpoint = 5.0,
      maxSetpoint = 30.0,
    )

  // Dilluns amb el tram de les 06:30 a 21,5 °C i copiat de dimarts a divendres
  private val mondayEdited =
    FakeThermostatRepository.DefaultDay.mapIndexed { index, entry ->
      if (index == 1) ProgramEntry(MinuteOfDay.of(6, 30), 21.5) else entry
    }
  val Copied =
    Edit.copy(days = week + DayOfWeek.entries.take(5).associateWith { mondayEdited })
}

@HomePreviews
@Composable
fun ScheduleEditPreview() {
  ScheduleContent(SchedulePreviewData.Edit, ScheduleActions())
}

/** Captura `19-programa-copy`. El full es pinta sense `ModalBottomSheet`, perquè layoutlib no pinta les finestres. */
@OptIn(ExperimentalMaterial3Api::class)
@HomePreviews
@Composable
fun ScheduleCopyPreview() {
  Box(Modifier.fillMaxSize()) {
    ScheduleContent(SchedulePreviewData.Edit, ScheduleActions())
    CaliuTheme(mood = Mood.OFF) {
      Scrim()
      Surface(
        modifier = Modifier.align(Alignment.BottomCenter),
        shape = BottomSheetDefaults.ExpandedShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          BottomSheetDefaults.DragHandle()
          CopyDaysSheetContent(
            source = DayOfWeek.MONDAY,
            selected = DayOfWeek.entries.subList(1, 5).toSet(),
            onSelectedChange = {},
            onCancel = {},
            onConfirm = {},
          )
        }
      }
    }
  }
}

/** Captura `19-programa-time`. Com el full, el diàleg es pinta a mà amb el mateix `TimeInput`. */
@OptIn(ExperimentalMaterial3Api::class)
@HomePreviews
@Composable
fun ScheduleTimePreview() {
  Box(Modifier.fillMaxSize()) {
    ScheduleContent(SchedulePreviewData.Edit, ScheduleActions())
    CaliuTheme(mood = Mood.OFF) {
      Scrim()
      Surface(
        modifier =
          Modifier
            .align(Alignment.Center)
            .padding(horizontal = CaliuSpacing.xl),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
      ) {
        Column(
          modifier = Modifier.padding(CaliuSpacing.xl),
          verticalArrangement = Arrangement.spacedBy(CaliuSpacing.l),
        ) {
          Text(
            stringResource(R.string.schedule_start_time),
            style = CaliuType.sectionLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          TimeInput(rememberTimePickerState(initialHour = 6, initialMinute = 30, is24Hour = true))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = {}) { Text(stringResource(R.string.action_cancel), style = CaliuType.button) }
            TextButton(onClick = {}) { Text(stringResource(R.string.action_ok), style = CaliuType.button) }
          }
        }
      }
    }
  }
}

/** Captura `19-programa-copied`: dies modificats amb el punt i el Snackbar «Copiat a 4 dies». */
@HomePreviews
@Composable
fun ScheduleCopiedPreview() {
  Box(Modifier.fillMaxSize()) {
    ScheduleContent(SchedulePreviewData.Copied, ScheduleActions())
    CaliuTheme(mood = Mood.OFF) {
      Snackbar(
        modifier =
          Modifier
            .align(Alignment.BottomCenter)
            .padding(CaliuSpacing.screen),
      ) {
        Text(pluralStringResource(R.plurals.schedule_copied, 4, 4))
      }
    }
  }
}

@HomePreviews
@Composable
fun ScheduleErrorPreview() {
  ScheduleContent(ScheduleUiState.Error(message = null), ScheduleActions())
}

@Composable
private fun Scrim() {
  Box(
    Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA)),
  )
}
