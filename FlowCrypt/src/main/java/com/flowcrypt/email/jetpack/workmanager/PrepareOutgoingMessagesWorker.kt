/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.workmanager

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.flowcrypt.email.jetpack.workmanager.base.BaseMsgWorker
import com.flowcrypt.email.service.ProcessingOutgoingMessageInfoHelper
import com.flowcrypt.email.util.FileAndDirectoryUtils
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.OutgoingMessageInfoManager

/**
 * @author Denys Bondarenko
 */
class PrepareOutgoingMessagesWorker(context: Context, params: WorkerParameters) :
  BaseMsgWorker(context, params) {
  override suspend fun doWork(): Result {
    LogsUtil.d(TAG, "doWork")
    if (isStopped) {
      return Result.success()
    }

    val folder = OutgoingMessageInfoManager.getOutgoingInfoDirectory(applicationContext)
    do {
      val file =
        FileAndDirectoryUtils.getFilesInDir(folder).firstOrNull() ?: return Result.success()
      try {
        val outgoingMessageInfo =
          OutgoingMessageInfoManager.getOutgoingMessageInfoFromFile(applicationContext, file)
        ProcessingOutgoingMessageInfoHelper.process(applicationContext, outgoingMessageInfo)
      } catch (e: Exception) {
        //need to think about this one .need to update messageEntity
        e.printStackTrace()
      }

      file.delete()
    } while (FileAndDirectoryUtils.getFilesInDir(folder).isNotEmpty())

    return Result.success()
  }

  companion object {
    private val TAG = PrepareOutgoingMessagesWorker::class.java.simpleName
    val NAME = PrepareOutgoingMessagesWorker::class.java.simpleName

    fun enqueue(context: Context, forceCreating: Boolean = false) {
      WorkManager
        .getInstance(context.applicationContext)
        .enqueueUniqueWork(
          NAME,
          if (forceCreating) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
          OneTimeWorkRequestBuilder<PrepareOutgoingMessagesWorker>().build()
        )
    }
  }
}