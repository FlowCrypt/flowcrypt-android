/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.workmanager.sync

import android.content.Context
import androidx.work.WorkerParameters
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.jetpack.workmanager.BaseWorker
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.sun.mail.util.MailConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.mail.FolderClosedException
import javax.mail.MessagingException

/**
 * @author Denis Bondarenko
 *         Date: 11/25/20
 *         Time: 6:08 PM
 *         E-mail: DenBond7@gmail.com
 */
abstract class BaseSyncWorker(context: Context, params: WorkerParameters) : BaseWorker(context, params) {
  protected fun handleExceptionWithResult(e: Throwable): Result {
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