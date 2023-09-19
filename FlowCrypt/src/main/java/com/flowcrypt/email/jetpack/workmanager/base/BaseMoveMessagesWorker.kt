/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.workmanager.base

import android.content.Context
import androidx.work.WorkerParameters
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.jetpack.workmanager.sync.BaseSyncWorker
import com.sun.mail.imap.IMAPFolder
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * @author Denys Bondarenko
 */
abstract class BaseMoveMessagesWorker(context: Context, params: WorkerParameters) :
  BaseSyncWorker(context, params) {

  abstract val queryMessageState: MessageState
  abstract suspend fun getDestinationFolderForIMAP(account: AccountEntity): LocalFolder?
  abstract fun getAddAndRemoveLabelIdsForGmailAPI(srcFolder: String): Pair<List<String>?, List<String>?>
  override suspend fun runIMAPAction(accountEntity: AccountEntity, store: Store) {
    moveMessages(accountEntity, store)
  }

  override suspend fun runAPIAction(accountEntity: AccountEntity) {
    moveMessages(accountEntity)
  }

  private suspend fun moveMessages(account: AccountEntity, store: Store) =
    withContext(Dispatchers.IO) {
      val localDestinationFolder = getDestinationFolderForIMAP(account) ?: return@withContext

      moveMessagesInternal(account) { folderName, uidList ->
        store.getFolder(folderName).use { folder ->
          val remoteSrcFolder = (folder as IMAPFolder).apply { open(Folder.READ_WRITE) }
          val msgs: List<Message> =
            remoteSrcFolder.getMessagesByUID(uidList.toLongArray()).filterNotNull()
          val remoteDestFolder = store.getFolder(localDestinationFolder.fullName) as IMAPFolder

          if (msgs.isNotEmpty()) {
            remoteSrcFolder.moveMessages(msgs.toTypedArray(), remoteDestFolder)
          }
        }
      }
    }

  private suspend fun moveMessages(account: AccountEntity) = withContext(Dispatchers.IO) {
    moveMessagesInternal(account) { folderName, uidList ->
      executeGMailAPICall(applicationContext) {
        val addAndRemoveLabelsPair = getAddAndRemoveLabelIdsForGmailAPI(folderName)
        GmailApiHelper.changeLabels(
          context = applicationContext,
          accountEntity = account,
          ids = uidList.map { java.lang.Long.toHexString(it).lowercase() },
          addLabelIds = addAndRemoveLabelsPair.first,
          removeLabelIds = addAndRemoveLabelsPair.second
        )
      }

      delay(2000)
    }
  }

  private suspend fun moveMessagesInternal(
    account: AccountEntity,
    action: suspend (folderName: String, uidList: List<Long>) -> Unit
  ) = withContext(Dispatchers.IO) {
    val foldersManager = FoldersManager.fromDatabaseSuspend(applicationContext, account)
    val folderAll = foldersManager.folderAll
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)

    while (true) {
      val candidatesForMoving = roomDatabase.msgDao().getMsgsWithStateSuspend(
        account.email,
        queryMessageState.value
      )

      if (candidatesForMoving.isEmpty()) {
        break
      } else {
        val setOfFolders = candidatesForMoving.map { it.folder }.toSet()
        for (srcFolder in setOfFolders) {
          val filteredMessages = candidatesForMoving.filter { it.folder == srcFolder }
          if (filteredMessages.isEmpty() || JavaEmailConstants.FOLDER_OUTBOX.equals(
              srcFolder,
              ignoreCase = true
            )
          ) {
            continue
          }

          val uidList = filteredMessages.map { it.uid }
          action.invoke(srcFolder, uidList)
          val movedMessages = candidatesForMoving.filter { it.uid in uidList }
            .map { it.copy(state = MessageState.NONE.value) }
          if (srcFolder.equals(folderAll?.fullName, true)) {
            roomDatabase.msgDao().updateSuspend(movedMessages)
          } else {
            roomDatabase.msgDao().deleteSuspend(movedMessages)
          }
        }
      }
    }
  }
}