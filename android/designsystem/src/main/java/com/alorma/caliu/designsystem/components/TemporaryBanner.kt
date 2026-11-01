package com.alorma.caliu.designsystem.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.alorma.caliu.designsystem.R
import com.alorma.caliu.designsystem.preview.CaliuPreviewTheme
import com.alorma.caliu.designsystem.preview.CaliuPreviews
import com.alorma.caliu.designsystem.theme.CaliuShapes
import com.alorma.caliu.designsystem.theme.CaliuSpacing
import com.alorma.caliu.designsystem.theme.CaliuTheme
import com.alorma.caliu.designsystem.theme.CaliuType
import com.alorma.caliu.designsystem.theme.Mood

/** Bàner «Temporal fins a les HH:MM», sempre amb colors d'escalfar, i el botó per tornar al programa. */
@Composable
fun TemporaryBanner(
  text: String,
  actionLabel: String,
  onAction: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  val colors = CaliuTheme.colors
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = CaliuShapes.control,
    color = colors.temporary,
    contentColor = colors.onTemporary,
  ) {
    Row(
      modifier =
        Modifier
          .heightIn(min = 56.dp)
          .padding(start = 20.dp, end = CaliuSpacing.s, top = CaliuSpacing.s, bottom = CaliuSpacing.s),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(painterResource(R.drawable.ic_back_hand_filled), contentDescription = null)
      Spacer(Modifier.width(CaliuSpacing.section))
      Text(text, style = CaliuType.button, modifier = Modifier.weight(1f))
      TextButton(
        onClick = onAction,
        enabled = enabled,
        colors =
          ButtonDefaults.textButtonColors(
            contentColor = colors.onTemporary,
            disabledContentColor = colors.onTemporary,
          ),
      ) {
        Text(actionLabel, style = CaliuType.button)
      }
    }
  }
}

@CaliuPreviews
@Composable
private fun TemporaryBannerPreview() {
  CaliuPreviewTheme(Mood.HEAT) {
    TemporaryBanner(text = "Temporal fins a les 14:00", actionLabel = "Tornar al programa", onAction = {})
  }
}
