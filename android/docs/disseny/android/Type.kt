package cat.termostat.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Caliu · tipografia.
 * - Roboto Flex (variable) per a tota la interfície. Títols a 800 amb eix d'amplada wdth = 118.
 * - Nunito 800 només per a xifres de temperatura, sempre amb xifres tabulars.
 *
 * Fonts: incloure-les a res/font (Roboto Flex variable i Nunito variable, de Google Fonts, llicència OFL)
 * o fer servir Downloadable Fonts. Substituir els TODO pels Font(...) corresponents.
 */
val RobotoFlex: FontFamily = FontFamily.Default // TODO Font(R.font.roboto_flex, variationSettings = …)
val RobotoFlexWide: FontFamily = FontFamily.Default // TODO Roboto Flex amb FontVariation.Settings(FontVariation.weight(800), FontVariation.width(118f))
val Nunito: FontFamily = FontFamily.Default // TODO Font(R.font.nunito, FontWeight.ExtraBold)

object CaliuType {
    val tempHero = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 100.sp, lineHeight = 100.sp, letterSpacing = (-3).sp, fontFeatureSettings = "tnum")
    val tempValue = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-0.7).sp, fontFeatureSettings = "tnum")
    val tempRow = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, lineHeight = 24.sp, fontFeatureSettings = "tnum")
    val tempWidget = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 36.sp, letterSpacing = (-1).sp, fontFeatureSettings = "tnum")
    val degreeUnit = TextStyle(fontFamily = RobotoFlexWide, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 40.sp)

    val screenTitle = TextStyle(fontFamily = RobotoFlexWide, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-0.7).sp)
    val stateTitle = TextStyle(fontFamily = RobotoFlexWide, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.3).sp)
    val sectionLabel = TextStyle(fontFamily = RobotoFlex, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp)
    val statusChip = TextStyle(fontFamily = RobotoFlex, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, lineHeight = 22.sp)
    val button = TextStyle(fontFamily = RobotoFlex, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp)
    val presetLabel = TextStyle(fontFamily = RobotoFlexWide, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, lineHeight = 24.sp)
    val bodyLarge = TextStyle(fontFamily = RobotoFlex, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 24.sp)
    val body = TextStyle(fontFamily = RobotoFlex, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp)
    val row = TextStyle(fontFamily = RobotoFlex, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, fontFeatureSettings = "tnum")
    val bodySmall = TextStyle(fontFamily = RobotoFlex, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp)
    val overline = TextStyle(fontFamily = RobotoFlex, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp)
}
