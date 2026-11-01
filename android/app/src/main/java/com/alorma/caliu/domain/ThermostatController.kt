package com.alorma.caliu.domain

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max

/**
 * Cas d'ús compartit per la pantalla principal i la rajola: llegeix l'estat amb la pausa aplicada i fa les accions
 * del menú d'estat (Activa, Pausa, Apaga) i els canvis de mode i de consigna.
 *
 * Pausa (`disseny/docs/01-especificacio.md` § Pausa): desa `{mode, consigna}`, passa a manual i baixa la consigna a
 * `max(min, floor((actual − 1) × 2) / 2)`. Sortir-ne (Activa o Apaga) torna al mode desat; en manual, també a la
 * consigna desada. En automàtic no cal: el programa ja la torna a posar.
 */
class ThermostatController(
  private val repository: ThermostatRepository,
  private val pauseStore: PauseStore,
) {
  /** Estat del dispositiu amb la pausa aplicada. Si algú ha canviat la consigna des de fora, la pausa s'abandona. */
  suspend fun state(): ThermostatState {
    val device = repository.fetch()
    val pause = pauseStore.get() ?: return device
    val abandoned =
      !device.power || device.mode != Mode.MANUAL || abs(device.setpoint - pause.pausedSetpoint) > TOLERANCE
    if (abandoned) {
      pauseStore.clear()
      return device
    }
    return device.copy(paused = true, mode = pause.mode, setpoint = pause.setpoint, override = null)
  }

  /** [state] és l'estat que es mostra. Si està apagat, l'encén i llegeix la temperatura abans de pausar. */
  suspend fun pause(state: ThermostatState) {
    if (state.paused && state.power) return
    var device = state
    if (!device.power) {
      repository.setPower(true)
      device = repository.fetch()
    }
    val current = device.currentTemp ?: throw ThermostatException.Api("No hi ha temperatura actual per pausar")
    val low = pausedSetpoint(current, device.minSetpoint)
    val restore = if (device.mode == Mode.AUTO) device.override?.setpoint ?: device.setpoint else device.setpoint
    pauseStore.save(SavedPause(mode = device.mode, setpoint = restore, pausedSetpoint = low))
    try {
      if (device.mode != Mode.MANUAL) repository.setMode(Mode.MANUAL)
      repository.setSetpoint(low)
    } catch (e: ThermostatException) {
      pauseStore.clear()
      throw e
    }
  }

  /** Activa: encén si cal i surt de la pausa. */
  suspend fun activate(state: ThermostatState) {
    if (!state.power) repository.setPower(true)
    resume()
  }

  /** Apaga: abans surt de la pausa perquè, en tornar a encendre, la consigna sigui la d'abans. */
  suspend fun turnOff() {
    resume()
    repository.setPower(false)
  }

  /** Alterna la pausa, per a la rajola: en pausa, activa; si no, pausa. */
  suspend fun togglePause() {
    val state = state()
    if (state.paused) activate(state) else pause(state)
  }

  /** En pausa només es canvia el mode al qual es tornarà; si no, el del dispositiu. */
  suspend fun setMode(
    state: ThermostatState,
    mode: Mode,
  ) {
    if (state.paused) {
      pauseStore.get()?.let { pauseStore.save(it.copy(mode = mode)) }
    } else {
      repository.setMode(mode)
    }
  }

  suspend fun setSetpoint(celsius: Double) = repository.setSetpoint(celsius)

  private suspend fun resume() {
    val pause = pauseStore.get() ?: return
    when (pause.mode) {
      Mode.AUTO -> repository.setMode(Mode.AUTO)
      Mode.MANUAL -> repository.setSetpoint(pause.setpoint)
    }
    pauseStore.clear()
  }

  companion object {
    private const val TOLERANCE = 0.01

    /** La consigna de la pausa: mig grau o més per sota de l'actual, en passos de 0,5 i mai per sota del mínim. */
    fun pausedSetpoint(
      current: Double,
      min: Double,
    ): Double = max(min, floor((current - 1) * 2) / 2)
  }
}

/** La consigna que mana ara: en manual, la consigna; en automàtic, el canvi temporal o la del tram actiu. */
fun ThermostatState.targetAt(now: MinuteOfDay): Double =
  when (mode) {
    Mode.MANUAL -> setpoint
    Mode.AUTO -> override?.setpoint ?: schedule.activeSlotIndex(now)?.let { schedule[it].setpoint } ?: setpoint
  }
