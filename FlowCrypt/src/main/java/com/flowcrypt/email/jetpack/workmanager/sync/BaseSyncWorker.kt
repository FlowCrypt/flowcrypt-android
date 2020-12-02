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
import java.net.SocketTimeoutException
import javax.mail.FolderNotFoundException
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
      is FolderNotFoundException -> {
        return Result.failure()
      }

      //reschedule a task if we have a connection issue
      is MessagingException, is SocketTimeoutException -> {
        return Result.retry()
      }

      else -> {
        ExceptionUtil.handleError(e)

        return e.cause?.let {
          handleExceptionWithResult(it)
        } ?: Result.failure()
      }
    }
  }

  companion object {
    const val TAG_SYNC = BuildConfig.APPLICATION_ID + ".SYNC"
  }
}