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
import com.flowcrypt.email.api.email.JavaEmailConstants
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
 * This task moves messages back to INBOX
 *
 * @author Denis Bondarenko
 *         Date: 10/18/19
 *         Time: 6:14 PM
 *         E-mail: DenBond7@gmail.com
 */
class MovingToInboxSyncTask(context: Context, params: WorkerParameters) : BaseSyncWorker(context, params) {
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
              moveMessagesToInbox(activeAccountEntity, store)
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

  private suspend fun moveMessagesToInbox(account: AccountEntity, store: Store) = withContext(Dispatchers.IO) {
    val foldersManager = FoldersManager.fromDatabaseSuspend(applicationContext, account.email)
    val inboxFolder = foldersManager.findInboxFolder() ?: return@withContext
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)

    while (true) {
      val candidatesForMovingToInbox = roomDatabase.msgDao().getMsgsWithStateSuspend(account.email,
          MessageState.PENDING_MOVE_TO_INBOX.value)

      if (candidatesForMovingToInbox.isEmpty()) {
        break
      } else {
        val setOfFolders = candidatesForMovingToInbox.map { it.folder }.toSet()

        for (folder in setOfFolders) {
          val filteredMsgs = candidatesForMovingToInbox.filter { it.folder == folder }

          if (filteredMsgs.isEmpty() || JavaEmailConstants.FOLDER_OUTBOX.equals(folder, ignoreCase = true)) {
            continue
          }

          val uidList = filteredMsgs.map { it.uid }
          val remoteSrcFolder = store.getFolder(folder) as IMAPFolder
          val remoteDestFolder = store.getFolder(inboxFolder.fullName) as IMAPFolder
          remoteSrcFolder.open(Folder.READ_WRITE)

          val msgs: List<Message> = remoteSrcFolder.getMessagesByUID(uidList.toLongArray()).filterNotNull()

          if (msgs.isNotEmpty()) {
            remoteSrcFolder.moveMessages(msgs.toTypedArray(), remoteDestFolder)

            val movedMessages = candidatesForMovingToInbox.filter { it.uid in uidList }.map { it.copy(state = MessageState.NONE.value) }
            roomDatabase.msgDao().updateSuspend(movedMessages)
          }

          remoteSrcFolder.close(false)
        }
      }
    }
  }

  companion object {
    const val GROUP_UNIQUE_TAG = BuildConfig.APPLICATION_ID + ".MOVING_MESSAGES_TO_INBOX"

    fun enqueue(context: Context) {
      val constraints = Constraints.Builder()
          .setRequiredNetworkType(NetworkType.CONNECTED)
          .build()

      WorkManager
          .getInstance(context.applicationContext)
          .enqueueUniqueWork(
              GROUP_UNIQUE_TAG,
              ExistingWorkPolicy.REPLACE,
              OneTimeWorkRequestBuilder<MovingToInboxSyncTask>()
                  .addTag(TAG_SYNC)
                  .setConstraints(constraints)
                  .build()
          )
    }
  }
}