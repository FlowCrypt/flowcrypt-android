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
import com.sun.mail.imap.IMAPStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.*
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Store


/**
 * This task finds all delete candidates in the local database and use that info to move marked
 * messages to TRASH folder on the remote server.
 *
 * @author Denis Bondarenko
 *         Date: 10/17/19
 *         Time: 6:16 PM
 *         E-mail: DenBond7@gmail.com
 */
class DeleteMessagesWorker(context: Context, params: WorkerParameters) : BaseSyncWorker(context, params) {
  override suspend fun runIMAPAction(accountEntity: AccountEntity, store: Store) {
    moveMsgsToTrash(accountEntity, store)
  }

  override suspend fun runAPIAction(accountEntity: AccountEntity) {
    moveMsgsToTrash(accountEntity)
  }

  private suspend fun moveMsgsToTrash(account: AccountEntity) = withContext(Dispatchers.IO) {
    moveMsgsToTrashInternal(account) { _, uidList ->
      GmailApiHelper.moveToTrash(applicationContext, account, uidList.map { java.lang.Long.toHexString(it).toLowerCase(Locale.US) })
      //need to wait while the Gmail server will update labels
      delay(2000)
    }
  }

  private suspend fun moveMsgsToTrash(account: AccountEntity, store: Store) = withContext(Dispatchers.IO) {
    moveMsgsToTrashInternal(account) { folderName, uidList ->
      store.getFolder(folderName).use { folder ->
        val foldersManager = FoldersManager.fromDatabaseSuspend(applicationContext, account)
        val trash = foldersManager.folderTrash ?: return@use
        val remoteDestFolder = store.getFolder(trash.fullName) as IMAPFolder
        val remoteSrcFolder = (folder as IMAPFolder).apply { open(Folder.READ_WRITE) }
        val msgs: List<Message> = remoteSrcFolder.getMessagesByUID(uidList.toLongArray()).filterNotNull()
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

  private suspend fun moveMsgsToTrashInternal(account: AccountEntity,
                                              action: suspend (folderName: String, list: List<Long>) -> Unit) = withContext(Dispatchers.IO)
  {
    try {
      val foldersManager = FoldersManager.fromDatabaseSuspend(applicationContext, account)
      val trash = foldersManager.folderTrash ?: return@withContext
      val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)

      while (true) {
        val candidatesForDeleting = roomDatabase.msgDao().getMsgsWithStateSuspend(
            account.email, MessageState.PENDING_DELETING.value)

        if (candidatesForDeleting.isEmpty()) {
          break
        } else {
          val setOfFolders = candidatesForDeleting.map { it.folder }.toSet()

          for (srcFolder in setOfFolders) {
            val filteredMsgs = candidatesForDeleting.filter { it.folder == srcFolder }
            if (filteredMsgs.isEmpty() || JavaEmailConstants.FOLDER_OUTBOX.equals(srcFolder, ignoreCase = true)) {
              continue
            }
            val uidList = filteredMsgs.map { it.uid }
            action.invoke(trash.fullName, uidList)
            roomDatabase.msgDao().deleteByUIDsSuspend(account.email, srcFolder, uidList)
          }
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  companion object {
    const val GROUP_UNIQUE_TAG = BuildConfig.APPLICATION_ID + ".MOVE_MESSAGES_TO_TRASH"

    fun enqueue(context: Context) {
      val constraints = Constraints.Builder()
          .setRequiredNetworkType(NetworkType.CONNECTED)
          .build()

      WorkManager
          .getInstance(context.applicationContext)
          .enqueueUniqueWork(
              GROUP_UNIQUE_TAG,
              ExistingWorkPolicy.REPLACE,
              OneTimeWorkRequestBuilder<DeleteMessagesWorker>()
                  .addTag(TAG_SYNC)
                  .setConstraints(constraints)
                  .build()
          )
    }
  }
}