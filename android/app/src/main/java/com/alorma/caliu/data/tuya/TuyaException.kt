package com.alorma.caliu.data.tuya

/** Resposta de Tuya amb `success: false`. */
class TuyaApiException(
  val code: Int,
  val msg: String,
) : Exception("Tuya ($code): $msg") {
  /** El token ha caducat o no és vàlid: cal demanar-ne un de nou i repetir la petició. */
  val isTokenInvalid: Boolean get() = code == TOKEN_INVALID

  companion object {
    const val TOKEN_INVALID = 1010
  }
}

/** La resposta no té el format esperat. */
class TuyaMalformedResponseException(
  message: String,
) : Exception(message)
