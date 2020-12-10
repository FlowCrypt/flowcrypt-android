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
import javax.mail.FetchProfile
import javax.mail.Folder
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
class InboxIdleMsgsAddedWorker(context: Context, params: WorkerParameters) : BaseIdleWorker(context, params) {
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

        val fetchProfile = FetchProfile().apply {
          add(UIDFolder.FetchProfileItem.UID)
        }
        remoteFolder.fetch(foundMsgs, fetchProfile)
        EmailUtil.fetchMsgs(remoteFolder, foundMsgs.filter { message -> remoteFolder.getUID(message) > newestCachedUID }.toTypedArray())
      } else {
        val newestMsgsFromFetchExceptExisted = remoteFolder.getMessagesByUID(newestCachedUID.toLong(), UIDFolder.LASTUID)
            .filterNot { remoteFolder.getUID(it) in cachedUIDSet }
        EmailUtil.fetchMsgs(remoteFolder, newestMsgsFromFetchExceptExisted.toTypedArray())
      }

      processNewMsgs(newMsgs, accountEntity, inboxLocalFolder, remoteFolder)
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
              OneTimeWorkRequestBuilder<InboxIdleMsgsAddedWorker>()
                  .addTag(TAG_SYNC)
                  .setConstraints(constraints)
                  .build()
          )
    }
  }
}
