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
import com.flowcrypt.email.database.entity.AccountEntity
import com.sun.mail.gimap.GmailRawSearchTerm
import com.sun.mail.imap.IMAPFolder
import java.util.*
import javax.mail.FetchProfile
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Session
import javax.mail.Store
import javax.mail.UIDFolder
import javax.mail.search.AndTerm
import javax.mail.search.BodyTerm
import javax.mail.search.FromStringTerm
import javax.mail.search.OrTerm
import javax.mail.search.RecipientStringTerm
import javax.mail.search.SearchTerm
import javax.mail.search.StringTerm
import javax.mail.search.SubjectTerm

/**
 * This task finds messages on some localFolder.
 *
 * @author DenBond7
 * Date: 26.04.2018
 * Time: 14:20
 * E-mail: DenBond7@gmail.com
 */

class SearchMessagesSyncTask(ownerKey: String,
                             requestCode: Int,
                             private val localFolder: LocalFolder,
                             private val countOfAlreadyLoadedMsgs: Int) : BaseSyncTask(ownerKey, requestCode) {

  override fun runIMAPAction(account: AccountEntity, session: Session, store: Store, listener: SyncListener) {
    super.runIMAPAction(account, session, store, listener)
    listener.onActionProgress(account, ownerKey, requestCode, R.id.progress_id_opening_store)

    val imapFolder = store.getFolder(localFolder.fullName) as IMAPFolder
    imapFolder.open(Folder.READ_ONLY)

    val countOfLoadedMsgs =
        when {
          countOfAlreadyLoadedMsgs < 0 -> 0
          else -> countOfAlreadyLoadedMsgs
        }

    val foundMsgs = imapFolder.search(generateSearchTerm(account))

    val messagesCount = foundMsgs.size
    val end = messagesCount - countOfLoadedMsgs
    val startCandidate = end - JavaEmailConstants.COUNT_OF_LOADED_EMAILS_BY_STEP + 1
    val start = when {
      startCandidate < 1 -> 1
      else -> startCandidate
    }

    listener.onActionProgress(account, ownerKey, requestCode, R.id.progress_id_getting_list_of_emails)

    if (end < 1) {
      listener.onSearchMsgsReceived(account, localFolder, imapFolder, arrayOf(), ownerKey, requestCode)
    } else {
      val bufferedMsgs = Arrays.copyOfRange(foundMsgs, start - 1, end)

      val fetchProfile = FetchProfile()
      fetchProfile.add(FetchProfile.Item.ENVELOPE)
      fetchProfile.add(FetchProfile.Item.FLAGS)
      fetchProfile.add(FetchProfile.Item.CONTENT_INFO)
      fetchProfile.add(UIDFolder.FetchProfileItem.UID)

      imapFolder.fetch(bufferedMsgs, fetchProfile)

      listener.onSearchMsgsReceived(account, localFolder, imapFolder, bufferedMsgs, ownerKey, requestCode)
    }

    imapFolder.close(false)
  }

  /**
   * Generate a [SearchTerm] depend on an input [AccountEntity].
   *
   * @param context Interface to global information about an application environment.
   * @param account An input [AccountEntity]
   * @return A generated [SearchTerm].
   */
  private fun generateSearchTerm(account: AccountEntity): SearchTerm {
    val isEncryptedModeEnabled = account.isShowOnlyEncrypted

    if (isEncryptedModeEnabled == true) {
      val searchTerm = EmailUtil.genEncryptedMsgsSearchTerm(account)

      return if (AccountEntity.ACCOUNT_TYPE_GOOGLE.equals(account.accountType, ignoreCase = true)) {
        val stringTerm = searchTerm as StringTerm
        GmailRawSearchTerm(localFolder.searchQuery + " AND (" + stringTerm.pattern + ")")
      } else {
        AndTerm(searchTerm, generateNonGmailSearchTerm())
      }
    } else {
      return if (AccountEntity.ACCOUNT_TYPE_GOOGLE.equals(account.accountType, ignoreCase = true)) {
        GmailRawSearchTerm(localFolder.searchQuery)
      } else {
        generateNonGmailSearchTerm()
      }
    }
  }

  private fun generateNonGmailSearchTerm(): SearchTerm {
    return OrTerm(arrayOf(
        SubjectTerm(localFolder.searchQuery),
        BodyTerm(localFolder.searchQuery),
        FromStringTerm(localFolder.searchQuery),
        RecipientStringTerm(Message.RecipientType.TO, localFolder.searchQuery),
        RecipientStringTerm(Message.RecipientType.CC, localFolder.searchQuery),
        RecipientStringTerm(Message.RecipientType.BCC, localFolder.searchQuery)
    ))
  }
}
