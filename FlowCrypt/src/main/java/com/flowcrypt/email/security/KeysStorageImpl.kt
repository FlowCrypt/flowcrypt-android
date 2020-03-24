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
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.model.KeysStorage
import com.flowcrypt.email.model.PgpContact

/**
 * This class implements [KeysStorage]. Here we collect information about imported private keys
 * and keep it in the memory.
 *
 * @author DenBond7
 * Date: 05.05.2017
 * Time: 13:06
 * E-mail: DenBond7@gmail.com
 */
class KeysStorageImpl private constructor(context: Context) : KeysStorage {
  val keysLiveData = MediatorLiveData<List<KeyEntity>>()
  private var keys = mutableListOf<KeyEntity>()
  private val onKeysUpdatedListeners: MutableList<OnKeysUpdatedListener> = mutableListOf()
  private val encryptedKeysLiveData: LiveData<List<KeyEntity>> = FlowCryptRoomDatabase.getDatabase(context).keysDao().getAllKeysLD()
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
  }

  /**
   * This method can be used as a manual trigger which helps to fetch existed private keys
   * manually. Don't call it from the main thread!
   *
   * @param context Interface to global information about an application environment.
   */
  @WorkerThread
  fun fetchKeysManually(context: Context) {
    val keys = FlowCryptRoomDatabase.getDatabase(context).keysDao().getAllKeys()
    val decryptedKeys = keys.map { getDecryptedKeyEntity(it) }
    manuallyDecryptedKeysLiveData.postValue(decryptedKeys)
  }

  override fun findPgpContact(longId: String?): PgpContact? {
    return null
  }

  override fun findPgpContacts(longId: Array<String>): List<PgpContact> {
    return emptyList()
  }

  override fun getPgpPrivateKey(longId: String?): KeyEntity? {
    return keys.firstOrNull { it.longId == longId }
  }

  override fun getFilteredPgpPrivateKeys(longIds: Array<String>): List<KeyEntity> {
    return keys.filter { longIds.contains(it.longId) }
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
