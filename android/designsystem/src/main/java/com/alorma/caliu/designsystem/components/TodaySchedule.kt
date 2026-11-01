package com.alorma.caliu.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alorma.caliu.designsystem.R
import com.alorma.caliu.designsystem.preview.CaliuPreviewTheme
import com.alorma.caliu.designsystem.preview.CaliuPreviews
import com.alorma.caliu.designsystem.theme.CaliuShapes
import com.alorma.caliu.designsystem.theme.CaliuSpacing
import com.alorma.caliu.designsystem.theme.CaliuTheme
import com.alorma.caliu.designsystem.theme.CaliuType
import com.alorma.caliu.designsystem.theme.Mood

/** Una fila del programa: «08:00 – 14:00» i «20,0 °C», ja formatats. */
@Immutable
data class ScheduleRow(
  val range: String,
  val value: String,
)

/**
 * Programa d'avui en files compactes. Els trams passats surten atenuats, els futurs en color secundari i l'actiu
 * ressaltat, més alt i amb −/+ petits.
 */
@Composable
fun TodaySchedule(
  rows: List<ScheduleRow>,
  activeIndex: Int?,
  onDecrease: () -> Unit,
  onIncrease: () -> Unit,
  decreaseLabel: String,
  increaseLabel: String,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  canDecrease: Boolean = true,
  canIncrease: Boolean = true,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = CaliuShapes.control,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(CaliuSpacing.joint),
    ) {
      rows.forEachIndexed { index, row ->
        if (index == activeIndex) {
          ActiveRow(
            row = row,
            onDecrease = onDecrease,
            onIncrease = onIncrease,
            decreaseLabel = decreaseLabel,
            increaseLabel = increaseLabel,
            canDecrease = enabled && canDecrease,
            canIncrease = enabled && canIncrease,
          )
        } else {
          PlainRow(row = row, past = activeIndex != null && index < activeIndex)
        }
      }
    }
  }
}

@Composable
private fun PlainRow(
  row: ScheduleRow,
  past: Boolean,
) {
  val color = if (past) CaliuTheme.colors.onSurfacePast else MaterialTheme.colorScheme.onSurfaceVariant
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 4.dp),
  ) {
    Text(row.range, style = CaliuType.row, color = color, modifier = Modifier.weight(1f))
    Text(row.value, style = CaliuType.row, color = color)
  }
}

@Composable
private fun ActiveRow(
  row: ScheduleRow,
  onDecrease: () -> Unit,
  onIncrease: () -> Unit,
  decreaseLabel: String,
  increaseLabel: String,
  canDecrease: Boolean,
  canIncrease: Boolean,
) {
  val content = MaterialTheme.colorScheme.onPrimaryContainer
  val track = CaliuTheme.colors.track
  Surface(
    modifier = Modifier.semantics { selected = true },
    shape = CaliuShapes.chip,
    color = MaterialTheme.colorScheme.primaryContainer,
    contentColor = content,
  ) {
    Row(
      modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = row.range,
        style = CaliuType.row.copy(fontWeight = FontWeight.ExtraBold),
        modifier = Modifier.weight(1f),
      )
      StepButton(
        icon = R.drawable.ic_remove,
        contentDescription = decreaseLabel,
        onClick = onDecrease,
        enabled = canDecrease,
        size = 40.dp,
        iconSize = 24.dp,
        shape = CaliuShapes.chip,
        container = track,
        content = content,
      )
      Text(
        text = row.value,
        style = CaliuType.tempRow,
        modifier = Modifier.padding(horizontal = 10.dp),
      )
      StepButton(
        icon = R.drawable.ic_add,
        contentDescription = increaseLabel,
        onClick = onIncrease,
        enabled = canIncrease,
        size = 40.dp,
        iconSize = 24.dp,
        shape = CaliuShapes.chip,
        container = track,
        content = content,
      )
    }
  }
}

@CaliuPreviews
@Composable
private fun TodaySchedulePreview() {
  CaliuPreviewTheme(Mood.HEAT) {
    TodaySchedule(
      rows =
        listOf(
          ScheduleRow("00:00 – 06:30", "17,0 °C"),
          ScheduleRow("06:30 – 08:00", "21,0 °C"),
          ScheduleRow("08:00 – 14:00", "20,0 °C"),
          ScheduleRow("14:00 – 18:00", "19,0 °C"),
          ScheduleRow("18:00 – 22:30", "21,0 °C"),
          ScheduleRow("22:30 – 24:00", "17,0 °C"),
        ),
      activeIndex = 2,
      onDecrease = {},
      onIncrease = {},
      decreaseLabel = "Baixa",
      increaseLabel = "Puja",
    )
  }
}
