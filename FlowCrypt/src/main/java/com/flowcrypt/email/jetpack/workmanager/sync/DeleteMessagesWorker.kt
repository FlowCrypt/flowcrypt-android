/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.workmanager.sync

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkerParameters
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import org.eclipse.angus.mail.imap.IMAPFolder
import org.eclipse.angus.mail.imap.IMAPStore
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext


/**
 * This task finds all delete candidates in the local database and use that info to move marked
 * messages to TRASH folder on the remote server.
 *
 * @author Denys Bondarenko
 */
class DeleteMessagesWorker(context: Context, params: WorkerParameters) :
  BaseSyncWorker(context, params) {
  override suspend fun runIMAPAction(accountEntity: AccountEntity, store: Store) {
    moveMsgsToTrash(accountEntity, store)
  }

  override suspend fun runAPIAction(accountEntity: AccountEntity) {
    moveMsgsToTrash(accountEntity)
  }

  private suspend fun moveMsgsToTrash(account: AccountEntity) = withContext(Dispatchers.IO) {
    moveMsgsToTrashInternal(account) { _, uidList ->
      executeGMailAPICall(applicationContext) {
        //todo-denbond7 need to improve this logic. We should delete local messages only if we'll
        // success delete remote messages
        GmailApiHelper.moveToTrash(
          context = applicationContext,
          accountEntity = account,
          ids = uidList.map { java.lang.Long.toHexString(it).lowercase() })
        //need to wait while the Gmail server will update labels
        delay(2000)
      }
    }
  }

  private suspend fun moveMsgsToTrash(account: AccountEntity, store: Store) =
    withContext(Dispatchers.IO) {
      moveMsgsToTrashInternal(account) { folderName, uidList ->
        store.getFolder(folderName).use { folder ->
          val foldersManager = FoldersManager.fromDatabaseSuspend(applicationContext, account)
          val trash = foldersManager.folderTrash ?: return@use
          val remoteDestFolder = store.getFolder(trash.fullName) as IMAPFolder
          val remoteSrcFolder = (folder as IMAPFolder).apply { open(Folder.READ_WRITE) }
          val msgs: List<Message> =
            remoteSrcFolder.getMessagesByUID(uidList.toLongArray()).filterNotNull()
          if (msgs.isNotEmpty()) {
            if ((store as IMAPStore).hasCapability("MOVE")) {
              remoteSrcFolder.moveMessages(msgs.toTypedArray(), remoteDestFolder)
            } else {
              remoteSrcFolder.copyMessages(msgs.toTypedArray(), remoteDestFolder)
            }
          }
        }
      }
    }

  private suspend fun moveMsgsToTrashInternal(
    account: AccountEntity,
    action: suspend (folderName: String, list: List<Long>) -> Unit
  ) = withContext(Dispatchers.IO)
  {
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)

    while (true) {
      val candidatesForDeleting = roomDatabase.msgDao().getMsgsWithStateSuspend(
        account.email, MessageState.PENDING_DELETING.value
      )

      if (candidatesForDeleting.isEmpty()) {
        break
      } else {
        val setOfFolders = candidatesForDeleting.map { it.folder }.toSet()

        for (srcFolder in setOfFolders) {
          val filteredMsgs = candidatesForDeleting.filter { it.folder == srcFolder }
          if (filteredMsgs.isEmpty() || JavaEmailConstants.FOLDER_OUTBOX.equals(
              srcFolder,
              ignoreCase = true
            )
          ) {
            continue
          }
          val uidList = filteredMsgs.map { it.uid }
          action.invoke(srcFolder, uidList)
          roomDatabase.msgDao().deleteByUIDsSuspend(account.email, srcFolder, uidList)
        }
      }
    }
  }

  companion object {
    const val GROUP_UNIQUE_TAG = BuildConfig.APPLICATION_ID + ".MOVE_MESSAGES_TO_TRASH"

    fun enqueue(context: Context) {
      enqueueWithDefaultParameters<DeleteMessagesWorker>(
        context = context,
        uniqueWorkName = GROUP_UNIQUE_TAG,
        existingWorkPolicy = ExistingWorkPolicy.REPLACE
      )
    }
  }
}
