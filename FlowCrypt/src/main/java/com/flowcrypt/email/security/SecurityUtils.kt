/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   DenBond7
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.security

import android.content.Context
import com.flowcrypt.email.R
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.toPgpKeyDetails
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.security.pgp.PgpDecrypt
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.security.pgp.PgpPwd
import com.flowcrypt.email.util.exception.DifferentPassPhrasesException
import com.flowcrypt.email.util.exception.NoKeyAvailableException
import com.flowcrypt.email.util.exception.NoPrivateKeysAvailableException
import com.flowcrypt.email.util.exception.PrivateKeyStrengthException
import org.apache.commons.codec.android.binary.Hex
import org.apache.commons.codec.android.digest.DigestUtils
import org.pgpainless.key.OpenPgpV4Fingerprint
import org.pgpainless.util.Passphrase
import java.util.UUID

/**
 * This class help to receive security information.
 *
 * @author DenBond7
 * Date: 05.05.2017
 * Time: 13:08
 * E-mail: DenBond7@gmail.com
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

        val keyDetails = key.toPgpKeyDetails()
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
    fun getRecipientsPubKeys(context: Context, emails: MutableList<String>): MutableList<String> {
      val publicKeys = mutableListOf<String>()
      val contacts = FlowCryptRoomDatabase.getDatabase(context).contactsDao()
        .getContactsByEmails(emails)

      for (contact in contacts) {
        if (contact.publicKey?.isNotEmpty() == true) {
          contact.publicKey.let { publicKeys.add(String(it)) }
        }
      }

      return publicKeys
    }

    /**
     * Get a sender key details. If we will find a few keys we will return the first;
     *
     * @param context     Interface to global information about an application environment.
     * @param account     The given account
     * @param senderEmail The sender email
     * @return <tt>String</tt> The sender key.
     * @throws NoKeyAvailableException
     */
    @JvmStatic
    fun getSenderKeyDetails(
      context: Context,
      account: AccountEntity,
      senderEmail: String
    ): PgpKeyDetails {
      val keysStorage = KeysStorageImpl.getInstance(context.applicationContext)
      val keys = keysStorage.getPGPSecretKeyRingsByUserId(senderEmail)

      if (keys.isEmpty()) {
        if (account.email.equals(senderEmail, ignoreCase = true)) {
          throw NoKeyAvailableException(context, account.email)
        } else {
          throw NoKeyAvailableException(context, account.email, senderEmail)
        }
      }

      return keys.first().toPgpKeyDetails()
    }

    /**
     * Filter an input file name to use only a safe part.
     */
    fun sanitizeFileName(originalFileName: String?): String {
      return originalFileName?.split("/")?.lastOrNull()?.replace("\\", "") ?: "unnamed"
    }

    /**
     * Generate uuid which is 40 characters long, containing only lowercase hex
     * characters 0-9a-f. Example: 8d43af.................................93. It uses [UUID] to generate these.
     */
    fun generateRandomUUID(): String {
      return String(Hex.encodeHex(DigestUtils.sha1(UUID.randomUUID().toString())))
    }

    /**
     * Check if the file extension fits the encrypted pattern.
     * If yes - it can mean the file is encrypted
     */
    fun isEncryptedData(fileName: String?): Boolean {
      return PgpDecrypt.DETECT_SEPARATE_ENCRYPTED_ATTACHMENTS_PATTERN.find(fileName ?: "") != null
    }
  }
}
