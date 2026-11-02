package com.alorma.caliu.widget

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.alorma.caliu.domain.MinuteOfDay
import com.alorma.caliu.domain.Mode
import com.alorma.caliu.domain.Override
import com.alorma.caliu.domain.ScheduleSlot
import com.alorma.caliu.domain.ThermostatState
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * L'últim estat conegut del termòstat, per als widgets i la rajola: es pinten sense esperar la xarxa. No hi ha res
 * secret (temperatures i el programa), així que no es xifra.
 */
class SnapshotStore(
  private val dataStore: DataStore<Preferences>,
) {
  private val json = Json { ignoreUnknownKeys = true }

  val snapshot: Flow<ThermostatState?> =
    dataStore.data.map { preferences ->
      preferences[STATE]?.let { value -> runCatching { json.decodeFromString<Stored>(value).toState() }.getOrNull() }
    }

  suspend fun get(): ThermostatState? = snapshot.first()

  suspend fun save(state: ThermostatState) {
    val value = json.encodeToString(Stored.serializer(), Stored.of(state))
    dataStore.edit { it[STATE] = value }
  }

  @Serializable
  private data class Stored(
    val power: Boolean,
    val paused: Boolean,
    val mode: String,
    val currentTemp: Double?,
    val setpoint: Double,
    val heating: Boolean,
    val overrideUntil: Int?,
    val overrideSetpoint: Double?,
    val schedule: List<Slot>,
    val battery: Int?,
    val lastUpdated: Long,
    val minSetpoint: Double,
    val maxSetpoint: Double,
  ) {
    fun toState() =
      ThermostatState(
        power = power,
        paused = paused,
        mode = Mode.valueOf(mode),
        currentTemp = currentTemp,
        setpoint = setpoint,
        heating = heating,
        override =
          if (overrideUntil != null && overrideSetpoint != null) {
            Override(MinuteOfDay(overrideUntil), overrideSetpoint)
          } else {
            null
          },
        schedule = schedule.map { ScheduleSlot(MinuteOfDay(it.start), MinuteOfDay(it.end), it.setpoint) },
        battery = battery,
        lastUpdated = Instant.ofEpochMilli(lastUpdated),
        minSetpoint = minSetpoint,
        maxSetpoint = maxSetpoint,
      )

    companion object {
      fun of(state: ThermostatState) =
        Stored(
          power = state.power,
          paused = state.paused,
          mode = state.mode.name,
          currentTemp = state.currentTemp,
          setpoint = state.setpoint,
          heating = state.heating,
          overrideUntil = state.override?.until?.value,
          overrideSetpoint = state.override?.setpoint,
          schedule = state.schedule.map { Slot(it.start.value, it.end.value, it.setpoint) },
          battery = state.battery,
          lastUpdated = state.lastUpdated.toEpochMilli(),
          minSetpoint = state.minSetpoint,
          maxSetpoint = state.maxSetpoint,
        )
    }
  }

  @Serializable
  private data class Slot(
    val start: Int,
    val end: Int,
    val setpoint: Double,
  )

  companion object {
    const val FILE_NAME = "snapshot"
    private val STATE = stringPreferencesKey("state")
  }
}
