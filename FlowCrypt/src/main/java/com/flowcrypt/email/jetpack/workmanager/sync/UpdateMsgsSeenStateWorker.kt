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
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.sun.mail.imap.IMAPFolder
import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * This task mark candidates as read/unread.
 *
 * @author Denis Bondarenko
 *         Date: 10/18/19
 *         Time: 12:31 PM
 *         E-mail: DenBond7@gmail.com
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
    changeMsgsReadStateInternal(account, state) { fullFolderName, uidList ->
      store.getFolder(fullFolderName).use { folder ->
        val imapFolder = (folder as IMAPFolder).apply { open(Folder.READ_WRITE) }
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
      changeMsgsReadStateInternal(account, state) { _, uidList ->
        executeGMailAPICall(applicationContext) {
          if (state == MessageState.PENDING_MARK_READ) {
            GmailApiHelper.changeLabels(
              context = applicationContext,
              accountEntity = account,
              ids = uidList.map { java.lang.Long.toHexString(it).lowercase() },
              removeLabelIds = listOf(GmailApiHelper.LABEL_UNREAD)
            )
          } else {
            GmailApiHelper.changeLabels(
              context = applicationContext,
              accountEntity = account,
              ids = uidList.map { java.lang.Long.toHexString(it).lowercase() },
              addLabelIds = listOf(GmailApiHelper.LABEL_UNREAD)
            )
          }
        }
      }
    }

  private suspend fun changeMsgsReadStateInternal(
    account: AccountEntity,
    state: MessageState,
    action: suspend (folderName: String, list: List<Long>) -> Unit
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

        val uidList = filteredMsgs.map { it.uid }
        action.invoke(folderName, uidList)
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
      val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

      WorkManager
        .getInstance(context.applicationContext)
        .enqueueUniqueWork(
          GROUP_UNIQUE_TAG,
          ExistingWorkPolicy.REPLACE,
          OneTimeWorkRequestBuilder<UpdateMsgsSeenStateWorker>()
            .addTag(TAG_SYNC)
            .setConstraints(constraints)
            .build()
        )
    }
  }
}
