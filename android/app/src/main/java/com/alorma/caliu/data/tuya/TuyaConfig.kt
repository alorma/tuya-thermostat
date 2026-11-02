package com.alorma.caliu.data.tuya

/** Centre de dades del projecte de Tuya Cloud. */
enum class TuyaRegion(
  val host: String,
) {
  EU("https://openapi.tuyaeu.com"),
  US("https://openapi.tuyaus.com"),
  CN("https://openapi.tuyacn.com"),
  IN("https://openapi.tuyain.com"),
  ;

  companion object {
    /** «eu», «EU»… Si no es reconeix, EU, que és la del termòstat de casa. */
    fun parse(value: String?): TuyaRegion =
      entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: EU
  }
}

data class TuyaConfig(
  val accessId: String,
  val accessSecret: String,
  val region: TuyaRegion,
  val deviceId: String,
) {
  val isComplete: Boolean get() = accessId.isNotBlank() && accessSecret.isNotBlank() && deviceId.isNotBlank()
}

/** D'on surten les credencials: el `CredentialsStore` xifrat, o unes de fixes per a «Provar connexió». */
fun interface TuyaConfigProvider {
  suspend fun config(): TuyaConfig?
}
