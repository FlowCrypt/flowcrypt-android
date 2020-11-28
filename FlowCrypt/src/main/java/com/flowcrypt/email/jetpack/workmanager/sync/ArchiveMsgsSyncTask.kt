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
import com.flowcrypt.email.api.email.IMAPStoreManager
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.jetpack.workmanager.BaseSyncWorker
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.sun.mail.imap.IMAPFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.mail.Folder
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Store

/**
 * This task moves marked messages to INBOX folder
 *
 * @author Denis Bondarenko
 *         Date: 10/18/19
 *         Time: 12:02 PM
 *         E-mail: DenBond7@gmail.com
 */
class ArchiveMsgsSyncTask(context: Context, params: WorkerParameters) : BaseSyncWorker(context, params) {
  override suspend fun doWork(): Result =
      withContext(Dispatchers.IO) {
        if (isStopped) {
          return@withContext Result.success()
        }

        try {
          val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)
          val activeAccountEntity = roomDatabase.accountDao().getActiveAccountSuspend()
          activeAccountEntity?.let {
            val connection = IMAPStoreManager.activeConnections[activeAccountEntity.id]
            connection?.store?.let { store ->
              archive(activeAccountEntity, store)
            }
          }

          return@withContext Result.success()
        } catch (e: Exception) {
          e.printStackTrace()
          ExceptionUtil.handleError(e)
          when (e) {
            is MessagingException -> {
              return@withContext Result.retry()
            }

            else -> return@withContext Result.failure()
          }
        }
      }

  private suspend fun archive(account: AccountEntity, store: Store) = withContext(Dispatchers.IO) {
    val foldersManager = FoldersManager.fromDatabaseSuspend(applicationContext, account.email)
    val inboxFolder = foldersManager.findInboxFolder() ?: return@withContext
    val allMailFolder = foldersManager.folderAll ?: return@withContext
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)

    while (true) {
      val candidatesForArchiving = roomDatabase.msgDao().getMsgsWithStateSuspend(account.email, MessageState.PENDING_ARCHIVING.value)

      if (candidatesForArchiving.isEmpty()) {
        break
      } else {
        val uidList = candidatesForArchiving.map { it.uid }
        val remoteSrcFolder = store.getFolder(inboxFolder.fullName) as IMAPFolder
        val remoteDestFolder = store.getFolder(allMailFolder.fullName) as IMAPFolder
        remoteSrcFolder.open(Folder.READ_WRITE)
        val msgs: List<Message> = remoteSrcFolder.getMessagesByUID(uidList.toLongArray()).filterNotNull()

        if (msgs.isNotEmpty()) {
          remoteSrcFolder.moveMessages(msgs.toTypedArray(), remoteDestFolder)
          roomDatabase.msgDao().deleteByUIDsSuspend(account.email, inboxFolder.fullName, uidList)
        }

        remoteSrcFolder.close()
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
              OneTimeWorkRequestBuilder<ArchiveMsgsSyncTask>()
                  .addTag(TAG_SYNC)
                  .setConstraints(constraints)
                  .build()
          )
    }
  }
}