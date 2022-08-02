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
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.sun.mail.imap.IMAPFolder
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * This task moves marked messages to INBOX folder
 *
 * @author Denis Bondarenko
 *         Date: 10/18/19
 *         Time: 12:02 PM
 *         E-mail: DenBond7@gmail.com
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
    archiveInternal(account) { folderName, uidList ->
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
    archiveInternal(account) { _, uidList ->
      executeGMailAPICall(applicationContext) {
        GmailApiHelper.changeLabels(
          context = applicationContext,
          accountEntity = account,
          ids = uidList.map { java.lang.Long.toHexString(it).lowercase() },
          removeLabelIds = listOf(GmailApiHelper.LABEL_INBOX)
        )
      }
    }
  }

  private suspend fun archiveInternal(
    account: AccountEntity,
    action: suspend (folderName: String, list: List<Long>) -> Unit
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
        val uidList = candidatesForArchiving.map { it.uid }
        action.invoke(inboxFolder.fullName, uidList)
        roomDatabase.msgDao().deleteByUIDsSuspend(account.email, inboxFolder.fullName, uidList)
      }
    }
  }

  companion object {
    const val GROUP_UNIQUE_TAG = BuildConfig.APPLICATION_ID + ".ARCHIVE_MESSAGES"

    fun enqueue(context: Context) {
      val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

      WorkManager
        .getInstance(context.applicationContext)
        .enqueueUniqueWork(
          GROUP_UNIQUE_TAG,
          ExistingWorkPolicy.REPLACE,
          OneTimeWorkRequestBuilder<ArchiveMsgsWorker>()
            .addTag(TAG_SYNC)
            .setConstraints(constraints)
            .build()
        )
    }
  }
}
