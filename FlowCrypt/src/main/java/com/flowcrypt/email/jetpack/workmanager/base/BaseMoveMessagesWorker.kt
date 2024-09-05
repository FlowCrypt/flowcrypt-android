/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.workmanager.base

import android.content.Context
import androidx.work.WorkerParameters
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.jetpack.workmanager.sync.BaseSyncWorker
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.eclipse.angus.mail.imap.IMAPFolder

/**
 * @author Denys Bondarenko
 */
abstract class BaseMoveMessagesWorker(context: Context, params: WorkerParameters) :
  BaseSyncWorker(context, params) {

  abstract val queryMessageState: MessageState
  abstract suspend fun getDestinationFolderForIMAP(account: AccountEntity): LocalFolder?
  abstract fun getAddAndRemoveLabelIdsForGmailAPI(srcFolder: String): GmailApiLabelsData

  abstract suspend fun onMessagesMovedOnServer(
    account: AccountEntity,
    srcFolder: String,
    messageEntities: List<MessageEntity>
  )

  override suspend fun runIMAPAction(accountEntity: AccountEntity, store: Store) {
    moveMessages(accountEntity, store)
  }

  override suspend fun runAPIAction(accountEntity: AccountEntity) {
    moveMessages(accountEntity)
  }

  private suspend fun moveMessages(account: AccountEntity, store: Store) =
    withContext(Dispatchers.IO) {
      val localDestinationFolder = getDestinationFolderForIMAP(account) ?: return@withContext

      moveMessagesInternal(account) { folderName, entities ->
        store.getFolder(folderName).use { folder ->
          val remoteSrcFolder = (folder as IMAPFolder).apply { open(Folder.READ_WRITE) }
          val uidList = entities.map { it.uid }
          val msgs: List<Message> =
            remoteSrcFolder.getMessagesByUID(uidList.toLongArray()).filterNotNull()
          val remoteDestFolder = store.getFolder(localDestinationFolder.fullName) as IMAPFolder

          if (msgs.isNotEmpty()) {
            remoteSrcFolder.moveMessages(msgs.toTypedArray(), remoteDestFolder)
          }
        }
      }
    }

  private suspend fun moveMessages(account: AccountEntity) = withContext(Dispatchers.IO) {
    moveMessagesInternal(account) { folderName, entities ->
      executeGMailAPICall(applicationContext) {
        val gmailApiLabelsData = getAddAndRemoveLabelIdsForGmailAPI(folderName)

        if (account.useConversationMode) {
          GmailApiHelper.changeLabelsForThreads(
            context = applicationContext,
            accountEntity = account,
            threadIdList = entities.mapNotNull { it.threadId }.toSet(),
            addLabelIds = gmailApiLabelsData.addLabelIds,
            removeLabelIds = gmailApiLabelsData.removeLabelIds
          )
        } else {
          val uidList = entities.map { it.uid }
          GmailApiHelper.changeLabels(
            context = applicationContext,
            accountEntity = account,
            ids = uidList.map { java.lang.Long.toHexString(it).lowercase() },
            addLabelIds = gmailApiLabelsData.addLabelIds,
            removeLabelIds = gmailApiLabelsData.removeLabelIds
          )
        }
      }

      delay(2000)
    }
  }

  private suspend fun moveMessagesInternal(
    account: AccountEntity,
    action: suspend (folderName: String, messageEntities: List<MessageEntity>) -> Unit
  ) = withContext(Dispatchers.IO) {
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)

    while (true) {
      val messagesToMove = roomDatabase.msgDao().getMsgsWithStateSuspend(
        account.email,
        queryMessageState.value
      )

      if (messagesToMove.isEmpty()) {
        break
      }

      val setOfFolders = messagesToMove.map { it.folder }.toSet()
      for (srcFolder in setOfFolders) {
        val filteredMessages = messagesToMove.filter { it.folder == srcFolder }
        if (filteredMessages.isEmpty() || JavaEmailConstants.FOLDER_OUTBOX.equals(
            srcFolder,
            ignoreCase = true
          )
        ) {
          continue
        }

        action.invoke(srcFolder, filteredMessages)
        val uidList = filteredMessages.map { it.uid }
        val movedMessages = messagesToMove.filter { it.uid in uidList }
          .map { it.copy(state = MessageState.NONE.value) }
        onMessagesMovedOnServer(account, srcFolder, movedMessages)
      }
    }
  }

  data class GmailApiLabelsData(
    val addLabelIds: List<String>? = null,
    val removeLabelIds: List<String>? = null
  )
}