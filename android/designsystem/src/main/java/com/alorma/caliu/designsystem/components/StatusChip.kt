package com.alorma.caliu.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.alorma.caliu.designsystem.R
import com.alorma.caliu.designsystem.preview.CaliuPreviewTheme
import com.alorma.caliu.designsystem.preview.CaliuPreviews
import com.alorma.caliu.designsystem.theme.CaliuSpacing
import com.alorma.caliu.designsystem.theme.CaliuType
import com.alorma.caliu.designsystem.theme.Mood

/** Una opció del menú d'estat. */
@Immutable
data class StatusMenuItem(
  val label: String,
  @param:DrawableRes val icon: Int,
  val selected: Boolean,
  val onClick: () -> Unit,
)

/**
 * Xip d'estat: un `Button` amb el color d'accent del mood que obre el menú d'estat. El menú té dos grups: [primaryItems]
 * (Activa, Pausa) i, separat i atenuat, [secondaryItems] (Apaga).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatusChip(
  label: String,
  @DrawableRes icon: Int,
  contentDescription: String,
  expanded: Boolean,
  onExpandedChange: (Boolean) -> Unit,
  primaryItems: List<StatusMenuItem>,
  secondaryItems: List<StatusMenuItem>,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier) {
    Button(
      onClick = { onExpandedChange(!expanded) },
      modifier =
        Modifier
          .height(48.dp)
          .semantics { this.contentDescription = contentDescription },
      colors =
        ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.tertiary,
          contentColor = MaterialTheme.colorScheme.onTertiary,
        ),
      contentPadding = PaddingValues(start = 18.dp, end = 14.dp),
    ) {
      Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
      Spacer(Modifier.width(CaliuSpacing.s + CaliuSpacing.xs))
      Text(label, style = CaliuType.statusChip)
      Spacer(Modifier.width(CaliuSpacing.s))
      Icon(
        painterResource(if (expanded) R.drawable.ic_keyboard_arrow_up else R.drawable.ic_keyboard_arrow_down),
        contentDescription = null,
      )
    }

    // Amplada del menú del handoff (captura 02)
    DropdownMenuPopup(
      expanded = expanded,
      onDismissRequest = { onExpandedChange(false) },
      modifier = Modifier.widthIn(min = 216.dp),
    ) {
      val groups = listOf(primaryItems, secondaryItems).filter { it.isNotEmpty() }
      groups.forEachIndexed { groupIndex, items ->
        if (groupIndex > 0) Spacer(Modifier.height(MenuDefaults.GroupSpacing))
        val secondary = groupIndex > 0
        DropdownMenuGroup(shapes = MenuDefaults.groupShape(groupIndex, groups.size)) {
          items.forEachIndexed { index, item ->
            StatusMenuEntry(item, index, items.size, secondary) {
              onExpandedChange(false)
              item.onClick()
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StatusMenuEntry(
  item: StatusMenuItem,
  index: Int,
  count: Int,
  secondary: Boolean,
  onClick: () -> Unit,
) {
  val colors =
    if (secondary) {
      val variant = MaterialTheme.colorScheme.onSurfaceVariant
      MenuDefaults.selectableItemColors(textColor = variant, leadingIconColor = variant)
    } else {
      MenuDefaults.selectableItemColors()
    }
  SelectableDropdownMenuItem(
    selected = item.selected,
    onClick = onClick,
    text = { Text(item.label, style = MaterialTheme.typography.bodyLarge) },
    shapes = MenuDefaults.itemShape(index, count),
    leadingIcon = { Icon(painterResource(item.icon), contentDescription = null) },
    colors = colors,
  )
}

@CaliuPreviews
@Composable
private fun StatusChipPreview() {
  CaliuPreviewTheme(Mood.HEAT) {
    StatusChip(
      label = "Escalfant",
      icon = R.drawable.ic_local_fire_department_filled,
      contentDescription = "Estat: Escalfant. Canvia l'estat",
      expanded = false,
      onExpandedChange = {},
      primaryItems = emptyList(),
      secondaryItems = emptyList(),
    )
  }
}
