/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   DenBond7
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.security

import android.content.Context
import com.flowcrypt.email.R
import com.flowcrypt.email.core.msg.RawBlockParser
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.armor
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.toPgpKeyDetails
import com.flowcrypt.email.extensions.org.pgpainless.key.info.usableForEncryption
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.security.pgp.PgpArmor
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.security.pgp.PgpPwd
import com.flowcrypt.email.util.exception.DifferentPassPhrasesException
import com.flowcrypt.email.util.exception.EmptyPassphraseException
import com.flowcrypt.email.util.exception.NoKeyAvailableException
import com.flowcrypt.email.util.exception.NoPrivateKeysAvailableException
import com.flowcrypt.email.util.exception.PrivateKeyStrengthException
import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.pgpainless.key.OpenPgpV4Fingerprint
import org.pgpainless.key.info.KeyRingInfo
import org.pgpainless.util.Passphrase
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

/**
 * This class help to receive security information.
 *
 * @author Denys Bondarenko
 */
class SecurityUtils {
  companion object {
    /**
     * Generate a new name for the private key which will be exported.
     *
     * @param email The user email.
     * @return A generated name for a new file.
     */
    @JvmStatic
    fun genPrivateKeyName(email: String): String {
      val sanitizedEmail = email.replace("[^a-z0-9]".toRegex(), "")
      return "flowcrypt-backup-$sanitizedEmail.key"
    }

    /**
     * Generate a private keys backup for the given account.
     *
     * @param context Interface to global information about an application environment.
     * @param account The given account
     * @return A string which includes private keys
     */
    fun genPrivateKeysBackup(context: Context, account: AccountEntity): String {
      val builder = StringBuilder()
      val keysStorage = KeysStorageImpl.getInstance(context.applicationContext)
      val keys = keysStorage.getPGPSecretKeyRings()

      if (keys.isEmpty()) {
        throw NoPrivateKeysAvailableException(context, account.email)
      }

      val fingerprintsWithEmptyPassphrase = keysStorage.getFingerprintsWithEmptyPassphrase()
      if (fingerprintsWithEmptyPassphrase.isNotEmpty()) {
        throw EmptyPassphraseException(
          fingerprints = fingerprintsWithEmptyPassphrase,
          message = context.getString(R.string.empty_pass_phrase)
        )
      }

      var firstPassPhrase: Passphrase? = null

      for (i in keys.indices) {
        val key = keys[i]
        val fingerprint = OpenPgpV4Fingerprint(key)
        val passPhrase = keysStorage.getPassphraseByFingerprint(fingerprint.toString())

        if (i == 0) {
          firstPassPhrase = passPhrase
        } else if (passPhrase != firstPassPhrase) {
          throw DifferentPassPhrasesException(context.getString(R.string.keys_have_different_pass_phrase))
        }

        if (passPhrase == null || passPhrase.isEmpty) {
          throw PrivateKeyStrengthException(context.getString(R.string.empty_pass_phrase))
        }

        PgpPwd.checkForWeakPassphrase(passPhrase)

        val keyDetails =
          key.toPgpKeyDetails(account.clientConfiguration?.shouldHideArmorMeta() ?: false)
        val encryptedKey = if (keyDetails.isFullyDecrypted) {
          PgpKey.encryptKey(keyDetails.privateKey ?: throw IllegalStateException(), passPhrase)
        } else {
          keyDetails.privateKey
        }

        builder.append(if (i > 0) "\n" + encryptedKey!! else encryptedKey)
      }

      return builder.toString()
    }

    /**
     * Get public keys for recipients;
     *
     * @param context     Interface to global information about an application environment.
     * @param emails      A list which contains recipients
     * @return A list of public keys.
     * @throws NoKeyAvailableException
     */
    @JvmStatic
    fun getRecipientsUsablePubKeys(context: Context, emails: MutableList<String>): List<String> {
      val publicKeys = mutableListOf<String>()
      val recipientsWithPubKeys = FlowCryptRoomDatabase.getDatabase(context).recipientDao()
        .getRecipientsWithPubKeysByEmails(emails)

      for (recipientWithPubKeys in recipientsWithPubKeys) {
        for (publicKeyEntity in recipientWithPubKeys.publicKeys) {
          val pgpKeyDetailsList = PgpKey.parseKeys(publicKeyEntity.publicKey).pgpKeyDetailsList
          for (pgpKeyDetails in pgpKeyDetailsList) {
            if (!pgpKeyDetails.isExpired && !pgpKeyDetails.isRevoked) {
              publicKeys.add(pgpKeyDetails.publicKey)
            }
          }
        }
      }

      return publicKeys
    }

    /**
     * Get sender public keys. We use this method to encrypt data for all user's available keys.
     *
     * @param context     Interface to global information about an application environment.
     * @param senderEmail The sender email
     */
    fun getSenderPublicKeys(
      context: Context,
      senderEmail: String
    ): List<String> {
      val keysStorage = KeysStorageImpl.getInstance(context.applicationContext)
      val matchingKeyRingInfoList = keysStorage.getPGPSecretKeyRingsByUserId(senderEmail)
        .map { KeyRingInfo(it) }.filter { it.usableForEncryption() }
      if (matchingKeyRingInfoList.isEmpty()) {
        throw IllegalStateException("There are no usable for encryption keys for $senderEmail")
      }
      return matchingKeyRingInfoList.map { PGPPublicKeyRing(it.publicKeys).armor() }
    }

    /**
     * Get a sender PGP keys details.
     *
     * @param context     Interface to global information about an application environment.
     * @param account     The given account
     * @param senderEmail The sender email
     * @throws NoKeyAvailableException
     */
    @JvmStatic
    fun getSenderPgpKeyDetails(
      context: Context,
      account: AccountEntity,
      senderEmail: String
    ): PgpKeyDetails {
      val keysStorage = KeysStorageImpl.getInstance(context.applicationContext)
      val pgpSecretKeyRing = keysStorage.getFirstUsableForEncryptionPGPSecretKeyRing(senderEmail)
        ?: if (account.email.equals(senderEmail, ignoreCase = true)) {
          throw NoKeyAvailableException(context, account.email)
        } else {
          throw NoKeyAvailableException(context, account.email, senderEmail)
        }

      return keysStorage.getPgpKeyDetailsList(listOf(pgpSecretKeyRing)).first()
    }

    /**
     * Filter an input file name to use only a safe part.
     */
    fun sanitizeFileName(originalFileName: String?): String {
      return originalFileName?.split("/")?.lastOrNull()?.replace("\\", "") ?: "unnamed"
    }

    /**
     * Check if the file extension fits the encrypted pattern.
     * If yes - it can mean the file is encrypted
     */
    fun isPossiblyEncryptedData(fileName: String?): Boolean {
      return RawBlockParser.ENCRYPTED_FILE_REGEX.containsMatchIn(fileName ?: "")
    }

    fun armor(
      hideArmorMeta: Boolean = false,
      headers: List<Pair<String, String>>? = PgpArmor.FLOWCRYPT_HEADERS,
      encode: (outputStream: OutputStream) -> Unit
    ): String {
      ByteArrayOutputStream().use { out ->
        ArmoredOutputStream(out).use { armoredOut ->
          if (headers != null) {
            if (hideArmorMeta) {
              armoredOut.clearHeaders()
            } else {
              for (header in headers) {
                armoredOut.setHeader(header.first, header.second)
              }
            }
          }
          encode.invoke(armoredOut)
        }
        return String(out.toByteArray(), StandardCharsets.US_ASCII)
      }
    }
  }
}
