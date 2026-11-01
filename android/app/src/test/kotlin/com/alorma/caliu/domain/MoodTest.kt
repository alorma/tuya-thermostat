package com.alorma.caliu.domain

import com.alorma.caliu.designsystem.theme.Mood
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class MoodTest {
  private val base =
    ThermostatState(
      power = true,
      paused = false,
      mode = Mode.AUTO,
      currentTemp = 19.3,
      setpoint = 20.0,
      heating = true,
      override = null,
      schedule = emptyList(),
      battery = null,
      lastUpdated = Instant.EPOCH,
    )

  @Test
  fun `apagat és OFF encara que escalfi o estigui en pausa`() {
    assertEquals(Mood.OFF, base.copy(power = false).mood())
    assertEquals(Mood.OFF, base.copy(power = false, paused = true).mood())
  }

  @Test
  fun `en pausa és PAUSE encara que escalfi`() {
    assertEquals(Mood.PAUSE, base.copy(paused = true).mood())
    assertEquals(Mood.PAUSE, base.copy(paused = true, heating = false).mood())
  }

  @Test
  fun `encès i escalfant és HEAT`() {
    assertEquals(Mood.HEAT, base.mood())
  }

  @Test
  fun `encès i sense escalfar és CALM`() {
    assertEquals(Mood.CALM, base.copy(heating = false).mood())
  }
}
