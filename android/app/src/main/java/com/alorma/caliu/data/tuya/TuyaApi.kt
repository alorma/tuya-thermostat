package com.alorma.caliu.data.tuya

import com.alorma.caliu.domain.ThermostatException
import java.io.IOException
import java.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Client de Tuya Cloud OpenAPI sobre OkHttp. Signa cada petició amb [TuyaSigner], guarda el token en memòria i el
 * renova un minut abans que caduqui, o quan Tuya diu que no és vàlid.
 *
 * Llança [TuyaApiException] si Tuya respon amb un error, [ThermostatException.MissingConfig] si no hi ha
 * credencials i [IOException] si falla la xarxa. [TuyaThermostatRepository] ho tradueix a errors de domini.
 */
class TuyaApi(
  private val client: OkHttpClient,
  private val configProvider: TuyaConfigProvider,
  private val clock: Clock,
  private val baseUrlOverride: String? = null,
) {
  private val json = Json { ignoreUnknownKeys = true }
  private val tokenMutex = Mutex()
  private var token: Token? = null

  private data class Token(
    val accessToken: String,
    val expiresAtMillis: Long,
    val config: TuyaConfig,
  )

  /** `GET /v1.0/devices/{id}/status` → valors per codi. */
  suspend fun status(): Map<String, JsonElement> {
    val result = authenticated("GET") { "/v1.0/devices/${it.deviceId}/status" }
    return result.jsonArray.associateCodes()
  }

  /** `GET /v2.0/cloud/thing/{id}/shadow/properties?codes=…` → valors per codi. */
  suspend fun properties(codes: List<String>): Map<String, JsonElement> {
    val result =
      authenticated("GET") { "/v2.0/cloud/thing/${it.deviceId}/shadow/properties?codes=${codes.joinToString(",")}" }
    val properties = result.jsonObject["properties"] ?: throw TuyaMalformedResponseException("Falten les propietats")
    return properties.jsonArray.associateCodes()
  }

  /** `POST /v1.0/devices/{id}/commands`, per al conjunt estàndard d'instruccions (`switch`, `temp_set`). */
  suspend fun sendCommand(
    code: String,
    value: JsonPrimitive,
  ) {
    val body =
      buildJsonObject {
        put(
          "commands",
          buildJsonArray {
            add(
              buildJsonObject {
                put("code", code)
                put("value", value)
              },
            )
          },
        )
      }
    val result = authenticated("POST", body.toString()) { "/v1.0/devices/${it.deviceId}/commands" }
    if ((result as? JsonPrimitive)?.booleanOrNull != true) {
      throw TuyaMalformedResponseException("El termòstat ha rebutjat l'ordre")
    }
  }

  /** `POST …/shadow/properties/issue`: escriu propietats del model complet (per exemple, `mode = home`). */
  suspend fun issueProperties(properties: JsonObject) {
    val body = buildJsonObject { put("properties", properties.toString()) }
    authenticated("POST", body.toString()) { "/v2.0/cloud/thing/${it.deviceId}/shadow/properties/issue" }
  }

  private suspend fun authenticated(
    method: String,
    body: String? = null,
    path: (TuyaConfig) -> String,
  ): JsonElement {
    val config = requireConfig()
    val first = validToken(config)
    return try {
      call(config, method, path(config), body, first)
    } catch (e: TuyaApiException) {
      if (!e.isTokenInvalid) throw e
      // Token caducat al servidor: se'n demana un de nou i es torna a provar una vegada
      tokenMutex.withLock { if (token?.accessToken == first) token = null }
      call(config, method, path(config), body, validToken(config))
    }
  }

  private suspend fun requireConfig(): TuyaConfig =
    configProvider.config()?.takeIf { it.isComplete } ?: throw ThermostatException.MissingConfig()

  private suspend fun validToken(config: TuyaConfig): String =
    tokenMutex.withLock {
      val now = clock.millis()
      token
        ?.takeIf { it.config == config && it.expiresAtMillis - TOKEN_MARGIN_MILLIS > now }
        ?.let { return@withLock it.accessToken }
      val result = call(config, "GET", "/v1.0/token?grant_type=1", body = null, accessToken = null).jsonObject
      val accessToken =
        result["access_token"]?.jsonPrimitive?.content
          ?: throw TuyaMalformedResponseException("Falta l'access_token")
      val expireSeconds = result["expire_time"]?.jsonPrimitive?.longOrNull ?: DEFAULT_EXPIRE_SECONDS
      token = Token(accessToken, now + expireSeconds * MILLIS_PER_SECOND, config)
      accessToken
    }

  private suspend fun call(
    config: TuyaConfig,
    method: String,
    pathAndQuery: String,
    body: String?,
    accessToken: String?,
  ): JsonElement =
    withContext(Dispatchers.IO) {
      val timestamp = clock.millis().toString()
      val bodyBytes = body?.toByteArray()
      val sign =
        TuyaSigner.sign(
          clientId = config.accessId,
          secret = config.accessSecret,
          timestamp = timestamp,
          method = method,
          pathAndQuery = pathAndQuery,
          body = bodyBytes,
          accessToken = accessToken,
        )
      val request =
        Request
          .Builder()
          .url((baseUrlOverride ?: config.region.host) + pathAndQuery)
          .method(method, bodyBytes?.toRequestBody(JSON_MEDIA_TYPE))
          .header("client_id", config.accessId)
          .header("sign", sign)
          .header("t", timestamp)
          .header("sign_method", "HMAC-SHA256")
          .apply { if (accessToken != null) header("access_token", accessToken) }
          .build()

      client.newCall(request).execute().use { response ->
        val text = response.body.string()
        val envelope =
          runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()
            ?: throw TuyaMalformedResponseException("Resposta inesperada (HTTP ${response.code})")
        if (envelope["success"]?.jsonPrimitive?.booleanOrNull != true) {
          throw TuyaApiException(
            code = envelope["code"]?.jsonPrimitive?.intOrNull ?: 0,
            msg = envelope["msg"]?.jsonPrimitive?.content.orEmpty(),
          )
        }
        envelope["result"] ?: JsonPrimitive(true)
      }
    }

  private fun JsonElement.associateCodes(): Map<String, JsonElement> =
    jsonArray
      .mapNotNull { item ->
        val obj = item.jsonObject
        val code = obj["code"]?.jsonPrimitive?.content ?: return@mapNotNull null
        code to (obj["value"] ?: return@mapNotNull null)
      }.toMap()

  companion object {
    private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    private const val TOKEN_MARGIN_MILLIS = 60_000L
    private const val DEFAULT_EXPIRE_SECONDS = 3_600L
    private const val MILLIS_PER_SECOND = 1_000L
  }
}
