/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.model.KeysStorage
import com.flowcrypt.email.node.Node
import com.flowcrypt.email.security.pgp.PgpKey

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
  private val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)
  private val nodeLiveData = Node.getInstance(context.applicationContext).liveData
  private var keys = mutableListOf<KeyEntity>()
  private var nodeKeyDetailsList = mutableListOf<NodeKeyDetails>()

  private val pureActiveAccountLiveData: LiveData<AccountEntity?> = Transformations.switchMap(nodeLiveData) {
    roomDatabase.accountDao().getActiveAccountLD()
  }

  private val encryptedKeysLiveData: LiveData<List<KeyEntity>> = Transformations.switchMap(pureActiveAccountLiveData) {
    roomDatabase.keysDao().getAllKeysByAccountLD(it?.email ?: "")
  }

  private val keysLiveData = encryptedKeysLiveData.switchMap { list ->
    liveData {
      emit(list.map {
        it.copy(
            privateKey = KeyStoreCryptoManager.decryptSuspend(it.privateKeyAsString).toByteArray(),
            passphrase = KeyStoreCryptoManager.decryptSuspend(it.passphrase))
      })
    }
  }

  val nodeKeyDetailsLiveData: LiveData<List<NodeKeyDetails>> = Transformations.switchMap(keysLiveData) {
    liveData {
      emit(PgpKey.parseKeysC(it.joinToString(separator = "\n") { keyEntity -> keyEntity.privateKeyAsString }))
    }
  }

  init {
    keysLiveData.observeForever {
      keys.clear()
      keys.addAll(it)
    }

    nodeKeyDetailsLiveData.observeForever {
      nodeKeyDetailsList.clear()
      nodeKeyDetailsList.addAll(it)
    }
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

  /**
   * Return the latest all private keys for an active account. We can use this method to fetch
   * keys as they are stored in the database. It can't be used in UI thread.
   */
  suspend fun getLatestAllPgpPrivateKeys(): List<KeyEntity> {
    val account = pureActiveAccountLiveData.value
        ?: roomDatabase.accountDao().getActiveAccountSuspend()
    account?.let { accountEntity ->
      val cachedKeysLongIds = keys.map { it.longId }.toSet()
      val latestEncryptedKeys = roomDatabase.keysDao().getAllKeysByAccountSuspend(accountEntity.email)
      val latestKeysLongIds = roomDatabase.keysDao().getAllKeysByAccountSuspend(accountEntity.email).map { it.longId }.toSet()

      if (cachedKeysLongIds == latestKeysLongIds) {
        return keys
      }

      return latestEncryptedKeys.map {
        it.copy(
            privateKey = KeyStoreCryptoManager.decryptSuspend(it.privateKeyAsString).toByteArray(),
            passphrase = KeyStoreCryptoManager.decryptSuspend(it.passphrase))
      }
    }

    return emptyList()
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
