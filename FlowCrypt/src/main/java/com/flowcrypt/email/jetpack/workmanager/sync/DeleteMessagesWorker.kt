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
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.extensions.kotlin.toHex
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.eclipse.angus.mail.imap.IMAPFolder
import org.eclipse.angus.mail.imap.IMAPStore


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
    moveMsgsToTrashInternal(account) { _, entities ->
      executeGMailAPICall(applicationContext) {
        if (account.useConversationMode) {
          val resultMap = GmailApiHelper.moveThreadsToTrash(
            context = applicationContext,
            accountEntity = account,
            ids = entities.mapNotNull { it.threadIdAsHEX }.toSet()
          )

          val successList = resultMap.filter { it.value }.keys
          val threadMessageEntitiesToBeDeleted = entities.filter { it.threadIdAsHEX in successList }
          cleanSomeThreadsCache(threadMessageEntitiesToBeDeleted, account)
          //need to wait while the Gmail server will update labels
          delay(2000)
          return@executeGMailAPICall threadMessageEntitiesToBeDeleted
        } else {
          val uidList = entities.map { it.uid.toHex().lowercase() }
          val resultMap = GmailApiHelper.moveToTrash(
            context = applicationContext,
            accountEntity = account,
            ids = uidList
          )
          val successList = resultMap.filter { it.value }.keys
          roomDatabase.msgDao()
            .deleteSuspend(entities.filter { it.uid.toHex().lowercase() in successList })
          //need to wait while the Gmail server will update labels
          delay(2000)
          return@executeGMailAPICall entities
        }
      }
    }
  }

  private suspend fun moveMsgsToTrash(account: AccountEntity, store: Store) =
    withContext(Dispatchers.IO) {
      moveMsgsToTrashInternal(account) { folderName, entities ->
        store.getFolder(folderName).use { folder ->
          val foldersManager = FoldersManager.fromDatabaseSuspend(applicationContext, account)
          val trash = foldersManager.folderTrash ?: return@use
          val remoteDestFolder = store.getFolder(trash.fullName) as IMAPFolder
          val remoteSrcFolder = (folder as IMAPFolder).apply { open(Folder.READ_WRITE) }
          val uidList = entities.map { it.uid }
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

        return@moveMsgsToTrashInternal entities
      }
    }

  private suspend fun moveMsgsToTrashInternal(
    account: AccountEntity,
    action: suspend (folderName: String, entities: List<MessageEntity>) -> List<MessageEntity>
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
          val candidatesToBeDeletedLocally = action.invoke(srcFolder, filteredMsgs)
          roomDatabase.msgDao()
            .deleteByUIDsSuspend(account.email, srcFolder, filteredMsgs.map { it.uid })
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
