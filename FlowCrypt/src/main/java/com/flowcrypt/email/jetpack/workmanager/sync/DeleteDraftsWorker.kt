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
import com.flowcrypt.email.database.entity.MessageEntity
import com.sun.mail.imap.IMAPFolder
import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @author Denis Bondarenko
 *         Date: 9/30/22
 *         Time: 10:51 AM
 *         E-mail: DenBond7@gmail.com
 */
class DeleteDraftsWorker(context: Context, params: WorkerParameters) :
  BaseSyncWorker(context, params) {
  override suspend fun runIMAPAction(accountEntity: AccountEntity, store: Store) {
    deleteDrafts(accountEntity, store)
  }

  override suspend fun runAPIAction(accountEntity: AccountEntity) {
    deleteDrafts(accountEntity)
  }

  private suspend fun deleteDrafts(account: AccountEntity, store: Store) =
    withContext(Dispatchers.IO) {
      deleteDraftsInternal(account) { localFolder, messageEntities ->
        store.getFolder(localFolder.fullName).use { folder ->
          val imapFolder = (folder as IMAPFolder).apply { open(Folder.READ_WRITE) }
          val msgs: List<Message> =
            imapFolder.getMessagesByUID(messageEntities.map { it.uid }.toLongArray())
              .filterNotNull()
          if (msgs.isNotEmpty()) {
            imapFolder.setFlags(msgs.toTypedArray(), Flags(Flags.Flag.DELETED), true)
          }
        }

        roomDatabase.msgDao().deleteSuspend(messageEntities)
      }
    }

  private suspend fun deleteDrafts(account: AccountEntity) = withContext(Dispatchers.IO) {
    deleteDraftsInternal(account) { localFolder, messageEntities ->
      executeGMailAPICall(applicationContext) {
        GmailApiHelper.deleteDrafts(
          context = applicationContext,
          accountEntity = account,
          ids = messageEntities.mapNotNull { it.draftId }
        )
        roomDatabase.msgDao().deleteSuspend(messageEntities)
      }
    }
  }

  private suspend fun deleteDraftsInternal(
    account: AccountEntity,
    action: suspend (localFolder: LocalFolder, messageEntities: List<MessageEntity>) -> Unit
  ) = withContext(Dispatchers.IO)
  {
    val foldersManager = FoldersManager.fromDatabaseSuspend(applicationContext, account)
    val drafts = foldersManager.folderDrafts ?: return@withContext

    while (true) {
      val candidatesForDeleting = roomDatabase.msgDao().getMsgsWithStateSuspend(
        account.email, drafts.fullName, MessageState.PENDING_DELETING_DRAFT.value
      )

      if (candidatesForDeleting.isEmpty()) {
        break
      } else {
        action.invoke(drafts, candidatesForDeleting)
      }
    }
  }

  companion object {
    const val GROUP_UNIQUE_TAG = BuildConfig.APPLICATION_ID + ".DELETE_DRAFTS"

    fun enqueue(context: Context) {
      val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

      WorkManager
        .getInstance(context.applicationContext)
        .enqueueUniqueWork(
          GROUP_UNIQUE_TAG,
          ExistingWorkPolicy.KEEP,
          OneTimeWorkRequestBuilder<DeleteDraftsWorker>()
            .addTag(TAG_SYNC)
            .setConstraints(constraints)
            .build()
        )
    }
  }
}

