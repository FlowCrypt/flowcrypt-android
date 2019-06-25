/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks

import android.content.Context
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.sync.SyncListener
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.sun.mail.gimap.GmailRawSearchTerm
import com.sun.mail.imap.IMAPFolder
import java.util.*
import javax.mail.FetchProfile
import javax.mail.Folder
import javax.mail.Session
import javax.mail.Store
import javax.mail.UIDFolder
import javax.mail.search.AndTerm
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

  override fun runIMAPAction(account: AccountDao, session: Session, store: Store, listener: SyncListener) {
    super.runIMAPAction(account, session, store, listener)

    val imapFolder = store.getFolder(localFolder.fullName) as IMAPFolder
    imapFolder.open(Folder.READ_ONLY)

    val countOfLoadedMsgs =
        when {
          countOfAlreadyLoadedMsgs < 0 -> 0
          else -> countOfAlreadyLoadedMsgs
        }

    val foundMsgs = imapFolder.search(generateSearchTerm(listener.context, account))

    val messagesCount = foundMsgs.size
    val end = messagesCount - countOfLoadedMsgs
    val startCandidate = end - JavaEmailConstants.COUNT_OF_LOADED_EMAILS_BY_STEP + 1
    val start = when {
      startCandidate < 1 -> 1
      else -> startCandidate
    }

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
   * Generate a [SearchTerm] depend on an input [AccountDao].
   *
   * @param context Interface to global information about an application environment.
   * @param account An input [AccountDao]
   * @return A generated [SearchTerm].
   */
  private fun generateSearchTerm(context: Context, account: AccountDao): SearchTerm {
    val isEncryptedModeEnabled = AccountDaoSource().isEncryptedModeEnabled(context, account.email)

    if (isEncryptedModeEnabled) {
      val searchTerm = EmailUtil.genEncryptedMsgsSearchTerm(account)

      return if (AccountDao.ACCOUNT_TYPE_GOOGLE.equals(account.accountType, ignoreCase = true)) {
        val stringTerm = searchTerm as StringTerm
        GmailRawSearchTerm(localFolder.searchQuery + " AND (" + stringTerm.pattern + ")")
      } else {
        AndTerm(searchTerm, SubjectTerm(localFolder.searchQuery))
      }
    } else {
      return if (AccountDao.ACCOUNT_TYPE_GOOGLE.equals(account.accountType, ignoreCase = true)) {
        GmailRawSearchTerm(localFolder.searchQuery)
      } else {
        SubjectTerm(localFolder.searchQuery)
      }
    }
  }
}
