/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.workmanager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.flowcrypt.email.api.email.MsgsCacheManager
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * @author Denys Bondarenko
 */
class MsgsCacheCleanerWorker(context: Context, params: WorkerParameters) :
  CoroutineWorker(context, params) {
  override suspend fun doWork(): Result =
    withContext(Dispatchers.IO) {
      if (isStopped) {
        return@withContext Result.success()
      }

      try {
        val msgDiskLruCache = MsgsCacheManager.diskLruCache
        val snapshots = msgDiskLruCache.snapshots()

        val currentTimeInMilliseconds = System.currentTimeMillis()
        while (snapshots.hasNext()) {
          val snapshot = snapshots.next()
          if (currentTimeInMilliseconds - snapshot.creationDateInMilliseconds > MAX_FILE_LIFETIME_DURATION) {
            msgDiskLruCache.remove(snapshot.key())
            LogsUtil.d(
              MsgsCacheCleanerWorker::class.java.simpleName,
              "${snapshot.key()} has been removed"
            )
          }
        }

        return@withContext Result.success()
      } catch (e: Exception) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
        return@withContext Result.failure()
      }
    }

  companion object {
    val NAME = MsgsCacheCleanerWorker::class.java.simpleName
    val MAX_FILE_LIFETIME_DURATION = TimeUnit.DAYS.toMillis(14)
  }
}
