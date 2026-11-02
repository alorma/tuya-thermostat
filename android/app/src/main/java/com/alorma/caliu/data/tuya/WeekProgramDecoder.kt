package com.alorma.caliu.data.tuya

import com.alorma.caliu.domain.MinuteOfDay
import com.alorma.caliu.domain.ProgramEntry
import java.util.Base64

/**
 * Decodifica `week_program_13_N`, portat de `ScheduleSlot.decode` de l'app de macOS. El valor és base64 de 25 bytes:
 * `[dia, (hora, minut, temp×10 en uint16 big-endian) × 6]`. No hi ha byte de recompte: el segon byte ja és l'hora del
 * primer tram (si se'n posa un, tots els camps queden desplaçats i l'app Smart Life mostra trams com `220:13 | 0.0°C`).
 *
 * Retorna els trams del dispositiu tal com són, sense afegir-ne cap a les 00:00: el tram de matinada surt de l'últim
 * tram del dia anterior i el calcula el domini (`daySchedule`).
 */
object WeekProgramDecoder {
  private const val HEADER = 1
  private const val ENTRY = 4

  fun decode(base64: String): List<ProgramEntry> {
    val bytes = runCatching { Base64.getDecoder().decode(base64.trim()) }.getOrNull() ?: return emptyList()
    if (bytes.size < HEADER) return emptyList()
    val count = (bytes.size - HEADER) / ENTRY
    return (0 until count)
      .mapNotNull { i ->
        val o = HEADER + i * ENTRY
        val hour = bytes[o].toUByte().toInt()
        val minute = bytes[o + 1].toUByte().toInt()
        val raw = (bytes[o + 2].toUByte().toInt() shl 8) or bytes[o + 3].toUByte().toInt()
        val time = hour * MinuteOfDay.MINUTES_PER_HOUR + minute
        if (minute >= MinuteOfDay.MINUTES_PER_HOUR || time >= MinuteOfDay.MINUTES_PER_DAY) return@mapNotNull null
        ProgramEntry(MinuteOfDay(time), ThermostatDpMap.toCelsius(raw))
      }.sortedBy { it.start.value }
      .distinctBy { it.start }
  }
}
