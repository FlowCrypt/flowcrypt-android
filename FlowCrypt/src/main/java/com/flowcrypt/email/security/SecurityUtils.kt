/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security

import android.content.Context
import android.text.TextUtils
import com.flowcrypt.email.Constants
import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.ContactsDaoSource
import com.flowcrypt.email.database.dao.source.KeysDaoSource
import com.flowcrypt.email.database.dao.source.UserIdEmailsKeysDaoSource
import com.flowcrypt.email.model.PgpKeyInfo
import com.flowcrypt.email.security.model.PrivateKeyInfo
import com.flowcrypt.email.util.exception.DifferentPassPhrasesException
import com.flowcrypt.email.util.exception.NoKeyAvailableException
import com.flowcrypt.email.util.exception.NoPrivateKeysAvailableException
import com.flowcrypt.email.util.exception.NodeException
import com.flowcrypt.email.util.exception.PrivateKeyStrengthException
import com.google.android.gms.common.util.CollectionUtils
import com.nulabinc.zxcvbn.Zxcvbn
import java.io.IOException
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
     * Get a PrivateKeyInfo list.
     *
     * @param context Interface to global information about an application environment.
     * @return <tt>List<PrivateKeyInfo></PrivateKeyInfo></tt> Return a list of PrivateKeyInfo objects.
     */
    @Throws(Exception::class)
    @JvmStatic
    fun getPrivateKeysInfo(context: Context): List<PrivateKeyInfo> {
      val privateKeysInfo = ArrayList<PrivateKeyInfo>()
      val cursor = context.contentResolver.query(KeysDaoSource().baseContentUri, null, null, null, null)

      val keyStoreCryptoManager = KeyStoreCryptoManager.getInstance(context)

      if (cursor != null && cursor.moveToFirst()) {
        do {
          val longId = cursor.getString(cursor.getColumnIndex(KeysDaoSource.COL_LONG_ID))
          val pubKey = cursor.getString(cursor.getColumnIndex(KeysDaoSource.COL_PUBLIC_KEY))

          val randomVector = KeyStoreCryptoManager.normalizeAlgorithmParameterSpecString(longId)

          val prvKey = keyStoreCryptoManager.decrypt(cursor.getString(
              cursor.getColumnIndex(KeysDaoSource.COL_PRIVATE_KEY)), randomVector)
          val passphrase = keyStoreCryptoManager.decrypt(cursor.getString(
              cursor.getColumnIndex(KeysDaoSource.COL_PASSPHRASE)), randomVector)

          val pgpKeyInfo = PgpKeyInfo(longId, prvKey, pubKey)
          val privateKeyInfo = PrivateKeyInfo(pgpKeyInfo, passphrase)

          privateKeysInfo.add(privateKeyInfo)
        } while (cursor.moveToNext())
      }

      cursor?.close()
      return privateKeysInfo
    }

    /**
     * Check is backup of keys exist in the database.
     *
     * @return <tt>Boolean</tt> true if exists one or more private keys, false otherwise;
     */
    @JvmStatic
    fun hasBackup(context: Context): Boolean {
      val cursor = context.contentResolver.query(KeysDaoSource().baseContentUri, null, null, null, null)

      var hasBackup = false
      if (cursor != null && cursor.moveToFirst()) {
        hasBackup = cursor.count > 0
      }

      cursor?.close()

      return hasBackup
    }

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
    @JvmStatic
    @Throws(PrivateKeyStrengthException::class, DifferentPassPhrasesException::class, NoPrivateKeysAvailableException::class, IOException::class, NodeException::class)
    fun genPrivateKeysBackup(context: Context, account: AccountDao): String {
      val builder = StringBuilder()
      val email = account.email
      val longIdsByEmail = UserIdEmailsKeysDaoSource().getLongIdsByEmail(context, email)
      val longids = longIdsByEmail.toTypedArray()
      val keysStorage = KeysStorageImpl.getInstance(context)
      val pgpKeyInfoList = keysStorage.getFilteredPgpPrivateKeys(longids)

      if (CollectionUtils.isEmpty(pgpKeyInfoList)) {
        throw NoPrivateKeysAvailableException(context, account.email)
      }

      var firstPassPhrase: String? = null

      for (i in pgpKeyInfoList.indices) {
        val (longid, private) = pgpKeyInfoList[i]

        val passPhrase = keysStorage.getPassphrase(longid)

        if (i == 0) {
          firstPassPhrase = passPhrase
        } else if (passPhrase != firstPassPhrase) {
          throw DifferentPassPhrasesException("The keys have different pass phrase")
        }

        if (TextUtils.isEmpty(passPhrase)) {
          throw PrivateKeyStrengthException("Empty pass phrase")
        }

        val zxcvbn = Zxcvbn()
        val measure = zxcvbn.measure(passPhrase!!, Arrays.asList(*Constants.PASSWORD_WEAK_WORDS)).guesses
        val passwordStrength = NodeCallsExecutor.zxcvbnStrengthBar(measure)

        if (passwordStrength != null) {
          when (passwordStrength.word!!.word) {
            Constants.PASSWORD_QUALITY_WEAK, Constants.PASSWORD_QUALITY_POOR -> throw PrivateKeyStrengthException("Pass phrase too weak")
          }
        }

        val nodeKeyDetailsList = NodeCallsExecutor.parseKeys(private!!)
        val (isDecrypted, privateKey) = nodeKeyDetailsList[0]

        val encryptedKey: String?

        if (isDecrypted!!) {
          val (encryptedKey1) = NodeCallsExecutor.encryptKey(private, passPhrase)
          encryptedKey = encryptedKey1
        } else {
          encryptedKey = privateKey
        }

        builder.append(if (i > 0) "\n" + encryptedKey!! else encryptedKey)
      }

      return builder.toString()
    }

    /**
     * Get public keys for recipients + keys of the sender;
     *
     * @param context     Interface to global information about an application environment.
     * @param contacts    A list which contains recipients
     * @param account     The given account
     * @param senderEmail The sender email
     * @return A list of public keys.
     * @throws NoKeyAvailableException
     */
    @JvmStatic
    @Throws(NoKeyAvailableException::class, IOException::class, NodeException::class)
    fun getRecipientsPubKeys(context: Context, contacts: List<String>,
                             account: AccountDao, senderEmail: String): List<String> {
      val publicKeys = ArrayList<String>()
      val pgpContacts = ContactsDaoSource().getPgpContacts(context, contacts)

      for (pgpContact in pgpContacts) {
        if (pgpContact != null && !TextUtils.isEmpty(pgpContact.pubkey)) {
          pgpContact.pubkey?.let { publicKeys.add(it) }
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
    @Throws(NoKeyAvailableException::class, IOException::class, NodeException::class)
    fun getSenderPublicKey(context: Context, account: AccountDao, senderEmail: String): String? {
      val userIdEmailsKeysDaoSource = UserIdEmailsKeysDaoSource()
      var longIds = userIdEmailsKeysDaoSource.getLongIdsByEmail(context, senderEmail)

      if (longIds.isEmpty()) {
        if (account.email.equals(senderEmail, ignoreCase = true)) {
          throw NoKeyAvailableException(context, account.email, null)
        } else {
          longIds = userIdEmailsKeysDaoSource.getLongIdsByEmail(context, account.email)
          if (longIds.isEmpty()) {
            throw NoKeyAvailableException(context, account.email, senderEmail)
          }
        }
      }

      val pgpKeyInfo = KeysStorageImpl.getInstance(context).getPgpPrivateKey(longIds[0])
      if (pgpKeyInfo != null) {
        val details = NodeCallsExecutor.parseKeys(pgpKeyInfo.private!!)
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
  }
}
