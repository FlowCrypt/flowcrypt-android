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
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Session
import javax.mail.Store

/**
 * This task moves marked messages to INBOX folder
 *
 * @author Denis Bondarenko
 *         Date: 10/18/19
 *         Time: 12:02 PM
 *         E-mail: DenBond7@gmail.com
 */
class ArchiveMsgsSyncTask(ownerKey: String, requestCode: Int) : BaseSyncTask(ownerKey, requestCode) {

  override fun runIMAPAction(account: AccountEntity, session: Session, store: Store, listener: SyncListener) {
    val context = listener.context
    val foldersManager = FoldersManager.fromDatabase(context, account.email)
    val inboxFolder = foldersManager.findInboxFolder()

    if (inboxFolder == null) {
      listener.onActionCompleted(account, ownerKey, requestCode)
      return
    }

    val allMailFolder = foldersManager.folderAll

    if (allMailFolder == null) {
      listener.onActionCompleted(account, ownerKey, requestCode)
      return
    }

    val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)

    while (true) {
      val candidatesForArchiving = roomDatabase.msgDao().getMsgsWithState(account.email,
          MessageState.PENDING_ARCHIVING.value)

      if (candidatesForArchiving.isEmpty()) {
        break
      } else {
        val uidList = candidatesForArchiving.map { it.uid.toLong() }
        val remoteSrcFolder = store.getFolder(inboxFolder.fullName) as IMAPFolder
        val remoteDestFolder = store.getFolder(allMailFolder.fullName) as IMAPFolder
        remoteSrcFolder.open(Folder.READ_WRITE)
        val msgs: List<Message> = remoteSrcFolder.getMessagesByUID(uidList.toLongArray()).filterNotNull()

        if (msgs.isNotEmpty()) {
          remoteSrcFolder.moveMessages(msgs.toTypedArray(), remoteDestFolder)
          roomDatabase.msgDao().deleteByUIDs(account.email, inboxFolder.fullName, uidList)
        }

        remoteSrcFolder.close()
      }
    }

    listener.onActionCompleted(account, ownerKey, requestCode)
  }
}