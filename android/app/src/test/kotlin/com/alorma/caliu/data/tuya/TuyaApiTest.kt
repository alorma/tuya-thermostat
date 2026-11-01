package com.alorma.caliu.data.tuya

import com.alorma.caliu.domain.ThermostatException
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TuyaApiTest {
  private val server = MockWebServer()
  private val clock = Clock.fixed(Instant.parse("2026-11-02T12:47:00Z"), ZoneOffset.UTC)
  private val config = TuyaConfig("client", "secret", TuyaRegion.EU, "device")
  private lateinit var api: TuyaApi

  @Before
  fun setUp() {
    server.start()
    api = api { config }
  }

  @After
  fun tearDown() {
    server.close()
  }

  private fun api(provider: TuyaConfigProvider) =
    TuyaApi(OkHttpClient(), provider, clock, baseUrlOverride = server.url("/").toString().trimEnd('/'))

  private fun enqueue(body: String) = server.enqueue(MockResponse.Builder().body(body).build())

  private fun token(value: String = "token1") =
    enqueue("""{"success":true,"result":{"access_token":"$value","expire_time":7200}}""")

  @Test
  fun `demana el token i signa la petició amb ell`() =
    runTest {
      token()
      enqueue("""{"success":true,"result":[{"code":"switch","value":true},{"code":"temp_set","value":200}]}""")

      val status = api.status()

      assertTrue(status.getValue("switch").jsonPrimitive.boolean)
      assertEquals(200, status.getValue("temp_set").jsonPrimitive.int)

      val tokenRequest = server.takeRequest()
      assertEquals("/v1.0/token?grant_type=1", tokenRequest.target)
      assertNull(tokenRequest.headers["access_token"])

      val statusRequest = server.takeRequest()
      assertEquals("/v1.0/devices/device/status", statusRequest.target)
      assertEquals("token1", statusRequest.headers["access_token"])
      assertEquals("client", statusRequest.headers["client_id"])
      assertEquals("HMAC-SHA256", statusRequest.headers["sign_method"])
      val expected =
        TuyaSigner.sign(
          clientId = "client",
          secret = "secret",
          timestamp = clock.millis().toString(),
          method = "GET",
          pathAndQuery = "/v1.0/devices/device/status",
          accessToken = "token1",
        )
      assertEquals(expected, statusRequest.headers["sign"])
    }

  @Test
  fun `reaprofita el token mentre no caduca`() =
    runTest {
      token()
      enqueue("""{"success":true,"result":[]}""")
      enqueue("""{"success":true,"result":[]}""")

      api.status()
      api.status()

      assertEquals(3, server.requestCount)
    }

  @Test
  fun `si el token no és vàlid en demana un de nou i repeteix la petició`() =
    runTest {
      token("old")
      enqueue("""{"success":false,"code":1010,"msg":"token invalid"}""")
      token("new")
      enqueue("""{"success":true,"result":[{"code":"switch","value":false}]}""")

      val status = api.status()

      assertEquals(false, status.getValue("switch").jsonPrimitive.boolean)
      server.takeRequest()
      assertEquals("old", server.takeRequest().headers["access_token"])
      server.takeRequest()
      assertEquals("new", server.takeRequest().headers["access_token"])
    }

  @Test
  fun `les propietats de v2 porten els codis a la query`() =
    runTest {
      token()
      enqueue("""{"success":true,"result":{"properties":[{"code":"mode","value":"home","time":1}]}}""")

      val properties = api.properties(listOf("mode", "work_state"))

      assertEquals("home", properties.getValue("mode").jsonPrimitive.content)
      server.takeRequest()
      assertEquals("/v2.0/cloud/thing/device/shadow/properties?codes=mode,work_state", server.takeRequest().target)
    }

  @Test
  fun `issue envia les propietats com a text JSON`() =
    runTest {
      token()
      enqueue("""{"success":true,"result":{}}""")

      api.issueProperties(kotlinx.serialization.json.buildJsonObject { put("mode", JsonPrimitive("auto")) })

      server.takeRequest()
      val request = server.takeRequest()
      assertEquals("POST", request.method)
      assertEquals("""{"properties":"{\"mode\":\"auto\"}"}""", request.body?.utf8())
    }

  @Test(expected = TuyaApiException::class)
  fun `un error de Tuya es llança amb el codi`() =
    runTest {
      enqueue(
        """{"success":false,"code":28841002,"msg":"No permissions. Your subscription to cloud development plan has expired."}""",
      )
      api.status()
    }

  @Test(expected = ThermostatException.MissingConfig::class)
  fun `sense credencials no es fa cap petició`() =
    runTest {
      api { null }.status()
    }

  @Test(expected = IOException::class)
  fun `si no hi ha connexió es llança IOException`() =
    runTest {
      server.close()
      api.status()
    }
}
