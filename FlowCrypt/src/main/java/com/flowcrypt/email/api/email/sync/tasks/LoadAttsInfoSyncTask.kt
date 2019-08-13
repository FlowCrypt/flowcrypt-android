/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks

import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.sync.SyncListener
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.imap.AttachmentDaoSource
import com.sun.mail.imap.IMAPFolder
import javax.mail.FetchProfile
import javax.mail.Folder
import javax.mail.Session
import javax.mail.Store
import javax.mail.internet.MimeMessage

/**
 * This task helps download attachments info of some message
 *
 * @author Denis Bondarenko
 *         Date: 7/24/19
 *         Time: 3:06 PM
 *         E-mail: DenBond7@gmail.com
 */
class LoadAttsInfoSyncTask(ownerKey: String,
                           requestCode: Int,
                           private val localFolder: LocalFolder,
                           private val uid: Long) : BaseSyncTask(ownerKey, requestCode) {

  override fun runIMAPAction(account: AccountDao, session: Session, store: Store, listener: SyncListener) {
    val imapFolder = store.getFolder(localFolder.fullName) as IMAPFolder
    imapFolder.open(Folder.READ_ONLY)

    val msg = imapFolder.getMessageByUID(uid) as? MimeMessage

    if (msg == null) {
      listener.onAttsInfoReceived(account, localFolder, imapFolder, uid, ownerKey, requestCode)
      imapFolder.close(false)
      return
    }

    val fetchProfile = FetchProfile()
    fetchProfile.add(FetchProfile.Item.SIZE)
    fetchProfile.add(FetchProfile.Item.CONTENT_INFO)
    imapFolder.fetch(arrayOf(msg), fetchProfile)

    AttachmentDaoSource().updateAttsTable(listener.context, account.email, localFolder.fullName, uid, msg)
    listener.onAttsInfoReceived(account, localFolder, imapFolder, uid, ownerKey, requestCode)

    imapFolder.close(false)
  }
}
