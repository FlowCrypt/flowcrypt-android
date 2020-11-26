/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks

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
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Store

/**
 * @author Denis Bondarenko
 *         Date: 7/3/20
 *         Time: 5:33 PM
 *         E-mail: DenBond7@gmail.com
 */
class DeleteMessagesPermanentlySyncTask(context: Context, params: WorkerParameters) : BaseSyncWorker(context, params) {
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
              deleteMsgsPermanently(activeAccountEntity, store)
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

  private suspend fun deleteMsgsPermanently(account: AccountEntity, store: Store) = withContext(Dispatchers.IO) {
    val foldersManager = FoldersManager.fromDatabase(applicationContext, account.email)
    val trash = foldersManager.folderTrash ?: return@withContext
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)

    while (true) {
      val candidatesForDeleting = roomDatabase.msgDao().getMsgsWithStateSuspend(
          account.email, trash.fullName, MessageState.PENDING_DELETING_PERMANENTLY.value)

      if (candidatesForDeleting.isEmpty()) {
        break
      } else {
        val uidList = candidatesForDeleting.map { it.uid }
        val remoteTrashFolder = store.getFolder(trash.fullName) as IMAPFolder
        remoteTrashFolder.open(Folder.READ_WRITE)

        val msgs: List<Message> = remoteTrashFolder.getMessagesByUID(uidList.toLongArray()).filterNotNull()

        if (msgs.isNotEmpty()) {
          remoteTrashFolder.setFlags(msgs.toTypedArray(), Flags(Flags.Flag.DELETED), true)
          roomDatabase.msgDao().deleteByUIDsSuspend(account.email, trash.fullName, uidList)
        }

        remoteTrashFolder.close(true)
      }
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
              OneTimeWorkRequestBuilder<DeleteMessagesPermanentlySyncTask>()
                  .addTag(TAG_SYNC)
                  .setConstraints(constraints)
                  .build()
          )
    }
  }
}