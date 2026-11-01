package com.alorma.caliu.data.tuya

import java.time.DayOfWeek

/**
 * Codis de dades (DP) de l'R11 i la seva escala, tot en un sol lloc. Comprovats amb l'app de macOS i el `CLAUDE.md`
 * de l'arrel.
 */
object ThermostatDpMap {
  // /v1.0/devices/{id}/status i /commands
  const val SWITCH = "switch"
  const val TEMP_SET = "temp_set"
  const val TEMP_CURRENT = "temp_current"
  const val LOWER_TEMP = "lower_temp"
  const val UPPER_TEMP = "upper_temp"
  const val BATTERY = "battery_percentage"

  // Només a v2 (shadow/properties)
  const val MODE = "mode"
  const val WORK_STATE = "work_state"

  // Valors de mode i de work_state
  const val MODE_MANUAL = "home"
  const val MODE_AUTO = "auto"
  const val MODE_TEMPORARY = "temporary"
  const val WORK_STATE_HEATING = "heating"

  // Les temperatures són enters ×10: temp_set 220 = 22,0 °C
  const val TEMP_SCALE = 10.0

  /** Programa setmanal: `week_program_13_1` (dilluns) … `_7` (diumenge). */
  fun weekProgram(day: DayOfWeek): String = "week_program_13_${day.value}"

  fun toCelsius(raw: Int): Double = raw / TEMP_SCALE

  fun toRaw(celsius: Double): Int = Math.round(celsius * TEMP_SCALE).toInt()
}
