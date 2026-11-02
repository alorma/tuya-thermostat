package com.alorma.caliu.data.settings

import android.app.UiModeManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Tema de l'app, com a Caducity i Converses. Per defecte segueix el sistema. */
enum class ThemeMode { LIGHT, DARK, SYSTEM }

interface ThemePreferences {
  val themeMode: Flow<ThemeMode>

  suspend fun setThemeMode(mode: ThemeMode)
}

/**
 * Desa el tema a DataStore i l'aplica amb [UiModeManager.setApplicationNightMode] (Android 12+). El sistema canvia
 * la configuració de l'app (pantalles, barres d'estat, diàlegs) i ho recorda entre execucions, així que la UI només ha
 * de fer servir `isSystemInDarkTheme()`. Els widgets no el segueixen: viuen al launcher i van amb el tema del
 * dispositiu, com la resta de widgets.
 */
class DataStoreThemePreferences(
  private val context: Context,
  private val dataStore: DataStore<Preferences>,
) : ThemePreferences {
  override val themeMode: Flow<ThemeMode> =
    dataStore.data.map { preferences ->
      preferences[THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

  override suspend fun setThemeMode(mode: ThemeMode) {
    dataStore.edit { it[THEME_MODE] = mode.name }
    context.getSystemService(UiModeManager::class.java)?.setApplicationNightMode(
      when (mode) {
        ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
        ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
        ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
      },
    )
  }

  companion object {
    const val FILE_NAME = "preferences"
    private val THEME_MODE = stringPreferencesKey("theme_mode")
  }
}
