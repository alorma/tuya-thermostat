package com.alorma.caliu.designsystem.preview

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.caliu.designsystem.theme.CaliuTheme
import com.alorma.caliu.designsystem.theme.Mood

/** Tema de les previews i dels tests de captures: la paleta del [mood] demanat, en clar o fosc segons la preview. */
@Composable
fun CaliuPreviewTheme(
  mood: Mood = Mood.OFF,
  content: @Composable () -> Unit,
) {
  CaliuTheme(mood = mood, content = content)
}

/** Cada preview es renderitza en clar i en fosc, en català, que és l'idioma del handoff. */
@Preview(name = "Clar", locale = "ca", showBackground = true)
@Preview(
  name = "Fosc",
  locale = "ca",
  showBackground = true,
  uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
annotation class CaliuPreviews
