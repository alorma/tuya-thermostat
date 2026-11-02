package com.alorma.caliu.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.alorma.caliu.data.tuya.TuyaConfig
import com.alorma.caliu.data.tuya.TuyaConfigProvider
import com.alorma.caliu.data.tuya.TuyaRegion
import java.util.Base64
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Les credencials de Tuya que es desen des de Configuració. */
interface CredentialsStore : TuyaConfigProvider {
  /** El que hi ha desat, encara que estigui incomplet; `null` si no s'ha desat mai res. */
  val credentials: Flow<TuyaConfig?>

  /** Emet cada vegada que es desa, encara que els valors no canviïn: la pantalla principal es refresca. */
  val saved: Flow<Unit>

  suspend fun save(config: TuyaConfig)

  /** Només les credencials completes: si en falta alguna, l'app mostra «Falta la configuració». */
  override suspend fun config(): TuyaConfig? = credentials.first()?.takeIf { it.isComplete }
}

/**
 * [CredentialsStore] sobre DataStore. Tota la configuració es desa com un sol valor xifrat amb [cipher] (en base64),
 * així que ni l'Access Secret ni la resta surten en text pla al fitxer.
 *
 * Mentre no s'ha desat mai res, es fan servir les credencials [preloaded] (a la debug, les de `local.properties` o els
 * GitHub Secrets). Si el valor desat no es pot desxifrar (per exemple, s'ha perdut la clau), és com si no n'hi hagués.
 */
class DataStoreCredentialsStore(
  private val dataStore: DataStore<Preferences>,
  private val cipher: SecretCipher,
  private val preloaded: TuyaConfig? = null,
) : CredentialsStore {
  private val _saved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
  override val saved: Flow<Unit> = _saved.asSharedFlow()

  override val credentials: Flow<TuyaConfig?> =
    dataStore.data.map { preferences ->
      val stored = preferences[CONFIG] ?: return@map preloaded
      runCatching { decode(stored) }.getOrNull()
    }

  override suspend fun save(config: TuyaConfig) {
    val encoded = encode(config)
    dataStore.edit { it[CONFIG] = encoded }
    _saved.emit(Unit)
  }

  private fun encode(config: TuyaConfig): String {
    val plain =
      Json.encodeToString(
        StoredConfig.serializer(),
        StoredConfig(config.accessId, config.accessSecret, config.region.name, config.deviceId),
      )
    return Base64.getEncoder().encodeToString(cipher.encrypt(plain.toByteArray()))
  }

  private fun decode(value: String): TuyaConfig {
    val plain = cipher.decrypt(Base64.getDecoder().decode(value)).decodeToString()
    val stored = Json.decodeFromString(StoredConfig.serializer(), plain)
    return TuyaConfig(stored.accessId, stored.accessSecret, TuyaRegion.parse(stored.region), stored.deviceId)
  }

  @Serializable
  private data class StoredConfig(
    val accessId: String,
    val accessSecret: String,
    val region: String,
    val deviceId: String,
  )

  companion object {
    /** Nom del fitxer de DataStore, que s'exclou de les còpies de seguretat. */
    const val FILE_NAME = "credentials"
    private val CONFIG = stringPreferencesKey("config")
  }
}
