package com.alorma.caliu.data.tuya

import com.alorma.caliu.domain.ThermostatException
import java.time.Clock
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient

/** «Provar connexió» de Configuració: comprova unes credencials abans de desar-les. */
fun interface ConnectionTester {
  /** `null` si la connexió és correcta; si no, l'error amb el text per a l'usuari. */
  suspend fun test(config: TuyaConfig): ThermostatException?
}

/**
 * Prova les credencials amb una lectura de l'estat (`GET /v1.0/devices/{id}/status`): valida l'Access ID, l'Access
 * Secret, la regió i que el Device ID sigui accessible. Només llegeix, així que no toca mai la calefacció, i es fa
 * contra Tuya encara que la debug faci servir el termòstat simulat.
 */
class TuyaConnectionTester(
  private val client: OkHttpClient,
  private val clock: Clock,
  private val errors: TuyaErrors,
) : ConnectionTester {
  override suspend fun test(config: TuyaConfig): ThermostatException? =
    try {
      TuyaApi(client = client, configProvider = { config }, clock = clock).status()
      null
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      errors.translate(e)
    }
}
