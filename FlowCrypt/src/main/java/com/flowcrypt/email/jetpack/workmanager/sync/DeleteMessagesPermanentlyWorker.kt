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
 * @author Denys Bondarenko
 */
class DeleteMessagesPermanentlyWorker(context: Context, params: WorkerParameters) :
  BaseSyncWorker(context, params) {
  override suspend fun runIMAPAction(accountEntity: AccountEntity, store: Store) {
    deleteMsgsPermanently(accountEntity, store)
  }

  override suspend fun runAPIAction(accountEntity: AccountEntity) {
    deleteMsgsPermanently(accountEntity)
  }

  private suspend fun deleteMsgsPermanently(account: AccountEntity, store: Store) =
    withContext(Dispatchers.IO) {
      deleteMsgsPermanentlyInternal(account) { folderName, uidList ->
        store.getFolder(folderName).use { folder ->
          val imapFolder = (folder as IMAPFolder).apply { open(Folder.READ_WRITE) }
          val msgs: List<Message> =
            imapFolder.getMessagesByUID(uidList.toLongArray()).filterNotNull()
          if (msgs.isNotEmpty()) {
            imapFolder.setFlags(msgs.toTypedArray(), Flags(Flags.Flag.DELETED), true)
          }
        }
      }
    }

  private suspend fun deleteMsgsPermanently(account: AccountEntity) = withContext(Dispatchers.IO) {
    deleteMsgsPermanentlyInternal(account) { _, uidList ->
      executeGMailAPICall(applicationContext) {
        GmailApiHelper.deleteMsgsPermanently(
          context = applicationContext,
          accountEntity = account,
          ids = uidList.map { java.lang.Long.toHexString(it).lowercase() })
      }
    }
  }

  private suspend fun deleteMsgsPermanentlyInternal(
    account: AccountEntity,
    action: suspend (folderName: String, list: List<Long>) -> Unit
  ) = withContext(Dispatchers.IO)
  {
    val foldersManager = FoldersManager.fromDatabaseSuspend(applicationContext, account)
    val trash = foldersManager.folderTrash ?: return@withContext
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)

    while (true) {
      val candidatesForDeleting = roomDatabase.msgDao().getMsgsWithStateSuspend(
        account.email, trash.fullName, MessageState.PENDING_DELETING_PERMANENTLY.value
      )

      if (candidatesForDeleting.isEmpty()) {
        break
      } else {
        val uidList = candidatesForDeleting.map { it.uid }
        action.invoke(trash.fullName, uidList)
        roomDatabase.msgDao().deleteByUIDsSuspend(account.email, trash.fullName, uidList)
      }
    }
  }

  companion object {
    const val GROUP_UNIQUE_TAG = BuildConfig.APPLICATION_ID + ".DELETE_MESSAGES_PERMANENTLY"

    fun enqueue(context: Context) {
      enqueueWithDefaultParameters<DeleteMessagesPermanentlyWorker>(
        context = context,
        uniqueWorkName = GROUP_UNIQUE_TAG,
        existingWorkPolicy = ExistingWorkPolicy.REPLACE
      )
    }
  }
}
