/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Base64InputStream
import android.util.Base64OutputStream
import androidx.annotation.WorkerThread
import com.flowcrypt.email.util.ProgressOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.source
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

/**
 * This class uses AndroidKeyStore for encrypt/decrypt information.
 * Since version 0.7.4 we use only AES cipher for encryption/decryption which uses
 * AndroidKeyStore for storing keys.
 *
 * Since version 0.9.9 we've removed all old code and made KeyStoreCryptoManager as an object.
 * Also, we've started to use coroutines for encryption/decryption purposes.
 * But for compatibility, we still leave code to use it via a regular approach.
 *
 * See the great articles https://proandroiddev.com/secure-data-in-android-encryption-7eda33e68f58
 *
 * @author Denys Bondarenko
 * @version 3.0 Migrated to use only AES cipher for encryption/decryption which uses
 * AndroidKeyStore for storing keys. Improved and simplified the code.
 */
object KeyStoreCryptoManager {
  const val BASE64_FLAGS = Base64.DEFAULT
  private const val TRANSFORMATION_AES_CBC_PKCS7_PADDING = "AES/CBC/PKCS7Padding"
  private const val PROVIDER_ANDROID_KEY_STORE = "AndroidKeyStore"
  private const val ANDROID_KEY_STORE_AES_ALIAS = "flowcrypt_main_aes"

  private val keyStore: KeyStore = KeyStore.getInstance(PROVIDER_ANDROID_KEY_STORE)
  private var secretKey: SecretKey? = null

  init {
    keyStore.load(null)
    initAESSecretKey()
  }

  /**
   * This method encrypts input bytes via AES symmetric algorithm and returns encrypted bytes.
   * It can be used with coroutines.
   *
   * @param bytes The input bytes which will be encrypted.
   * @return Encrypted bytes (base64)
   * @throws Exception The encryption process can throw a lot of exceptions.
   */
  suspend fun encryptSuspend(bytes: ByteArray?): ByteArray = withContext(Dispatchers.IO) {
    return@withContext encrypt(bytes)
  }

  /**
   * This method encrypts an input text via AES symmetric algorithm and returns encrypted data.
   * It can be used with coroutines.
   *
   * @param plainData The input text which will be encrypted.
   * @return A base64 encoded encrypted result.
   * @throws Exception The encryption process can throw a lot of exceptions.
   */
  suspend fun encryptSuspend(plainData: String?): String = withContext(Dispatchers.IO) {
    return@withContext encrypt(plainData)
  }

  /**
   * This method encrypts an input text via AES symmetric algorithm and returns encrypted data.
   * Don't call it from the main thread.
   *
   * @param plainData The input text which will be encrypted.
   * @return A base64 encoded encrypted result.
   * @throws Exception The encryption process can throw a lot of exceptions.
   */
  @WorkerThread
  fun encrypt(plainData: String?): String {
    val input = (plainData ?: "").toByteArray(StandardCharsets.UTF_8)
    return String(encrypt(input))
  }

  /**
   * This method encrypts input bytes via AES symmetric algorithm and returns encrypted data.
   * Don't call it from the main thread.
   *
   * @param bytes The input bytes which will be encrypted.
   * @return Encrypted bytes (base64)
   * @throws Exception The encryption process can throw a lot of exceptions.
   */
  @WorkerThread
  fun encrypt(bytes: ByteArray?): ByteArray {
    val input = bytes ?: byteArrayOf()
    synchronized(this) {
      val cipher = getCipherForEncryption()
      val encryptedBytes = cipher.doFinal(input)
      return Base64.encode(cipher.iv, BASE64_FLAGS) +
          "\n".toByteArray() +
          Base64.encode(encryptedBytes, BASE64_FLAGS)
    }
  }

  @WorkerThread
  fun getCipherForEncryption(): Cipher {
    return Cipher.getInstance(TRANSFORMATION_AES_CBC_PKCS7_PADDING)
      .apply { init(Cipher.ENCRYPT_MODE, secretKey) }
  }

  /**
   * This method decrypts the input encrypted text via AES symmetric algorithm and returns decrypted data.
   * It can be used with coroutines.
   *
   * @param encryptedData The input encrypted text, which must be encrypted and encoded in base64.
   * @return <tt>String</tt> Return decrypted data.
   * @throws Exception The decryption process can throw a lot of exceptions.
   */
  suspend fun decryptSuspend(encryptedData: String?): String = withContext(Dispatchers.IO) {
    return@withContext decrypt(encryptedData)
  }

  /**
   * This method decrypts the input encrypted bytes via AES symmetric algorithm and returns decrypted bytes.
   * It can be used with coroutines.
   *
   * @param encryptedBytes The input encrypted bytes, which must be encrypted and encoded in base64.
   * @return <tt>String</tt> Return decrypted bytes.
   * @throws Exception The decryption process can throw a lot of exceptions.
   */
  suspend fun decryptSuspend(encryptedBytes: ByteArray?): ByteArray = withContext(Dispatchers.IO) {
    return@withContext decrypt(encryptedBytes)
  }

  /**
   * This method decrypts the input encrypted text via AES symmetric algorithm and returns decrypted data.
   * Don't call it from the main thread.
   *
   * @param encryptedData The input encrypted text, which must be encrypted and encoded in base64.
   * @return <tt>String</tt> Return decrypted data.
   * @throws Exception The decryption process can throw a lot of exceptions.
   */
  @WorkerThread
  fun decrypt(encryptedData: String?): String {
    return String(decrypt(encryptedData?.toByteArray()))
  }

  /**
   * This method decrypts the input encrypted bytes via AES symmetric algorithm and returns decrypted bytes.
   * Don't call it from the main thread.
   *
   * @param encryptedBytes The input encrypted bytes, which must be encrypted and encoded in base64.
   * @return <tt>String</tt> Return decrypted bytes.
   * @throws Exception The decryption process can throw a lot of exceptions.
   */
  @WorkerThread
  fun decrypt(encryptedBytes: ByteArray?): ByteArray {
    val bytes = encryptedBytes ?: return byteArrayOf()
    val splitPosition = bytes.indexOf('\n'.code.toByte())

    if (splitPosition == -1) {
      throw IllegalArgumentException("wrong encryptedData")
    }

    val iv = bytes.slice(0..splitPosition).toByteArray()
    synchronized(this) {
      val cipher = getCipherForDecryption(iv)
      return cipher.doFinal(
        Base64.decode(
          bytes.slice(splitPosition + 1..<bytes.size).toByteArray(),
          BASE64_FLAGS
        )
      )
    }
  }

  @WorkerThread
  fun getCipherForDecryption(iv: String): Cipher {
    return getCipherForDecryption(iv.toByteArray())
  }

  @WorkerThread
  fun getCipherForDecryption(iv: ByteArray): Cipher {
    return Cipher.getInstance(TRANSFORMATION_AES_CBC_PKCS7_PADDING).apply {
      init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(Base64.decode(iv, BASE64_FLAGS)))
    }
  }

  @WorkerThread
  fun getCipherInputStream(inputStream: InputStream): CipherInputStream {
    val bufferedInputStream = BufferedInputStream(inputStream)
    val buffer = Buffer()

    val bufferedInputStreamSource = bufferedInputStream.source()
    while (true) {
      if (bufferedInputStreamSource.read(buffer, 1) != -1L) {
        val b = buffer[buffer.size - 1]
        if (b == (-1).toByte() || b == '\n'.code.toByte()) {
          break
        }
      } else break
    }

    val iv = String(buffer.readByteArray(buffer.size - 1))
    val base64InputStream = Base64InputStream(bufferedInputStream, BASE64_FLAGS)
    return CipherInputStream(base64InputStream, getCipherForDecryption(iv))
  }

  @WorkerThread
  fun encryptOutputStream(
    outputStream: OutputStream,
    action: (cipherOutputStream: CipherOutputStream) -> Unit
  ) {
    val progressOutputStream = ProgressOutputStream(outputStream)
    val cipherForEncryption = getCipherForEncryption()
    val base64OutputStream = Base64OutputStream(progressOutputStream, BASE64_FLAGS)
    val cipherOutputStream = CipherOutputStream(base64OutputStream, cipherForEncryption)
    cipherOutputStream.use {
      progressOutputStream.write(
        Base64.encodeToString(cipherForEncryption.iv, BASE64_FLAGS).toByteArray()
      )
      progressOutputStream.write("\n".toByteArray())
      action.invoke(it)
    }
  }

  /**
   * Do initialization of AES [SecretKey] object.
   *
   * @throws Exception The initialization can throw a lot of exceptions.
   */
  private fun initAESSecretKey() {
    if (!keyStore.containsAlias(ANDROID_KEY_STORE_AES_ALIAS)) {
      genAESSecretKey()
    }
    this.secretKey = keyStore.getKey(ANDROID_KEY_STORE_AES_ALIAS, null) as SecretKey
  }

  /**
   * Generate [SecretKey] using AndroidKeyStore for the AES symmetric algorithm.
   */
  private fun genAESSecretKey() {
    val keyGenerator = KeyGenerator.getInstance(
      KeyProperties.KEY_ALGORITHM_AES,
      PROVIDER_ANDROID_KEY_STORE
    ).apply {
      val purposes = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
      init(
        KeyGenParameterSpec.Builder(ANDROID_KEY_STORE_AES_ALIAS, purposes)
          .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
          .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
          .build()
      )
    }

    keyGenerator.generateKey()
  }
}
