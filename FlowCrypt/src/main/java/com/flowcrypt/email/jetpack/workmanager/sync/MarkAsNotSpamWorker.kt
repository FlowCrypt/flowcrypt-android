/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.workmanager.sync

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkerParameters
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.jetpack.workmanager.base.BaseMoveMessagesWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @author Denys Bondarenko
 */
class MarkAsNotSpamWorker(context: Context, params: WorkerParameters) :
  BaseMoveMessagesWorker(context, params) {
  override val queryMessageState: MessageState = MessageState.PENDING_MARK_AS_NOT_SPAM
  override suspend fun getDestinationFolderForIMAP(account: AccountEntity): LocalFolder? =
    withContext(Dispatchers.IO) {
      //we don't have the IMAP realization
      return@withContext null
    }

  override fun getAddAndRemoveLabelIdsForGmailAPI(srcFolder: String): Pair<List<String>?, List<String>?> {
    return Pair(
      listOf(GmailApiHelper.LABEL_INBOX),
      listOf(GmailApiHelper.LABEL_SPAM)
    )
  }

  companion object {
    const val GROUP_UNIQUE_TAG = BuildConfig.APPLICATION_ID + ".MARK_MESSAGES_AS_NOT_SPAM"

    fun enqueue(context: Context) {
      enqueueWithDefaultParameters<MarkAsNotSpamWorker>(
        context = context,
        uniqueWorkName = GROUP_UNIQUE_TAG,
        existingWorkPolicy = ExistingWorkPolicy.REPLACE
      )
    }
  }
}