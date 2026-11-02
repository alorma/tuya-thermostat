package com.alorma.caliu.ui.schedule

import org.junit.Assert.assertEquals
import org.junit.Test

class TimelineLevelsTest {
  @Test
  fun `amb el programa del handoff surt un nivell per a cada temperatura`() {
    val level = timelineLevels(listOf(17.0, 21.0, 20.0, 19.0, 21.0, 17.0, 21.5))
    assertEquals(listOf(0, 1, 2, 3, 4), listOf(17.0, 19.0, 20.0, 21.0, 21.5).map(level))
  }

  @Test
  fun `amb el programa real les tres temperatures queden ben separades`() {
    val level = timelineLevels(listOf(22.0, 20.5, 22.0, 23.0, 22.0, 20.5))
    assertEquals(listOf(0, 2, 4), listOf(20.5, 22.0, 23.0).map(level))
  }

  @Test
  fun `si totes les temperatures són iguals es fa servir el nivell del mig`() {
    assertEquals(2, timelineLevels(listOf(21.0, 21.0))(21.0))
  }

  @Test
  fun `una temperatura que no és a la llista pren el nivell de la de sota`() {
    val level = timelineLevels(listOf(20.0, 21.0, 22.0))
    assertEquals(2, level(21.5))
    assertEquals(0, level(15.0))
  }
}
