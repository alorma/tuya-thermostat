package com.alorma.caliu.data.tuya

import java.security.MessageDigest
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Signatura de les peticions a Tuya Cloud, tal com la descriu la documentació oficial («Sign requests for Cloud
 * authorization»):
 *
 * ```
 * stringToSign = METHOD \n SHA256(body) \n [clau:valor \n per cada capçalera signada] \n url_amb_query_ordenada
 * str = client_id + [access_token] + t + nonce + stringToSign
 * sign = HMAC-SHA256(secret, str), en majúscules
 * ```
 *
 * Les peticions del token no porten `access_token`.
 */
object TuyaSigner {
  private const val HMAC = "HmacSHA256"

  fun sign(
    clientId: String,
    secret: String,
    timestamp: String,
    method: String,
    pathAndQuery: String,
    body: ByteArray? = null,
    accessToken: String? = null,
    nonce: String = "",
    signedHeaders: List<Pair<String, String>> = emptyList(),
  ): String {
    val message =
      clientId + accessToken.orEmpty() + timestamp + nonce +
        stringToSign(method, pathAndQuery, body, signedHeaders)
    val mac = Mac.getInstance(HMAC).apply { init(SecretKeySpec(secret.toByteArray(), HMAC)) }
    return mac.doFinal(message.toByteArray()).toHex().uppercase(Locale.ROOT)
  }

  fun stringToSign(
    method: String,
    pathAndQuery: String,
    body: ByteArray?,
    signedHeaders: List<Pair<String, String>> = emptyList(),
  ): String {
    val bodyHash = MessageDigest.getInstance("SHA-256").digest(body ?: ByteArray(0)).toHex()
    val headers = signedHeaders.joinToString(separator = "") { (key, value) -> "$key:$value\n" }
    return "${method.uppercase(Locale.ROOT)}\n$bodyHash\n$headers\n${sortedQuery(pathAndQuery)}"
  }

  /** `/a?b=2&a=1` → `/a?a=1&b=2`. Sense paràmetres, el camí tal qual. */
  internal fun sortedQuery(pathAndQuery: String): String {
    val path = pathAndQuery.substringBefore('?')
    val query = pathAndQuery.substringAfter('?', missingDelimiterValue = "")
    if (query.isEmpty()) return path
    val params = query.split('&').filter { it.isNotEmpty() }.sortedBy { it.substringBefore('=') }
    return "$path?${params.joinToString("&")}"
  }

  private fun ByteArray.toHex(): String = joinToString(separator = "") { "%02x".format(it) }
}
