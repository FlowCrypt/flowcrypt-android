/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security

import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.text.TextUtils
import android.util.Base64
import com.flowcrypt.email.broadcastreceivers.CorruptedStorageBroadcastReceiver
import com.flowcrypt.email.util.exception.ManualHandledException
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.PrivateKey
import java.security.ProviderException
import java.security.PublicKey
import java.security.SecureRandom
import java.security.UnrecoverableKeyException
import java.util.*
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * This class uses AndroidKeyStore for encrypt/decrypt information.
 * Since version 0.7.4 we use only AES cipher for encryption/decryption which uses
 * AndroidKeyStore for storing keys.
 *
 *
 * But to support the older versions for compatibility we also use RSA + AES schema.
 * Since encryption which uses the RSA has a limit on the maximum size of the data that can be encrypted
 * ("The RSA algorithm can only encrypt data that has a maximum byte length of the RSA key length in bits
 * divided with eight minus eleven padding bytes, i.e. number of maximum bytes = key length in bits / 8 - 11.", see
 * http://stackoverflow.com/questions/10007147/getting-a-illegalblocksizeexception-data-must-not-be-longer-than-256
 * -bytes-when), we use the following algorithm for option RSA + AES:
 *
 *  * Generate an RSA key pair via `
 *  KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, PROVIDER_ANDROID_KEY_STORE)`
 *  * Generate a 128 bits symmetric key with use [SecureRandom]
 *  * Encrypt and save the symmetric key with the RSA key from Android Keystore System to the shared preferences
 *  * Encrypt the data with the decrypted symmetric key
 *  * Decrypt the data with the decrypted symmetric key
 *
 * See the great articles https://proandroiddev.com/secure-data-in-android-encryption-7eda33e68f58
 *
 * @author DenBond7
 * Date: 12.05.2017
 * Time: 12:29
 * E-mail: DenBond7@gmail.com
 * @version 2.0 Added using AES cipher for encryption/decryption which uses AndroidKeyStore for storing keys.
 */

class KeyStoreCryptoManager
/**
 * This constructor does initialization of symmetric (AES) and asymmetric keys (RSA).
 *
 * @throws Exception Initialization can throw exceptions.
 */
private constructor(context: Context?) {

  private val keyStore: KeyStore
  private var privateKey: PrivateKey? = null
  private var publicKey: PublicKey? = null
  private var secretKey: SecretKey? = null

  private val decryptLock = Any()

  private val isOldLogicUsed: Boolean

  init {
    keyStore = KeyStore.getInstance(PROVIDER_ANDROID_KEY_STORE)
    keyStore.load(null)

    isOldLogicUsed = keyStore.containsAlias(ANDROID_KEY_STORE_RSA_ALIAS)

    if (context == null) {
      throw NullPointerException("The context is null!")
    }

    val appContext = context.applicationContext
    setup(appContext)
  }

  /**
   * This method encrypts an input text via AES symmetric algorithm and returns encrypted data.
   *
   * @param plainData                    The input text which will be encrypted.
   * @param algorithmParameterSpecString The algorithm parameter spec which will be used to randomize encryption.
   * The size must be equal 16 byte. It only used for compatibility with the
   * older versions.
   * @return <tt>String</tt> A base64 encoded encrypted result.
   * @throws Exception The encryption process can throw a lot of exceptions.
   */
  fun encrypt(plainData: String, algorithmParameterSpecString: String?): String {
    if (!TextUtils.isEmpty(plainData)) {
      if (isOldLogicUsed) {
        if (TextUtils.isEmpty(algorithmParameterSpecString)) {
          throw IllegalArgumentException("The algorithm parameter spec must not be null!")
        }

        if (algorithmParameterSpecString!!.length != SIZE_OF_ALGORITHM_PARAMETER_SPEC) {
          throw IllegalArgumentException("The algorithm parameter spec size must be equal "
              + SIZE_OF_ALGORITHM_PARAMETER_SPEC + " bytes!")
        }

        val cipher = Cipher.getInstance(TRANSFORMATION_AES_CBC_PKCS5_PADDING)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(algorithmParameterSpecString.toByteArray()))
        val encryptedBytes = cipher.doFinal(plainData.toByteArray(StandardCharsets.UTF_8))

        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
      } else {
        val cipher = Cipher.getInstance(TRANSFORMATION_AES_CBC_PKCS7_PADDING)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plainData.toByteArray(StandardCharsets.UTF_8))

        return Base64.encodeToString(iv, Base64.DEFAULT) + "\n" + Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
      }
    } else
      return plainData
  }

  /**
   * This method decrypts the input encrypted text via AES symmetric algorithm and returns decrypted data.
   *
   * @param encryptedData                The input encrypted text, which must be encrypted and encoded in base64.
   * @param algorithmParameterSpecString The algorithm parameter spec which will be used to randomize encryption.
   * The size must be equal 16 byte. It only used for compatibility with the
   * older versions.
   * @return <tt>String</tt> Return decrypted data.
   * @throws Exception The decryption process can throw a lot of exceptions.
   */
  fun decrypt(encryptedData: String?, algorithmParameterSpecString: String?): String {
    if (!TextUtils.isEmpty(encryptedData)) {
      if (isOldLogicUsed) {
        if (TextUtils.isEmpty(algorithmParameterSpecString)) {
          throw IllegalArgumentException("The algorithm parameter spec must not be null!")
        }

        if (algorithmParameterSpecString!!.length != SIZE_OF_ALGORITHM_PARAMETER_SPEC) {
          throw IllegalArgumentException("The algorithm parameter spec size must be equal "
              + SIZE_OF_ALGORITHM_PARAMETER_SPEC + " bytes!")
        }

        val cipher = Cipher.getInstance(TRANSFORMATION_AES_CBC_PKCS5_PADDING)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(algorithmParameterSpecString.toByteArray()))
        val decodedBytes = cipher.doFinal(Base64.decode(encryptedData, Base64.DEFAULT))
        return String(decodedBytes, StandardCharsets.UTF_8)
      } else {
        val splitPosition = encryptedData!!.indexOf('\n')

        if (splitPosition == -1) {
          throw IllegalArgumentException("wrong encryptedData")
        }

        val iv = encryptedData.substring(0, splitPosition)
        val cipher = Cipher.getInstance(TRANSFORMATION_AES_CBC_PKCS7_PADDING)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(Base64.decode(iv, Base64.DEFAULT)))
        val decodedBytes = cipher.doFinal(Base64.decode(encryptedData.substring(splitPosition + 1), Base64.DEFAULT))
        return String(decodedBytes, StandardCharsets.UTF_8)
      }
    } else
      return ""
  }

  /**
   * This method encrypts an input text and returns encrypted data.
   * It uses RSA for the older versions (which have such support) or AES.
   *
   * @param plainData The input text which will be encrypted.
   * @return <tt>String</tt> A base64 encoded encrypted result.
   * @throws Exception The encryption process can throw a lot of exceptions.
   */
  fun encryptWithRSAOrAES(plainData: String): String {
    if (!TextUtils.isEmpty(plainData)) {
      if (isOldLogicUsed) {
        val cipher = Cipher.getInstance(TRANSFORMATION_TYPE_RSA_ECB_PKCS1_PADDING)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)

        val plainDataBytes = plainData.toByteArray(StandardCharsets.UTF_8)
        val encryptedBytes = cipher.doFinal(plainDataBytes)

        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
      } else {
        return encrypt(plainData, null)
      }
    } else
      return plainData
  }

  /**
   * This method decrypts an input encrypted text and returns decrypted data.
   * It uses RSA for the older versions (which have such support) or AES.
   *
   * @param context       Interface to global information about an application environment;
   * @param encryptedData - The input encrypted text, which must be encrypted and encoded in base64.
   * @return <tt>String</tt> Return decrypt
   * @throws Exception The decryption process can throw a lot of exceptions.
   */
  fun decryptWithRSAOrAES(context: Context, encryptedData: String?): String? {
    return if (!TextUtils.isEmpty(encryptedData)) {
      if (isOldLogicUsed) {
        synchronized(decryptLock) {
          val cipher = Cipher.getInstance(TRANSFORMATION_TYPE_RSA_ECB_PKCS1_PADDING)
          cipher.init(Cipher.DECRYPT_MODE, privateKey)

          val encryptedBytes = Base64.decode(encryptedData, Base64.DEFAULT)
          val decryptedBytes: ByteArray
          try {
            decryptedBytes = cipher.doFinal(encryptedBytes)
          } catch (e: BadPaddingException) {
            e.printStackTrace()

            val runtimeMsg = "error:04000044:RSA routines:OPENSSL_internal:internal error"
            if (e is RuntimeException && runtimeMsg == e.message) {
              context.sendBroadcast(Intent(context, CorruptedStorageBroadcastReceiver::class.java))
              throw RuntimeException("Storage was corrupted", e)
            }

            val badPaddingMsg = "error:0407109F:rsa routines:RSA_padding_check_PKCS1_type_2:pkcs decoding error"
            if (badPaddingMsg == e.message) {
              context.sendBroadcast(Intent(context, CorruptedStorageBroadcastReceiver::class.java))
              throw GeneralSecurityException("Storage was corrupted", e)
            }

            throw e
          } catch (e: RuntimeException) {
            e.printStackTrace()
            val runtimeMsg = "error:04000044:RSA routines:OPENSSL_internal:internal error"
            if (runtimeMsg == e.message) {
              context.sendBroadcast(Intent(context, CorruptedStorageBroadcastReceiver::class.java))
              throw RuntimeException("Storage was corrupted", e)
            }
            val badPaddingMsg = "error:0407109F:rsa routines:RSA_padding_check_PKCS1_type_2:pkcs decoding error"
            if (e is BadPaddingException && badPaddingMsg == e.message) {
              context.sendBroadcast(Intent(context, CorruptedStorageBroadcastReceiver::class.java))
              throw GeneralSecurityException("Storage was corrupted", e)
            }
            throw e
          }

          return String(decryptedBytes, StandardCharsets.UTF_8)
        }
      } else {
        decrypt(encryptedData, null)
      }
    } else
      encryptedData
  }

  private fun setup(context: Context) {
    if (isOldLogicUsed) {
      initRSAKeys()
      initAESSecretKey(context)
    } else {
      initAESSecretKey(context)
    }
  }

  /**
   * Do initialization of RSA keys.
   *
   * @throws Exception The initialization can throw a lot of exceptions.
   */
  private fun initRSAKeys() {
    try {
      this.privateKey = keyStore.getKey(ANDROID_KEY_STORE_RSA_ALIAS, null) as PrivateKey
    } catch (e: UnrecoverableKeyException) {
      e.printStackTrace()
      throw ManualHandledException("Your device is currently not supported: KeystoreService not available.")
    }

    if (privateKey != null) {
      val certificate = keyStore.getCertificate(ANDROID_KEY_STORE_RSA_ALIAS)
          ?: throw ManualHandledException("Your device is currently not supported: KeystoreService not available.")
      this.publicKey = certificate.publicKey
    }
  }

  /**
   * Do initialization of AES [SecretKey] object.
   *
   * @param context Interface to global information about an application environment;
   * @throws Exception The initialization can throw a lot of exceptions.
   */
  private fun initAESSecretKey(context: Context) {
    if (isOldLogicUsed) {
      initAESSecretKeyFromSharedPreferences(context)
    } else {
      if (!keyStore.containsAlias(ANDROID_KEY_STORE_AES_ALIAS)) {
        genAESSecretKey()
      }

      try {
        this.secretKey = keyStore.getKey(ANDROID_KEY_STORE_AES_ALIAS, null) as SecretKey
      } catch (e: UnrecoverableKeyException) {
        e.printStackTrace()
        throw ManualHandledException("Your device is currently not supported: KeystoreService not available.")
      }

    }
  }

  /**
   * Do initialization of AES [SecretKey] from the shared preferences.
   *
   * @param context Interface to global information about an application environment;
   * @throws Exception The initialization can throw a lot of exceptions.
   */
  private fun initAESSecretKeyFromSharedPreferences(context: Context) {
    val encryptedSecretKey = getSecretKeyFromSharedPreferences(context)
    val decryptedSecretKey = decryptWithRSAOrAES(context, encryptedSecretKey)

    secretKey = SecretKeySpec(Base64.decode(decryptedSecretKey, Base64.DEFAULT), KeyProperties.KEY_ALGORITHM_AES)
  }

  /**
   * Get an encrypted secret key from SharedPreferences.
   *
   * @param context Interface to global information about an application environment;
   * @return <tt>[String]</tt> An encrypted secret key or null if it not found.
   */
  private fun getSecretKeyFromSharedPreferences(context: Context): String? {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    return sharedPreferences.getString(PREFERENCE_KEY_SECRET, null)
  }

  /**
   * Generate [SecretKey] using AndroidKeyStore for the AES symmetric algorithm.
   */
  private fun genAESSecretKey() {

    val keyPairGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,
        PROVIDER_ANDROID_KEY_STORE)

    keyPairGenerator.init(KeyGenParameterSpec.Builder(ANDROID_KEY_STORE_AES_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
        .build())

    try {
      keyPairGenerator.generateKey()
    } catch (e: ProviderException) {
      e.printStackTrace()
      throw ManualHandledException("Your device is currently not supported: KeystoreService not available.")
    }
  }

  companion object {
    private const val SIZE_OF_ALGORITHM_PARAMETER_SPEC = 16
    private const val PREFERENCE_KEY_SECRET = "preference_key_secret"
    private const val TRANSFORMATION_TYPE_RSA_ECB_PKCS1_PADDING = "RSA/ECB/PKCS1Padding"
    private const val TRANSFORMATION_AES_CBC_PKCS5_PADDING = "AES/CBC/PKCS5Padding"
    private const val TRANSFORMATION_AES_CBC_PKCS7_PADDING = "AES/CBC/PKCS7Padding"
    private const val PROVIDER_ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val ANDROID_KEY_STORE_RSA_ALIAS = "flowcrypt_main"
    private const val ANDROID_KEY_STORE_AES_ALIAS = "flowcrypt_main_aes"

    @Volatile
    private var INSTANCE: KeyStoreCryptoManager? = null

    @JvmStatic
    fun getInstance(context: Context): KeyStoreCryptoManager {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: KeyStoreCryptoManager(context).also { INSTANCE = it }
      }
    }

    /**
     * Generate a random 16 byte size String for algorithm parameter spec.
     *
     * @return <tt>String</tt> Return a generated String.
     */
    @JvmStatic
    fun generateAlgorithmParameterSpecString(): String {
      return UUID.randomUUID().toString().substring(0, SIZE_OF_ALGORITHM_PARAMETER_SPEC)
    }

    /**
     * Normalize an input String to return only 16 bytes for algorithm parameter spec.
     *
     * @param rawString The input String which must be equals or longer then
     * [KeyStoreCryptoManager.SIZE_OF_ALGORITHM_PARAMETER_SPEC]
     * @return <tt>String</tt> Return a normalized String.
     */
    @JvmStatic
    fun normalizeAlgorithmParameterSpecString(rawString: String): String {
      return if (!TextUtils.isEmpty(rawString) && rawString.length >= SIZE_OF_ALGORITHM_PARAMETER_SPEC) {
        rawString.substring(0, SIZE_OF_ALGORITHM_PARAMETER_SPEC)
      } else {
        throw IllegalArgumentException("The rawString must be equals or longer then " +
            SIZE_OF_ALGORITHM_PARAMETER_SPEC + " bytes")
      }
    }
  }
}
