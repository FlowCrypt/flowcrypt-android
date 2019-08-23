/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks

import android.content.ContentValues
import android.text.TextUtils
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.sync.SyncListener
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.database.dao.source.ContactsDaoSource
import com.flowcrypt.email.model.EmailAndNamePair
import com.sun.mail.imap.IMAPFolder
import java.util.*
import javax.mail.FetchProfile
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Session
import javax.mail.Store
import javax.mail.internet.InternetAddress

/**
 * This [SyncTask] loads information about contacts from the SENT folder.
 *
 * @author Denis Bondarenko
 * Date: 23.04.2018
 * Time: 14:53
 * E-mail: DenBond7@gmail.com
 */
class LoadContactsSyncTask : BaseSyncTask("", 0) {

  override fun runIMAPAction(account: AccountDao, session: Session, store: Store, listener: SyncListener) {
    val foldersManager = FoldersManager.fromDatabase(listener.context, account.email)
    val folderSent = foldersManager.folderSent ?: return
    val imapFolder = store.getFolder(folderSent.fullName) as IMAPFolder
    imapFolder.open(Folder.READ_ONLY)

    val msgs = imapFolder.messages

    if (msgs.isNotEmpty()) {
      val fetchProfile = FetchProfile()
      fetchProfile.add(Message.RecipientType.TO.toString().toUpperCase(Locale.getDefault()))
      fetchProfile.add(Message.RecipientType.CC.toString().toUpperCase(Locale.getDefault()))
      fetchProfile.add(Message.RecipientType.BCC.toString().toUpperCase(Locale.getDefault()))
      imapFolder.fetch(msgs, fetchProfile)

      updateContacts(listener, msgs)

      val contentValues = ContentValues()
      contentValues.put(AccountDaoSource.COL_IS_CONTACTS_LOADED, true)

      AccountDaoSource().updateAccountInformation(listener.context, account.account, contentValues)
    }

    imapFolder.close(false)
  }

  private fun updateContacts(listener: SyncListener, msgs: Array<Message>) {
    val emailAndNamePairs = mutableListOf<EmailAndNamePair>()
    for (msg in msgs) {
      emailAndNamePairs.addAll(parseRecipients(msg, Message.RecipientType.TO))
      emailAndNamePairs.addAll(parseRecipients(msg, Message.RecipientType.CC))
      emailAndNamePairs.addAll(parseRecipients(msg, Message.RecipientType.BCC))
    }

    val contactsDaoSource = ContactsDaoSource()
    val availablePgpContacts = contactsDaoSource.getAllPgpContacts(listener.context)

    val contactsInDatabase = HashSet<String>()
    val contactsWhichWillBeUpdated = HashSet<String>()
    val contactsWhichWillBeCreated = HashSet<String>()
    val emailNamePairsMap = HashMap<String, String?>()

    val newCandidates = mutableListOf<EmailAndNamePair>()
    val updateCandidates = mutableListOf<EmailAndNamePair>()

    for ((email, name) in availablePgpContacts) {
      contactsInDatabase.add(email.toLowerCase(Locale.getDefault()))
      emailNamePairsMap[email.toLowerCase(Locale.getDefault())] = name
    }

    for (emailAndNamePair in emailAndNamePairs) {
      if (contactsInDatabase.contains(emailAndNamePair.email)) {
        if (TextUtils.isEmpty(emailNamePairsMap[emailAndNamePair.email])) {
          if (!contactsWhichWillBeUpdated.contains(emailAndNamePair.email)) {
            emailAndNamePair.email?.let {
              contactsWhichWillBeUpdated.add(it)
            }
            updateCandidates.add(emailAndNamePair)
          }
        }
      } else {
        if (!contactsWhichWillBeCreated.contains(emailAndNamePair.email)) {
          emailAndNamePair.email?.let {
            contactsWhichWillBeCreated.add(it)
          }
          newCandidates.add(emailAndNamePair)
        }
      }
    }

    contactsDaoSource.updatePgpContacts(listener.context, updateCandidates)
    contactsDaoSource.addRows(listener.context, newCandidates)
  }

  /**
   * Generate an array of [EmailAndNamePair] objects from the input message.
   * This information will be retrieved from "to" , "cc" or "bcc" headers.
   *
   * @param msg           The input [Message].
   * @param recipientType The input [Message.RecipientType].
   * @return An array of EmailAndNamePair objects, which contains information about emails and names.
   */
  private fun parseRecipients(msg: Message?, recipientType: Message.RecipientType?): List<EmailAndNamePair> {
    if (msg != null && recipientType != null) {
      try {
        val header = msg.getHeader(recipientType.toString()) ?: return emptyList()
        if (header.isNotEmpty()) {
          if (!TextUtils.isEmpty(header[0])) {
            val addresses = InternetAddress.parse(header[0])
            val emailAndNamePairs = mutableListOf<EmailAndNamePair>()
            for (address in addresses) {
              emailAndNamePairs.add(EmailAndNamePair(
                  address.address.toLowerCase(Locale.getDefault()), address.personal))
            }

            return emailAndNamePairs
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }

      return emptyList()
    } else {
      return emptyList()
    }
  }
}
