/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import android.security.keystore.KeyProperties
import android.text.TextUtils
import android.util.Base64
import androidx.preference.PreferenceManager
import com.flowcrypt.email.broadcastreceivers.CorruptedStorageBroadcastReceiver
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.util.exception.ExceptionUtil
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.Key
import java.security.KeyStore
import java.security.PrivateKey
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * This class describes actions which should fix https://github.com/FlowCrypt/flowcrypt-android/issues/854
 *
 * @author Denis Bondarenko
 *         Date: 3/20/20
 *         Time: 11:49 AM
 *         E-mail: DenBond7@gmail.com
 */
class CryptoMigrationUtil {
  class FlowCryptSQLiteOpenHelper(context: Context) : SQLiteOpenHelper(context, FlowCryptRoomDatabase.DB_NAME, null, FlowCryptRoomDatabase.DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase?) {
      //omitted as unused
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
      //omitted as unused
    }
  }

  companion object {
    private const val SIZE_OF_ALGORITHM_PARAMETER_SPEC = 16
    private const val PREFERENCE_KEY_SECRET = "preference_key_secret"
    private const val TRANSFORMATION_TYPE_RSA_ECB_PKCS1_PADDING = "RSA/ECB/PKCS1Padding"
    private const val TRANSFORMATION_AES_CBC_PKCS5_PADDING = "AES/CBC/PKCS5Padding"
    private const val PROVIDER_ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val ANDROID_KEY_STORE_RSA_ALIAS = "flowcrypt_main"

    /**
     * Here we check is "flowcrypt_main" alias exist in AndroidKeyStore. If yes we do the
     * following steps:
     *  * Get the RSA private key from AndroidKeyStore
     *  * Decrypt the AES private key using the RSA private key
     *  * Decrypt Node certs via the AES private key and encrypt it via [KeyStoreCryptoManager.encrypt]
     *  * Decrypt private keys and their passphrases via the AES private key and encrypt them via [KeyStoreCryptoManager.encrypt]
     *  * Decrypt account passwords and uuid via the AES private key and encrypt this info via [KeyStoreCryptoManager.encrypt]
     *  * Remove "flowcrypt_main" alias from AndroidKeyStore
     */
    fun doMigrationIfNeeded(context: Context) {
      val globalContext = context.applicationContext
      val keyStore = KeyStore.getInstance(PROVIDER_ANDROID_KEY_STORE)
      keyStore.load(null)

      val isOldLogicUsed = keyStore.containsAlias(ANDROID_KEY_STORE_RSA_ALIAS)

      if (isOldLogicUsed) {
        try {
          val privateKey = keyStore.getKey(ANDROID_KEY_STORE_RSA_ALIAS, null) as PrivateKey
          val encryptedSecretKey = getSecretKeyFromSharedPreferences(context)
          val decryptedSecretKey = decryptWithRSA(context, encryptedSecretKey, privateKey)
          val secretKey = SecretKeySpec(Base64.decode(decryptedSecretKey, Base64.DEFAULT), KeyProperties.KEY_ALGORITHM_AES)

          val databaseOpenHelper = FlowCryptSQLiteOpenHelper(globalContext)
          val database = databaseOpenHelper.writableDatabase

          updateKeysUsage(database, secretKey)
          updateAccountsUsage(globalContext, database, privateKey)

          databaseOpenHelper.close()

          keyStore.deleteEntry(ANDROID_KEY_STORE_RSA_ALIAS)
        } catch (e: Exception) {
          e.printStackTrace()
          ExceptionUtil.handleError(e)
        }
      }
    }

    private fun updateKeysUsage(db: SQLiteDatabase, secretKey: SecretKeySpec) {
      val longIdColumnName = "long_id"
      val privateKeyColumnName = "private_key"
      val passphraseColumnName = "passphrase"
      val projection = arrayOf(BaseColumns._ID, longIdColumnName, privateKeyColumnName,
          passphraseColumnName)

      val cursor = db.query(
          "keys",
          projection,
          null,
          null,
          null,
          null,
          null
      )

      while (cursor.moveToNext()) {
        val id = cursor.getString(cursor.getColumnIndex(BaseColumns._ID))
        val encryptedPrvKey = cursor.getString(cursor.getColumnIndex(privateKeyColumnName))
        val encryptedPassphrase = cursor.getString(cursor.getColumnIndex(passphraseColumnName))

        val longId = cursor.getString(cursor.getColumnIndex(longIdColumnName))
        val randomVector = normalizeAlgorithmParameterSpecString(longId)

        val decryptedPrvKey = decrypt(encryptedPrvKey, secretKey, randomVector)
        val decryptedPassphrase = decrypt(encryptedPassphrase, secretKey, randomVector)

        val values = ContentValues().apply {
          put(privateKeyColumnName, KeyStoreCryptoManager.encrypt(decryptedPrvKey))
          put(passphraseColumnName, KeyStoreCryptoManager.encrypt(decryptedPassphrase))
        }

        val selection = "${BaseColumns._ID} = ?"
        val selectionArgs = arrayOf(id)
        db.update("keys", values, selection, selectionArgs)
      }

      cursor.close()
    }

    private fun updateAccountsUsage(context: Context, db: SQLiteDatabase, privateKey: Key) {
      val passwordColumnName = "password"
      val smtpPasswordColumnName = "smtp_password"
      val uuidColumnName = "uuid"
      val projection = arrayOf(BaseColumns._ID, passwordColumnName, smtpPasswordColumnName,
          uuidColumnName)

      val cursor = db.query(
          "accounts",
          projection,
          null,
          null,
          null,
          null,
          null
      )

      while (cursor.moveToNext()) {
        val id = cursor.getString(cursor.getColumnIndex(BaseColumns._ID))
        val encryptedPassword = cursor.getString(cursor.getColumnIndex(passwordColumnName))
        val encryptedSmtpPassword = cursor.getString(cursor.getColumnIndex(smtpPasswordColumnName))
        val encryptedUuid = cursor.getString(cursor.getColumnIndex(uuidColumnName))

        val decryptedPassword = decryptWithRSA(context, encryptedPassword, privateKey)
        val decryptedSmtpPassword = decryptWithRSA(context, encryptedSmtpPassword, privateKey)
        val decryptedUuid = decryptWithRSA(context, encryptedUuid, privateKey)

        val values = ContentValues().apply {
          put(passwordColumnName, KeyStoreCryptoManager.encrypt(decryptedPassword))
          put(smtpPasswordColumnName, KeyStoreCryptoManager.encrypt(decryptedSmtpPassword))
          put(uuidColumnName, KeyStoreCryptoManager.encrypt(decryptedUuid))
        }

        val selection = "${BaseColumns._ID} = ?"
        val selectionArgs = arrayOf(id)
        db.update("accounts", values, selection, selectionArgs)
      }

      cursor.close()
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
    private fun decrypt(encryptedData: String?, secretKey: Key, algorithmParameterSpecString: String?): String {
      if (!TextUtils.isEmpty(encryptedData)) {
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
      } else
        return ""
    }

    /**
     * This method decrypts an input encrypted text via RSA and returns decrypted data.
     *
     * @param context       Interface to global information about an application environment;
     * @param encryptedData - The input encrypted text, which must be encrypted and encoded in base64.
     * @return <tt>String</tt> Return decrypt
     * @throws Exception The decryption process can throw a lot of exceptions.
     */
    private fun decryptWithRSA(context: Context, encryptedData: String?, privateKey: Key): String? {
      return if (!TextUtils.isEmpty(encryptedData)) {
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
      } else
        encryptedData
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
     * Normalize an input String to return only 16 bytes for algorithm parameter spec.
     *
     * @param rawString The input String which must be equals or longer then [SIZE_OF_ALGORITHM_PARAMETER_SPEC]
     * @return <tt>String</tt> Return a normalized String.
     */
    private fun normalizeAlgorithmParameterSpecString(rawString: String): String {
      return if (!TextUtils.isEmpty(rawString) && rawString.length >= SIZE_OF_ALGORITHM_PARAMETER_SPEC) {
        rawString.substring(0, SIZE_OF_ALGORITHM_PARAMETER_SPEC)
      } else {
        throw IllegalArgumentException("The rawString must be equals or longer then " +
            SIZE_OF_ALGORITHM_PARAMETER_SPEC + " bytes")
      }
    }
  }
}