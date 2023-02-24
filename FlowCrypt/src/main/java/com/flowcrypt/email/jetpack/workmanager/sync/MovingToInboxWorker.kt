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
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.sun.mail.imap.IMAPFolder
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * This task moves messages back to INBOX
 *
 * @author Denys Bondarenko
 */
class MovingToInboxWorker(context: Context, params: WorkerParameters) :
  BaseSyncWorker(context, params) {
  override suspend fun runIMAPAction(accountEntity: AccountEntity, store: Store) {
    moveMessagesToInbox(accountEntity, store)
  }

  override suspend fun runAPIAction(accountEntity: AccountEntity) {
    moveMessagesToInbox(accountEntity)
  }

  private suspend fun moveMessagesToInbox(account: AccountEntity, store: Store) =
    withContext(Dispatchers.IO) {
      val foldersManager = FoldersManager.fromDatabaseSuspend(applicationContext, account)
      val inboxFolder = foldersManager.findInboxFolder() ?: return@withContext

      moveMessagesToInboxInternal(account) { folderName, uidList ->
        store.getFolder(folderName).use { folder ->
          val remoteSrcFolder = (folder as IMAPFolder).apply { open(Folder.READ_WRITE) }
          val msgs: List<Message> =
            remoteSrcFolder.getMessagesByUID(uidList.toLongArray()).filterNotNull()
          val remoteDestFolder = store.getFolder(inboxFolder.fullName) as IMAPFolder

          if (msgs.isNotEmpty()) {
            remoteSrcFolder.moveMessages(msgs.toTypedArray(), remoteDestFolder)
          }
        }
      }
    }

  private suspend fun moveMessagesToInbox(account: AccountEntity) = withContext(Dispatchers.IO) {
    moveMessagesToInboxInternal(account) { folderName, uidList ->
      executeGMailAPICall(applicationContext) {
        GmailApiHelper.changeLabels(
          context = applicationContext,
          accountEntity = account,
          ids = uidList.map { java.lang.Long.toHexString(it).lowercase() },
          addLabelIds = listOf(GmailApiHelper.LABEL_INBOX),
          removeLabelIds = if (GmailApiHelper.LABEL_TRASH.equals(folderName, true)) listOf(
            GmailApiHelper.LABEL_TRASH
          ) else null
        )
      }

      delay(2000)
    }
  }

  private suspend fun moveMessagesToInboxInternal(
    account: AccountEntity,
    action: suspend (folderName: String, uidList: List<Long>) -> Unit
  ) = withContext(Dispatchers.IO) {
    val foldersManager = FoldersManager.fromDatabaseSuspend(applicationContext, account)
    val folderTrash = foldersManager.folderTrash
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)

    while (true) {
      val candidatesForMovingToInbox = roomDatabase.msgDao().getMsgsWithStateSuspend(
        account.email,
        MessageState.PENDING_MOVE_TO_INBOX.value
      )

      if (candidatesForMovingToInbox.isEmpty()) {
        break
      } else {
        val setOfFolders = candidatesForMovingToInbox.map { it.folder }.toSet()
        for (srcFolder in setOfFolders) {
          val filteredMsgs = candidatesForMovingToInbox.filter { it.folder == srcFolder }
          if (filteredMsgs.isEmpty() || JavaEmailConstants.FOLDER_OUTBOX.equals(
              srcFolder,
              ignoreCase = true
            )
          ) {
            continue
          }

          val uidList = filteredMsgs.map { it.uid }
          action.invoke(srcFolder, uidList)
          val movedMessages = candidatesForMovingToInbox.filter { it.uid in uidList }
            .map { it.copy(state = MessageState.NONE.value) }
          if (srcFolder.equals(folderTrash?.fullName, true)) {
            roomDatabase.msgDao().deleteSuspend(movedMessages)
          } else {
            roomDatabase.msgDao().updateSuspend(movedMessages)
          }
        }
      }
    }
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
