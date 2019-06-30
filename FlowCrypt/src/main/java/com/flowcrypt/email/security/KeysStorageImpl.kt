/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security

import android.content.Context
import com.flowcrypt.email.model.KeysStorage
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.model.PgpKeyInfo
import com.flowcrypt.email.util.exception.ExceptionUtil
import java.util.*

/**
 * This class implements [KeysStorage]. Here we collect information about imported private keys.
 *
 * @author DenBond7
 * Date: 05.05.2017
 * Time: 13:06
 * E-mail: DenBond7@gmail.com
 */

class KeysStorageImpl private constructor(context: Context) : KeysStorage {

  private var pgpKeyInfoList: LinkedList<PgpKeyInfo> = LinkedList()
  private var passphrases: LinkedList<String> = LinkedList()
  private val onRefreshListeners: MutableList<OnRefreshListener>

  init {
    this.onRefreshListeners = ArrayList()
    setup(context)
  }

  override fun findPgpContact(longid: String): PgpContact? {
    return null
  }

  override fun findPgpContacts(longid: Array<String>): List<PgpContact> {
    return emptyList()
  }

  override fun getPgpPrivateKey(longid: String): PgpKeyInfo? {
    for (pgpKeyInfo in pgpKeyInfoList) {
      if (longid == pgpKeyInfo.longid) {
        return pgpKeyInfo
      }
    }
    return null
  }

  override fun getFilteredPgpPrivateKeys(longid: Array<String>): List<PgpKeyInfo> {
    val pgpKeyInfos = ArrayList<PgpKeyInfo>()
    for (id in longid) {
      for (pgpKeyInfo in this.pgpKeyInfoList) {
        if (pgpKeyInfo.longid == id) {
          pgpKeyInfos.add(pgpKeyInfo)
          break
        }
      }
    }
    return pgpKeyInfos
  }

  override fun getAllPgpPrivateKeys(): List<PgpKeyInfo> {
    return pgpKeyInfoList
  }

  override fun getPassphrase(longid: String): String? {
    for (i in pgpKeyInfoList.indices) {
      val (longid1) = pgpKeyInfoList[i]
      if (longid == longid1) {
        return passphrases[i]
      }
    }

    return null
  }

  @Synchronized
  override fun refresh(context: Context) {
    setup(context)

    for (onRefreshListener in onRefreshListeners) {
      onRefreshListener.onRefresh()
    }
  }

  fun attachOnRefreshListener(onRefreshListener: OnRefreshListener?) {
    if (onRefreshListener != null) {
      this.onRefreshListeners.add(onRefreshListener)
    }
  }

  fun removeOnRefreshListener(onRefreshListener: OnRefreshListener?) {
    if (onRefreshListener != null) {
      this.onRefreshListeners.remove(onRefreshListener)
    }
  }

  private fun setup(context: Context?) {
    if (context == null) {
      return
    }

    val appContext = context.applicationContext

    pgpKeyInfoList.clear()
    passphrases.clear()
    try {
      for (pgpKeyInfo in SecurityUtils.getPgpKeyInfoList(appContext)) {
        pgpKeyInfoList.add(pgpKeyInfo)
        if(pgpKeyInfo.passphrase != null) { // pass phrases may be optionally kept out of storage in the future https://github.com/FlowCrypt/flowcrypt-android/issues/372
          passphrases.add(pgpKeyInfo.passphrase)
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    }

  }

  interface OnRefreshListener {
    fun onRefresh()
  }

  companion object {
    @Volatile
    private var INSTANCE: KeysStorageImpl? = null

    @JvmStatic
    fun getInstance(context: Context): KeysStorageImpl {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: KeysStorageImpl(context).also { INSTANCE = it }
      }
    }
  }
}
