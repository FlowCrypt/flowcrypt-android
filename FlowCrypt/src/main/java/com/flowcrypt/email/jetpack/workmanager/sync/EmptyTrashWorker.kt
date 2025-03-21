/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.workmanager.sync

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkerParameters
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.eclipse.angus.mail.imap.IMAPFolder

/**
 * @author Denys Bondarenko
 */
class EmptyTrashWorker(context: Context, params: WorkerParameters) :
  BaseSyncWorker(context, params) {
  override suspend fun runIMAPAction(accountEntity: AccountEntity, store: Store) {
    emptyTrash(accountEntity, store)
  }

  override suspend fun runAPIAction(accountEntity: AccountEntity) {
    emptyTrash(accountEntity)
  }

  private suspend fun emptyTrash(account: AccountEntity) = withContext(Dispatchers.IO) {
    emptyTrashInternal(account) { folderName ->
      roomDatabase.msgDao()
        .changeMsgsStateSuspend(account.email, folderName, MessageState.PENDING_EMPTY_TRASH.value)
      try {
        executeGMailAPICall(applicationContext) {
          val messages = GmailApiHelper.loadTrashMsgs(applicationContext, account)
          if (messages.isEmpty()) {
            return@executeGMailAPICall
          }

          if (account.useConversationMode) {
            GmailApiHelper.deleteThreadsPermanently(
              applicationContext,
              account,
              messages.mapNotNull { it.threadId }.toSet()
            )
          } else {
            GmailApiHelper.deleteMsgsPermanently(
              applicationContext,
              account,
              messages.map { it.id })
          }
          //need to wait while the Gmail server will update labels
          delay(2000)
        }
      } catch (e: Exception) {
        roomDatabase.msgDao().changeMsgsStateSuspend(account.email, folderName)
        throw e
      }

      val candidatesForDeleting = roomDatabase.msgDao().getMsgsSuspend(account.email, folderName)
      roomDatabase.msgDao().deleteSuspend(candidatesForDeleting)
    }
  }


  private suspend fun emptyTrash(account: AccountEntity, store: Store) =
    withContext(Dispatchers.IO) {
      emptyTrashInternal(account) { folderName ->
        store.getFolder(folderName).use { folder ->
          val remoteTrashFolder = (folder as IMAPFolder).apply { open(Folder.READ_WRITE) }
          val msgs = remoteTrashFolder.messages

          if (msgs.isNotEmpty()) {
            roomDatabase.msgDao().changeMsgsStateSuspend(
              account.email,
              folderName,
              MessageState.PENDING_EMPTY_TRASH.value
            )
            try {
              remoteTrashFolder.setFlags(msgs, Flags(Flags.Flag.DELETED), true)
            } catch (e: Exception) {
              roomDatabase.msgDao().changeMsgsStateSuspend(account.email, folderName)
              throw e
            }

            val candidatesForDeleting =
              roomDatabase.msgDao().getMsgsSuspend(account.email, folderName)
            roomDatabase.msgDao().deleteSuspend(candidatesForDeleting)
          }
        }
      }
    }

  private suspend fun emptyTrashInternal(
    account: AccountEntity,
    action: suspend (folderName: String) -> Unit
  ) = withContext(Dispatchers.IO)
  {
    val foldersManager = FoldersManager.fromDatabaseSuspend(applicationContext, account)
    val trash = foldersManager.folderTrash ?: return@withContext
    action.invoke(trash.fullName)
  }

  companion object {
    const val GROUP_UNIQUE_TAG = BuildConfig.APPLICATION_ID + ".EMPTY_TRASH"

    fun enqueue(context: Context) {
      enqueueWithDefaultParameters<EmptyTrashWorker>(
        context = context,
        uniqueWorkName = GROUP_UNIQUE_TAG,
        existingWorkPolicy = ExistingWorkPolicy.REPLACE
      )
    }
  }
}
