/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.workmanager.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.service.MessagesNotificationManager
import com.flowcrypt.email.util.GeneralUtil
import com.sun.mail.imap.IMAPFolder
import jakarta.mail.Folder
import jakarta.mail.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * This task identifies encrypted messages and updates information about messages in the local database.
 *
 * @author Denis Bondarenko
 * Date: 02.06.2018
 * Time: 14:30
 * E-mail: DenBond7@gmail.com
 */
class CheckIsLoadedMessagesEncryptedWorker(context: Context, params: WorkerParameters) :
  BaseSyncWorker(context, params) {
  override suspend fun runIMAPAction(accountEntity: AccountEntity, store: Store) {
    identifyEncryptedMsgs(accountEntity, store)
  }

  override suspend fun runAPIAction(accountEntity: AccountEntity) {

  }

  private suspend fun identifyEncryptedMsgs(account: AccountEntity, store: Store) =
    withContext(Dispatchers.IO) {
      val folderFullName = inputData.getString(KEY_FOLDER_FULL_NAME) ?: return@withContext
      val foldersManager = FoldersManager.fromDatabaseSuspend(applicationContext, account)
      val localFolder = foldersManager.getFolderByFullName(folderFullName) ?: return@withContext
      val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)
      val uidList = roomDatabase.msgDao().getNotCheckedUIDs(account.email, folderFullName)

      if (uidList.isEmpty()) {
        return@withContext
      }

      store.getFolder(folderFullName).use { folder ->
        val imapFolder = (folder as IMAPFolder).apply { open(Folder.READ_ONLY) }

        val encryptionStates = EmailUtil.getMsgsEncryptionStates(imapFolder, uidList)
        if (encryptionStates.isNotEmpty()) {
          roomDatabase.msgDao()
            .updateEncryptionStates(account.email, folderFullName, encryptionStates)
        }

        val email = account.email
        val folderType = FoldersManager.getFolderType(localFolder)

        if (folderType === FoldersManager.FolderType.INBOX && !GeneralUtil.isAppForegrounded()) {
          val detailsList = roomDatabase.msgDao().getNewMsgsSuspend(email, folderFullName)
          MessagesNotificationManager(applicationContext).notify(
            applicationContext,
            account,
            localFolder,
            detailsList
          )
        }
      }
    }

  companion object {
    const val GROUP_UNIQUE_TAG = BuildConfig.APPLICATION_ID + ".IDENTIFY_ENCRYPTED_MESSAGES"
    private const val KEY_FOLDER_FULL_NAME = "KEY_FOLDER"

    fun enqueue(context: Context, localFolder: LocalFolder) {
      val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

      val inputData = Data.Builder()
        .putString(KEY_FOLDER_FULL_NAME, localFolder.fullName)
        .build()

      WorkManager
        .getInstance(context.applicationContext)
        .enqueueUniqueWork(
          GROUP_UNIQUE_TAG,
          ExistingWorkPolicy.REPLACE,
          OneTimeWorkRequestBuilder<CheckIsLoadedMessagesEncryptedWorker>()
            .setInputData(inputData)
            .addTag(TAG_SYNC)
            .setConstraints(constraints)
            .build()
        )
    }
  }
}
