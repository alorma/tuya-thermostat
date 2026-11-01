package com.alorma.caliu.data.tuya

import com.alorma.caliu.BuildConfig

/**
 * Credencials precarregades a la debug des de `local.properties` o els GitHub Secrets. A la release són buides. Se
 * substitueix pel `CredentialsStore` a la fase 5.
 */
class BuildConfigTuyaConfigProvider : TuyaConfigProvider {
  override suspend fun config(): TuyaConfig? =
    TuyaConfig(
      accessId = BuildConfig.TUYA_ACCESS_ID,
      accessSecret = BuildConfig.TUYA_ACCESS_SECRET,
      region = TuyaRegion.parse(BuildConfig.TUYA_REGION),
      deviceId = BuildConfig.TUYA_DEVICE_ID,
    ).takeIf { it.isComplete }
}
