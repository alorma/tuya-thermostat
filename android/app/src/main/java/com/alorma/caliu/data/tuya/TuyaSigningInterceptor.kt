package com.alorma.caliu.data.tuya

import java.time.Clock
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer

/**
 * Signa amb [TuyaSigner] les peticions que porten un [TuyaAuth] i les envia al host de la regió de les credencials
 * (o a [baseUrlOverride], als tests). La signatura es fa sobre el camí, la query i el cos tal com surten.
 */
class TuyaSigningInterceptor(
  private val clock: Clock,
  private val baseUrlOverride: String? = null,
) : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    val auth = request.tag(TuyaAuth::class.java) ?: return chain.proceed(request)
    val config = auth.config

    val base = (baseUrlOverride ?: config.region.host).toHttpUrl()
    val url =
      request.url
        .newBuilder()
        .scheme(base.scheme)
        .host(base.host)
        .port(base.port)
        .build()
    val pathAndQuery = url.encodedPath + (url.encodedQuery?.let { "?$it" } ?: "")
    val body = request.body?.let { body -> Buffer().also { body.writeTo(it) }.readByteArray() }
    val timestamp = clock.millis().toString()
    val sign =
      TuyaSigner.sign(
        clientId = config.accessId,
        secret = config.accessSecret,
        timestamp = timestamp,
        method = request.method,
        pathAndQuery = pathAndQuery,
        body = body,
        accessToken = auth.accessToken,
      )

    val signed =
      request
        .newBuilder()
        .url(url)
        .header("client_id", config.accessId)
        .header("sign", sign)
        .header("t", timestamp)
        .header("sign_method", "HMAC-SHA256")
        .apply { auth.accessToken?.let { header("access_token", it) } }
        .build()
    return chain.proceed(signed)
  }
}
