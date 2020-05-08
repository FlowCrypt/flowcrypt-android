/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks

import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.sync.SyncListener
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.sun.mail.imap.IMAPFolder
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Session
import javax.mail.Store


/**
 * This task finds all delete candidates in the local database and use that info to delete
 * messages on the remote server.
 *
 * @author Denis Bondarenko
 *         Date: 10/17/19
 *         Time: 6:16 PM
 *         E-mail: DenBond7@gmail.com
 */
class DeleteMessagesSyncTask(ownerKey: String,
                             requestCode: Int) : BaseSyncTask(ownerKey, requestCode) {

  override fun runIMAPAction(account: AccountEntity, session: Session, store: Store, listener: SyncListener) {
    val context = listener.context
    val foldersManager = FoldersManager.fromDatabase(context, account.email)
    val trash = foldersManager.folderTrash ?: return
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)

    while (true) {
      val candidatesForDeleting = roomDatabase.msgDao().getMsgsWithState(
          account.email, MessageState.PENDING_DELETING.value)

      if (candidatesForDeleting.isEmpty()) {
        break
      } else {
        val setOfFolders = candidatesForDeleting.map { it.folder }.toSet()

        for (folder in setOfFolders) {
          val filteredMsgs = candidatesForDeleting.filter { it.folder == folder }

          if (filteredMsgs.isEmpty() || JavaEmailConstants.FOLDER_OUTBOX.equals(folder, ignoreCase = true)) {
            continue
          }

          val uidList = filteredMsgs.map { it.uid.toLong() }
          val remoteSrcFolder = store.getFolder(folder) as IMAPFolder
          val remoteDestFolder = store.getFolder(trash.fullName) as IMAPFolder
          remoteSrcFolder.open(Folder.READ_WRITE)

          val msgs: List<Message> = remoteSrcFolder.getMessagesByUID(uidList.toLongArray()).filterNotNull()

          if (msgs.isNotEmpty()) {
            remoteSrcFolder.moveMessages(msgs.toTypedArray(), remoteDestFolder)
            roomDatabase.msgDao().deleteByUIDs(account.email, folder, uidList)
          }

          remoteSrcFolder.close()
        }
      }
    }
  }
}