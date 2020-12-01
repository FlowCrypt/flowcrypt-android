/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email

import android.content.Context
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * @author Denis Bondarenko
 *         Date: 11/20/20
 *         Time: 11:30 AM
 *         E-mail: DenBond7@gmail.com
 */
class IMAPStoreConnection(override val context: Context, override val accountEntity: AccountEntity) : StoreConnection {
  override val session = OpenStoreHelper.getAccountSess(context, accountEntity)

  @Volatile
  var store = OpenStoreHelper.getStore(accountEntity, session)

  /**
   * This object will be used with critical sections to guarantee synchronized access
   */
  private val mutex = Mutex()

  override suspend fun connect(): Boolean {
    mutex.withLock {
      if (isConnected()) {
        return true
      }

      return try {
        OpenStoreHelper.openStore(context, accountEntity, store)
        true
      } catch (e: Exception) {
        e.printStackTrace()
        false
      }
    }
  }

  override suspend fun reconnect(): Boolean {
    if (!disconnect()) {
      throw IllegalStateException("Can't disconnect")
    }
    return connect()
  }

  override suspend fun disconnect(): Boolean {
    mutex.withLock {
      if (!isConnected()) {
        return true
      }

      return try {
        if (store.isConnected) {
          store.close()
        }

        true
      } catch (e: Exception) {
        e.printStackTrace()
        false
      }
    }
  }

  override suspend fun isConnected(): Boolean {
    return store.isConnected
  }

  override suspend fun <T> execute(action: suspend () -> Result<T>): Result<T> = withContext(Dispatchers.IO) {
    if (!isConnected()) {
      connect()
    }

    return@withContext try {
      action.invoke()
    } catch (e: Exception) {
      e.printStackTrace()
      Result.exception(e)
    }
  }
}