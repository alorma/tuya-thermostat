package com.alorma.caliu.data.tuya

import com.alorma.caliu.data.fake.FakeThermostatRepository
import com.alorma.caliu.domain.MinuteOfDay
import com.alorma.caliu.domain.Mode
import java.time.Instant
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TuyaStateMapperTest {
  private val instant = Instant.parse("2026-11-02T12:47:00Z")
  private val program = "week_program_13_1"
  private val now = MinuteOfDay.of(12, 47)

  private val status: Map<String, JsonElement> =
    mapOf(
      "switch" to JsonPrimitive(true),
      "temp_set" to JsonPrimitive(200),
      "temp_current" to JsonPrimitive(193),
      "lower_temp" to JsonPrimitive(50),
      "upper_temp" to JsonPrimitive(300),
      "mode" to JsonPrimitive("auto"),
    )

  private fun properties(
    mode: String,
    workState: String? = "heating",
  ): Map<String, JsonElement> =
    buildMap {
      put("mode", JsonPrimitive(mode))
      workState?.let { put("work_state", JsonPrimitive(it)) }
      put(program, JsonPrimitive("AQAAAKoGHgDSCAAAyA4AAL4SAADSFh4Aqg=="))
    }

  private fun map(
    status: Map<String, JsonElement>,
    properties: Map<String, JsonElement>,
    program: String,
    now: MinuteOfDay,
  ) = TuyaStateMapper.map(status, properties, program, now, instant)

  @Test
  fun `auto llegeix escala, límits, programa i work_state`() {
    val state = map(status, properties("auto"), program, now)
    assertTrue(state.power)
    assertEquals(Mode.AUTO, state.mode)
    assertEquals(20.0, state.setpoint, 0.0)
    assertEquals(19.3, state.currentTemp!!, 0.0)
    assertEquals(5.0, state.minSetpoint, 0.0)
    assertEquals(30.0, state.maxSetpoint, 0.0)
    assertTrue(state.heating)
    assertNull(state.override)
    assertNull(state.battery)
    assertEquals(FakeThermostatRepository.DefaultSchedule, state.schedule)
  }

  @Test
  fun `home és manual i el mode de v2 mana sobre el de v1`() {
    assertEquals(Mode.MANUAL, map(status, properties("home"), program, now).mode)
  }

  @Test
  fun `temporary és automàtic amb un canvi temporal fins al final del tram`() {
    val state = map(status + ("temp_set" to JsonPrimitive(220)), properties("temporary"), program, now)
    assertEquals(Mode.AUTO, state.mode)
    assertEquals(MinuteOfDay.of(14), state.override?.until)
    assertEquals(22.0, state.override!!.setpoint, 0.0)
  }

  @Test
  fun `stop vol dir que no escalfa`() {
    assertFalse(map(status, properties("auto", workState = "stop"), program, now).heating)
  }

  @Test
  fun `sense work_state es deriva de la temperatura`() {
    assertTrue(map(status, properties("auto", workState = null), program, now).heating)
  }

  @Test
  fun `apagat no té temperatura ni escalfa`() {
    val state = map(status + ("switch" to JsonPrimitive(false)), properties("auto"), program, now)
    assertFalse(state.power)
    assertNull(state.currentTemp)
    assertFalse(state.heating)
  }
}
