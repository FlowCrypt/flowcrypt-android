/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks

import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.sync.SyncListener
import com.flowcrypt.email.database.dao.source.AccountDao
import com.sun.mail.imap.IMAPFolder
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Session
import javax.mail.Store

/**
 * This task does job of moving messages.
 *
 * @param sourceLocalFolder      A local implementation of the remote folder which is the source.
 * @param destinationLocalFolder A local implementation of the remote folder which is the destination.
 * @param uids              The [com.sun.mail.imap.protocol.UID] of the moving
 *
 * @author DenBond7
 * Date: 28.06.2017
 * Time: 15:20
 * E-mail: DenBond7@gmail.com
 */

class MoveMessagesSyncTask(ownerKey: String,
                           requestCode: Int,
                           private val sourceLocalFolderName: LocalFolder,
                           private val destinationLocalFolderName: LocalFolder,
                           private val uids: LongArray) : BaseSyncTask(ownerKey, requestCode) {

  @Throws(Exception::class)
  override fun runIMAPAction(account: AccountDao, session: Session, store: Store, listener: SyncListener) {
    val srcFolder = store.getFolder(sourceLocalFolderName.fullName) as IMAPFolder
    val destFolder = store.getFolder(destinationLocalFolderName.fullName) as IMAPFolder

    if (!srcFolder.exists()) {
      throw IllegalArgumentException("The invalid source folder: \"$sourceLocalFolderName\"")
    }

    srcFolder.open(Folder.READ_WRITE)

    val isSingleMoving = uids.size == 1

    val msgs: List<Message> = srcFolder.getMessagesByUID(uids).filterNotNull()

    if (msgs.isNotEmpty()) {
      if (!destFolder.exists()) {
        throw IllegalArgumentException("The invalid destination folder: \"$destFolder\"")
      }

      destFolder.open(Folder.READ_WRITE)
      srcFolder.moveMessages(msgs.toTypedArray(), destFolder)
      if (isSingleMoving) {
        listener.onMsgMoved(account, srcFolder, destFolder, msgs.first(), ownerKey, requestCode)
      } else {
        listener.onMsgsMoved(account, srcFolder, destFolder, msgs, ownerKey, requestCode)
      }

      destFolder.close(false)
    } else {
      if (isSingleMoving) {
        listener.onMsgMoved(account, srcFolder, destFolder, null, ownerKey, requestCode)
      } else {
        listener.onMsgsMoved(account, srcFolder, destFolder, emptyList(), ownerKey, requestCode)
      }
    }

    srcFolder.close(false)
  }
}
