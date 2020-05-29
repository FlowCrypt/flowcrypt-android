/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks

import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.sync.SyncListener
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.sun.mail.imap.IMAPFolder
import java.util.*
import javax.mail.FetchProfile
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Session
import javax.mail.Store
import javax.mail.UIDFolder

/**
 * This task does a job of loading all new messages which not exist in the cache but exist on the server and updates
 * existing messages in the local database.
 *
 * @author DenBond7
 * Date: 22.06.2017
 * Time: 17:12
 * E-mail: DenBond7@gmail.com
 */
class RefreshMessagesSyncTask(ownerKey: String,
                              requestCode: Int,
                              val localFolder: LocalFolder) : BaseSyncTask(ownerKey, requestCode) {

  override fun runIMAPAction(account: AccountEntity, session: Session, store: Store, listener: SyncListener) {
    val context = listener.context
    val folderName = localFolder.fullName
    val imapFolder = store.getFolder(folderName) as IMAPFolder
    imapFolder.open(Folder.READ_ONLY)

    val nextUID = imapFolder.uidNext

    var newMsgs: Array<Message> = emptyArray()
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)

    val newestCachedUID = roomDatabase.msgDao().getLastUIDOfMsgForLabel(account.email, folderName)
    val countOfLoadedMsgs = roomDatabase.msgDao().count(account.email, folderName)
    val isEncryptedModeEnabled = account.isShowOnlyEncrypted

    if (newestCachedUID in (2 until nextUID - 1)) {
      if (isEncryptedModeEnabled == true) {
        val foundMsgs = imapFolder.search(EmailUtil.genEncryptedMsgsSearchTerm(account))

        val fetchProfile = FetchProfile()
        fetchProfile.add(UIDFolder.FetchProfileItem.UID)

        imapFolder.fetch(foundMsgs, fetchProfile)

        val newMsgsList = ArrayList<Message>()

        for (message in foundMsgs) {
          if (imapFolder.getUID(message) > newestCachedUID) {
            newMsgsList.add(message)
          }
        }

        newMsgs = EmailUtil.fetchMsgs(imapFolder, newMsgsList.toTypedArray())
      } else {
        val msgs = imapFolder.getMessagesByUID((newestCachedUID + 1).toLong(), nextUID - 1)
        newMsgs = EmailUtil.fetchMsgs(imapFolder, msgs)
      }
    }

    val updatedMsgs = if (isEncryptedModeEnabled == true) {
      val oldestCachedUID = roomDatabase.msgDao().getOldestUIDOfMsgForLabel(account.email, folderName)
      EmailUtil.getUpdatedMsgsByUID(imapFolder, oldestCachedUID.toLong(), newestCachedUID.toLong())
    } else {
      val countOfNewMsgs = newMsgs.size
      EmailUtil.getUpdatedMsgs(imapFolder, countOfLoadedMsgs, countOfNewMsgs)
    }

    listener.onRefreshMsgsReceived(account, localFolder, imapFolder, newMsgs, updatedMsgs, ownerKey, requestCode)

    imapFolder.close(false)
  }
}
