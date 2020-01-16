/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks

import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.sync.SyncListener
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.dao.source.AccountDao
import com.sun.mail.imap.IMAPFolder
import javax.mail.Folder
import javax.mail.Session
import javax.mail.Store

/**
 * This task identifies encrypted messages and updates information about messages in the local database.
 *
 * @property localFolder The local implementation of the remote folder
 *
 * @author Denis Bondarenko
 * Date: 02.06.2018
 * Time: 14:30
 * E-mail: DenBond7@gmail.com
 */
class CheckIsLoadedMessagesEncryptedSyncTask(ownerKey: String,
                                             requestCode: Int,
                                             val localFolder: LocalFolder) : BaseSyncTask(ownerKey, requestCode) {
  override fun runIMAPAction(account: AccountDao, session: Session, store: Store, listener: SyncListener) {
    super.runIMAPAction(account, session, store, listener)

    val context = listener.context
    val folder = localFolder.fullName

    val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)

    val uidList = roomDatabase.msgDao().getNotCheckedUIDs(account.email, folder)

    if (uidList.isEmpty()) {
      return
    }

    val imapFolder = store.getFolder(localFolder.fullName) as IMAPFolder
    imapFolder.open(Folder.READ_ONLY)

    val encryptionStates = EmailUtil.getMsgsEncryptionStates(imapFolder, uidList)
    if (encryptionStates.isNotEmpty()) {
      roomDatabase.msgDao().updateEncryptionStates(account.email, folder, encryptionStates)
    }

    listener.onIdentificationToEncryptionCompleted(account, localFolder, imapFolder, ownerKey, requestCode)

    imapFolder.close(false)
  }
}

