package com.alorma.caliu.data.tuya

import com.alorma.caliu.domain.ProgramEntry
import com.alorma.caliu.domain.WeekProgram
import java.util.Base64

/** L'invers de [WeekProgramDecoder]: 25 bytes, sense byte de recompte. */
object WeekProgramEncoder {
  const val SIZE = 25

  /**
   * [day] és el primer byte (1 = dilluns … 7 = diumenge, com el dispositiu). Cal passar exactament 6 trams en ordre
   * d'hora; si no, es llança `IllegalArgumentException` abans d'enviar res al dispositiu.
   */
  fun encode(
    day: Int,
    entries: List<ProgramEntry>,
  ): String {
    require(day in 1..7) { "Dia fora de la setmana: $day" }
    require(entries.size == WeekProgram.SLOTS_PER_DAY) {
      "Cal tenir ${WeekProgram.SLOTS_PER_DAY} trams i n'hi ha ${entries.size}"
    }
    require(entries.zipWithNext().all { (a, b) -> a.start.value < b.start.value }) { "Trams desordenats: $entries" }
    val bytes = ByteArray(SIZE)
    bytes[0] = day.toByte()
    entries.forEachIndexed { i, entry ->
      val o = 1 + i * 4
      val raw = ThermostatDpMap.toRaw(entry.setpoint)
      require(raw in 0..0xFFFF) { "Temperatura fora de rang: ${entry.setpoint}" }
      bytes[o] = entry.start.hour.toByte()
      bytes[o + 1] = entry.start.minute.toByte()
      bytes[o + 2] = (raw shr 8).toByte()
      bytes[o + 3] = (raw and 0xFF).toByte()
    }
    return Base64.getEncoder().encodeToString(bytes)
  }
}
