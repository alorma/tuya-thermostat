package com.alorma.caliu.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.alorma.caliu.designsystem.R
import com.alorma.caliu.designsystem.preview.CaliuPreviewTheme
import com.alorma.caliu.designsystem.preview.CaliuPreviews
import com.alorma.caliu.designsystem.theme.CaliuSpacing
import com.alorma.caliu.designsystem.theme.CaliuType
import com.alorma.caliu.designsystem.theme.Mood

/** Peu de la pantalla principal: bateria (si n'hi ha) a l'esquerra i hora d'actualització a la dreta. */
@Composable
fun HomeFooter(
  battery: Int?,
  batteryText: String?,
  updatedText: String,
  modifier: Modifier = Modifier,
) {
  val color = MaterialTheme.colorScheme.onSurfaceVariant
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = CaliuSpacing.screen),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(CaliuSpacing.xs),
  ) {
    if (battery != null && batteryText != null) {
      Icon(
        painterResource(batteryIcon(battery)),
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(20.dp),
      )
      Text(batteryText, style = CaliuType.bodySmall, color = color)
    }
    Spacer(Modifier.weight(1f))
    Text(updatedText, style = CaliuType.bodySmall, color = color)
  }
}

private const val BATTERY_FULL = 95
private const val BATTERY_LOW = 10
private const val BATTERY_BARS = 6

/** Icona de bateria de Material Symbols per a un percentatge. */
@DrawableRes
fun batteryIcon(percent: Int): Int =
  when {
    percent >= BATTERY_FULL -> R.drawable.ic_battery_full
    percent < BATTERY_LOW -> R.drawable.ic_battery_alert_filled
    else ->
      when ((percent * (BATTERY_BARS + 1) / 100).coerceIn(0, BATTERY_BARS)) {
        0 -> R.drawable.ic_battery_0_bar
        1 -> R.drawable.ic_battery_1_bar
        2 -> R.drawable.ic_battery_2_bar
        3 -> R.drawable.ic_battery_3_bar
        4 -> R.drawable.ic_battery_4_bar
        5 -> R.drawable.ic_battery_5_bar
        else -> R.drawable.ic_battery_6_bar
      }
  }

@CaliuPreviews
@Composable
private fun HomeFooterPreview() {
  CaliuPreviewTheme(Mood.HEAT) {
    HomeFooter(battery = 87, batteryText = "87 %", updatedText = "Actualitzat a les 12:47")
  }
}
