package com.alorma.caliu.testing

import com.alorma.caliu.domain.Mode
import com.alorma.caliu.domain.PauseStore
import com.alorma.caliu.domain.SavedPause
import com.alorma.caliu.domain.ThermostatRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class InMemoryPauseStore(
  var pause: SavedPause? = null,
) : PauseStore {
  override suspend fun get() = pause

  override suspend fun save(pause: SavedPause) {
    this.pause = pause
  }

  override suspend fun clear() {
    pause = null
  }
}

/** Apunta les ordres que arriben al repositori, per comprovar què s'hauria enviat al dispositiu. */
class RecordingRepository(
  private val delegate: ThermostatRepository,
) : ThermostatRepository by delegate {
  val commands = mutableListOf<String>()

  override suspend fun setPower(on: Boolean) {
    delegate.setPower(on)
    commands += "power=$on"
  }

  override suspend fun setSetpoint(celsius: Double) {
    delegate.setSetpoint(celsius)
    commands += "setpoint=$celsius"
  }

  override suspend fun setMode(mode: Mode) {
    delegate.setMode(mode)
    commands += "mode=$mode"
  }
}

/** Rellotge que es pot moure, en UTC. */
class MutableClock(
  var instant: Instant,
) : Clock() {
  override fun getZone(): ZoneId = ZoneOffset.UTC

  override fun withZone(zone: ZoneId?): Clock = this

  override fun instant(): Instant = instant
}
