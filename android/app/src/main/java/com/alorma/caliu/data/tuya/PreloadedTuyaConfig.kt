package com.alorma.caliu.data.tuya

import com.alorma.caliu.BuildConfig

/**
 * Credencials precarregades a la debug des de `local.properties` o els GitHub Secrets, que fa servir el
 * `CredentialsStore` mentre no s'hi ha desat res. A la release són buides i, per tant, `null`.
 */
fun preloadedTuyaConfig(): TuyaConfig? =
  TuyaConfig(
    accessId = BuildConfig.TUYA_ACCESS_ID,
    accessSecret = BuildConfig.TUYA_ACCESS_SECRET,
    region = TuyaRegion.parse(BuildConfig.TUYA_REGION),
    deviceId = BuildConfig.TUYA_DEVICE_ID,
  ).takeIf { it.accessId.isNotBlank() || it.accessSecret.isNotBlank() || it.deviceId.isNotBlank() }
