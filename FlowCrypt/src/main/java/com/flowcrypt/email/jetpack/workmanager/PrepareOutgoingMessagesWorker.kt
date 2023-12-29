/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.workmanager

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.flowcrypt.email.jetpack.workmanager.base.BaseMsgWorker
import com.flowcrypt.email.jetpack.workmanager.sync.BaseSyncWorker
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

    val id = inputData.getLong(KEY_ID, NO_ID).takeIf { it != NO_ID } ?: return Result.success()

    try {
      val folder = OutgoingMessageInfoManager.getOutgoingInfoDirectory(applicationContext)
      val messageEntity = roomDatabase.msgDao().getMsgById(id)
      val file = FileAndDirectoryUtils.getFilesInDir(folder)
        .firstOrNull { it.name == id.toString() }

      if (messageEntity == null) {
        //looks like there is no such outgoing message in the database. We can delete the file too.
        file?.delete()
        return Result.success()
      }

      if (file == null) {
        //update message entity
        return Result.success()
      }

      val outgoingMessageInfo = OutgoingMessageInfoManager.getOutgoingMessageInfoFromFile(
        context = applicationContext, file = file
      )
      ProcessingOutgoingMessageInfoHelper.process(
        context = applicationContext,
        outgoingMessageInfo = outgoingMessageInfo,
        messageEntity = messageEntity
      )

      //delete already handled outgoing message info
      file.delete()
    } catch (e: Exception) {
      //need to think about this one. need to update messageEntity
      e.printStackTrace()
      return if (MAX_ATTEMPT_COUNT == runAttemptCount) {
        Result.failure()
      } else {
        Result.retry()
      }
    }

    return Result.success()
  }

  companion object {
    private val TAG = PrepareOutgoingMessagesWorker::class.java.simpleName
    private const val KEY_ID = "KEY_ID"
    private const val NO_ID = -1L
    private const val MAX_ATTEMPT_COUNT = 5

    val NAME = PrepareOutgoingMessagesWorker::class.java.simpleName

    /**
     * Enqueue a new work that will handle [com.flowcrypt.email.api.email.model.OutgoingMessageInfo]
     *
     * @param context Interface to global information about an application environment.
     * @param id      [com.flowcrypt.email.database.entity.MessageEntity.id] value
     */
    fun enqueue(context: Context, id: Long) {
      WorkManager
        .getInstance(context.applicationContext)
        .enqueueUniqueWork(
          "${NAME}_$id",
          ExistingWorkPolicy.REPLACE,
          OneTimeWorkRequestBuilder<PrepareOutgoingMessagesWorker>()
            .addTag(BaseSyncWorker.TAG_SYNC)
            .setInputData(
              Data.Builder().putLong(KEY_ID, id).build()
            ).build()
        )
    }
  }
}