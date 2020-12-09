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
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.service.MessagesNotificationManager
import com.flowcrypt.email.util.GeneralUtil
import com.sun.mail.imap.IMAPFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.mail.FetchProfile
import javax.mail.Folder
import javax.mail.Message
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
class InboxIdleSyncWorker(context: Context, params: WorkerParameters) : BaseSyncWorker(context, params) {
  private val notificationManager = MessagesNotificationManager(applicationContext)

  override suspend fun runIMAPAction(accountEntity: AccountEntity, store: Store) {
    syncMessages(accountEntity, store)
  }

  private suspend fun syncMessages(accountEntity: AccountEntity, store: Store) = withContext(Dispatchers.IO) {
    val foldersManager = FoldersManager.fromDatabaseSuspend(applicationContext, accountEntity.email)
    val inboxLocalFolder = foldersManager.findInboxFolder() ?: return@withContext
    val folderFullName = inboxLocalFolder.fullName

    store.getFolder(folderFullName).use { folder ->
      val remoteFolder = (folder as IMAPFolder).apply { open(Folder.READ_ONLY) }
      val newestCachedUID = roomDatabase.msgDao().getLastUIDOfMsgForLabelSuspend(accountEntity.email, folderFullName)
      val oldestCachedUID = roomDatabase.msgDao().getOldestUIDOfMsgForLabelSuspend(accountEntity.email, folderFullName)
      val mapOfUIDAndMsgFlags = roomDatabase.msgDao().getMapOfUIDAndMsgFlagsSuspend(accountEntity.email, folderFullName)
      val cachedUIDSet = mapOfUIDAndMsgFlags.keys.toSet()

      if (accountEntity.isShowOnlyEncrypted == true) {
        val foundMsgs = remoteFolder.search(EmailUtil.genEncryptedMsgsSearchTerm(accountEntity))
        val fetchProfile = FetchProfile().apply {
          add(UIDFolder.FetchProfileItem.UID)
        }
        remoteFolder.fetch(foundMsgs, fetchProfile)
        EmailUtil.fetchMsgs(remoteFolder, foundMsgs.filter { message -> remoteFolder.getUID(message) > newestCachedUID }.toTypedArray())
      } else {
        val allMsgsFromOldestExisted = EmailUtil.getUpdatedMsgsByUID(remoteFolder, oldestCachedUID.toLong(), UIDFolder.LASTUID)

        //delete not found messages
        val deleteCandidatesUIDs = EmailUtil.genDeleteCandidates(cachedUIDSet, remoteFolder, allMsgsFromOldestExisted)
        roomDatabase.msgDao().deleteByUIDsSuspend(accountEntity.email, folderFullName, deleteCandidatesUIDs)
        if (!GeneralUtil.isAppForegrounded()) {
          for (uid in deleteCandidatesUIDs) {
            notificationManager.cancel(uid.toInt())
          }
        }

        //update flags of existed messages
        val updateCandidates = EmailUtil.genUpdateCandidates(mapOfUIDAndMsgFlags, remoteFolder, allMsgsFromOldestExisted)
            .map { remoteFolder.getUID(it) to it.flags }.toMap()
        roomDatabase.msgDao().updateFlagsSuspend(accountEntity.email, folderFullName, updateCandidates)

        //add new messages
        val newCandidates = EmailUtil.genNewCandidates(cachedUIDSet, remoteFolder, allMsgsFromOldestExisted)
        processNewMsgs(newCandidates, accountEntity, inboxLocalFolder, remoteFolder)
      }
    }
  }

  private suspend fun processNewMsgs(newMsgs: Array<Message>, accountEntity: AccountEntity,
                                     localFolder: LocalFolder, remoteFolder: IMAPFolder) = withContext(Dispatchers.IO) {
    if (newMsgs.isNotEmpty()) {
      EmailUtil.fetchMsgs(remoteFolder, newMsgs)
      val msgsEncryptionStates = EmailUtil.getMsgsEncryptionInfo(accountEntity.isShowOnlyEncrypted, remoteFolder, newMsgs)
      val msgEntities = MessageEntity.genMessageEntities(
          context = applicationContext,
          email = accountEntity.email,
          label = localFolder.fullName,
          folder = remoteFolder,
          msgs = newMsgs,
          msgsEncryptionStates = msgsEncryptionStates,
          isNew = !GeneralUtil.isAppForegrounded(),
          areAllMsgsEncrypted = false
      )

      roomDatabase.msgDao().insertWithReplaceSuspend(msgEntities)

      if (!GeneralUtil.isAppForegrounded()) {
        val detailsList = roomDatabase.msgDao().getNewMsgsSuspend(accountEntity.email, localFolder.fullName)
        notificationManager.notify(applicationContext, accountEntity, localFolder, detailsList)
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
