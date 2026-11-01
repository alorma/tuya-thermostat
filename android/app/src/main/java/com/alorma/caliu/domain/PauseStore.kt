package com.alorma.caliu.domain

/**
 * Estat de la pausa, propi de l'app: el [mode] i la [setpoint] als quals es tornarà, i la consigna baixa que es va
 * posar al dispositiu ([pausedSetpoint]). Si el dispositiu ja no té aquesta consigna, és que algú l'ha canviada des de
 * fora i la pausa s'abandona.
 */
data class SavedPause(
  val mode: Mode,
  val setpoint: Double,
  val pausedSetpoint: Double,
)

interface PauseStore {
  suspend fun get(): SavedPause?

  suspend fun save(pause: SavedPause)

  suspend fun clear()
}
