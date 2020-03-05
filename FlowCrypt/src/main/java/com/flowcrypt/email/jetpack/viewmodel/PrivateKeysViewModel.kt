/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor
import com.flowcrypt.email.api.retrofit.node.PgpApiRepository
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.dao.KeysDao
import com.flowcrypt.email.database.dao.source.KeysDaoSource
import com.flowcrypt.email.database.dao.source.UserIdEmailsKeysDaoSource
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.NoPrivateKeysAvailableException
import com.google.android.gms.common.util.CollectionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This [ViewModel] implementation can be used to fetch details about imported keys.
 *
 * @author Denis Bondarenko
 * Date: 2/14/19
 * Time: 10:50 AM
 * E-mail: DenBond7@gmail.com
 */
class PrivateKeysViewModel(application: Application) : BaseNodeApiViewModel(application),
    KeysStorageImpl.OnRefreshListener {
  val changePassphraseLiveData = MutableLiveData<Result<Boolean>>()
  private lateinit var keysStorage: KeysStorageImpl
  private lateinit var apiRepository: PgpApiRepository

  override fun onRefresh() {
    checkAndFetchKeys()
  }

  fun init(apiRepository: PgpApiRepository) {
    this.apiRepository = apiRepository
    this.keysStorage = KeysStorageImpl.getInstance(getApplication())
    this.keysStorage.attachOnRefreshListener(this)
    checkAndFetchKeys()
  }

  private fun fetchKeys(rawKey: String?) {
    apiRepository.fetchKeyDetails(R.id.live_data_id_fetch_keys, responsesLiveData, rawKey)
  }

  fun changePassphrase(newPassphrase: String) {
    viewModelScope.launch {
      try {
        changePassphraseLiveData.value = Result.loading()
        val account = FlowCryptRoomDatabase.getDatabase(getApplication()).accountDao().getActiveAccount()
        requireNotNull(account)

        val longIds = UserIdEmailsKeysDaoSource().getLongIdsByEmail(getApplication(), account.email)

        val keysStore = KeysStorageImpl.getInstance(getApplication())
        val pgpKeyInfoList = keysStore.getFilteredPgpPrivateKeys(longIds.toTypedArray())

        if (CollectionUtils.isEmpty(pgpKeyInfoList)) {
          throw NoPrivateKeysAvailableException(getApplication(), account.email)
        }

        val keyStoreCryptoManager = KeyStoreCryptoManager.getInstance(getApplication())
        val keysDaoList = ArrayList<KeysDao>()

        for ((longid, private) in pgpKeyInfoList) {
          val passphrase = keysStore.getPassphrase(longid)
          private?.let { privateKey ->
            val modifiedNodeKeyDetails =
                getModifiedNodeKeyDetails(passphrase, newPassphrase, privateKey)
            keysDaoList.add(KeysDao.generateKeysDao(keyStoreCryptoManager, modifiedNodeKeyDetails, newPassphrase))
          }
        }

        val contentProviderResults = KeysDaoSource().updateKeys(getApplication(), keysDaoList)

        if (contentProviderResults.isEmpty()) {
          throw IllegalArgumentException("An error occurred during changing passphrases")
        }

        for (contentProviderResult in contentProviderResults) {
          if (contentProviderResult.count < 1) {
            throw IllegalArgumentException("An error occurred when we tried update " + contentProviderResult.uri)
          }
        }

        keysStore.refresh(getApplication())
        changePassphraseLiveData.value = Result.success(true)
      } catch (e: Exception) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
        changePassphraseLiveData.value = Result.exception(e)
      }
    }
  }

  private suspend fun getModifiedNodeKeyDetails(oldPassphrase: String?,
                                                newPassphrase: String,
                                                originalPrivateKey: String?): NodeKeyDetails =
      withContext(Dispatchers.IO) {
        val keyDetailsList = NodeCallsExecutor.parseKeys(originalPrivateKey!!)
        if (CollectionUtils.isEmpty(keyDetailsList) || keyDetailsList.size != 1) {
          throw IllegalStateException("Parse keys error")
        }

        val nodeKeyDetails = keyDetailsList[0]
        val longId = nodeKeyDetails.longId

        if (TextUtils.isEmpty(oldPassphrase)) {
          throw IllegalStateException("Passphrase for key with longid $longId not found")
        }

        val (decryptedKey) = NodeCallsExecutor.decryptKey(nodeKeyDetails.privateKey!!, oldPassphrase!!)

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

        modifiedKeyDetailsList[0]
      }

  private fun checkAndFetchKeys() {
    val pgpKeyInfoList = keysStorage.getAllPgpPrivateKeys()
    if (!CollectionUtils.isEmpty(pgpKeyInfoList)) {
      val builder = StringBuilder()
      for (keyInfo in pgpKeyInfoList) {
        builder.append(keyInfo.private).append("\n")
      }

      fetchKeys(builder.toString())
    } else {
      fetchKeys(null)
    }
  }
}
