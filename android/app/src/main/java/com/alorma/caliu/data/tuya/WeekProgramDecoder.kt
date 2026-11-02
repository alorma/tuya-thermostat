package com.alorma.caliu.data.tuya

import com.alorma.caliu.domain.MinuteOfDay
import com.alorma.caliu.domain.ScheduleSlot
import java.util.Base64

/**
 * Decodifica `week_program_13_N`, portat de `ScheduleSlot.decode` de l'app de macOS. El valor és base64 de 25 bytes:
 * `[dia, (hora, minut, temp×10 en uint16 big-endian) × 6]`. No hi ha byte de recompte: el segon byte ja és l'hora del
 * primer tram (si se'n posa un, tots els camps queden desplaçats i l'app Smart Life mostra trams com `220:13 | 0.0°C`).
 *
 * Cada tram acaba on comença el següent i l'últim a les 24:00. Si el primer no comença a les 00:00, el tram de
 * mitjanit pren la consigna de l'últim (és el que continua del dia anterior).
 */
object WeekProgramDecoder {
  private const val HEADER = 1
  private const val ENTRY = 4

  fun decode(base64: String): List<ScheduleSlot> {
    val bytes = runCatching { Base64.getDecoder().decode(base64.trim()) }.getOrNull() ?: return emptyList()
    if (bytes.size < HEADER) return emptyList()
    val count = (bytes.size - HEADER) / ENTRY
    val starts =
      (0 until count)
        .mapNotNull { i ->
          val o = HEADER + i * ENTRY
          if (o + ENTRY > bytes.size) return@mapNotNull null
          val hour = bytes[o].toUByte().toInt()
          val minute = bytes[o + 1].toUByte().toInt()
          val raw = (bytes[o + 2].toUByte().toInt() shl 8) or bytes[o + 3].toUByte().toInt()
          val time = hour * MinuteOfDay.MINUTES_PER_HOUR + minute
          if (time >= MinuteOfDay.MINUTES_PER_DAY) return@mapNotNull null
          time to ThermostatDpMap.toCelsius(raw)
        }.sortedBy { it.first }
        .distinctBy { it.first }
    if (starts.isEmpty()) return emptyList()

    val withMidnight = if (starts.first().first == 0) starts else listOf(0 to starts.last().second) + starts
    return withMidnight.mapIndexed { index, (start, setpoint) ->
      val end = withMidnight.getOrNull(index + 1)?.first ?: MinuteOfDay.MINUTES_PER_DAY
      ScheduleSlot(MinuteOfDay(start), MinuteOfDay(end), setpoint)
    }
  }
}
