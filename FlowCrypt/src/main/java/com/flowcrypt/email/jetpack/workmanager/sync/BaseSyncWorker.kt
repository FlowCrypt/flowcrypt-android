/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.workmanager.sync

import android.content.Context
import androidx.work.WorkerParameters
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.api.email.IMAPStoreManager
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.jetpack.workmanager.BaseWorker
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.sun.mail.util.MailConnectException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.mail.FolderClosedException
import javax.mail.MessagingException
import javax.mail.Store

/**
 * @author Denis Bondarenko
 *         Date: 11/25/20
 *         Time: 6:08 PM
 *         E-mail: DenBond7@gmail.com
 */
abstract class BaseSyncWorker(context: Context, params: WorkerParameters) : BaseWorker(context, params) {
  abstract suspend fun runIMAPAction(accountEntity: AccountEntity, store: Store)

  override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    if (isStopped) {
      return@withContext Result.success()
    }

    try {
      val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)
      val activeAccountEntity = roomDatabase.accountDao().getActiveAccountSuspend()
      activeAccountEntity?.let {
        val connection = IMAPStoreManager.activeConnections[activeAccountEntity.id]
        connection?.executeIMAPAction {
          runIMAPAction(activeAccountEntity, it)
        }
      }

      return@withContext Result.success()
    } catch (e: Exception) {
      e.printStackTrace()
      return@withContext handleExceptionWithResult(e)
    }
  }

  private fun handleExceptionWithResult(e: Throwable): Result {
    when (e) {
      //reschedule a task if we have a connection issue
      is UnknownHostException, is MailConnectException, is FolderClosedException, is SocketTimeoutException -> {
        return Result.retry()
      }

      is IllegalStateException -> {
        return if (e.message.equals("Not connected", true)) {
          Result.retry()
        } else Result.failure()
      }

      is MessagingException -> {
        return e.message?.let {
          if (it.contains("Connection closed by peer")
              || it.contains("Connection reset by peer")) {
            Result.retry()
          } else Result.failure()

        } ?: Result.failure()
      }

      else -> {
        return if (e.cause == null) {
          ExceptionUtil.handleError(e)
          Result.failure()
        } else {
          e.cause?.let {
            handleExceptionWithResult(it)
          } ?: Result.failure()
        }
      }
    }
  }

  companion object {
    const val TAG_SYNC = BuildConfig.APPLICATION_ID + ".SYNC"
  }
}