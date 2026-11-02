package com.alorma.caliu.tile

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.alorma.caliu.R
import com.alorma.caliu.domain.ThermostatController
import com.alorma.caliu.domain.ThermostatException
import com.alorma.caliu.domain.ThermostatState
import com.alorma.caliu.ui.format.formatTemperature
import com.alorma.caliu.widget.SnapshotStore
import com.alorma.caliu.widget.WidgetUpdater
import com.alorma.caliu.widget.locale
import com.alorma.caliu.widget.statusIcon
import com.alorma.caliu.widget.statusLabel
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Rajola de configuració ràpida (`disseny/docs/01-especificacio.md` § Rajola). Títol «Termòstat» i subtítol
 * «20,4 °C · Escalfant». Tocar-la alterna la pausa (no l'encesa): activa és `STATE_ACTIVE`, en pausa `STATE_INACTIVE` i
 * apagat `STATE_UNAVAILABLE`, perquè la rajola no l'encén.
 *
 * En obrir el panell es pinta amb l'últim estat desat i es torna a llegir el termòstat. Mantenir-la premuda obre l'app
 * (`ACTION_QS_TILE_PREFERENCES` a `MainActivity`): Android no deixa que una rajola de tercers tingui dos punts de toc
 * com la de Dades mòbils.
 *
 * El canvi de pausa corre a l'scope de l'app, no al del servei: si es tanca el panell a mig fer, l'ordre no es talla.
 */
class ThermostatTileService :
  TileService(),
  KoinComponent {
  private val controller: ThermostatController by inject()
  private val snapshots: SnapshotStore by inject()
  private val updater: WidgetUpdater by inject()
  private val appScope: CoroutineScope by inject()

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var job: Job? = null
  private var listening = false

  override fun onStartListening() {
    super.onStartListening()
    listening = true
    job?.cancel()
    job =
      scope.launch {
        // La rajola pinta l'últim estat desat, el mateix que els widgets; la lectura i el canvi de pausa el desen
        launch { snapshots.snapshot.collect(::render) }
        if (toggling?.isActive != true) refresh()
      }
  }

  override fun onStopListening() {
    listening = false
    job?.cancel()
    super.onStopListening()
  }

  override fun onClick() {
    super.onClick()
    if (toggling?.isActive == true) return
    toggling =
      appScope.launch {
        val before = snapshots.get()
        if (before == null || !before.power) return@launch
        // Canvi optimista: la rajola (i els widgets) responen a l'instant; si l'ordre falla es torna enrere
        updater.publish(before.copy(paused = !before.paused, heating = false))
        try {
          controller.togglePause()
        } catch (e: ThermostatException) {
          updater.publish(before)
          return@launch
        }
        // El núvol triga una mica a reflectir el canvi: llegir-lo a l'instant tornaria l'estat d'abans
        delay(SYNC_DELAY)
        refresh()
      }
  }

  override fun onDestroy() {
    scope.cancel()
    super.onDestroy()
  }

  /** Llegeix el termòstat; `controller.state()` ja desa el resultat i actualitza els widgets. */
  private suspend fun refresh() {
    try {
      controller.state()
    } catch (e: ThermostatException) {
      // Sense xarxa o sense configuració: es queda l'últim estat desat
    }
  }

  private fun render(state: ThermostatState?) {
    if (!listening) return
    val tile = qsTile ?: return
    tile.label = getString(R.string.tile_label)
    tile.icon = Icon.createWithResource(this, statusIcon(state))
    tile.state =
      when {
        state == null -> Tile.STATE_INACTIVE
        !state.power -> Tile.STATE_UNAVAILABLE
        state.paused -> Tile.STATE_INACTIVE
        else -> Tile.STATE_ACTIVE
      }
    tile.subtitle = state?.let(::subtitle)
    tile.updateTile()
  }

  /** «20,4 °C · Escalfant», «… · En pausa» o «Apagat». */
  private fun subtitle(state: ThermostatState): String {
    val status = getString(statusLabel(state))
    val current = state.currentTemp?.takeIf { state.power } ?: return status
    val temp = getString(R.string.temp_value, formatTemperature(current, locale()))
    return getString(R.string.tile_subtitle, temp, status)
  }

  private companion object {
    val SYNC_DELAY = 2.seconds

    /** Un sol canvi alhora, encara que el sistema hagi recreat el servei. */
    var toggling: Job? = null
  }
}
