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
import javax.mail.FolderNotFoundException
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Store

/**
 * This task mark candidates as read/unread.
 *
 * @author Denis Bondarenko
 *         Date: 10/18/19
 *         Time: 12:31 PM
 *         E-mail: DenBond7@gmail.com
 */
class ChangeMsgsReadStateSyncTask(context: Context, params: WorkerParameters) : BaseSyncWorker(context, params) {
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
              changeMsgsReadState(activeAccountEntity, store, MessageState.PENDING_MARK_UNREAD)
              changeMsgsReadState(activeAccountEntity, store, MessageState.PENDING_MARK_READ)
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

  private suspend fun changeMsgsReadState(account: AccountEntity, store: Store, state: MessageState) = withContext(Dispatchers.IO) {
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)
    val candidatesForMark = roomDatabase.msgDao().getMsgsWithStateSuspend(account.email, state.value)

    if (candidatesForMark.isNotEmpty()) {
      val setOfFolders = candidatesForMark.map { it.folder }.toSet()

      for (folder in setOfFolders) {
        val filteredMsgs = candidatesForMark.filter { it.folder == folder }

        if (filteredMsgs.isEmpty()) {
          continue
        }

        try {
          val imapFolder = store.getFolder(folder) as IMAPFolder
          imapFolder.open(Folder.READ_WRITE)

          val uidList = filteredMsgs.map { it.uid }
          val msgs: List<Message> = imapFolder.getMessagesByUID(uidList.toLongArray()).filterNotNull()

          if (msgs.isNotEmpty()) {
            val value = state == MessageState.PENDING_MARK_READ
            imapFolder.setFlags(msgs.toTypedArray(), Flags(Flags.Flag.SEEN), value)
            for (uid in uidList) {
              val msgEntity = roomDatabase.msgDao().getMsgSuspend(account.email, folder, uid)

              if (msgEntity?.msgState == state) {
                roomDatabase.msgDao().updateSuspend(msgEntity.copy(state = MessageState.NONE.value))
              }
            }
          }

          imapFolder.close()
        } catch (e: Exception) {
          e.printStackTrace()
          when (e) {
            is FolderNotFoundException -> {
              //don't send ACRA reports
            }

            is MessagingException -> {
              val msg = e.message ?: ""
              if (!msg.equals("folder cannot contain messages", true)) {
                ExceptionUtil.handleError(e)
              }

              throw e
            }

            else -> ExceptionUtil.handleError(e)
          }
        }
      }
    }
  }

  companion object {
    const val GROUP_UNIQUE_TAG = BuildConfig.APPLICATION_ID + ".UPDATE_MESSAGES_SEEN_STATE_ON_SERVER"

    fun enqueue(context: Context) {
      val constraints = Constraints.Builder()
          .setRequiredNetworkType(NetworkType.CONNECTED)
          .build()

      WorkManager
          .getInstance(context.applicationContext)
          .enqueueUniqueWork(
              GROUP_UNIQUE_TAG,
              ExistingWorkPolicy.REPLACE,
              OneTimeWorkRequestBuilder<ChangeMsgsReadStateSyncTask>()
                  .addTag(TAG_SYNC)
                  .setConstraints(constraints)
                  .build()
          )
    }
  }
}