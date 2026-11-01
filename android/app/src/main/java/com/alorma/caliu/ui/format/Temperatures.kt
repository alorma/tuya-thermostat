package com.alorma.caliu.ui.format

import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Temperatura amb un decimal i el separador decimal de l'idioma: «20,4» en català i «20.4» en anglès. Sense unitat:
 * la unitat la posa el recurs `temp_value` o la targeta hero.
 */
fun formatTemperature(
  celsius: Double,
  locale: Locale,
): String =
  NumberFormat
    .getNumberInstance(locale)
    .apply {
      minimumFractionDigits = 1
      maximumFractionDigits = 1
      isGroupingUsed = false
      roundingMode = RoundingMode.HALF_UP
    }.format(celsius)

/** Temperatura de drecera, sense decimals: 21.0 → 21. */
fun presetDegrees(celsius: Double): Int = celsius.roundToInt()
