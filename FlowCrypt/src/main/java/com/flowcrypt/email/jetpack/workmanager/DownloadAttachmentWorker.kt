/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.workmanager

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.service.attachment.AttachmentNotificationManager
import com.flowcrypt.email.util.LogsUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * @author Denys Bondarenko
 */
class DownloadAttachmentWorker(context: Context, params: WorkerParameters) :
  BaseWorker(context, params) {
  override suspend fun doWork() = withContext(Dispatchers.IO) {
    LogsUtil.d(TAG, "doWork")

    val email = inputData.getString(KEY_EMAIL) ?: ""
    val folder = inputData.getString(KEY_FOLDER) ?: ""
    val uid = inputData.getLong(KEY_UID, -1)
    val path = inputData.getString(KEY_PATH) ?: ""

    val attachmentEntity = roomDatabase.attachmentDao().getAttachment(email, folder, uid, path)
      ?: return@withContext Result.success()

    if (isStopped) {
      return@withContext Result.success()
    }

    val attInfo = attachmentEntity.toAttInfo()

    AttachmentNotificationManager(applicationContext).attachmentAddedToLoadQueue(
      applicationContext,
      attachmentEntity.toAttInfo()
    )

    delay(10000)

    AttachmentNotificationManager(applicationContext).downloadCompleted(
      context = applicationContext,
      attInfo = attInfo,
      uri = Uri.EMPTY,
      useContentApp = false
    )

    return@withContext Result.success()
  }

  companion object {
    private val TAG = DownloadAttachmentWorker::class.java.simpleName
    private const val KEY_EMAIL = "EMAIL"
    private const val KEY_FOLDER = "FOLDER"
    private const val KEY_UID = "UID"
    private const val KEY_PATH = "PATH"

    fun enqueue(context: Context, attachmentInfo: AttachmentInfo) {
      val constraints = Constraints.Builder()
        .build()

      WorkManager
        .getInstance(context.applicationContext)
        .enqueueUniqueWork(
          attachmentInfo.uniqueStringId,
          ExistingWorkPolicy.KEEP,
          OneTimeWorkRequestBuilder<DownloadAttachmentWorker>()
            .setConstraints(constraints)
            .setInputData(
              workDataOf(
                KEY_EMAIL to attachmentInfo.email,
                KEY_FOLDER to attachmentInfo.folder,
                KEY_UID to attachmentInfo.uid,
                KEY_PATH to attachmentInfo.path
              )
            )
            .build()
        )
    }
  }
}
