/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks

import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.sync.SyncListener
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.sun.mail.imap.IMAPFolder
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Session
import javax.mail.Store

/**
 * @author Denis Bondarenko
 *         Date: 7/3/20
 *         Time: 5:33 PM
 *         E-mail: DenBond7@gmail.com
 */
class DeleteMessagesPermanentlySyncTask(ownerKey: String,
                                        requestCode: Int) : BaseSyncTask(ownerKey, requestCode) {

  override fun runIMAPAction(account: AccountEntity, session: Session, store: Store, listener: SyncListener) {
    val context = listener.context
    val foldersManager = FoldersManager.fromDatabase(context, account.email)
    val trash = foldersManager.folderTrash

    if (trash == null) {
      listener.onActionCompleted(account, ownerKey, requestCode)
      return
    }

    val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)

    while (true) {
      val candidatesForDeleting = roomDatabase.msgDao().getMsgsWithState(
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
          roomDatabase.msgDao().deleteByUIDs(account.email, trash.fullName, uidList)
        }

        remoteTrashFolder.close(true)
      }
    }

    listener.onActionCompleted(account, ownerKey, requestCode)
  }
}