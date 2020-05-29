/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks

import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.sync.SyncListener
import com.flowcrypt.email.database.entity.AccountEntity
import com.sun.mail.imap.IMAPFolder

import javax.mail.FetchProfile
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Session
import javax.mail.Store
import javax.mail.UIDFolder

/**
 * This task does job to load messages.
 *
 * @author DenBond7
 * Date: 20.06.2017
 * Time: 15:06
 * E-mail: DenBond7@gmail.com
 */
class LoadMessagesSyncTask(ownerKey: String,
                           requestCode: Int,
                           private val localFolder: LocalFolder,
                           private val start: Int,
                           private val end: Int) : BaseSyncTask(ownerKey, requestCode) {

  override fun runIMAPAction(account: AccountEntity, session: Session, store: Store, listener: SyncListener) {
    val imapFolder = store.getFolder(localFolder.fullName) as IMAPFolder
    imapFolder.open(Folder.READ_ONLY)

    val msgsCount = imapFolder.messageCount

    if (end !in (1..msgsCount) || start < 1) {
      listener.onMsgsReceived(account, localFolder, imapFolder, arrayOf(), ownerKey, requestCode)
    } else {
      val messages: Array<Message>

      if (end == start) {
        messages = arrayOf(imapFolder.getMessage(end))
      } else {
        messages = imapFolder.getMessages(start, end)
      }

      val fetchProfile = FetchProfile()
      fetchProfile.add(FetchProfile.Item.ENVELOPE)
      fetchProfile.add(FetchProfile.Item.FLAGS)
      fetchProfile.add(FetchProfile.Item.CONTENT_INFO)
      fetchProfile.add(UIDFolder.FetchProfileItem.UID)

      imapFolder.fetch(messages, fetchProfile)

      listener.onMsgsReceived(account, localFolder, imapFolder, messages, ownerKey, requestCode)
    }

    imapFolder.close(false)
  }
}
