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
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.sun.mail.imap.IMAPFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.MessagingException
import javax.mail.Store

/**
 * @author Denis Bondarenko
 *         Date: 7/7/20
 *         Time: 10:19 AM
 *         E-mail: DenBond7@gmail.com
 */
class EmptyTrashWorker(context: Context, params: WorkerParameters) : BaseSyncWorker(context, params) {
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
              emptyTrash(activeAccountEntity, store)
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

  private suspend fun emptyTrash(account: AccountEntity, store: Store) = withContext(Dispatchers.IO) {
    val foldersManager = FoldersManager.fromDatabaseSuspend(applicationContext, account.email)
    val trash = foldersManager.folderTrash ?: return@withContext
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)
    val remoteTrashFolder = store.getFolder(trash.fullName) as IMAPFolder
    remoteTrashFolder.open(Folder.READ_WRITE)

    val msgs = remoteTrashFolder.messages

    if (msgs.isNotEmpty()) {
      roomDatabase.msgDao().changeMsgsStateSuspend(account.email, trash.fullName, MessageState.PENDING_EMPTY_TRASH.value)
      try {
        remoteTrashFolder.setFlags(msgs, Flags(Flags.Flag.DELETED), true)
        remoteTrashFolder.close(true)
      } catch (e: Exception) {
        roomDatabase.msgDao().changeMsgsStateSuspend(account.email, trash.fullName)
        throw e
      }

      val candidatesForDeleting = roomDatabase.msgDao().getMsgsSuspend(account.email, trash.fullName)
      roomDatabase.msgDao().deleteSuspend(candidatesForDeleting)
    }
  }

  companion object {
    const val GROUP_UNIQUE_TAG = BuildConfig.APPLICATION_ID + ".EMPTY_TRASH"

    fun enqueue(context: Context) {
      val constraints = Constraints.Builder()
          .setRequiredNetworkType(NetworkType.CONNECTED)
          .build()

      WorkManager
          .getInstance(context.applicationContext)
          .enqueueUniqueWork(
              GROUP_UNIQUE_TAG,
              ExistingWorkPolicy.REPLACE,
              OneTimeWorkRequestBuilder<EmptyTrashWorker>()
                  .addTag(TAG_SYNC)
                  .setConstraints(constraints)
                  .build()
          )
    }
  }
}