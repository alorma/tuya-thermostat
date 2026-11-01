package com.alorma.caliu.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Tema de Caliu: Material 3 Expressive. Provisional fins que arribi el disseny (Claude Design): color dinàmic quan
 * n'hi ha, i l'esquema expressiu per defecte si no.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CaliuTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      darkTheme -> darkColorScheme()
      else -> expressiveLightColorScheme()
    }

  MaterialExpressiveTheme(
    colorScheme = colorScheme,
    motionScheme = MotionScheme.expressive(),
    typography = CaliuTypography,
    content = content,
  )
}
