/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.protocol

import android.accounts.AccountManager
import android.content.Context
import com.flowcrypt.email.accounts.FlowcryptAccountAuthenticator
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.AuthCredentials
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.google.android.gms.auth.GoogleAuthException
import jakarta.mail.MessagingException
import jakarta.mail.Session
import jakarta.mail.Transport
import java.io.IOException

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
    fun prepareSmtpTransport(
      context: Context,
      session: Session,
      accountEntity: AccountEntity
    ): Transport {
      val transport = session.getTransport(JavaEmailConstants.PROTOCOL_SMTP)

      when (accountEntity.accountType) {
        AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
          val userName = accountEntity.email
          val password = EmailUtil.getGmailAccountToken(context, accountEntity)
          transport.connect(
            accountEntity.smtpServer,
            accountEntity.smtpPort ?: 0,
            userName,
            password
          )
        }

        else -> {
          val userName: String?
          var password: String?

          if (accountEntity.smtpUseCustomSign == true) {
            userName = accountEntity.smtpUsername
            password = accountEntity.smtpPassword
          } else {
            userName = accountEntity.username
            password = accountEntity.password
          }

          if (accountEntity.useOAuth2) {
            val accountManager = AccountManager.get(context)
            val oAuthAccount =
              accountManager.accounts.firstOrNull { it.name == accountEntity.email }
            if (oAuthAccount != null) {
              val encryptedToken = accountManager.blockingGetAuthToken(
                oAuthAccount,
                FlowcryptAccountAuthenticator.AUTH_TOKEN_TYPE_EMAIL, true
              )
              password = if (encryptedToken.isNullOrEmpty()) {
                ""//need to think about
              } else {
                KeyStoreCryptoManager.decrypt(encryptedToken)
              }
            }
          }

          transport.connect(
            accountEntity.smtpServer, accountEntity.smtpPort
              ?: 0, userName, password
          )
        }
      }

      return transport
    }

    /**
     * Prepare a [Transport] for SMTP protocol.
     *
     * @param session The [Session] object.
     * @param authCredentials [AuthCredentials] information which will be used of connection.
     * @return Generated [Transport]
     * @throws MessagingException
     * @throws IOException
     * @throws GoogleAuthException
     */
    fun prepareSmtpTransport(session: Session, authCredentials: AuthCredentials): Transport {
      val transport = session.getTransport(JavaEmailConstants.PROTOCOL_SMTP)
      val username: String?
      val password: String?

      if (authCredentials.hasCustomSignInForSmtp) {
        username = authCredentials.smtpSigInUsername
        password = authCredentials.peekSmtpPassword()
      } else {
        username = authCredentials.username
        password = authCredentials.peekPassword()
      }

      transport.connect(
        authCredentials.smtpServer,
        authCredentials.smtpPort,
        username,
        password
      )

      return transport
    }
  }
}
