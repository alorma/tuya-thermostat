package com.alorma.caliu.data.tuya

import com.alorma.caliu.domain.ThermostatException
import java.io.IOException
import java.time.Clock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
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
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Client de Tuya Cloud OpenAPI sobre Retrofit ([TuyaService]). [TuyaSigningInterceptor] signa cada petició; aquí es
 * guarda el token en memòria i es renova un minut abans que caduqui, o quan Tuya diu que no és vàlid.
 *
 * Llança [TuyaApiException] si Tuya respon amb un error, [ThermostatException.MissingConfig] si no hi ha
 * credencials i [IOException] si falla la xarxa. [TuyaThermostatRepository] ho tradueix a errors de domini.
 */
class TuyaApi(
  client: OkHttpClient,
  private val configProvider: TuyaConfigProvider,
  private val clock: Clock,
  baseUrlOverride: String? = null,
) {
  private val json = Json { ignoreUnknownKeys = true }
  private val service: TuyaService =
    Retrofit
      .Builder()
      // El host real el posa l'interceptor segons la regió de les credencials
      .baseUrl(PLACEHOLDER_BASE_URL)
      .client(client.newBuilder().addInterceptor(TuyaSigningInterceptor(clock, baseUrlOverride)).build())
      .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
      .build()
      .create(TuyaService::class.java)
  private val tokenMutex = Mutex()
  private var token: Token? = null

  private data class Token(
    val accessToken: String,
    val expiresAtMillis: Long,
    val config: TuyaConfig,
  )

  /** `GET /v1.0/devices/{id}/status` → valors per codi. */
  suspend fun status(): Map<String, JsonElement> =
    authenticated { config, auth -> service.status(config.deviceId, auth) }.jsonArray.associateCodes()

  /** `GET /v2.0/cloud/thing/{id}/shadow/properties?codes=…` → valors per codi. */
  suspend fun properties(codes: List<String>): Map<String, JsonElement> {
    val result =
      authenticated { config, auth -> service.properties(config.deviceId, codes.joinToString(","), auth) }
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
    val result = authenticated { config, auth -> service.commands(config.deviceId, body, auth) }
    if ((result as? JsonPrimitive)?.booleanOrNull != true) {
      throw TuyaMalformedResponseException("El termòstat ha rebutjat l'ordre")
    }
  }

  /** `POST …/shadow/properties/issue`: escriu propietats del model complet (per exemple, `mode = home`). */
  suspend fun issueProperties(properties: JsonObject) {
    val body = buildJsonObject { put("properties", properties.toString()) }
    authenticated { config, auth -> service.issueProperties(config.deviceId, body, auth) }
  }

  private suspend fun authenticated(call: suspend (TuyaConfig, TuyaAuth) -> JsonObject): JsonElement {
    val config = requireConfig()
    val first = validToken(config)
    return try {
      unwrap { call(config, TuyaAuth(config, first)) }
    } catch (e: TuyaApiException) {
      if (!e.isTokenInvalid) throw e
      // Token caducat al servidor: se'n demana un de nou i es torna a provar una vegada
      tokenMutex.withLock { if (token?.accessToken == first) token = null }
      unwrap { call(config, TuyaAuth(config, validToken(config))) }
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
      val result = unwrap { service.token(TuyaAuth(config, accessToken = null)) }.jsonObject
      val accessToken =
        result["access_token"]?.jsonPrimitive?.content
          ?: throw TuyaMalformedResponseException("Falta l'access_token")
      val expireSeconds = result["expire_time"]?.jsonPrimitive?.longOrNull ?: DEFAULT_EXPIRE_SECONDS
      token = Token(accessToken, now + expireSeconds * MILLIS_PER_SECOND, config)
      accessToken
    }

  /** Desembolica el sobre de Tuya: `result` si `success`, i si no, [TuyaApiException] amb el codi. */
  private suspend fun unwrap(call: suspend () -> JsonObject): JsonElement {
    val envelope =
      try {
        call()
      } catch (e: HttpException) {
        // Tuya respon els errors amb HTTP 200; un altre codi amb el sobre a dins també es tracta com a error de Tuya
        e
          .response()
          ?.errorBody()
          ?.string()
          ?.let { runCatching { json.parseToJsonElement(it).jsonObject }.getOrNull() }
          ?: throw TuyaMalformedResponseException("Resposta inesperada (HTTP ${e.code()})")
      } catch (e: SerializationException) {
        throw TuyaMalformedResponseException("Resposta inesperada: ${e.message}")
      }
    if (envelope["success"]?.jsonPrimitive?.booleanOrNull != true) {
      throw TuyaApiException(
        code = envelope["code"]?.jsonPrimitive?.intOrNull ?: 0,
        msg = envelope["msg"]?.jsonPrimitive?.content.orEmpty(),
      )
    }
    return envelope["result"] ?: JsonPrimitive(true)
  }

  private fun JsonElement.associateCodes(): Map<String, JsonElement> =
    jsonArray
      .mapNotNull { item ->
        val obj = item.jsonObject
        val code = obj["code"]?.jsonPrimitive?.content ?: return@mapNotNull null
        code to (obj["value"] ?: return@mapNotNull null)
      }.toMap()

  companion object {
    private const val PLACEHOLDER_BASE_URL = "https://openapi.tuyaeu.com/"
    private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    private const val TOKEN_MARGIN_MILLIS = 60_000L
    private const val DEFAULT_EXPIRE_SECONDS = 3_600L
    private const val MILLIS_PER_SECOND = 1_000L
  }
}
