package com.alorma.caliu.designsystem.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

private const val MOOD_TRANSITION_MILLIS = 300

/**
 * Tema de Caliu: Material 3 Expressive sense color dinàmic. La paleta depèn del [mood] del termòstat i segueix el
 * mode clar o fosc del sistema. Quan el mood canvia, cada rol s'interpola en uns 300 ms.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CaliuTheme(
  mood: Mood = Mood.OFF,
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val palette = animatePalette(moodPalette(mood, darkTheme))
  val colorScheme = remember(palette, darkTheme) { colorSchemeOf(palette, darkTheme) }
  val caliuColors = remember(palette, darkTheme) { caliuColorsOf(palette, darkTheme) }

  CompositionLocalProvider(LocalCaliuColors provides caliuColors) {
    MaterialExpressiveTheme(
      colorScheme = colorScheme,
      motionScheme = MotionScheme.expressive(),
      typography = CaliuTypography,
      content = content,
    )
  }
}

/** Accés als rols de Caliu que no són de Material 3: `CaliuTheme.colors.hero`, etc. */
object CaliuTheme {
  val colors: CaliuColors
    @Composable
    @ReadOnlyComposable
    get() = LocalCaliuColors.current
}

@Composable
private fun animatePalette(target: MoodPalette): MoodPalette {
  val spec: AnimationSpec<Color> = tween(MOOD_TRANSITION_MILLIS)

  @Composable
  fun Color.animated(): Color = animateColorAsState(this, spec, label = "mood").value

  return MoodPalette(
    surface = target.surface.animated(),
    card = target.card.animated(),
    cardHigh = target.cardHigh.animated(),
    containerHighest = target.containerHighest.animated(),
    onSurface = target.onSurface.animated(),
    onSurfaceVariant = target.onSurfaceVariant.animated(),
    onSurfacePast = target.onSurfacePast.animated(),
    outline = target.outline.animated(),
    outlineVariant = target.outlineVariant.animated(),
    primary = target.primary.animated(),
    onPrimary = target.onPrimary.animated(),
    primaryContainer = target.primaryContainer.animated(),
    onPrimaryContainer = target.onPrimaryContainer.animated(),
    hero = target.hero.animated(),
    onHero = target.onHero.animated(),
    heroVariant = target.heroVariant.animated(),
    accent = target.accent.animated(),
    onAccent = target.onAccent.animated(),
    dial = target.dial.animated(),
  )
}
