/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.protocol

import android.content.Context
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailConstants
import com.flowcrypt.email.database.entity.AccountEntity
import com.google.android.gms.auth.GoogleAuthException
import java.io.IOException
import javax.mail.MessagingException
import javax.mail.Session
import javax.mail.Transport

/**
 * This class describes methods for a work with SMTP protocol.
 *
 * @author Denis Bondarenko
 * Date: 15.01.2018
 * Time: 13:12
 * E-mail: DenBond7@gmail.com
 */

class SmtpProtocolUtil {

  companion object {
    /**
     * Prepare a [Transport] for SMTP protocol.
     *
     * @param context Interface to global information about an application environment.
     * @param session The [Session] object.
     * @param accountEntity [AccountEntity] information which will be used of connection.
     * @return Generated [Transport]
     * @throws MessagingException
     * @throws IOException
     * @throws GoogleAuthException
     */
    @JvmStatic
    fun prepareSmtpTransport(context: Context, session: Session, accountEntity: AccountEntity): Transport {
      val transport = session.getTransport(JavaEmailConstants.PROTOCOL_SMTP)

      when (accountEntity.accountType) {
        AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
          val userName = accountEntity.email
          val password = EmailUtil.getGmailAccountToken(context, accountEntity)
          transport.connect(GmailConstants.GMAIL_SMTP_SERVER, GmailConstants.GMAIL_SMTP_PORT, userName, password)
        }

        else -> {
          val userName: String?
          val password: String?

          if (accountEntity.useCustomSignForSmtp == true) {
            userName = accountEntity.smtpUsername
            password = accountEntity.smtpPassword
          } else {
            userName = accountEntity.username
            password = accountEntity.password
          }

          transport.connect(accountEntity.smtpServer, accountEntity.smtpPort
              ?: 0, userName, password)
        }
      }

      return transport
    }
  }
}
