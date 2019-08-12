/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource
import com.google.android.gms.auth.GoogleAuthException
import com.sun.mail.imap.IMAPFolder
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.IOException
import javax.mail.FetchProfile
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.UIDFolder

/**
 * @author Denis Bondarenko
 * Date: 17.08.2018
 * Time: 09:48
 * E-mail: DenBond7@gmail.com
 */
class AddMessageToDatabaseRule(val account: AccountDao, val localFolder: LocalFolder) : BaseRule() {
  private var message: Message? = null

  init {
    try {
      val session = OpenStoreHelper.getAccountSess(targetContext, account)
      val store = OpenStoreHelper.openStore(targetContext, account, session)

      val imapFolder = store.getFolder(localFolder.fullName) as IMAPFolder
      imapFolder.open(javax.mail.Folder.READ_ONLY)

      val messages = arrayOf(imapFolder.getMessage(imapFolder.messageCount))

      val fetchProfile = FetchProfile()
      fetchProfile.add(FetchProfile.Item.ENVELOPE)
      fetchProfile.add(FetchProfile.Item.FLAGS)
      fetchProfile.add(FetchProfile.Item.CONTENT_INFO)
      fetchProfile.add(UIDFolder.FetchProfileItem.UID)

      this.message = messages[0]

      imapFolder.fetch(messages, fetchProfile)
    } catch (e: MessagingException) {
      e.printStackTrace()
    } catch (e: IOException) {
      e.printStackTrace()
    } catch (e: GoogleAuthException) {
      e.printStackTrace()
    }
  }

  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        saveMsgToDatabase()
        base.evaluate()
      }
    }
  }

  private fun saveMsgToDatabase() {
    MessageDaoSource().addRow(targetContext, account.email, localFolder.fullName, 0, message, false)
  }
}
