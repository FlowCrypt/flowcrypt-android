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
import javax.mail.Session
import javax.mail.Store

/**
 * @author Denis Bondarenko
 *         Date: 7/7/20
 *         Time: 10:19 AM
 *         E-mail: DenBond7@gmail.com
 */
class EmptyTrashSyncTask(ownerKey: String,
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
    val remoteTrashFolder = store.getFolder(trash.fullName) as IMAPFolder
    remoteTrashFolder.open(Folder.READ_WRITE)

    val msgs = remoteTrashFolder.messages

    if (msgs.isNotEmpty()) {
      roomDatabase.msgDao().changeMsgsState(account.email, trash.fullName, MessageState.PENDING_EMPTY_TRASH.value)
      try {
        remoteTrashFolder.setFlags(msgs, Flags(Flags.Flag.DELETED), true)
        remoteTrashFolder.close(true)
      } catch (e: Exception) {
        roomDatabase.msgDao().changeMsgsState(account.email, trash.fullName)
        throw e
      }

      val candidatesForDeleting = roomDatabase.msgDao().getMsgsAsList(account.email, trash.fullName)
      if (candidatesForDeleting.isNotEmpty()) {
        roomDatabase.msgDao().delete(candidatesForDeleting)
      }
    }

    listener.onActionCompleted(account, ownerKey, requestCode)
  }
}