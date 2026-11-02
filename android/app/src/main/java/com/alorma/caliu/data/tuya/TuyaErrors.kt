package com.alorma.caliu.data.tuya

import android.content.Context
import com.alorma.caliu.R
import com.alorma.caliu.domain.ThermostatException
import java.io.IOException

/**
 * Tradueix els errors de Tuya i de xarxa a errors de domini amb el text per a l'usuari, com fa `TuyaError` de l'app
 * de macOS. Els problemes del projecte de Tuya (prova caducada, permisos) porten el botó per anar a iot.tuya.com.
 */
class TuyaErrors(
  private val context: Context,
) {
  fun translate(error: Throwable): ThermostatException =
    when (error) {
      is ThermostatException -> error
      is TuyaApiException -> api(error)
      is TuyaMalformedResponseException ->
        ThermostatException.Api(context.getString(R.string.tuya_unexpected_response), cause = error)
      is IOException -> ThermostatException.Offline(error)
      else -> ThermostatException.Api(error.message ?: error.toString(), cause = error)
    }

  private fun api(error: TuyaApiException): ThermostatException.Api =
    when (kind(error.code, error.msg)) {
      Kind.TRIAL_EXPIRED ->
        ThermostatException.Api(context.getString(R.string.tuya_trial_expired), openConsole = true, cause = error)
      Kind.NOT_AUTHORIZED ->
        ThermostatException.Api(context.getString(R.string.tuya_not_authorized), openConsole = true, cause = error)
      Kind.INVALID_CREDENTIALS ->
        ThermostatException.Api(context.getString(R.string.tuya_invalid_credentials), cause = error)
      Kind.WRONG_REGION ->
        ThermostatException.Api(context.getString(R.string.tuya_wrong_region), cause = error)
      Kind.OTHER ->
        ThermostatException.Api(context.getString(R.string.tuya_error_generic, error.code, error.msg), cause = error)
    }

  enum class Kind { TRIAL_EXPIRED, NOT_AUTHORIZED, INVALID_CREDENTIALS, WRONG_REGION, OTHER }

  companion object {
    private val notAuthorized = setOf(1106, 28841001, 28841105)
    private val invalidCredentials = setOf(1001, 1002, 1004)
    private const val TRIAL_EXPIRED = 28841002

    // «The data center is suspended»: el projecte no és d'aquesta regió
    private const val WRONG_REGION = 28841107

    /** Classificació dels codis de Tuya, la mateixa que `TuyaError.from` de l'app de macOS. */
    fun kind(
      code: Int,
      message: String,
    ): Kind =
      when {
        code == TRIAL_EXPIRED || message.contains("expire", ignoreCase = true) -> Kind.TRIAL_EXPIRED
        code in notAuthorized -> Kind.NOT_AUTHORIZED
        code in invalidCredentials -> Kind.INVALID_CREDENTIALS
        code == WRONG_REGION -> Kind.WRONG_REGION
        else -> Kind.OTHER
      }
  }
}
