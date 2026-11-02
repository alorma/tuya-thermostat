package com.alorma.caliu.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.alorma.caliu.data.tuya.TuyaConfig
import com.alorma.caliu.data.tuya.TuyaRegion
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreCredentialsStoreTest {
  @get:Rule
  val folder = TemporaryFolder()

  private val scope = CoroutineScope(SupervisorJob())
  private val config = TuyaConfig("my-access-id", "my-very-secret-secret", TuyaRegion.US, "bf-device")
  private val preloaded = TuyaConfig("pre-id", "pre-secret", TuyaRegion.EU, "pre-device")

  private val file: File by lazy { File(folder.root, "credentials.preferences_pb") }
  private val dataStore by lazy { PreferenceDataStoreFactory.create(scope = scope) { file } }

  @After
  fun tearDown() {
    scope.cancel()
  }

  private fun store(
    cipher: SecretCipher = XorCipher(),
    preloaded: TuyaConfig? = null,
  ) = DataStoreCredentialsStore(dataStore, cipher, preloaded)

  @Test
  fun `desa i torna a llegir les credencials`() =
    runTest {
      val store = store()
      store.save(config)

      assertEquals(config, store.credentials.first())
      assertEquals(config, store.config())
    }

  @Test
  fun `el fitxer no conté cap valor en text pla`() =
    runTest {
      store().save(config)

      val bytes = file.readBytes().decodeToString()
      listOf(config.accessId, config.accessSecret, config.deviceId).forEach {
        assertFalse("«$it» surt en text pla", bytes.contains(it))
      }
    }

  @Test
  fun `sense res desat fa servir les precarregades`() =
    runTest {
      assertEquals(preloaded, store(preloaded = preloaded).config())
      assertNull(store().config())
    }

  @Test
  fun `el que es desa té prioritat sobre les precarregades, encara que sigui incomplet`() =
    runTest {
      val store = store(preloaded = preloaded)
      val incomplete = config.copy(accessSecret = "")
      store.save(incomplete)

      assertEquals(incomplete, store.credentials.first())
      assertNull(store.config())
    }

  @Test
  fun `si no es pot desxifrar és com si no hi hagués res`() =
    runTest {
      store().save(config)

      val broken =
        object : SecretCipher {
          override fun encrypt(plain: ByteArray) = plain

          override fun decrypt(encrypted: ByteArray): ByteArray = error("Clau perduda")
        }
      assertNull(store(cipher = broken).config())
    }

  @Test
  fun `avisa cada vegada que es desa, encara que no canviï res`() =
    runTest {
      val store = store()
      val saves = async(start = CoroutineStart.UNDISPATCHED) { store.saved.take(2).toList() }

      store.save(config)
      store.save(config)

      assertEquals(2, saves.await().size)
    }
}

/** Xifrat de joguina per als tests: Android Keystore no existeix a la JVM. */
private class XorCipher : SecretCipher {
  override fun encrypt(plain: ByteArray) = plain.map { (it.toInt() xor KEY).toByte() }.toByteArray()

  override fun decrypt(encrypted: ByteArray) = encrypt(encrypted)

  private companion object {
    const val KEY = 0x5A
  }
}
