/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import com.flowcrypt.email.api.retrofit.node.NodeRepository
import com.flowcrypt.email.api.retrofit.request.node.ParseKeysRequest
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.model.KeysStorage

/**
 * This class implements [KeysStorage]. Here we collect information about imported private keys
 * for an active account and keep it in the memory.
 *
 * @author DenBond7
 * Date: 05.05.2017
 * Time: 13:06
 * E-mail: DenBond7@gmail.com
 */
class KeysStorageImpl private constructor(context: Context) : KeysStorage {
  val keysLiveData = MediatorLiveData<List<KeyEntity>>()
  val nodeKeyDetailsLiveData: LiveData<List<NodeKeyDetails>> = Transformations.switchMap(keysLiveData) {
    liveData {
      val raw = it.joinToString { keyEntity -> keyEntity.privateKeyAsString }
      val result = NodeRepository().fetchKeyDetails(ParseKeysRequest(raw))
      if (result.status == Result.Status.SUCCESS) {
        emit(result.data?.nodeKeyDetails ?: emptyList<NodeKeyDetails>())
      } else {
        emit(emptyList<NodeKeyDetails>())
      }
    }
  }

  private val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)
  private var keys = mutableListOf<KeyEntity>()
  private var nodeKeyDetailsList = mutableListOf<NodeKeyDetails>()
  private val onKeysUpdatedListeners: MutableList<OnKeysUpdatedListener> = mutableListOf()
  private val pureActiveAccountLiveData: LiveData<AccountEntity?> = roomDatabase.accountDao().getActiveAccountLD()
  private val encryptedKeysLiveData: LiveData<List<KeyEntity>> = Transformations.switchMap(pureActiveAccountLiveData) {
    roomDatabase.keysDao().getAllKeysByAccountLD(it?.email ?: "")
  }

  private val decryptedKeysLiveData = encryptedKeysLiveData.switchMap { list ->
    liveData {
      emit(list.map {
        it.copy(
            privateKey = KeyStoreCryptoManager.decryptSuspend(it.privateKeyAsString).toByteArray(),
            passphrase = KeyStoreCryptoManager.decryptSuspend(it.passphrase))
      })
    }
  }
  private val manuallyDecryptedKeysLiveData: MutableLiveData<List<KeyEntity>> = MutableLiveData()

  init {
    keysLiveData.addSource(decryptedKeysLiveData) { keysLiveData.value = it }
    keysLiveData.addSource(manuallyDecryptedKeysLiveData) { keysLiveData.value = it }

    keysLiveData.observeForever {
      keys.clear()
      keys.addAll(it)

      for (onRefreshListener in onKeysUpdatedListeners) {
        onRefreshListener.onKeysUpdated()
      }
    }

    nodeKeyDetailsLiveData.observeForever {
      nodeKeyDetailsList.clear()
      nodeKeyDetailsList.addAll(it)
    }
  }

  /**
   * This method can be used as a manual trigger which helps to fetch existed private keys
   * manually. Don't call it from the main thread!
   *
   */
  @WorkerThread
  fun fetchKeysManually() {
    val activeAccountEntity = roomDatabase.accountDao().getActiveAccount()
    val keys = roomDatabase.keysDao().getAllKeysByAccount(activeAccountEntity?.email ?: "")
    val decryptedKeys = keys.map { getDecryptedKeyEntity(it) }
    manuallyDecryptedKeysLiveData.postValue(decryptedKeys)
  }

  override fun getPgpPrivateKey(longId: String?): KeyEntity? {
    return keys.firstOrNull { it.longId == longId }
  }

  override fun getFilteredPgpPrivateKeys(longIds: Array<String>): List<KeyEntity> {
    return keys.filter { longIds.contains(it.longId) }
  }

  override fun getPgpPrivateKeysByEmail(email: String?): List<KeyEntity> {
    val keys = mutableListOf<KeyEntity>()

    nodeKeyDetailsList.forEach { nodeKeyDetails ->
      for (contact in nodeKeyDetails.pgpContacts) {
        if (email?.equals(contact.email, true) == true && !nodeKeyDetails.isExpired) {
          getPgpPrivateKey(nodeKeyDetails.longId)?.let { keyEntity -> keys.add(keyEntity) }
        }
      }
    }

    return keys
  }

  override fun getNodeKeyDetailsListByEmail(email: String?): List<NodeKeyDetails> {
    val list = mutableListOf<NodeKeyDetails>()

    nodeKeyDetailsList.forEach { nodeKeyDetails ->
      for (contact in nodeKeyDetails.pgpContacts) {
        if (email?.equals(contact.email, true) == true && !nodeKeyDetails.isExpired) {
          list.add(nodeKeyDetails)
        }
      }
    }

    return list
  }

  override fun getAllPgpPrivateKeys(): List<KeyEntity> {
    return keys
  }

  fun hasKeys(): Boolean {
    return keys.isNotEmpty()
  }

  private fun getDecryptedKeyEntity(keyEntity: KeyEntity): KeyEntity {
    val privateKey = KeyStoreCryptoManager.decrypt(keyEntity.privateKeyAsString)
    val passphrase = KeyStoreCryptoManager.decrypt(keyEntity.passphrase)

    return keyEntity.copy(privateKey = privateKey.toByteArray(), passphrase = passphrase)
  }

  interface OnKeysUpdatedListener {
    fun onKeysUpdated()
  }

  companion object {
    @Volatile
    private var INSTANCE: KeysStorageImpl? = null

    @JvmStatic
    fun getInstance(context: Context): KeysStorageImpl {
      val appContext = context.applicationContext
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: KeysStorageImpl(appContext).also { INSTANCE = it }
      }
    }
  }
}
