package com.alorma.caliu.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.alorma.caliu.designsystem.R
import com.alorma.caliu.designsystem.preview.CaliuPreviewTheme
import com.alorma.caliu.designsystem.preview.CaliuPreviews
import com.alorma.caliu.designsystem.theme.CaliuShapes
import com.alorma.caliu.designsystem.theme.CaliuSpacing
import com.alorma.caliu.designsystem.theme.CaliuType
import com.alorma.caliu.designsystem.theme.Mood
import com.alorma.caliu.designsystem.theme.moodPalette

private val DayHeight = 52.dp
private val TimelineHeight = 28.dp
private val SlotControlSize = 44.dp

/** Els nivells de color de la barra de 24 h: de 0 (el més fresc) a [TIMELINE_LEVELS] − 1 (el més calent). */
const val TIMELINE_LEVELS = 5

/**
 * Selector de dia del programa: 7 `ToggleButton` connectats de selecció única. Avui porta un anell `outline` i els
 * dies modificats i no desats, un punt `primary`.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DaySelector(
  labels: List<String>,
  descriptions: List<String>,
  selectedIndex: Int,
  todayIndex: Int?,
  modifiedIndices: Set<Int>,
  onSelect: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(CaliuSpacing.joint),
  ) {
    labels.forEachIndexed { index, label ->
      val checked = index == selectedIndex
      val shapes = connectedShapes(index, labels.size)
      // El punt de modificat va a la cantonada del botó, no damunt del text
      Box(
        modifier =
          Modifier
            .weight(1f)
            .height(DayHeight),
      ) {
        ToggleButton(
          checked = checked,
          onCheckedChange = { onSelect(index) },
          shapes = shapes,
          colors =
            ToggleButtonDefaults.colors(
              containerColor = colors.surfaceContainerHigh,
              contentColor = colors.onSurface,
              checkedContainerColor = colors.primary,
              checkedContentColor = colors.onPrimary,
            ),
          border = if (index == todayIndex && !checked) BorderStroke(1.dp, colors.outline) else null,
          contentPadding = PaddingValues(0.dp),
          modifier =
            Modifier
              .fillMaxSize()
              .semantics {
                role = Role.RadioButton
                contentDescription = descriptions[index]
              },
        ) {
          Text(label, style = CaliuType.button)
        }
        if (index in modifiedIndices) {
          Box(
            Modifier
              .align(Alignment.TopEnd)
              .padding(top = 8.dp, end = 10.dp)
              .size(6.dp)
              .background(if (checked) colors.onPrimary else colors.primary, CircleShape),
          )
        }
      }
    }
  }
}

/** Un tram de la barra de 24 h: de [start] a [end] en minuts del dia, amb el nivell de color [level]. */
@Immutable
data class TimelineSegment(
  val start: Int,
  val end: Int,
  val level: Int,
)

/**
 * Barra de 24 h del dia: els trams com a segments proporcionals, acolorits per temperatura amb [TIMELINE_LEVELS]
 * nivells (el més fresc, neutre; després de `heat-hero` a `heat`), i les marques 0 · 6 · 12 · 18 · 24. Només és visual.
 */
@Composable
fun DayTimeline(
  segments: List<TimelineSegment>,
  description: String,
  modifier: Modifier = Modifier,
) {
  val levels = timelineColors()
  Column(
    modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
    verticalArrangement = Arrangement.spacedBy(CaliuSpacing.xs),
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .height(TimelineHeight)
          .clip(RoundedCornerShape(TimelineHeight / 2)),
      horizontalArrangement = Arrangement.spacedBy(CaliuSpacing.joint),
    ) {
      segments.filter { it.end > it.start }.forEach { segment ->
        Box(
          Modifier
            .weight((segment.end - segment.start).toFloat())
            .fillMaxHeight()
            .background(levels[segment.level.coerceIn(0, TIMELINE_LEVELS - 1)]),
        )
      }
    }
    TimelineTicks()
  }
}

/** Les marques de les hores, centrades a la seva posició; la primera i l'última, arran de les vores. */
@Composable
private fun TimelineTicks() {
  val hours = listOf(0, 6, 12, 18, 24)
  Layout(
    content = {
      hours.forEach {
        Text(
          text = it.toString(),
          style = CaliuType.overline,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
    modifier = Modifier.fillMaxWidth(),
  ) { measurables, constraints ->
    val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
    val width = constraints.maxWidth
    layout(width, placeables.maxOf { it.height }) {
      placeables.forEachIndexed { index, placeable ->
        val center = width * hours[index] / 24
        val x = (center - placeable.width / 2).coerceIn(0, width - placeable.width)
        placeable.place(x, 0)
      }
    }
  }
}

/** Els colors dels nivells: el primer és neutre i la resta van de `heat-hero` cap a `heat`. */
@Composable
private fun timelineColors(): List<Color> {
  val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
  val heat = moodPalette(Mood.HEAT, dark)
  val neutral = MaterialTheme.colorScheme.surfaceContainerHighest
  val warmest = 0.5f
  return listOf(neutral) +
    (0 until TIMELINE_LEVELS - 1).map { lerp(heat.hero, heat.accent, warmest * it / (TIMELINE_LEVELS - 2)) }
}

/** La targeta amb els trams del dia. */
@Composable
fun ProgramCard(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = CaliuShapes.control,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(CaliuSpacing.section),
    ) {
      content()
    }
  }
}

/** Un tram del programa: l'hora d'inici en una píndola tonal (tocar-la la canvia) i la temperatura amb −/+. */
@Composable
fun ProgramSlotRow(
  time: String,
  timeDescription: String,
  onTimeClick: () -> Unit,
  value: String,
  onDecrease: () -> Unit,
  onIncrease: () -> Unit,
  decreaseLabel: String,
  increaseLabel: String,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  canDecrease: Boolean = true,
  canIncrease: Boolean = true,
) {
  val container = MaterialTheme.colorScheme.surfaceContainerHigh
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    FilledTonalButton(
      onClick = onTimeClick,
      enabled = enabled,
      colors =
        ButtonDefaults.filledTonalButtonColors(
          containerColor = container,
          contentColor = MaterialTheme.colorScheme.onSurface,
        ),
      contentPadding = PaddingValues(horizontal = 12.dp),
      modifier =
        Modifier
          .width(88.dp)
          .height(SlotControlSize)
          .semantics { contentDescription = timeDescription },
    ) {
      Text(time, style = CaliuType.tempRow)
    }
    Box(Modifier.weight(1f))
    StepButton(
      icon = R.drawable.ic_remove,
      contentDescription = decreaseLabel,
      onClick = onDecrease,
      enabled = enabled && canDecrease,
      size = SlotControlSize,
      iconSize = 24.dp,
      shape = CaliuShapes.chip,
      container = container,
    )
    Text(
      text = value,
      style = CaliuType.tempRow,
      modifier = Modifier.padding(horizontal = 10.dp),
    )
    StepButton(
      icon = R.drawable.ic_add,
      contentDescription = increaseLabel,
      onClick = onIncrease,
      enabled = enabled && canIncrease,
      size = SlotControlSize,
      iconSize = 24.dp,
      shape = CaliuShapes.chip,
      container = container,
    )
  }
}

/** «Copia a altres dies»: botó tonal ample amb la icona de copiar. */
@Composable
fun CopyDaysButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  FilledTonalButton(
    onClick = onClick,
    enabled = enabled,
    colors =
      ButtonDefaults.filledTonalButtonColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
      ),
    modifier =
      modifier
        .fillMaxWidth()
        .height(56.dp),
  ) {
    Icon(painterResource(R.drawable.ic_content_copy), contentDescription = null, modifier = Modifier.size(24.dp))
    Text(text, style = CaliuType.button, modifier = Modifier.padding(start = CaliuSpacing.s))
  }
}

/**
 * Un dia del full «Copia el … a»: cercle de selecció múltiple. Seleccionat, ple de `primary`; si no, amb vora. El dia
 * d'origen surt desactivat.
 */
@Composable
fun CopyDayToggle(
  label: String,
  description: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  val colors = MaterialTheme.colorScheme
  @OptIn(ExperimentalMaterial3ExpressiveApi::class)
  ToggleButton(
    checked = checked,
    onCheckedChange = onCheckedChange,
    enabled = enabled,
    shapes = ToggleButtonShapes(shape = CircleShape, pressedShape = CircleShape, checkedShape = CircleShape),
    colors =
      ToggleButtonDefaults.colors(
        containerColor = Color.Transparent,
        contentColor = colors.onSurface,
        checkedContainerColor = colors.primary,
        checkedContentColor = colors.onPrimary,
        disabledContainerColor = Color.Transparent,
        disabledContentColor = colors.onSurface.copy(alpha = 0.38f),
      ),
    border =
      when {
        checked -> null
        enabled -> BorderStroke(1.dp, colors.outline)
        else -> BorderStroke(1.dp, colors.outlineVariant)
      },
    contentPadding = PaddingValues(0.dp),
    modifier =
      modifier
        .size(46.dp)
        .semantics { contentDescription = description },
  ) {
    Text(label, style = CaliuType.button)
  }
}

@CaliuPreviews
@Composable
private fun WeekProgramPreview() {
  val days = listOf("Dl", "Dt", "Dc", "Dj", "Dv", "Ds", "Dg")
  CaliuPreviewTheme(Mood.OFF) {
    Column(verticalArrangement = Arrangement.spacedBy(CaliuSpacing.section)) {
      DaySelector(
        labels = days,
        descriptions = days,
        selectedIndex = 1,
        todayIndex = 0,
        modifiedIndices = setOf(1, 4),
        onSelect = {},
      )
      DayTimeline(
        segments =
          listOf(
            TimelineSegment(0, 390, 0),
            TimelineSegment(390, 480, 4),
            TimelineSegment(480, 840, 3),
            TimelineSegment(840, 1080, 2),
            TimelineSegment(1080, 1350, 4),
            TimelineSegment(1350, 1440, 0),
          ),
        description = "",
      )
      ProgramCard {
        ProgramSlotRow("06:30", "", {}, "21,0 °C", {}, {}, "Baixa", "Puja")
        ProgramSlotRow("22:30", "", {}, "17,0 °C", {}, {}, "Baixa", "Puja")
      }
      CopyDaysButton("Copia a altres dies", {})
      Row(horizontalArrangement = Arrangement.spacedBy(CaliuSpacing.s)) {
        CopyDayToggle("Dl", "", checked = false, onCheckedChange = {}, enabled = false)
        CopyDayToggle("Dt", "", checked = true, onCheckedChange = {})
        CopyDayToggle("Ds", "", checked = false, onCheckedChange = {})
      }
    }
  }
}
