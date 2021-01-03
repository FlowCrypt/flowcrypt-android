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
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.sun.mail.imap.IMAPFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Store

/**
 * This task moves messages back to INBOX
 *
 * @author Denis Bondarenko
 *         Date: 10/18/19
 *         Time: 6:14 PM
 *         E-mail: DenBond7@gmail.com
 */
class MovingToInboxWorker(context: Context, params: WorkerParameters) : BaseSyncWorker(context, params) {
  override suspend fun runIMAPAction(accountEntity: AccountEntity, store: Store) {
    moveMessagesToInbox(accountEntity, store)
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
        val remoteDestFolder = store.getFolder(inboxFolder.fullName) as IMAPFolder

        for (srcFolder in setOfFolders) {
          val filteredMsgs = candidatesForMovingToInbox.filter { it.folder == srcFolder }

          if (filteredMsgs.isEmpty() || JavaEmailConstants.FOLDER_OUTBOX.equals(srcFolder, ignoreCase = true)) {
            continue
          }

          store.getFolder(srcFolder).use { folder ->
            val uidList = filteredMsgs.map { it.uid }
            val remoteSrcFolder = (folder as IMAPFolder).apply { open(Folder.READ_WRITE) }
            val msgs: List<Message> = remoteSrcFolder.getMessagesByUID(uidList.toLongArray()).filterNotNull()

            if (msgs.isNotEmpty()) {
              remoteSrcFolder.moveMessages(msgs.toTypedArray(), remoteDestFolder)

              val movedMessages = candidatesForMovingToInbox.filter { it.uid in uidList }.map { it.copy(state = MessageState.NONE.value) }
              roomDatabase.msgDao().updateSuspend(movedMessages)
            }
          }
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
              OneTimeWorkRequestBuilder<MovingToInboxWorker>()
                  .addTag(TAG_SYNC)
                  .setConstraints(constraints)
                  .build()
          )
    }
  }
}