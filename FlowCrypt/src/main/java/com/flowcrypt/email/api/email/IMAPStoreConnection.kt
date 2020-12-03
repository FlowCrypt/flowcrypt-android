/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email

import android.content.Context
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.util.LogsUtil
import com.sun.mail.util.MailConnectException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.mail.Store

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

  override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
    mutex.withLock {
      LogsUtil.d(IMAPStoreConnection::class.java.simpleName, "connect(${accountEntity.email}): check connection")
      if (isConnected()) {
        LogsUtil.d(IMAPStoreConnection::class.java.simpleName, "connect(${accountEntity.email}): connected, skip connection")
        return@withContext true
      }

      return@withContext try {
        LogsUtil.d(IMAPStoreConnection::class.java.simpleName, "connect(${accountEntity.email}): try to open a new connection to store")
        OpenStoreHelper.openStore(context, accountEntity, store)
        LogsUtil.d(IMAPStoreConnection::class.java.simpleName, "connect(${accountEntity.email}): connected to store")
        true
      } catch (e: Exception) {
        LogsUtil.d(IMAPStoreConnection::class.java.simpleName, "connect(${accountEntity.email}): connection to store failed", e)
        e.printStackTrace()
        when (e) {
          is MailConnectException -> throw e
          else -> false
        }
      }
    }
  }

  override suspend fun reconnect(): Boolean = withContext(Dispatchers.IO) {
    LogsUtil.d(IMAPStoreConnection::class.java.simpleName, "reconnect(${accountEntity.email}): begin reconnection")
    if (!disconnect()) {
      throw IllegalStateException("Can't disconnect")
    }
    LogsUtil.d(IMAPStoreConnection::class.java.simpleName, "reconnect(${accountEntity.email}): reconnection completed")
    return@withContext connect()
  }

  override suspend fun disconnect(): Boolean = withContext(Dispatchers.IO) {
    mutex.withLock {
      LogsUtil.d(IMAPStoreConnection::class.java.simpleName, "disconnect(${accountEntity.email}): check connection")
      if (!isConnected()) {
        LogsUtil.d(IMAPStoreConnection::class.java.simpleName, "disconnect(${accountEntity.email}): disconnected, skipping...")
        return@withContext true
      }

      return@withContext try {
        LogsUtil.d(IMAPStoreConnection::class.java.simpleName, "disconnect(${accountEntity.email}): disconnecting...")
        if (store.isConnected) {
          store.close()
        }
        LogsUtil.d(IMAPStoreConnection::class.java.simpleName, "disconnect(${accountEntity.email}): disconnected")
        true
      } catch (e: Exception) {
        LogsUtil.d(IMAPStoreConnection::class.java.simpleName, "disconnect(${accountEntity.email}): disconnecting failed", e)
        e.printStackTrace()
        false
      }
    }
  }

  override suspend fun isConnected(): Boolean = withContext(Dispatchers.IO) {
    return@withContext store.isConnected
  }

  override suspend fun <T> executeWithResult(action: suspend () -> Result<T>): Result<T> = withContext(Dispatchers.IO) {
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

  override suspend fun executeIMAPAction(action: suspend (store: Store) -> Unit) = withContext(Dispatchers.IO) {
    LogsUtil.d(IMAPStoreConnection::class.java.simpleName, "executeIMAPAction(${accountEntity.email}): start ${action.javaClass}")
    if (!isConnected()) {
      connect()
    }

    try {
      LogsUtil.d(IMAPStoreConnection::class.java.simpleName, "executeIMAPAction(${accountEntity.email}): start invoke ${action.javaClass}")
      action.invoke(store)
      LogsUtil.d(IMAPStoreConnection::class.java.simpleName, "executeIMAPAction(${accountEntity.email}): invoke ${action.javaClass} completed")
    } catch (e: Exception) {
      LogsUtil.d(IMAPStoreConnection::class.java.simpleName, "executeIMAPAction(${accountEntity.email}): invoke ${action.javaClass} failed", e)
      throw e
    }
  }
}