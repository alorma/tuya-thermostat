package com.alorma.caliu.data.tuya

import com.alorma.caliu.data.tuya.TuyaErrors.Kind
import org.junit.Assert.assertEquals
import org.junit.Test

class TuyaErrorsTest {
  @Test
  fun `la prova d'IoT Core caducada es reconeix pel codi o pel missatge`() {
    assertEquals(Kind.TRIAL_EXPIRED, TuyaErrors.kind(28841002, "No permissions"))
    assertEquals(Kind.TRIAL_EXPIRED, TuyaErrors.kind(1, "Your subscription has EXPIRED"))
  }

  @Test
  fun `permisos i credencials`() {
    assertEquals(Kind.NOT_AUTHORIZED, TuyaErrors.kind(1106, "permission deny"))
    assertEquals(Kind.NOT_AUTHORIZED, TuyaErrors.kind(28841105, ""))
    assertEquals(Kind.INVALID_CREDENTIALS, TuyaErrors.kind(1004, "sign invalid"))
    assertEquals(Kind.WRONG_REGION, TuyaErrors.kind(28841107, "No permission. The data center is suspended."))
  }

  @Test
  fun `la resta són errors genèrics`() {
    assertEquals(Kind.OTHER, TuyaErrors.kind(2008, "command or value not support"))
  }
}
