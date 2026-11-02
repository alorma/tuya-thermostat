package com.alorma.caliu.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.alorma.caliu.domain.ThermostatObserver
import com.alorma.caliu.domain.ThermostatState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Desa cada estat llegit a [SnapshotStore] i torna a pintar els dos widgets. La feina va a [scope] (el de l'app),
 * perquè qui llegeix l'estat (la pantalla principal, la rajola…) no l'hagi d'esperar.
 */
class WidgetUpdater(
  private val context: Context,
  private val store: SnapshotStore,
  private val scope: CoroutineScope,
) : ThermostatObserver {
  override fun onState(state: ThermostatState) {
    scope.launch { publish(state) }
  }

  /** Desa [state] i actualitza els widgets ara mateix, per als canvis optimistes des del widget. */
  suspend fun publish(state: ThermostatState) {
    store.save(state)
    updateWidgets(context)
  }

  companion object {
    suspend fun updateWidgets(context: Context) {
      SmallThermostatWidget().updateAll(context)
      MediumThermostatWidget().updateAll(context)
    }
  }
}
