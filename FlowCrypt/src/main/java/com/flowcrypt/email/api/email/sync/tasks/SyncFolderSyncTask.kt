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
import javax.mail.FetchProfile
import javax.mail.Message
import javax.mail.Session
import javax.mail.Store
import javax.mail.UIDFolder

/**
 * This task does syncing a local folder with a remote. (Server -> client)
 *
 * @author Denis Bondarenko
 * Date: 25.07.2018
 * Time: 14:19
 * E-mail: DenBond7@gmail.com
 */
class SyncFolderSyncTask(ownerKey: String,
                         requestCode: Int,
                         private val localFolder: LocalFolder) : BaseSyncTask(ownerKey, requestCode) {

  override fun runIMAPAction(account: AccountEntity, session: Session, store: Store, listener: SyncListener) {
    val context = listener.context
    val folderName = localFolder.fullName
    val isEncryptedModeEnabled = account.isShowOnlyEncrypted

    val folder = store.getFolder(folderName) as IMAPFolder
    folder.open(javax.mail.Folder.READ_ONLY)

    val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)

    val nextUID = folder.uidNext
    val newestCachedUID = roomDatabase.msgDao().getLastUIDOfMsgForLabel(account.email, folderName)
    val loadedMsgsCount = roomDatabase.msgDao().count(account.email, folderName)

    var newMsgs = emptyArray<Message>()

    if (newestCachedUID > 1 && newestCachedUID < nextUID - 1) {
      if (isEncryptedModeEnabled == true) {
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
        val tempMsgs = folder.getMessagesByUID((newestCachedUID + 1).toLong(), nextUID - 1)
        newMsgs = EmailUtil.fetchMsgs(folder, tempMsgs)
      }
    }

    val updatedMsgs: Array<Message>
    updatedMsgs = if (isEncryptedModeEnabled == true) {
      val oldestCachedUID = roomDatabase.msgDao().getOldestUIDOfMsgForLabel(account.email, folderName)
      EmailUtil.getUpdatedMsgsByUID(folder, oldestCachedUID.toLong(), newestCachedUID.toLong())
    } else {
      EmailUtil.getUpdatedMsgs(folder, loadedMsgsCount, newMsgs.size)
    }

    listener.onRefreshMsgsReceived(account, localFolder, folder, newMsgs, updatedMsgs, ownerKey, requestCode)

    if (newMsgs.isNotEmpty()) {
      val array = EmailUtil.getMsgsEncryptionInfo(isEncryptedModeEnabled, folder, newMsgs)
      listener.onNewMsgsReceived(account, localFolder, folder, newMsgs, array, ownerKey, requestCode)
    }

    folder.close(false)
  }
}
