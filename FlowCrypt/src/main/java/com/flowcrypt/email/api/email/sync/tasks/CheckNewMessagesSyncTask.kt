/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks

import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.sync.SyncListener
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource
import com.sun.mail.imap.IMAPFolder
import javax.mail.FetchProfile
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Session
import javax.mail.Store
import javax.mail.UIDFolder

/**
 * This task does a job of loading all new messages which not exist in the cache but exist on the server.
 *
 * @author Denis Bondarenko
 * Date: 22.06.2018
 * Time: 15:50
 * E-mail: DenBond7@gmail.com
 */
class CheckNewMessagesSyncTask(ownerKey: String,
                               requestCode: Int,
                               val localFolder: LocalFolder) : BaseSyncTask(ownerKey, requestCode) {

  @Throws(Exception::class)
  override fun runIMAPAction(account: AccountDao, session: Session, store: Store, listener: SyncListener) {
    val context = listener.context
    val email = account.email
    val folderAlias = localFolder.folderAlias!!
    val isEncryptedModeEnabled = AccountDaoSource().isEncryptedModeEnabled(context, email)

    val folder = store.getFolder(localFolder.fullName) as IMAPFolder
    folder.open(Folder.READ_ONLY)

    val nextUID = folder.uidNext
    val newestCachedUID = MessageDaoSource().getLastUIDOfMsgInLabel(context, email, folderAlias)
    var newMsgs: Array<Message> = emptyArray()

    if (newestCachedUID < nextUID - 1) {
      if (isEncryptedModeEnabled) {
        val foundMsgs = folder.search(EmailUtil.genEncryptedMsgsSearchTerm(account))

        val fetchProfile = FetchProfile()
        fetchProfile.add(UIDFolder.FetchProfileItem.UID)

        folder.fetch(foundMsgs, fetchProfile)

        val newMsgsList = mutableListOf<Message>()

        for (msg in foundMsgs) {
          if (folder.getUID(msg) > newestCachedUID) {
            newMsgsList.add(msg)
          }
        }

        newMsgs = EmailUtil.fetchMsgs(folder, newMsgsList.toTypedArray())
      } else {
        newMsgs = EmailUtil.fetchMsgs(folder, folder.getMessagesByUID((newestCachedUID + 1).toLong(), nextUID - 1))
      }
    }

    if (newMsgs.isNotEmpty()) {
      val array = EmailUtil.getMsgsEncryptionInfo(isEncryptedModeEnabled, folder, newMsgs)
      listener.onNewMsgsReceived(account, localFolder, folder, newMsgs, array, ownerKey, requestCode)
    }
    folder.close(false)
  }
}
