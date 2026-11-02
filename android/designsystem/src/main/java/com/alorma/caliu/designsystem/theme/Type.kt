package com.alorma.caliu.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.alorma.caliu.designsystem.R

/*
 * Caliu · tipografia. Portat de `docs/disseny/android/Type.kt`.
 * - Roboto Flex (variable) per a tota la interfície. Títols a 800 amb amplada 118 i mida òptica 72.
 * - Nunito 800 només per a xifres de temperatura, sempre amb xifres tabulars.
 */

private const val DEFAULT_WIDTH = 100f
private const val TITLE_WIDTH = 118f
private val TitleOpticalSize = 72.sp
private const val TABULAR = "tnum"

private fun robotoFlex(
  weight: Int,
  width: Float = DEFAULT_WIDTH,
  opticalSize: TextUnit? = null,
) = Font(
  resId = R.font.roboto_flex,
  weight = FontWeight(weight),
  variationSettings =
    FontVariation.Settings(
      *listOfNotNull(
        FontVariation.weight(weight),
        FontVariation.width(width),
        opticalSize?.let { FontVariation.opticalSizing(it) },
      ).toTypedArray(),
    ),
)

val RobotoFlex: FontFamily =
  FontFamily(
    robotoFlex(400),
    robotoFlex(500),
    robotoFlex(600),
    robotoFlex(700),
    robotoFlex(800),
  )

val RobotoFlexWide: FontFamily = FontFamily(robotoFlex(800, TITLE_WIDTH, TitleOpticalSize))

val Nunito: FontFamily =
  FontFamily(
    Font(
      resId = R.font.nunito,
      weight = FontWeight.ExtraBold,
      variationSettings = FontVariation.Settings(FontVariation.weight(800)),
    ),
  )

/** Estils propis de Caliu, fora de l'escala de Material. */
object CaliuType {
  val tempHero = numerals(size = 100, lineHeight = 100, letterSpacing = -3.0)
  val tempValue = numerals(size = 40, lineHeight = 44, letterSpacing = -0.7)
  val tempRow = numerals(size = 18, lineHeight = 24)
  val tempWidget = numerals(size = 34, lineHeight = 36, letterSpacing = -1.0)
  val degreeUnit =
    TextStyle(fontFamily = RobotoFlexWide, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 40.sp)

  val screenTitle = wide(size = 40, lineHeight = 44, letterSpacing = -0.7)
  val stateTitle = wide(size = 26, lineHeight = 30, letterSpacing = -0.3)
  val presetLabel = wide(size = 18, lineHeight = 24)

  val sectionLabel = sans(FontWeight.Bold, size = 14, lineHeight = 20)
  val statusChip = sans(FontWeight.ExtraBold, size = 17, lineHeight = 22)
  val button = sans(FontWeight.Bold, size = 16, lineHeight = 22)
  val bodyLarge = sans(FontWeight.Normal, size = 17, lineHeight = 24)
  val body = sans(FontWeight.Normal, size = 15, lineHeight = 21)
  val row = sans(FontWeight.Medium, size = 14, lineHeight = 20).copy(fontFeatureSettings = TABULAR)
  val bodySmall = sans(FontWeight.Normal, size = 13, lineHeight = 18)
  val overline = sans(FontWeight.Bold, size = 12, lineHeight = 16).copy(letterSpacing = 0.3.sp)
}

private fun numerals(
  size: Int,
  lineHeight: Int,
  letterSpacing: Double = 0.0,
) = TextStyle(
  fontFamily = Nunito,
  fontWeight = FontWeight.ExtraBold,
  fontSize = size.sp,
  lineHeight = lineHeight.sp,
  letterSpacing = letterSpacing.sp,
  fontFeatureSettings = TABULAR,
)

private fun wide(
  size: Int,
  lineHeight: Int,
  letterSpacing: Double = 0.0,
) = TextStyle(
  fontFamily = RobotoFlexWide,
  fontWeight = FontWeight.ExtraBold,
  fontSize = size.sp,
  lineHeight = lineHeight.sp,
  letterSpacing = letterSpacing.sp,
)

private fun sans(
  weight: FontWeight,
  size: Int,
  lineHeight: Int,
) = TextStyle(fontFamily = RobotoFlex, fontWeight = weight, fontSize = size.sp, lineHeight = lineHeight.sp)

/**
 * Escala de Material amb Roboto Flex. Els rols que el disseny fixa (`02-disseny-i-components.md` § Tipografia) prenen
 * els estils de Caliu; la resta són els de M3 amb la família canviada.
 */
internal val CaliuTypography: Typography =
  Typography().let { base ->
    base.copy(
      displayLarge = base.displayLarge.copy(fontFamily = RobotoFlex),
      displayMedium = base.displayMedium.copy(fontFamily = RobotoFlex),
      // Títol desplegat de LargeFlexibleTopAppBar («Configuració»); plegat fa servir titleLarge
      displaySmall = CaliuType.screenTitle,
      headlineLarge = base.headlineLarge.copy(fontFamily = RobotoFlex),
      headlineMedium = base.headlineMedium.copy(fontFamily = RobotoFlex),
      headlineSmall = base.headlineSmall.copy(fontFamily = RobotoFlex),
      titleLarge = base.titleLarge.copy(fontFamily = RobotoFlex),
      titleMedium = base.titleMedium.copy(fontFamily = RobotoFlex),
      titleSmall = base.titleSmall.copy(fontFamily = RobotoFlex),
      bodyLarge = CaliuType.bodyLarge,
      bodyMedium = CaliuType.body,
      bodySmall = CaliuType.bodySmall,
      labelLarge = CaliuType.button,
      labelMedium = base.labelMedium.copy(fontFamily = RobotoFlex),
      labelSmall = base.labelSmall.copy(fontFamily = RobotoFlex),
    )
  }
