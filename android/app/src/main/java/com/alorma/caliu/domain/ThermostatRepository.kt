package com.alorma.caliu.domain

/**
 * Accés al termòstat. La implementació real (Tuya Cloud) arriba a la fase 3; fins aleshores, i a la debug per defecte,
 * es fa servir `FakeThermostatRepository`, que no toca el dispositiu.
 *
 * L'estat que retorna [fetch] sempre té `paused = false`: la pausa és un estat propi de l'app i no del dispositiu.
 * Tots els mètodes llancen [ThermostatException] si fallen.
 */
interface ThermostatRepository {
  suspend fun fetch(): ThermostatState

  suspend fun setPower(on: Boolean)

  /** En manual canvia la consigna; en automàtic crea un canvi temporal fins al final del tram actiu. */
  suspend fun setSetpoint(celsius: Double)

  /** Passar a [Mode.AUTO] també descarta el canvi temporal, si n'hi ha. */
  suspend fun setMode(mode: Mode)
}

sealed class ThermostatException(
  message: String? = null,
  cause: Throwable? = null,
) : Exception(message, cause) {
  /** Error de l'API de Tuya. [message] ja és el text per a l'usuari; [openConsole] diu si cal anar a iot.tuya.com. */
  class Api(
    override val message: String,
    val openConsole: Boolean = false,
    cause: Throwable? = null,
  ) : ThermostatException(message, cause)

  class Offline(
    cause: Throwable? = null,
  ) : ThermostatException(cause = cause)

  class MissingConfig : ThermostatException()
}
