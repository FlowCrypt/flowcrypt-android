/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader

import android.content.Context
import android.text.TextUtils
import androidx.loader.content.AsyncTaskLoader
import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.database.dao.KeysDao
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.KeysDaoSource
import com.flowcrypt.email.database.dao.source.UserIdEmailsKeysDaoSource
import com.flowcrypt.email.model.results.LoaderResult
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.NoPrivateKeysAvailableException
import com.google.android.gms.common.util.CollectionUtils
import java.util.*

/**
 * This loader can be used for changing a pass phrase of private keys of some account.
 *
 * @author Denis Bondarenko
 * Date: 06.08.2018
 * Time: 9:25
 * E-mail: DenBond7@gmail.com
 */
class ChangePassPhraseAsyncTaskLoader(context: Context,
                                      private val account: AccountDao,
                                      private val newPassphrase: String) : AsyncTaskLoader<LoaderResult>(context) {
  private var isActionStarted: Boolean = false
  private var data: LoaderResult? = null

  public override fun onStartLoading() {
    if (data != null) {
      deliverResult(data)
    } else {
      if (!isActionStarted) {
        forceLoad()
      }
    }
  }

  override fun loadInBackground(): LoaderResult? {
    isActionStarted = true
    try {
      val longIds = UserIdEmailsKeysDaoSource().getLongIdsByEmail(context, account.email)

      val keysStore = KeysStorageImpl.getInstance(context)
      val pgpKeyInfoList = keysStore.getFilteredPgpPrivateKeys(longIds.toTypedArray())

      if (CollectionUtils.isEmpty(pgpKeyInfoList)) {
        throw NoPrivateKeysAvailableException(context, account.email)
      }

      val keyStoreCryptoManager = KeyStoreCryptoManager.getInstance(context)
      val keysDaoList = ArrayList<KeysDao>()

      for ((longid, private) in pgpKeyInfoList) {
        val passphrase = keysStore.getPassphrase(longid)
        val modifiedNodeKeyDetails = getModifiedNodeKeyDetails(passphrase, private)
        keysDaoList.add(KeysDao.generateKeysDao(keyStoreCryptoManager, modifiedNodeKeyDetails, newPassphrase))
      }

      val contentProviderResults = KeysDaoSource().updateKeys(context, keysDaoList)

      if (contentProviderResults.isEmpty()) {
        throw IllegalArgumentException("An error occurred during changing passphrases")
      }

      for (contentProviderResult in contentProviderResults) {
        if (contentProviderResult.count < 1) {
          throw IllegalArgumentException("An error occurred when we tried update " + contentProviderResult.uri)
        }
      }

      return LoaderResult(true, null)
    } catch (e: Exception) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
      return LoaderResult(null, e)
    }
  }

  override fun deliverResult(data: LoaderResult?) {
    this.data = data
    super.deliverResult(data)
  }

  private fun getModifiedNodeKeyDetails(passphrase: String?, originalPrivateKey: String?): NodeKeyDetails {
    val keyDetailsList = NodeCallsExecutor.parseKeys(originalPrivateKey!!)
    if (CollectionUtils.isEmpty(keyDetailsList) || keyDetailsList.size != 1) {
      throw IllegalStateException("Parse keys error")
    }

    val nodeKeyDetails = keyDetailsList[0]
    val longId = nodeKeyDetails.longId

    if (TextUtils.isEmpty(passphrase)) {
      throw IllegalStateException("Passphrase for key with longid $longId not found")
    }

    val (decryptedKey) = NodeCallsExecutor.decryptKey(nodeKeyDetails.privateKey!!, passphrase!!)

    if (TextUtils.isEmpty(decryptedKey)) {
      throw IllegalStateException("Can't decrypt key with longid " + longId!!)
    }

    val (encryptedKey) = NodeCallsExecutor.encryptKey(decryptedKey!!, newPassphrase)

    if (TextUtils.isEmpty(encryptedKey)) {
      throw IllegalStateException("Can't encrypt key with longid " + longId!!)
    }

    val modifiedKeyDetailsList = NodeCallsExecutor.parseKeys(encryptedKey!!)
    if (CollectionUtils.isEmpty(modifiedKeyDetailsList) || modifiedKeyDetailsList.size != 1) {
      throw IllegalStateException("Parse keys error")
    }

    return modifiedKeyDetailsList[0]
  }
}
