/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks

import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.sync.SyncListener
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.sun.mail.imap.IMAPFolder
import javax.mail.FetchProfile
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Session
import javax.mail.Store
import javax.mail.UIDFolder

/**
 * This task loads the older messages via some step.
 *
 * @author DenBond7
 * Date: 23.06.2017
 * Time: 11:26
 * E-mail: DenBond7@gmail.com
 */

class LoadMessagesToCacheSyncTask(ownerKey: String,
                                  requestCode: Int,
                                  private val localFolder: LocalFolder,
                                  private val countOfAlreadyLoadedMsgs: Int) : BaseSyncTask(ownerKey, requestCode) {

  override fun runIMAPAction(account: AccountEntity, session: Session, store: Store, listener: SyncListener) {
    val imapFolder = store.getFolder(localFolder.fullName) as IMAPFolder
    listener.onActionProgress(account, ownerKey, requestCode, R.id.progress_id_opening_store)
    imapFolder.open(Folder.READ_ONLY)

    val countOfLoadedMsgs =
        when {
          countOfAlreadyLoadedMsgs < 0 -> 0
          else -> countOfAlreadyLoadedMsgs
        }

    val context = listener.context
    val isEncryptedModeEnabled = account.isShowOnlyEncrypted
    var foundMsgs: Array<Message> = emptyArray()
    var msgsCount = 0

    if (isEncryptedModeEnabled == true) {
      foundMsgs = imapFolder.search(EmailUtil.genEncryptedMsgsSearchTerm(account))
      foundMsgs?.let {
        msgsCount = foundMsgs.size
      }
    } else {
      msgsCount = imapFolder.messageCount
    }

    val end = msgsCount - countOfLoadedMsgs
    val startCandidate = end - JavaEmailConstants.COUNT_OF_LOADED_EMAILS_BY_STEP + 1
    val start =
        when {
          startCandidate < 1 -> 1
          else -> startCandidate
        }
    val folderName = imapFolder.fullName

    val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)
    val label = roomDatabase.labelDao().getLabel(account.email, folderName)
    label?.let {
      roomDatabase.labelDao().update(it.copy(msgsCount = msgsCount))
    }

    listener.onActionProgress(account, ownerKey, requestCode, R.id.progress_id_getting_list_of_emails)
    if (end < 1) {
      listener.onMsgsReceived(account, localFolder, imapFolder, arrayOf(), ownerKey, requestCode)
    } else {
      val msgs: Array<Message> = if (isEncryptedModeEnabled == true) {
        foundMsgs.copyOfRange(start - 1, end)
      } else {
        imapFolder.getMessages(start, end)
      }

      val fetchProfile = FetchProfile()
      fetchProfile.add(FetchProfile.Item.ENVELOPE)
      fetchProfile.add(FetchProfile.Item.FLAGS)
      fetchProfile.add(FetchProfile.Item.CONTENT_INFO)
      fetchProfile.add(UIDFolder.FetchProfileItem.UID)
      imapFolder.fetch(msgs, fetchProfile)

      listener.onMsgsReceived(account, localFolder, imapFolder, msgs, ownerKey, requestCode)
    }
    imapFolder.close(false)
  }
}
