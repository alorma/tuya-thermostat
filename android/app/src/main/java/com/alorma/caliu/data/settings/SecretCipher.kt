package com.alorma.caliu.data.settings

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Xifra i desxifra dades petites. Als tests JVM se substitueix per una versió sense Android Keystore. */
interface SecretCipher {
  fun encrypt(plain: ByteArray): ByteArray

  /** Llança una excepció si les dades no es poden desxifrar (clau perduda, dades malmeses). */
  fun decrypt(encrypted: ByteArray): ByteArray
}

/**
 * AES/GCM amb una clau d'Android Keystore, que no surt mai del dispositiu. El resultat és `IV (12 bytes) + xifrat`.
 * Si es restaura una còpia de seguretat en un altre dispositiu, la clau no hi és i no es pot desxifrar: per això el
 * fitxer de credencials s'exclou de les còpies (`backup_rules.xml`, `data_extraction_rules.xml`).
 */
class KeystoreSecretCipher(
  private val alias: String = DEFAULT_ALIAS,
) : SecretCipher {
  override fun encrypt(plain: ByteArray): ByteArray {
    val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
    return cipher.iv + cipher.doFinal(plain)
  }

  override fun decrypt(encrypted: ByteArray): ByteArray {
    require(encrypted.size > IV_SIZE) { "Dades xifrades massa curtes" }
    val iv = encrypted.copyOfRange(0, IV_SIZE)
    val cipher =
      Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv)) }
    return cipher.doFinal(encrypted, IV_SIZE, encrypted.size - IV_SIZE)
  }

  private fun key(): SecretKey {
    val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
    (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
    val spec =
      KeyGenParameterSpec
        .Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(KEY_BITS)
        .build()
    return KeyGenerator
      .getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
      .apply { init(spec) }
      .generateKey()
  }

  private companion object {
    const val KEYSTORE = "AndroidKeyStore"
    const val DEFAULT_ALIAS = "caliu_credentials"
    const val TRANSFORMATION = "AES/GCM/NoPadding"
    const val IV_SIZE = 12
    const val TAG_BITS = 128
    const val KEY_BITS = 256
  }
}
