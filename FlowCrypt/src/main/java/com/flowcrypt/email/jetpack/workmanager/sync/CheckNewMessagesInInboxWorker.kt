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
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.service.MessagesNotificationManager
import com.flowcrypt.email.util.GeneralUtil
import com.sun.mail.imap.IMAPFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.mail.FetchProfile
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Store
import javax.mail.UIDFolder

/**
 * This task does a job of loading all new messages which not exist in the cache but exist on the server.
 *
 * @author Denis Bondarenko
 * Date: 22.06.2018
 * Time: 15:50
 * E-mail: DenBond7@gmail.com
 */
class CheckNewMessagesInInboxWorker(context: Context, params: WorkerParameters) : BaseSyncWorker(context, params) {
  override suspend fun runIMAPAction(accountEntity: AccountEntity, store: Store) {
    checkAndProcessNewMessages(accountEntity, store)
  }

  private suspend fun checkAndProcessNewMessages(accountEntity: AccountEntity, store: Store) = withContext(Dispatchers.IO) {
    val foldersManager = FoldersManager.fromDatabaseSuspend(applicationContext, accountEntity.email)
    val inboxLocalFolder = foldersManager.findInboxFolder() ?: return@withContext
    val folderFullName = inboxLocalFolder.fullName

    store.getFolder(folderFullName).use { folder ->
      val remoteFolder = (folder as IMAPFolder).apply { open(Folder.READ_ONLY) }
      val newestCachedUID = roomDatabase.msgDao().getLastUIDOfMsgForLabelSuspend(accountEntity.email, folderFullName)
      val cachedUIDSet = roomDatabase.msgDao().getUIDsForLabel(accountEntity.email, folderFullName).toSet()
      val newMsgs = if (accountEntity.isShowOnlyEncrypted == true) {
        val foundMsgs = remoteFolder.search(EmailUtil.genEncryptedMsgsSearchTerm(accountEntity))

        val fetchProfile = FetchProfile()
        fetchProfile.add(UIDFolder.FetchProfileItem.UID)

        remoteFolder.fetch(foundMsgs, fetchProfile)

        val newMsgsList = mutableListOf<Message>()

        for (message in foundMsgs) {
          if (remoteFolder.getUID(message) > newestCachedUID) {
            newMsgsList.add(message)
          }
        }

        EmailUtil.fetchMsgs(remoteFolder, newMsgsList.toTypedArray())
      } else {
        val newestMsgsFromFetchExceptExisted = remoteFolder.getMessagesByUID(newestCachedUID.toLong(), UIDFolder.LASTUID)
            .filterNot { remoteFolder.getUID(it) in cachedUIDSet }
        EmailUtil.fetchMsgs(remoteFolder, newestMsgsFromFetchExceptExisted.toTypedArray())
      }

      if (newMsgs.isNotEmpty()) {
        val msgsEncryptionStates = EmailUtil.getMsgsEncryptionInfo(accountEntity.isShowOnlyEncrypted, folder, newMsgs)
        val msgEntities = MessageEntity.genMessageEntities(
            context = applicationContext,
            email = accountEntity.email,
            label = folderFullName,
            folder = remoteFolder,
            msgs = newMsgs,
            msgsEncryptionStates = msgsEncryptionStates,
            isNew = !GeneralUtil.isAppForegrounded(),
            areAllMsgsEncrypted = false
        )

        roomDatabase.msgDao().insertWithReplaceSuspend(msgEntities)

        if (!GeneralUtil.isAppForegrounded()) {
          val detailsList = roomDatabase.msgDao().getNewMsgsSuspend(accountEntity.email, folderFullName)
          MessagesNotificationManager(applicationContext)
              .notify(applicationContext, accountEntity, inboxLocalFolder, detailsList)
        }
      }
    }
  }

  companion object {
    const val GROUP_UNIQUE_TAG = BuildConfig.APPLICATION_ID + ".CHECK_NEW_MESSAGES_IN_INBOX"

    fun enqueue(context: Context) {
      val constraints = Constraints.Builder()
          .setRequiredNetworkType(NetworkType.CONNECTED)
          .build()

      WorkManager
          .getInstance(context.applicationContext)
          .enqueueUniqueWork(
              GROUP_UNIQUE_TAG,
              ExistingWorkPolicy.REPLACE,
              OneTimeWorkRequestBuilder<CheckNewMessagesInInboxWorker>()
                  .addTag(TAG_SYNC)
                  .setConstraints(constraints)
                  .build()
          )
    }
  }
}
