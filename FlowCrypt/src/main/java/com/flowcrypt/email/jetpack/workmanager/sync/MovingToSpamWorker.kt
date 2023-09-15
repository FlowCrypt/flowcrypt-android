/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.workmanager.sync

import android.content.Context
import androidx.work.WorkerParameters
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.jetpack.workmanager.base.BaseMoveMessagesWorker

/**
 * @author Denys Bondarenko
 */
class MovingToSpamWorker(context: Context, params: WorkerParameters) :
  BaseMoveMessagesWorker(context, params) {
  override val queryMessageState: MessageState = MessageState.PENDING_MOVE_TO_SPAM

  override suspend fun getDestinationFolderForIMAP(account: AccountEntity): LocalFolder? {
    TODO("Not yet implemented")
  }

  override fun getAddAndRemoveLabelIdsForGmailAPI(srcFolder: String): Pair<List<String>?, List<String>?> {
    return Pair(
      listOf(GmailApiHelper.LABEL_SPAM),
      null
    )
  }

  companion object {
    const val GROUP_UNIQUE_TAG = BuildConfig.APPLICATION_ID + ".MOVING_MESSAGES_TO_SPAM"

    fun enqueue(context: Context) {
      enqueueWithDefaultParameters<MovingToSpamWorker>(context, GROUP_UNIQUE_TAG)
    }
  }
}