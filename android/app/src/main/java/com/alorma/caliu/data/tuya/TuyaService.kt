package com.alorma.caliu.data.tuya

import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Tag

/**
 * Credencials i token d'una petició. Viatgen com a tag de Retrofit fins a [TuyaSigningInterceptor], que signa la
 * petició i hi posa el host de la regió. Sense [accessToken] és la petició del token.
 */
data class TuyaAuth(
  val config: TuyaConfig,
  val accessToken: String?,
)

/**
 * Endpoints de Tuya Cloud OpenAPI que fa servir Caliu. Totes les respostes són el sobre de Tuya
 * (`success`, `code`, `msg`, `result`); [TuyaApi] el desembolica.
 */
interface TuyaService {
  @GET("v1.0/token?grant_type=1")
  suspend fun token(
    @Tag auth: TuyaAuth,
  ): JsonObject

  @GET("v1.0/devices/{deviceId}/status")
  suspend fun status(
    @Path("deviceId") deviceId: String,
    @Tag auth: TuyaAuth,
  ): JsonObject

  // `encoded = true` perquè les comes arribin tal qual: la signatura es calcula sobre la URL que s'envia
  @GET("v2.0/cloud/thing/{deviceId}/shadow/properties")
  suspend fun properties(
    @Path("deviceId") deviceId: String,
    @Query("codes", encoded = true) codes: String,
    @Tag auth: TuyaAuth,
  ): JsonObject

  @POST("v1.0/devices/{deviceId}/commands")
  suspend fun commands(
    @Path("deviceId") deviceId: String,
    @Body body: JsonObject,
    @Tag auth: TuyaAuth,
  ): JsonObject

  @POST("v2.0/cloud/thing/{deviceId}/shadow/properties/issue")
  suspend fun issueProperties(
    @Path("deviceId") deviceId: String,
    @Body body: JsonObject,
    @Tag auth: TuyaAuth,
  ): JsonObject
}
