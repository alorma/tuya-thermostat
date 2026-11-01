package com.alorma.caliu.data.tuya

import org.junit.Assert.assertEquals
import org.junit.Test

/** Exemples de la documentació oficial de Tuya, «Sign requests for Cloud authorization». */
class TuyaSignerTest {
  private val clientId = "1KAD46OrT9HafiKdsXeg"
  private val secret = "4OHBOnWOqaEC1mWXOpVL3yV50s0qGSRC"
  private val timestamp = "1588925778000"
  private val nonce = "5138cc3a9033d69856923fd07b491173"
  private val headers = listOf("area_id" to "29a33e8796834b1efa6", "call_id" to "8afdb70ab2ed11eb85290242ac130003")

  @Test
  fun `signatura de la petició del token`() {
    val sign =
      TuyaSigner.sign(
        clientId = clientId,
        secret = secret,
        timestamp = timestamp,
        method = "GET",
        pathAndQuery = "/v1.0/token?grant_type=1",
        nonce = nonce,
        signedHeaders = headers,
      )
    assertEquals("9E48A3E93B302EEECC803C7241985D0A34EB944F40FB573C7B5C2A82158AF13E", sign)
  }

  @Test
  fun `signatura d'una petició de negoci amb access_token i query desordenada`() {
    val sign =
      TuyaSigner.sign(
        clientId = clientId,
        secret = secret,
        timestamp = timestamp,
        method = "GET",
        pathAndQuery = "/v2.0/apps/schema/users?page_size=50&page_no=1",
        accessToken = "3f4eda2bdec17232f67c0b188af3eec1",
        nonce = nonce,
        signedHeaders = headers,
      )
    assertEquals("AE4481C692AA80B25F3A7E12C3A5FD9BBF6251539DD78E565A1A72A508A88784", sign)
  }

  @Test
  fun `stringToSign sense capçaleres signades deixa una línia buida, com l'app de macOS`() {
    val expected =
      "POST\n" +
        "015abd7f5cc57a2dd94b7590f04ad8084273905ee33ec5cebeae62276a97f862\n" +
        "\n" +
        "/v1.0/devices/x/commands"
    assertEquals(expected, TuyaSigner.stringToSign("POST", "/v1.0/devices/x/commands", "{\"a\":1}".toByteArray()))
  }

  @Test
  fun `la query s'ordena per clau i sense paràmetres queda el camí`() {
    assertEquals("/a?a=1&b=2", TuyaSigner.sortedQuery("/a?b=2&a=1"))
    assertEquals("/a", TuyaSigner.sortedQuery("/a"))
  }
}
