package com.alorma.caliu.designsystem.preview

import androidx.compose.runtime.Composable
import com.alorma.caliu.designsystem.theme.CaliuTheme

/** Tema de les previews: sense color dinàmic, perquè el resultat no depengui del fons de pantalla. */
@Composable
fun CaliuPreviewTheme(content: @Composable () -> Unit) {
  CaliuTheme(dynamicColor = false, content = content)
}
