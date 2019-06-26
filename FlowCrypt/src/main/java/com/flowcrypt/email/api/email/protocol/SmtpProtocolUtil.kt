/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.protocol

import android.accounts.Account
import android.content.Context
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailConstants
import com.flowcrypt.email.database.dao.source.AccountDao
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
     * @param accountDao The accountDao information which will be used of connection.
     * @return Generated [Transport]
     * @throws MessagingException
     * @throws IOException
     * @throws GoogleAuthException
     */
    @JvmStatic
    fun prepareSmtpTransport(context: Context, session: Session, accountDao: AccountDao?): Transport {
      val transport = session.getTransport(JavaEmailConstants.PROTOCOL_SMTP)
      val account: Account = accountDao?.account ?: throw NullPointerException("An account can't be a null!")

      when (accountDao.accountType) {
        AccountDao.ACCOUNT_TYPE_GOOGLE -> {
          val userName = accountDao.email
          val password = EmailUtil.getGmailAccountToken(context, account)
          transport.connect(GmailConstants.GMAIL_SMTP_SERVER, GmailConstants.GMAIL_SMTP_PORT, userName, password)
        }

        else -> {
          val authCreds = accountDao.authCreds ?: throw NullPointerException("The AuthCredentials can't be a null!")
          val userName: String?
          val password: String?

          if (authCreds.hasCustomSignInForSmtp) {
            userName = authCreds.smtpSigInUsername
            password = authCreds.smtpSignInPassword
          } else {
            userName = authCreds.username
            password = authCreds.password
          }

          transport.connect(authCreds.smtpServer, authCreds.smtpPort, userName, password)
        }
      }

      return transport
    }
  }
}
