package com.alorma.caliu.data.tuya

import com.alorma.caliu.domain.MinuteOfDay
import com.alorma.caliu.domain.Mode
import com.alorma.caliu.domain.Override
import com.alorma.caliu.domain.ScheduleSlot
import com.alorma.caliu.domain.ThermostatState
import com.alorma.caliu.domain.activeSlotIndex
import java.time.Instant
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Converteix els valors de v1 (`/status`) i v2 (`shadow/properties`) en un [ThermostatState]. Si un codi surt a tots
 * dos llocs, mana v2.
 *
 * Mode: `home` → [Mode.MANUAL]; `auto` → [Mode.AUTO]; `temporary` → [Mode.AUTO] amb un [Override] fins al final del
 * tram actiu (el dispositiu hi passa sol quan es toca la consigna en automàtic).
 */
object TuyaStateMapper {
  fun map(
    status: Map<String, JsonElement>,
    properties: Map<String, JsonElement>,
    programCode: String,
    now: MinuteOfDay,
    lastUpdated: Instant,
  ): ThermostatState {
    val values = status + properties

    fun int(code: String) = values[code]?.jsonPrimitive?.intOrNull

    fun string(code: String) = values[code]?.jsonPrimitive?.contentOrNull

    fun celsius(code: String) = int(code)?.let(ThermostatDpMap::toCelsius)

    val power = values[ThermostatDpMap.SWITCH]?.jsonPrimitive?.booleanOrNull ?: false
    val rawMode = string(ThermostatDpMap.MODE)
    val mode = if (rawMode == ThermostatDpMap.MODE_MANUAL) Mode.MANUAL else Mode.AUTO
    val setpoint = celsius(ThermostatDpMap.TEMP_SET) ?: 0.0
    val current = celsius(ThermostatDpMap.TEMP_CURRENT)
    val schedule = string(programCode)?.let(WeekProgramDecoder::decode).orEmpty()
    val workState = string(ThermostatDpMap.WORK_STATE)
    val heating =
      if (workState != null) {
        power && workState == ThermostatDpMap.WORK_STATE_HEATING
      } else {
        power && current != null && current < setpoint
      }

    return ThermostatState(
      power = power,
      paused = false,
      mode = mode,
      currentTemp = current.takeIf { power },
      setpoint = setpoint,
      heating = heating,
      override = if (rawMode == ThermostatDpMap.MODE_TEMPORARY) override(schedule, now, setpoint) else null,
      schedule = schedule,
      battery = int(ThermostatDpMap.BATTERY),
      lastUpdated = lastUpdated,
      minSetpoint = celsius(ThermostatDpMap.LOWER_TEMP) ?: ThermostatState.DEFAULT_MIN_SETPOINT,
      maxSetpoint = celsius(ThermostatDpMap.UPPER_TEMP) ?: ThermostatState.DEFAULT_MAX_SETPOINT,
    )
  }

  private fun override(
    schedule: List<ScheduleSlot>,
    now: MinuteOfDay,
    setpoint: Double,
  ): Override {
    val until = schedule.activeSlotIndex(now)?.let { schedule[it].end } ?: MinuteOfDay.EndOfDay
    return Override(until = until, setpoint = setpoint)
  }
}
