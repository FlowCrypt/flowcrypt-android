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
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.MessageEntity
import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.angus.mail.imap.IMAPFolder

/**
 * This task mark candidates as read/unread.
 *
 * @author Denys Bondarenko
 */
class UpdateMsgsSeenStateWorker(context: Context, params: WorkerParameters) :
  BaseSyncWorker(context, params) {
  override suspend fun runIMAPAction(accountEntity: AccountEntity, store: Store) {
    changeMsgsReadState(accountEntity, store, MessageState.PENDING_MARK_UNREAD)
    changeMsgsReadState(accountEntity, store, MessageState.PENDING_MARK_READ)
  }

  override suspend fun runAPIAction(accountEntity: AccountEntity) {
    changeMsgsReadState(accountEntity, MessageState.PENDING_MARK_UNREAD)
    changeMsgsReadState(accountEntity, MessageState.PENDING_MARK_READ)
  }

  private suspend fun changeMsgsReadState(
    account: AccountEntity,
    store: Store,
    state: MessageState
  ) = withContext(Dispatchers.IO) {
    changeMsgsReadStateInternal(account, state) { fullFolderName, entities ->
      store.getFolder(fullFolderName).use { folder ->
        val imapFolder = (folder as IMAPFolder).apply { open(Folder.READ_WRITE) }
        val uidList = entities.map { it.uid }
        val msgs: List<Message> = imapFolder.getMessagesByUID(uidList.toLongArray()).filterNotNull()
        if (msgs.isNotEmpty()) {
          imapFolder.setFlags(
            msgs.toTypedArray(),
            Flags(Flags.Flag.SEEN),
            state == MessageState.PENDING_MARK_READ
          )
        }
      }
    }
  }

  private suspend fun changeMsgsReadState(account: AccountEntity, state: MessageState) =
    withContext(Dispatchers.IO) {
      changeMsgsReadStateInternal(account, state) { _, entities ->
        executeGMailAPICall(applicationContext) {
          val removeLabelIds = if (state == MessageState.PENDING_MARK_READ) {
            listOf(GmailApiHelper.LABEL_UNREAD)
          } else null

          val addLabelIds = if (state == MessageState.PENDING_MARK_READ) {
            null
          } else listOf(GmailApiHelper.LABEL_UNREAD)

          if (account.useConversationMode) {
            GmailApiHelper.changeLabelsForThreads(
              context = applicationContext,
              accountEntity = account,
              threadIdList = entities.mapNotNull { it.threadIdAsHEX }.toSet(),
              removeLabelIds = removeLabelIds,
              addLabelIds = addLabelIds
            )
          } else {
            GmailApiHelper.changeLabels(
              context = applicationContext,
              accountEntity = account,
              ids = entities.map { java.lang.Long.toHexString(it.uid).lowercase() },
              removeLabelIds = removeLabelIds,
              addLabelIds = addLabelIds
            )
          }
        }
      }
    }

  private suspend fun changeMsgsReadStateInternal(
    account: AccountEntity,
    state: MessageState,
    action: suspend (folderName: String, entities: List<MessageEntity>) -> Unit
  ) = withContext(Dispatchers.IO) {
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)
    val candidatesForMark =
      roomDatabase.msgDao().getMsgsWithStateSuspend(account.email, state.value)

    if (candidatesForMark.isNotEmpty()) {
      val setOfFolders = candidatesForMark.map { it.folder }.toSet()

      for (folderName in setOfFolders) {
        val filteredMsgs = candidatesForMark.filter { it.folder == folderName }

        if (filteredMsgs.isEmpty()) {
          continue
        }

        action.invoke(folderName, filteredMsgs)
        val uidList = filteredMsgs.map { it.uid }
        val entities = roomDatabase.msgDao().getMsgsByUIDs(account.email, folderName, uidList)
          .filter { it.msgState == state }
          .map { it.copy(state = MessageState.NONE.value) }
        roomDatabase.msgDao().updateSuspend(entities)
      }
    }
  }

  companion object {
    const val GROUP_UNIQUE_TAG =
      BuildConfig.APPLICATION_ID + ".UPDATE_MESSAGES_SEEN_STATE_ON_SERVER"

    fun enqueue(context: Context) {
      enqueueWithDefaultParameters<UpdateMsgsSeenStateWorker>(
        context = context,
        uniqueWorkName = GROUP_UNIQUE_TAG,
        existingWorkPolicy = ExistingWorkPolicy.REPLACE
      )
    }
  }
}
