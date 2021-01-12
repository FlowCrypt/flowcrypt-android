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
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.database.entity.AccountEntity
import com.sun.mail.imap.IMAPFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.mail.FetchProfile
import javax.mail.Folder
import javax.mail.Store
import javax.mail.UIDFolder

/**
 * This task does a job of loading all new messages which not exist in the cache but exist on the server and updates
 * existing messages in the local database.
 *
 * @author DenBond7
 * Date: 22.06.2017
 * Time: 17:12
 * E-mail: DenBond7@gmail.com
 */
open class InboxIdleSyncWorker(context: Context, params: WorkerParameters) : BaseIdleWorker(context, params) {

  override suspend fun runIMAPAction(accountEntity: AccountEntity, store: Store) {
    syncMessages(accountEntity, store)
  }

  override suspend fun runAPIAction(accountEntity: AccountEntity) {

  }

  private suspend fun syncMessages(accountEntity: AccountEntity, store: Store) = withContext(Dispatchers.IO) {
    val foldersManager = FoldersManager.fromDatabaseSuspend(applicationContext, accountEntity)
    val inboxLocalFolder = foldersManager.findInboxFolder() ?: return@withContext
    val folderFullName = inboxLocalFolder.fullName

    store.getFolder(folderFullName).use { folder ->
      val remoteFolder = (folder as IMAPFolder).apply { open(Folder.READ_ONLY) }
      val mapOfUIDAndMsgFlags = roomDatabase.msgDao().getMapOfUIDAndMsgFlagsSuspend(accountEntity.email, folderFullName)
      val cachedUIDSet = mapOfUIDAndMsgFlags.keys.toSet()

      if (accountEntity.isShowOnlyEncrypted == true) {
        val foundMsgs = remoteFolder.search(EmailUtil.genEncryptedMsgsSearchTerm(accountEntity))
        val fetchProfile = FetchProfile().apply {
          add(UIDFolder.FetchProfileItem.UID)
        }
        remoteFolder.fetch(foundMsgs, fetchProfile)
        val updatedMsgs = EmailUtil.getUpdatedMsgsByUIDs(remoteFolder, cachedUIDSet.toLongArray())
        processUpdatedMsgs(mapOfUIDAndMsgFlags, remoteFolder, updatedMsgs, accountEntity, folderFullName)
        processDeletedMsgs(cachedUIDSet, remoteFolder, updatedMsgs, accountEntity, folderFullName)

        val newestCachedUID = roomDatabase.msgDao()
            .getLastUIDOfMsgForLabelSuspend(accountEntity.email, folderFullName) ?: return@use
        val newMsgs = EmailUtil.fetchMsgs(remoteFolder, foundMsgs.filter { message -> remoteFolder.getUID(message) > newestCachedUID }.toTypedArray())
        val newCandidates = EmailUtil.genNewCandidates(cachedUIDSet, remoteFolder, newMsgs)
        processNewMsgs(newCandidates, accountEntity, inboxLocalFolder, remoteFolder)
      } else {
        val oldestCachedUID = roomDatabase.msgDao()
            .getOldestUIDOfMsgForLabelSuspend(accountEntity.email, folderFullName) ?: return@use
        val allMsgsFromOldestExisted = EmailUtil.getUpdatedMsgsByUID(remoteFolder, oldestCachedUID.toLong(), UIDFolder.LASTUID)
        processDeletedMsgs(cachedUIDSet, remoteFolder, allMsgsFromOldestExisted, accountEntity, folderFullName)
        processUpdatedMsgs(mapOfUIDAndMsgFlags, remoteFolder, allMsgsFromOldestExisted, accountEntity, folderFullName)

        val newCandidates = EmailUtil.genNewCandidates(cachedUIDSet, remoteFolder, allMsgsFromOldestExisted)
        processNewMsgs(newCandidates, accountEntity, inboxLocalFolder, remoteFolder)
      }
    }
  }

  companion object {
    const val GROUP_UNIQUE_TAG = BuildConfig.APPLICATION_ID + ".IDLE_SYNC_INBOX"

    fun enqueue(context: Context) {
      val constraints = Constraints.Builder()
          .setRequiredNetworkType(NetworkType.CONNECTED)
          .build()

      WorkManager
          .getInstance(context.applicationContext)
          .enqueueUniqueWork(
              GROUP_UNIQUE_TAG,
              ExistingWorkPolicy.REPLACE,
              OneTimeWorkRequestBuilder<InboxIdleSyncWorker>()
                  .addTag(TAG_SYNC)
                  .setConstraints(constraints)
                  .build()
          )
    }
  }
}
