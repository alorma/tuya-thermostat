package com.alorma.caliu.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alorma.caliu.designsystem.preview.CaliuPreviewTheme
import com.alorma.caliu.designsystem.preview.CaliuPreviews
import com.alorma.caliu.designsystem.theme.CaliuSpacing
import com.alorma.caliu.designsystem.theme.CaliuType
import com.alorma.caliu.designsystem.theme.Mood

private val OuterCorner = 24.dp
private val InnerCorner = 8.dp

/**
 * Selector de mode (Automàtic / Manual): grup connectat de `ToggleButton` de selecció única. L'opció seleccionada fa
 * servir `primaryContainer`.
 */
@Composable
fun ModeSelector(
  options: List<String>,
  selectedIndex: Int?,
  onSelect: (Int) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  ConnectedToggleGroup(
    options = options,
    selectedIndex = selectedIndex,
    onSelect = onSelect,
    enabled = enabled,
    height = 48.dp,
    textStyle = CaliuType.button,
    checkedContainer = MaterialTheme.colorScheme.primaryContainer,
    checkedContent = MaterialTheme.colorScheme.onPrimaryContainer,
    modifier = modifier,
  )
}

/** Temperatures ràpides (20°, 21°, 23°): la que coincideix amb la consigna queda seleccionada, en `primary`. */
@Composable
fun QuickTemperatures(
  options: List<String>,
  selectedIndex: Int?,
  onSelect: (Int) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  ConnectedToggleGroup(
    options = options,
    selectedIndex = selectedIndex,
    onSelect = onSelect,
    enabled = enabled,
    height = 52.dp,
    textStyle = CaliuType.presetLabel,
    checkedContainer = MaterialTheme.colorScheme.primary,
    checkedContent = MaterialTheme.colorScheme.onPrimary,
    modifier = modifier,
  )
}

/** Regió de Tuya (EU · US · CN · IN) a Configuració: l'opció seleccionada en `primary`. */
@Composable
fun RegionSelector(
  options: List<String>,
  selectedIndex: Int?,
  onSelect: (Int) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  ConnectedToggleGroup(
    options = options,
    selectedIndex = selectedIndex,
    onSelect = onSelect,
    enabled = enabled,
    height = 48.dp,
    textStyle = CaliuType.button,
    checkedContainer = MaterialTheme.colorScheme.primary,
    checkedContent = MaterialTheme.colorScheme.onPrimary,
    modifier = modifier,
  )
}

/**
 * Grup connectat de M3 Expressive: `ToggleButton` amb les formes `ButtonGroupDefaults.connected*ButtonShapes` (24 dp
 * a fora, 8 dp a dins, píndola quan està seleccionat) i 2 dp de separació. És el patró oficial de grup connectat de
 * selecció única; `ButtonGroup` a la 1.5.0-beta01 només té la variant amb desbordament.
 *
 * Desactivat es veu igual que actiu: qui el desactiva decideix com s'atenua.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ConnectedToggleGroup(
  options: List<String>,
  selectedIndex: Int?,
  onSelect: (Int) -> Unit,
  enabled: Boolean,
  height: Dp,
  textStyle: TextStyle,
  checkedContainer: Color,
  checkedContent: Color,
  modifier: Modifier = Modifier,
) {
  val container = MaterialTheme.colorScheme.surfaceContainerHigh
  val content = MaterialTheme.colorScheme.onSurface
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(CaliuSpacing.joint),
  ) {
    options.forEachIndexed { index, label ->
      val checked = index == selectedIndex
      ToggleButton(
        checked = checked,
        onCheckedChange = { onSelect(index) },
        enabled = enabled,
        shapes = connectedShapes(index, options.size),
        colors =
          ToggleButtonDefaults.colors(
            containerColor = container,
            contentColor = content,
            checkedContainerColor = checkedContainer,
            checkedContentColor = checkedContent,
            disabledContainerColor = if (checked) checkedContainer else container,
            disabledContentColor = if (checked) checkedContent else content,
          ),
        modifier =
          Modifier
            .weight(1f)
            .height(height)
            .semantics { role = Role.RadioButton },
      ) {
        Text(label, style = textStyle)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun connectedShapes(
  index: Int,
  count: Int,
): ToggleButtonShapes =
  when {
    count == 1 -> ToggleButtonDefaults.shapesFor(ToggleButtonDefaults.MinHeight)
    index == 0 ->
      ButtonGroupDefaults.connectedLeadingButtonShapes(
        shape = RoundedCornerShape(OuterCorner, InnerCorner, InnerCorner, OuterCorner),
      )
    index == count - 1 ->
      ButtonGroupDefaults.connectedTrailingButtonShapes(
        shape = RoundedCornerShape(InnerCorner, OuterCorner, OuterCorner, InnerCorner),
      )
    else -> ButtonGroupDefaults.connectedMiddleButtonShapes(shape = RoundedCornerShape(InnerCorner))
  }

@CaliuPreviews
@Composable
private fun ConnectedToggleGroupPreview() {
  CaliuPreviewTheme(Mood.CALM) {
    Column(verticalArrangement = Arrangement.spacedBy(CaliuSpacing.section)) {
      ModeSelector(options = listOf("Automàtic", "Manual"), selectedIndex = 1, onSelect = {})
      QuickTemperatures(options = listOf("20°", "21°", "23°"), selectedIndex = 1, onSelect = {})
    }
  }
}
