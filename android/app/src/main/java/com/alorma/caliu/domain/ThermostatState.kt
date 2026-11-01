package com.alorma.caliu.domain

import java.time.Instant
import java.time.LocalTime
import java.util.Locale

/**
 * Estat del termòstat tal com el mostra l'app (`disseny/docs/01-especificacio.md` § Model d'estat).
 *
 * Quan [paused] és cert, [mode] i [setpoint] són els valors desats que es recuperaran en sortir de la pausa, no els
 * que té el dispositiu en aquell moment (que està forçat a manual i amb la consigna baixa).
 */
data class ThermostatState(
  val power: Boolean,
  val paused: Boolean,
  val mode: Mode,
  // Temperatura actual en °C. Apagat no n'hi ha
  val currentTemp: Double?,
  // Consigna en °C, en passos de 0,5
  val setpoint: Double,
  // Si la caldera escalfa ara
  val heating: Boolean,
  // Canvi temporal mentre està en automàtic
  val override: Override?,
  // Programa d'avui, ordenat i sense forats de 00:00 a 24:00
  val schedule: List<ScheduleSlot>,
  // Bateria en %, si el dispositiu la dona
  val battery: Int?,
  val lastUpdated: Instant,
  val minSetpoint: Double = DEFAULT_MIN_SETPOINT,
  val maxSetpoint: Double = DEFAULT_MAX_SETPOINT,
) {
  companion object {
    const val DEFAULT_MIN_SETPOINT = 5.0
    const val DEFAULT_MAX_SETPOINT = 35.0
    const val SETPOINT_STEP = 0.5
  }
}

/** `AUTO` segueix el programa setmanal (`auto` i `temporary` a Tuya); `MANUAL` és `home`. */
enum class Mode { AUTO, MANUAL }

/** Canvi temporal de consigna en automàtic, vigent fins al final del tram actiu. */
data class Override(
  val until: MinuteOfDay,
  val setpoint: Double,
)

/** Un tram del programa d'avui: de [start] (inclòs) a [end] (exclòs), amb la consigna [setpoint]. */
data class ScheduleSlot(
  val start: MinuteOfDay,
  val end: MinuteOfDay,
  val setpoint: Double,
) {
  operator fun contains(time: MinuteOfDay): Boolean = time.value >= start.value && time.value < end.value
}

/** Índex del tram que conté [time], o `null` si el programa és buit. */
fun List<ScheduleSlot>.activeSlotIndex(time: MinuteOfDay): Int? = indexOfFirst { time in it }.takeIf { it >= 0 }

/**
 * Minut del dia, de 0 (00:00) a 1440 (24:00). No es fa servir [LocalTime] perquè l'últim tram del programa acaba a les
 * 24:00, que `LocalTime` no pot representar.
 */
@JvmInline
value class MinuteOfDay(
  val value: Int,
) {
  init {
    require(value in 0..MINUTES_PER_DAY) { "Minut fora del dia: $value" }
  }

  val hour: Int get() = value / MINUTES_PER_HOUR
  val minute: Int get() = value % MINUTES_PER_HOUR

  /** «08:00», «24:00». */
  override fun toString(): String = String.format(Locale.ROOT, "%02d:%02d", hour, minute)

  companion object {
    const val MINUTES_PER_HOUR = 60
    const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
    val Midnight = MinuteOfDay(0)
    val EndOfDay = MinuteOfDay(MINUTES_PER_DAY)

    fun of(
      hour: Int,
      minute: Int = 0,
    ) = MinuteOfDay(hour * MINUTES_PER_HOUR + minute)

    fun of(time: LocalTime) = of(time.hour, time.minute)
  }
}
