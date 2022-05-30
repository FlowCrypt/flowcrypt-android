/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.google.android.gms.auth.GoogleAuthException
import com.sun.mail.imap.IMAPFolder
import jakarta.mail.FetchProfile
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.MessagingException
import jakarta.mail.UIDFolder
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.IOException

/**
 * @author Denis Bondarenko
 * Date: 17.08.2018
 * Time: 09:48
 * E-mail: DenBond7@gmail.com
 */
class AddMessageToDatabaseRule(val account: AccountEntity, val localFolder: LocalFolder) :
  BaseRule() {
  private var message: Message? = null

  init {
    try {
      OpenStoreHelper.openStore(
        targetContext,
        account,
        OpenStoreHelper.getAccountSess(targetContext, account)
      ).use { store ->
        store.getFolder(localFolder.fullName).use { folder ->
          val imapFolder = (folder as IMAPFolder).apply { open(Folder.READ_ONLY) }
          val messages = arrayOf(imapFolder.getMessage(imapFolder.messageCount))

          val fetchProfile = FetchProfile()
          fetchProfile.add(FetchProfile.Item.ENVELOPE)
          fetchProfile.add(FetchProfile.Item.FLAGS)
          fetchProfile.add(FetchProfile.Item.CONTENT_INFO)
          fetchProfile.add(UIDFolder.FetchProfileItem.UID)

          this.message = messages[0]

          imapFolder.fetch(messages, fetchProfile)
        }
      }
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
    message?.let {
      val msgEntity = MessageEntity.genMsgEntity(
        email = account.email,
        label = localFolder.fullName,
        msg = it,
        uid = 0,
        isNew = false
      )

      FlowCryptRoomDatabase.getDatabase(targetContext).msgDao().insert(msgEntity)
    }
  }
}
