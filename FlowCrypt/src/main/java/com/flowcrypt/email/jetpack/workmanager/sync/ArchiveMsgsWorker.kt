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
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.angus.mail.imap.IMAPFolder

/**
 * This task moves marked messages to Archive folder
 *
 * @author Denys Bondarenko
 */
class ArchiveMsgsWorker(context: Context, params: WorkerParameters) :
  BaseSyncWorker(context, params) {
  override suspend fun runIMAPAction(accountEntity: AccountEntity, store: Store) {
    archive(accountEntity, store)
  }

  override suspend fun runAPIAction(accountEntity: AccountEntity) {
    archive(accountEntity)
  }

  private suspend fun archive(account: AccountEntity, store: Store) = withContext(Dispatchers.IO) {
    archiveInternal(account) { folderName, entities ->
      val uidList = entities.map { it.uid }
      val foldersManager = FoldersManager.fromDatabaseSuspend(applicationContext, account)
      val allMailFolder = foldersManager.folderAll ?: return@archiveInternal

      store.getFolder(folderName).use { folder ->
        val remoteSrcFolder = (folder as IMAPFolder).apply { open(Folder.READ_WRITE) }
        val msgs: List<Message> =
          remoteSrcFolder.getMessagesByUID(uidList.toLongArray()).filterNotNull()

        if (msgs.isNotEmpty()) {
          val remoteDestFolder = store.getFolder(allMailFolder.fullName) as IMAPFolder
          remoteSrcFolder.moveMessages(msgs.toTypedArray(), remoteDestFolder)
        }
      }
    }
  }

  private suspend fun archive(account: AccountEntity) = withContext(Dispatchers.IO) {
    archiveInternal(account) { _, entities ->
      executeGMailAPICall(applicationContext) {
        if (account.useConversationMode) {
          GmailApiHelper.changeLabelsForThreads(
            context = applicationContext,
            accountEntity = account,
            threadIdList = entities.mapNotNull { it.threadId },
            removeLabelIds = listOf(GmailApiHelper.LABEL_INBOX)
          )
        } else {
          val uidList = entities.map { it.uid }
          GmailApiHelper.changeLabels(
            context = applicationContext,
            accountEntity = account,
            ids = uidList.map { java.lang.Long.toHexString(it).lowercase() },
            removeLabelIds = listOf(GmailApiHelper.LABEL_INBOX)
          )
        }
      }
    }
  }

  private suspend fun archiveInternal(
    account: AccountEntity,
    action: suspend (folderName: String, entities: List<MessageEntity>) -> Unit
  ) = withContext(Dispatchers.IO) {
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)
    val foldersManager = FoldersManager.fromDatabaseSuspend(applicationContext, account)
    val inboxFolder = foldersManager.findInboxFolder() ?: return@withContext

    while (true) {
      val candidatesForArchiving = roomDatabase.msgDao()
        .getMsgsWithStateSuspend(account.email, MessageState.PENDING_ARCHIVING.value)
      if (candidatesForArchiving.isEmpty()) {
        break
      } else {
        action.invoke(inboxFolder.fullName, candidatesForArchiving)
        if (account.isGoogleSignInAccount && account.useAPI) {
          val messageEntitiesToBeDeleted =
            candidatesForArchiving.filter { it.folder == JavaEmailConstants.FOLDER_INBOX }.toSet()
          roomDatabase.msgDao().deleteSuspend(messageEntitiesToBeDeleted)
          val messageEntitiesToBeUpdated = candidatesForArchiving - messageEntitiesToBeDeleted
          roomDatabase.msgDao().updateSuspend(messageEntitiesToBeUpdated.map { messageEntity ->
            val labelIds = messageEntity.labelIds
              ?.split(MessageEntity.LABEL_IDS_SEPARATOR)?.toMutableSet()
              ?.apply { remove(JavaEmailConstants.FOLDER_INBOX) }
              ?.joinToString(MessageEntity.LABEL_IDS_SEPARATOR)
            messageEntity.copy(state = MessageState.NONE.value, labelIds = labelIds)
          })
        } else {
          roomDatabase.msgDao().deleteByUIDsSuspend(
            account.email,
            inboxFolder.fullName,
            candidatesForArchiving.map { it.uid })
        }
      }
    }
  }

  companion object {
    const val GROUP_UNIQUE_TAG = BuildConfig.APPLICATION_ID + ".ARCHIVE_MESSAGES"

    fun enqueue(context: Context) {
      enqueueWithDefaultParameters<ArchiveMsgsWorker>(
        context = context,
        uniqueWorkName = GROUP_UNIQUE_TAG,
        existingWorkPolicy = ExistingWorkPolicy.REPLACE
      )
    }
  }
}
