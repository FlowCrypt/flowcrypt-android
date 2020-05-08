/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks

import android.text.TextUtils
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.sync.SyncListener
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.ContactEntity
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

  override fun runIMAPAction(account: AccountEntity, session: Session, store: Store, listener: SyncListener) {
    if (account.areContactsLoaded == true) return

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
      FlowCryptRoomDatabase.getDatabase(listener.context).accountDao().updateAccount(account.copy(areContactsLoaded = true))
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

    val context = listener.context
    val contactsDao = FlowCryptRoomDatabase.getDatabase(context).contactsDao()
    val availableContacts = contactsDao.getAllContacts()

    val contactsInDatabase = HashSet<String>()
    val contactsWhichWillBeUpdated = HashSet<String>()
    val contactsWhichWillBeCreated = HashSet<String>()
    val contactsByEmailMap = HashMap<String, ContactEntity?>()

    val newCandidates = mutableListOf<ContactEntity>()
    val updateCandidates = mutableListOf<ContactEntity>()

    for (contact in availableContacts) {
      contactsInDatabase.add(contact.email.toLowerCase(Locale.getDefault()))
      contactsByEmailMap[contact.email.toLowerCase(Locale.getDefault())] = contact
    }

    for (emailAndNamePair in emailAndNamePairs) {
      if (contactsInDatabase.contains(emailAndNamePair.email)) {
        val contactEntity = contactsByEmailMap[emailAndNamePair.email]
        if (contactEntity?.email.isNullOrEmpty()) {
          if (!contactsWhichWillBeUpdated.contains(emailAndNamePair.email)) {
            emailAndNamePair.email?.let {
              contactsWhichWillBeUpdated.add(it)
            }
            contactEntity?.copy(name = emailAndNamePair.name)?.let { updateCandidates.add(it) }
          }
        }
      } else {
        if (!contactsWhichWillBeCreated.contains(emailAndNamePair.email)) {
          emailAndNamePair.email?.let {
            contactsWhichWillBeCreated.add(it)
            newCandidates.add(ContactEntity(email = it, name = emailAndNamePair.name, hasPgp = false))
          }
        }
      }
    }

    contactsDao.update(updateCandidates)
    contactsDao.insert(newCandidates)
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
