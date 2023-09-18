/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.workmanager.sync

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkerParameters
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.database.entity.AccountEntity
import com.sun.mail.imap.IMAPFolder
import jakarta.mail.Folder
import jakarta.mail.Store
import jakarta.mail.UIDFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @author Denys Bondarenko
 */
class InboxIdleMsgsRemovedWorker(context: Context, params: WorkerParameters) : BaseIdleWorker(
  context,
  params
) {
  override suspend fun runIMAPAction(accountEntity: AccountEntity, store: Store) {
    syncInboxAndRemoveRedundantMsgs(accountEntity, store)
  }

  override suspend fun runAPIAction(accountEntity: AccountEntity) {

  }

  private suspend fun syncInboxAndRemoveRedundantMsgs(accountEntity: AccountEntity, store: Store) =
    withContext(Dispatchers.IO) {
      val foldersManager = FoldersManager.fromDatabaseSuspend(applicationContext, accountEntity)
      val inboxLocalFolder = foldersManager.findInboxFolder() ?: return@withContext
      val folderFullName = inboxLocalFolder.fullName

      store.getFolder(folderFullName).use { folder ->
        val remoteFolder = (folder as IMAPFolder).apply { open(Folder.READ_ONLY) }
        val oldestCachedUID = roomDatabase.msgDao()
          .getOldestUIDOfMsgForLabelSuspend(accountEntity.email, folderFullName) ?: 0
        val cachedUIDSet =
          roomDatabase.msgDao().getUIDsForLabel(accountEntity.email, folderFullName).toSet()
        val updatedMsgs = EmailUtil.getUpdatedMsgsByUID(
          folder = remoteFolder,
          first = oldestCachedUID.toLong(),
          end = UIDFolder.LASTUID,
          fetchFlags = false
        )

        processDeletedMsgs(cachedUIDSet, remoteFolder, updatedMsgs, accountEntity, folderFullName)
      }
    }

  companion object {
    const val GROUP_UNIQUE_TAG = BuildConfig.APPLICATION_ID + ".INBOX_IDLE_MESSAGES_REMOVED"

    fun enqueue(context: Context) {
      enqueueWithDefaultParameters<InboxIdleMsgsRemovedWorker>(
        context = context,
        uniqueWorkName = GROUP_UNIQUE_TAG,
        existingWorkPolicy = ExistingWorkPolicy.REPLACE
      )
    }
  }
}
