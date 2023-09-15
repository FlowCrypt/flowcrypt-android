/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.workmanager.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.jetpack.workmanager.base.BaseMoveMessagesWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * This task moves messages back to INBOX
 *
 * @author Denys Bondarenko
 */
class MovingToInboxWorker(context: Context, params: WorkerParameters) :
  BaseMoveMessagesWorker(context, params) {

  override val queryMessageState: MessageState = MessageState.PENDING_MOVE_TO_INBOX

  override suspend fun getDestinationFolderForIMAP(account: AccountEntity): LocalFolder? =
    withContext(Dispatchers.IO) {
      val foldersManager = FoldersManager.fromDatabaseSuspend(applicationContext, account)
      return@withContext foldersManager.findInboxFolder()
    }

  override fun getAddAndRemoveLabelIdsForGmailAPI(srcFolder: String): Pair<List<String>?, List<String>?> {
    return Pair(
      listOf(GmailApiHelper.LABEL_INBOX),
      if (GmailApiHelper.LABEL_TRASH.equals(srcFolder, true)) listOf(
        GmailApiHelper.LABEL_TRASH
      ) else null
    )
  }

  companion object {
    const val GROUP_UNIQUE_TAG = BuildConfig.APPLICATION_ID + ".MOVING_MESSAGES_TO_INBOX"

    fun enqueue(context: Context) {
      val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

      WorkManager
        .getInstance(context.applicationContext)
        .enqueueUniqueWork(
          GROUP_UNIQUE_TAG,
          ExistingWorkPolicy.REPLACE,
          OneTimeWorkRequestBuilder<MovingToInboxWorker>()
            .addTag(TAG_SYNC)
            .setConstraints(constraints)
            .build()
        )
    }
  }
}
