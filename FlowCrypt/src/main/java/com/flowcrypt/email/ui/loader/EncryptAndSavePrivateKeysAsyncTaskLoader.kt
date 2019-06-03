/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader

import android.content.Context
import android.text.TextUtils
import android.util.Pair
import androidx.loader.content.AsyncTaskLoader
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.database.dao.KeysDao
import com.flowcrypt.email.database.dao.source.ContactsDaoSource
import com.flowcrypt.email.database.dao.source.KeysDaoSource
import com.flowcrypt.email.database.dao.source.UserIdEmailsKeysDaoSource
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.model.results.LoaderResult
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.KeyAlreadyAddedException
import java.util.*

/**
 * This loader try to encrypt and save encrypted key with entered password by
 * [KeyStoreCryptoManager] to the database.
 *
 *
 * Return true if one or more key saved, false otherwise;
 *
 * @author DenBond7
 * Date: 03.05.2017
 * Time: 11:47
 * E-mail: DenBond7@gmail.com
 */

class EncryptAndSavePrivateKeysAsyncTaskLoader(context: Context,
                                               details: ArrayList<NodeKeyDetails>,
                                               private val type: KeyDetails.Type,
                                               private val passphrase: String) :
    AsyncTaskLoader<LoaderResult>(context) {
  private val details: List<NodeKeyDetails>

  private val keysDaoSource: KeysDaoSource

  init {
    this.details = details
    this.keysDaoSource = KeysDaoSource()
    onContentChanged()
  }

  override fun loadInBackground(): LoaderResult? {
    val acceptedKeysList = ArrayList<NodeKeyDetails>()
    try {
      val keyStoreCryptoManager = KeyStoreCryptoManager.getInstance(context)
      for (keyDetails in details) {
        var tempPassphrase = passphrase
        if (keyDetails.isPrivate) {
          val decryptedKey: String?
          if (keyDetails.isDecrypted!!) {
            tempPassphrase = ""
            decryptedKey = keyDetails.privateKey
          } else {
            val (decryptedKey1) = NodeCallsExecutor.decryptKey(keyDetails.privateKey!!, passphrase)
            decryptedKey = decryptedKey1
          }

          if (!TextUtils.isEmpty(decryptedKey)) {
            if (!keysDaoSource.hasKey(context, keyDetails.longId!!)) {
              val keysDao = KeysDao.generateKeysDao(keyStoreCryptoManager, type, keyDetails, tempPassphrase)
              val uri = keysDaoSource.addRow(context, keysDao)

              val contactsDaoSource = ContactsDaoSource()
              val pairs: List<Pair<String, String>> = genPairs(keyDetails, keyDetails.pgpContacts, contactsDaoSource)

              if (uri != null) {
                acceptedKeysList.add(keyDetails)
                val userIdEmailsKeysDaoSource = UserIdEmailsKeysDaoSource()

                for (pair in pairs) {
                  userIdEmailsKeysDaoSource.addRow(context, pair.first, pair.second)
                }
              }
            } else if (details.size == 1) {
              return LoaderResult(null, KeyAlreadyAddedException(keyDetails,
                  context.getString(R.string.the_key_already_added)))
            } else {
              acceptedKeysList.add(keyDetails)
            }
          } else if (details.size == 1) {
            return LoaderResult(null, IllegalArgumentException(context.getString(R.string
                .password_is_incorrect)))
          }
        } else if (details.size == 1) {
          return LoaderResult(null, IllegalArgumentException(context.getString(R.string.not_private_key)))
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
      return LoaderResult(null, e)
    }

    return LoaderResult(acceptedKeysList, null)
  }

  public override fun onStartLoading() {
    if (takeContentChanged()) {
      forceLoad()
    }
  }

  public override fun onStopLoading() {
    cancelLoad()
  }

  private fun genPairs(keyDetails: NodeKeyDetails, contacts: List<PgpContact>,
                       daoSource: ContactsDaoSource): List<Pair<String, String>> {
    val pairs = ArrayList<Pair<String, String>>()
    for (pgpContact in contacts) {
      pgpContact.pubkey = keyDetails.publicKey
      val temp = daoSource.getPgpContact(context, pgpContact.email)
      if (GeneralUtil.isEmailValid(pgpContact.email) && temp == null) {
        ContactsDaoSource().addRow(context, pgpContact)
        //todo-DenBond7 Need to resolve a situation with different public keys.
        //For example we can have a situation when we have to different public
        // keys with the same email
      }

      pairs.add(Pair.create(keyDetails.longId, pgpContact.email))
    }
    return pairs
  }
}
