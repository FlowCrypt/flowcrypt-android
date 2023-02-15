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
import com.flowcrypt.email.util.exception.CommonConnectionException
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.sun.mail.util.FolderClosedIOException
import com.sun.mail.util.MailConnectException
import jakarta.mail.FolderClosedException
import jakarta.mail.MessagingException
import jakarta.mail.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLProtocolException

/**
 * @author Denis Bondarenko
 *         Date: 11/20/20
 *         Time: 11:30 AM
 *         E-mail: DenBond7@gmail.com
 */
class IMAPStoreConnection(
  override val context: Context,
  override val accountEntity: AccountEntity
) : StoreConnection {
  override val session = OpenStoreHelper.getAccountSess(context, accountEntity)

  @Volatile
  var store = OpenStoreHelper.getStore(accountEntity, session)

  /**
   * This object will be used with critical sections to guarantee synchronized access
   */
  private val mutex = Mutex()

  override suspend fun connect() = withContext(Dispatchers.IO) {
    mutex.withLock {
      LogsUtil.d(
        IMAPStoreConnection::class.java.simpleName,
        "connect(${accountEntity.email}): check connection"
      )
      if (isConnected()) {
        LogsUtil.d(
          IMAPStoreConnection::class.java.simpleName,
          "connect(${accountEntity.email}): connected, skip connection"
        )
        return@withContext
      }

      try {
        LogsUtil.d(
          IMAPStoreConnection::class.java.simpleName,
          "connect(${accountEntity.email}): try to open a new connection to store"
        )
        OpenStoreHelper.openStore(context, accountEntity, store)
        LogsUtil.d(
          IMAPStoreConnection::class.java.simpleName,
          "connect(${accountEntity.email}): connected to store"
        )
      } catch (e: Throwable) {
        LogsUtil.d(
          IMAPStoreConnection::class.java.simpleName,
          "connect(${accountEntity.email}): connection to store failed",
          e
        )
        throw e
      }
    }
  }

  override suspend fun reconnect() = withContext(Dispatchers.IO) {
    LogsUtil.d(
      IMAPStoreConnection::class.java.simpleName,
      "reconnect(${accountEntity.email}): begin reconnection"
    )
    disconnect()
    connect()
    LogsUtil.d(
      IMAPStoreConnection::class.java.simpleName,
      "reconnect(${accountEntity.email}): reconnection completed"
    )
  }

  override suspend fun disconnect() = withContext(Dispatchers.IO) {
    mutex.withLock {
      LogsUtil.d(
        IMAPStoreConnection::class.java.simpleName,
        "disconnect(${accountEntity.email}): check connection"
      )
      if (!isConnected()) {
        LogsUtil.d(
          IMAPStoreConnection::class.java.simpleName,
          "disconnect(${accountEntity.email}): disconnected, skipping..."
        )
        return@withContext
      }

      try {
        LogsUtil.d(
          IMAPStoreConnection::class.java.simpleName,
          "disconnect(${accountEntity.email}): disconnecting..."
        )
        if (store.isConnected) {
          store.close()
        }
        LogsUtil.d(
          IMAPStoreConnection::class.java.simpleName,
          "disconnect(${accountEntity.email}): disconnected"
        )
      } catch (e: Throwable) {
        LogsUtil.d(
          IMAPStoreConnection::class.java.simpleName,
          "disconnect(${accountEntity.email}): disconnecting failed",
          e
        )
        throw e
      }
    }
  }

  override suspend fun isConnected(): Boolean = withContext(Dispatchers.IO) {
    return@withContext store.isConnected
  }

  override suspend fun <T> execute(action: suspend (store: Store) -> T): T =
    withContext(Dispatchers.IO) {
      return@withContext try {
        if (!isConnected()) {
          connect()
        }

        action.invoke(store)
      } catch (e: Exception) {
        throw processException(e)
      }
    }

  override suspend fun <T> executeWithResult(action: suspend (store: Store) -> Result<T>): Result<T> =
    withContext(Dispatchers.IO) {
      return@withContext try {
        if (!isConnected()) {
          connect()
        }

        action.invoke(store)
      } catch (e: Exception) {
        when (val exception = processException(e)) {
          is CommonConnectionException -> Result.exception(exception)

          else -> {
            ExceptionUtil.handleError(exception)
            Result.exception(exception)
          }
        }
      }
    }

  override suspend fun <T> executeIMAPAction(action: suspend (store: Store) -> T): T =
    withContext(Dispatchers.IO) {
      try {
        LogsUtil.d(
          IMAPStoreConnection::class.java.simpleName,
          "executeIMAPAction(${accountEntity.email}): start ${action.javaClass}"
        )
        if (!isConnected()) {
          connect()
        }

        LogsUtil.d(
          IMAPStoreConnection::class.java.simpleName,
          "executeIMAPAction(${accountEntity.email}): start invoke ${action.javaClass}"
        )
        val result = action.invoke(store)
        LogsUtil.d(
          IMAPStoreConnection::class.java.simpleName,
          "executeIMAPAction(${accountEntity.email}): invoke ${action.javaClass} completed"
        )
        result
      } catch (e: Exception) {
        LogsUtil.d(
          IMAPStoreConnection::class.java.simpleName,
          "executeIMAPAction(${accountEntity.email}): invoke ${action.javaClass} failed",
          e
        )
        val exception = processException(e)
        throw exception
      }
    }

  private fun processException(e: Throwable): Throwable {
    return when (e) {
      is UnknownHostException, is MailConnectException, is FolderClosedException, is SocketTimeoutException, is FolderClosedIOException -> {
        CommonConnectionException(e)
      }

      is IllegalStateException -> {
        if (e.message.equals("Not connected", true)) {
          CommonConnectionException(e)
        } else e
      }

      is SSLHandshakeException, is SSLProtocolException, is MessagingException -> {
        e.message?.let {
          if (hasConnectionIssueMsg(it)) {
            CommonConnectionException(e)
          } else e
        } ?: e
      }

      else -> e.cause?.let {
        processException(it)
      } ?: e
    }
  }

  private fun hasConnectionIssueMsg(it: String) = (it.contains("Connection closed by peer")
      || it.contains("I/O error during system call")
      || it.contains("Failure in SSL library, usually a protocol error")
      || it.contains("Handshake failed")
      || it.contains("Exception reading response")
      || it.contains("connection failure")
      || it.contains("Error reading input stream")
      || it.contains("Connection reset;"))
}
