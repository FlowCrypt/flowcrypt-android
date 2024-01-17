/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.workmanager

import android.content.Context
import android.text.TextUtils
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.flowcrypt.email.Constants
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.extensions.java.lang.printStackTraceIfDebugOnly
import com.flowcrypt.email.jetpack.workmanager.base.BaseMsgWorker
import com.flowcrypt.email.jetpack.workmanager.sync.BaseSyncWorker
import com.flowcrypt.email.service.ProcessingOutgoingMessageInfoHelper
import com.flowcrypt.email.ui.notifications.ErrorNotificationManager
import com.flowcrypt.email.util.FileAndDirectoryUtils
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.OutgoingMessageInfoManager
import com.flowcrypt.email.util.exception.NoKeyAvailableException
import java.io.File

/**
 * @author Denys Bondarenko
 */
class PrepareOutgoingMessagesWorker(context: Context, params: WorkerParameters) :
  BaseMsgWorker(context, params) {
  override suspend fun doWork(): Result {
    LogsUtil.d(TAG, "doWork")

    OutgoingMessageInfoManager.checkAndCleanCache(applicationContext)

    val id = inputData.getLong(KEY_ID, NO_ID).takeIf { it != NO_ID } ?: return Result.success()
    val messageEntity = roomDatabase.msgDao().getMsgById(id)

    try {
      val folder = OutgoingMessageInfoManager.getOutgoingInfoDirectory(applicationContext)
      val file = FileAndDirectoryUtils.getFilesInDir(folder)
        .firstOrNull { it.name == id.toString() }

      if (messageEntity == null) {
        //looks like there is no such outgoing message in the database. We can delete the file too.
        file?.delete()
        return Result.success()
      }

      if (file == null) {
        //looks like unexpected error has occurred. Need to update the given message entity
        roomDatabase.msgDao().getMsgById(id)?.let {
          roomDatabase.msgDao().updateSuspend(
            it.copy(
              state = MessageState.ERROR_DURING_CREATION.value,
              errorMsg = "Unexpected error. The outgoing info source is missed!"
            )
          )
        }
        return Result.success()
      }

      val outgoingMessageInfo = OutgoingMessageInfoManager.getOutgoingMessageInfoFromFile(
        context = applicationContext, file = file
      )
      ProcessingOutgoingMessageInfoHelper.process(
        context = applicationContext,
        originalOutgoingMessageInfo = outgoingMessageInfo,
        messageEntity = messageEntity
      ) {
        //delete already handled outgoing message info
        file.delete()
      }
    } catch (e: Exception) {
      e.printStackTraceIfDebugOnly()
      val existingMessageEntity = roomDatabase.msgDao().getMsgById(id)

      if (existingMessageEntity != null) {
        when (e) {
          is NoKeyAvailableException -> {
            roomDatabase.msgDao().updateSuspend(
              existingMessageEntity.copy(
                state = MessageState.ERROR_PRIVATE_KEY_NOT_FOUND.value,
                errorMsg = if (TextUtils.isEmpty(e.alias)) e.email else e.alias,
                rawMessageWithoutAttachments = null
              )
            )
          }

          else -> {
            roomDatabase.msgDao().updateSuspend(
              existingMessageEntity.copy(
                state = MessageState.ERROR_DURING_CREATION.value,
                errorMsg = e.message,
                rawMessageWithoutAttachments = null
              )
            )
          }
        }

        //need to delete unused cache
        roomDatabase.attachmentDao().deleteSuspend(
          roomDatabase.attachmentDao().getAttachmentsSuspend(
            existingMessageEntity.email,
            existingMessageEntity.folder,
            existingMessageEntity.uid
          )
        )

        try {
          if (existingMessageEntity.attachmentsDirectory != null) {
            val cacheDirectory = File(
              existingMessageEntity.attachmentsDirectory,
              Constants.ATTACHMENTS_CACHE_DIR
            )

            FileAndDirectoryUtils.cleanDir(cacheDirectory)
          }
        } catch (e: Exception) {
          e.printStackTraceIfDebugOnly()
        }
      }

      val accountEntity =
        roomDatabase.accountDao().getActiveAccountSuspend() ?: return Result.failure()
      val failedOutgoingMessagesCount =
        roomDatabase.msgDao().getFailedOutgoingMessagesCountSuspend(accountEntity.email)
      if (failedOutgoingMessagesCount > 0) {
        ErrorNotificationManager(applicationContext).notifyUserAboutProblemWithOutgoingMessages(
          accountEntity,
          failedOutgoingMessagesCount
        )
      }

      return Result.failure()
    }

    return Result.success()
  }

  companion object {
    private val TAG = PrepareOutgoingMessagesWorker::class.java.simpleName
    private const val KEY_ID = "KEY_ID"
    private const val NO_ID = -1L

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