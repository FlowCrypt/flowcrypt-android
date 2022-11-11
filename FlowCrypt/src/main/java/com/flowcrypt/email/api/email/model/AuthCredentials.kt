/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model

import android.os.Parcelable
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.database.entity.AccountEntity
import kotlinx.parcelize.Parcelize

/**
 * This class describes a details information about auth settings for some IMAP and SMTP servers.
 *
 * @author DenBond7
 * Date: 14.09.2017.
 * Time: 15:11.
 * E-mail: DenBond7@gmail.com
 */
@Parcelize
data class AuthCredentials constructor(
  val email: String,
  val username: String,
  var password: String,
  val imapServer: String,
  val imapPort: Int = 143,
  val imapOpt: SecurityType.Option = SecurityType.Option.NONE,
  val smtpServer: String,
  val smtpPort: Int = 25,
  val smtpOpt: SecurityType.Option = SecurityType.Option.NONE,
  val hasCustomSignInForSmtp: Boolean = false,
  val smtpSigInUsername: String? = null,
  var smtpSignInPassword: String? = null,
  val faqUrl: String? = null,
  val useOAuth2: Boolean = false,
  val displayName: String? = null,
  val authTokenInfo: AuthTokenInfo? = null
) : Parcelable {

  fun peekPassword(): String {
    return if (useOAuth2) authTokenInfo?.accessToken ?: password else password
  }

  fun peekSmtpPassword(): String? {
    return if (useOAuth2) authTokenInfo?.accessToken ?: password else smtpSignInPassword
  }

  companion object {
    fun from(accountEntity: AccountEntity): AuthCredentials {
      with(accountEntity) {
        var imapOpt: SecurityType.Option = SecurityType.Option.NONE

        if (imapUseSslTls == true) {
          imapOpt = SecurityType.Option.SSL_TLS
        } else if (imapUseStarttls == true) {
          imapOpt = SecurityType.Option.STARTLS
        }

        var smtpOpt: SecurityType.Option = SecurityType.Option.NONE

        if (smtpUseSslTls == true) {
          smtpOpt = SecurityType.Option.SSL_TLS
        } else if (smtpUseStarttls == true) {
          smtpOpt = SecurityType.Option.STARTLS
        }

        return AuthCredentials(
          email = email,
          username = username,
          password = password,
          imapServer = imapServer,
          imapPort = imapPort ?: JavaEmailConstants.DEFAULT_IMAP_PORT,
          imapOpt = imapOpt,
          smtpServer = smtpServer,
          smtpPort = smtpPort ?: JavaEmailConstants.DEFAULT_SMTP_PORT,
          smtpOpt = smtpOpt,
          hasCustomSignInForSmtp = smtpUsername?.isNotEmpty() == true && smtpPassword?.isNotEmpty() == true,
          smtpSigInUsername = if (smtpUsername.isNullOrEmpty()) null else smtpUsername,
          smtpSignInPassword = if (smtpPassword.isNullOrEmpty()) null else smtpPassword,
          useOAuth2 = accountEntity.imapAuthMechanisms == JavaEmailConstants.AUTH_MECHANISMS_XOAUTH2
        )
      }
    }
  }
}
