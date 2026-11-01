package com.alorma.caliu.ui.format

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class TemperaturesTest {
  private val catalan = Locale.forLanguageTag("ca")

  @Test
  fun `en català fa servir coma decimal i un decimal`() {
    assertEquals("20,4", formatTemperature(20.4, catalan))
    assertEquals("21,0", formatTemperature(21.0, catalan))
    assertEquals("19,3", formatTemperature(19.25, catalan))
  }

  @Test
  fun `en anglès fa servir punt decimal`() {
    assertEquals("20.4", formatTemperature(20.4, Locale.ENGLISH))
  }

  @Test
  fun `les dreceres no porten decimals`() {
    assertEquals(21, presetDegrees(21.0))
  }
}
