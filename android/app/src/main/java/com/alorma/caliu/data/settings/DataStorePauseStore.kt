package com.alorma.caliu.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.alorma.caliu.domain.Mode
import com.alorma.caliu.domain.PauseStore
import com.alorma.caliu.domain.SavedPause
import kotlinx.coroutines.flow.first

private val Context.pauseDataStore: DataStore<Preferences> by preferencesDataStore(name = "pause")

/** La pausa desada a DataStore, perquè sobrevisqui si es tanca l'app. */
class DataStorePauseStore(
  context: Context,
) : PauseStore {
  private val dataStore = context.pauseDataStore

  override suspend fun get(): SavedPause? {
    val preferences = dataStore.data.first()
    val mode = preferences[MODE]?.let { runCatching { Mode.valueOf(it) }.getOrNull() } ?: return null
    return SavedPause(
      mode = mode,
      setpoint = preferences[SETPOINT] ?: return null,
      pausedSetpoint = preferences[PAUSED_SETPOINT] ?: return null,
    )
  }

  override suspend fun save(pause: SavedPause) {
    dataStore.edit {
      it[MODE] = pause.mode.name
      it[SETPOINT] = pause.setpoint
      it[PAUSED_SETPOINT] = pause.pausedSetpoint
    }
  }

  override suspend fun clear() {
    dataStore.edit { it.clear() }
  }

  private companion object {
    val MODE = stringPreferencesKey("mode")
    val SETPOINT = doublePreferencesKey("setpoint")
    val PAUSED_SETPOINT = doublePreferencesKey("paused_setpoint")
  }
}
