/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security

import android.content.Context
import android.text.TextUtils
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.util.exception.DifferentPassPhrasesException
import com.flowcrypt.email.util.exception.NoKeyAvailableException
import com.flowcrypt.email.util.exception.NoPrivateKeysAvailableException
import com.flowcrypt.email.util.exception.PrivateKeyStrengthException
import com.google.android.gms.common.util.CollectionUtils
import com.nulabinc.zxcvbn.Zxcvbn
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import java.util.*

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
      val longIdsByEmail = FlowCryptRoomDatabase.getDatabase(context).userIdEmailsKeysDao().getLongIdsByEmail(account.email)
      val longids = longIdsByEmail.toTypedArray()
      val keysStorage = KeysStorageImpl.getInstance(context)
      val keys = keysStorage.getFilteredPgpPrivateKeys(longids)

      if (CollectionUtils.isEmpty(keys)) {
        throw NoPrivateKeysAvailableException(context, account.email)
      }

      var firstPassPhrase: String? = null

      for (i in keys.indices) {
        val key = keys[i]

        val passPhrase = key.passphrase
        val private = key.privateKeyAsString

        if (i == 0) {
          firstPassPhrase = passPhrase
        } else if (passPhrase != firstPassPhrase) {
          throw DifferentPassPhrasesException(context.getString(R.string.keys_have_different_pass_phrase))
        }

        if (TextUtils.isEmpty(passPhrase)) {
          throw PrivateKeyStrengthException(context.getString(R.string.empty_pass_phrase))
        }

        val zxcvbn = Zxcvbn()
        val measure = zxcvbn.measure(passPhrase!!, listOf(*Constants.PASSWORD_WEAK_WORDS)).guesses
        val passwordStrength = NodeCallsExecutor.zxcvbnStrengthBar(measure)

        when (passwordStrength.word?.word) {
          Constants.PASSWORD_QUALITY_WEAK,
          Constants.PASSWORD_QUALITY_POOR -> throw PrivateKeyStrengthException(context.getString(R.string.pass_phrase_too_weak))

          Constants.PASSWORD_QUALITY_REASONABLE, Constants.PASSWORD_QUALITY_GOOD,
          Constants.PASSWORD_QUALITY_GREAT, Constants.PASSWORD_QUALITY_PERFECT -> {
            //everything looks good
          }

          else -> throw IllegalArgumentException(context.getString(R.string.missing_pass_phrase_strength_evalutaion))
        }

        val nodeKeyDetailsList = NodeCallsExecutor.parseKeys(private)
        val keyDetails = nodeKeyDetailsList.first()

        val encryptedKey = if (keyDetails.isFullyDecrypted == true) {
          val encryptKeResult = NodeCallsExecutor.encryptKey(private, passPhrase)
          encryptKeResult.encryptedKey
        } else {
          keyDetails.privateKey
        }

        builder.append(if (i > 0) "\n" + encryptedKey!! else encryptedKey)
      }

      return builder.toString()
    }

    /**
     * Get public keys for recipients + keys of the sender;
     *
     * @param context     Interface to global information about an application environment.
     * @param emails      A list which contains recipients
     * @param account     The given account
     * @param senderEmail The sender email
     * @return A list of public keys.
     * @throws NoKeyAvailableException
     */
    @JvmStatic
    fun getRecipientsPubKeys(context: Context, emails: MutableList<String>,
                             account: AccountEntity, senderEmail: String): List<String> {
      val publicKeys = ArrayList<String>()
      val contacts = FlowCryptRoomDatabase.getDatabase(context).contactsDao()
          .getContactsByEmails(emails)

      for (contact in contacts) {
        if (contact.publicKey?.isNotEmpty() == true) {
          contact.publicKey.let { publicKeys.add(String(it)) }
        }
      }

      getSenderPublicKey(context, account, senderEmail)?.let { publicKeys.add(it) }

      return publicKeys
    }

    /**
     * Get a public key of the sender;
     *
     * @param context     Interface to global information about an application environment.
     * @param account     The given account
     * @param senderEmail The sender email
     * @return <tt>String</tt> The sender public key.
     * @throws NoKeyAvailableException
     */
    @JvmStatic
    fun getSenderPublicKey(context: Context, account: AccountEntity, senderEmail: String): String? {
      val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)
      var longIds = roomDatabase.userIdEmailsKeysDao().getLongIdsByEmail(senderEmail)

      if (longIds.isEmpty()) {
        if (account.email.equals(senderEmail, ignoreCase = true)) {
          throw NoKeyAvailableException(context, account.email)
        } else {
          longIds = roomDatabase.userIdEmailsKeysDao().getLongIdsByEmail(account.email)
          if (longIds.isEmpty()) {
            throw NoKeyAvailableException(context, account.email, senderEmail)
          }
        }
      }

      val pgpKeyInfo = KeysStorageImpl.getInstance(context).getPgpPrivateKey(longIds[0])
      if (pgpKeyInfo != null) {
        val details = NodeCallsExecutor.parseKeys(pgpKeyInfo.privateKeyAsString)
        if (CollectionUtils.isEmpty(details)) {
          throw IllegalStateException("There are no details about the given private key")
        }

        if (details.size > 1) {
          throw IllegalStateException("A wrong private key")
        }

        return details[0].publicKey
      }

      throw IllegalArgumentException("Internal error: PgpKeyInfo is null!")
    }

    /**
     * Generate uuid which is 40 characters long, containing only lowercase hex
     * characters 0-9a-f. Example: 8d43af.................................93. It uses [UUID] to generate these.
     */
    fun generateRandomUUID(): String {
      return String(Hex.encodeHex(DigestUtils.sha1(UUID.randomUUID().toString())))
    }
  }
}
