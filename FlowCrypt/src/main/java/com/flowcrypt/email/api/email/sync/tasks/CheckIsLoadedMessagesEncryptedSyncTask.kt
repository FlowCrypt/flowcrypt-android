/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks

import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.sync.SyncListener
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource
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
open class CheckIsLoadedMessagesEncryptedSyncTask(ownerKey: String,
                                                  requestCode: Int,
                                                  private val localFolder: LocalFolder?)
  : BaseSyncTask(ownerKey, requestCode) {
  override fun runIMAPAction(account: AccountDao, session: Session, store: Store, listener: SyncListener) {
    super.runIMAPAction(account, session, store, listener)
    localFolder?.let {
      val context = listener.context
      val folder = it.folderAlias
      val msgDaoSource = MessageDaoSource()
      val uidList = msgDaoSource.getNotCheckedUIDs(context, account.email, folder) ?: return

      if (uidList.isEmpty()) {
        return
      }

      val imapFolder = store.getFolder(it.fullName) as IMAPFolder
      imapFolder.open(Folder.READ_ONLY)

      val booleanLongSparseArray = EmailUtil.getMsgsEncryptionStates(imapFolder, uidList)

      if (booleanLongSparseArray.size() > 0) {
        msgDaoSource.updateEncryptionStates(context, account.email, folder, booleanLongSparseArray)
      }

      listener.onIdentificationToEncryptionCompleted(account, it, imapFolder, ownerKey, requestCode)

      imapFolder.close(false)
    }
  }
}

