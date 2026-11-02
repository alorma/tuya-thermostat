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
import com.alorma.caliu.widget.locale
import com.alorma.caliu.widget.statusIcon
import com.alorma.caliu.widget.statusLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Rajola de configuració ràpida (`disseny/docs/01-especificacio.md` § Rajola). Títol «Termòstat» i subtítol
 * «20,4 °C · Escalfant». Tocar-la alterna la pausa (no l'encesa): activa és `STATE_ACTIVE`, en pausa `STATE_INACTIVE` i
 * apagat `STATE_UNAVAILABLE`, perquè la rajola no l'encén.
 *
 * En obrir el panell es pinta amb l'últim estat desat i es torna a llegir el termòstat.
 */
class ThermostatTileService :
  TileService(),
  KoinComponent {
  private val controller: ThermostatController by inject()
  private val snapshots: SnapshotStore by inject()

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var job: Job? = null

  override fun onStartListening() {
    super.onStartListening()
    job?.cancel()
    job =
      scope.launch {
        render(snapshots.get())
        render(read() ?: return@launch)
      }
  }

  override fun onStopListening() {
    job?.cancel()
    super.onStopListening()
  }

  override fun onClick() {
    super.onClick()
    job?.cancel()
    job =
      scope.launch {
        val before = snapshots.get()
        // Canvi optimista: el panell respon a l'instant, i si l'ordre falla es torna a pintar el que hi hagi
        before?.takeIf { it.power }?.let { render(it.copy(paused = !it.paused, heating = false)) }
        try {
          controller.togglePause()
        } catch (e: ThermostatException) {
          render(before)
        }
        read()?.let { render(it) }
      }
  }

  override fun onDestroy() {
    scope.cancel()
    super.onDestroy()
  }

  private suspend fun read(): ThermostatState? =
    try {
      controller.state()
    } catch (e: ThermostatException) {
      null
    }

  private fun render(state: ThermostatState?) {
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
}
