/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email

import com.flowcrypt.email.api.email.model.AuthCredentials
import com.flowcrypt.email.api.email.model.SecurityType
import com.flowcrypt.email.util.GeneralUtil

/**
 * This class describes the base settings for added email providers
 *
 * @author Denis Bondarenko
 *         Date: 7/24/20
 *         Time: 9:09 AM
 *         E-mail: DenBond7@gmail.com
 */
object EmailProviderSettingsHelper {
  /*********************** Microsoft **********************/
  private const val PROVIDER_OUTLOOK = "outlook.com"
  private const val PROVIDER_HOTMAIL = "hotmail.com"
  private const val IMAP_SERVER_OUTLOOK = "outlook.office365.com"
  private const val SMTP_SERVER_OUTLOOK = "smtp.office365.com"

  /*********************** Yahoo **********************/
  private const val PROVIDER_YAHOO = "yahoo.com"
  private const val IMAP_SERVER_YAHOO = "imap.mail.yahoo.com"
  private const val SMTP_SERVER_YAHOO = "smtp.mail.yahoo.com"

  /*********************** Account for internal testing **********************/
  private const val PROVIDER_TESTS = "denbond7.com"
  private const val IMAP_SERVER_TESTS = "imap.denbond7.com"
  private const val SMTP_SERVER_TESTS = "smtp.denbond7.com"

  /**
   * Get the base settings for the given account.
   */
  fun getBaseSettings(email: String, password: String): AuthCredentials? {
    if (!GeneralUtil.isEmailValid(email)) {
      return null
    }

    return when {
      PROVIDER_OUTLOOK.equals(EmailUtil.getDomain(email), true) -> getOutlookSettings(email, password)
      PROVIDER_HOTMAIL.equals(EmailUtil.getDomain(email), true) -> getOutlookSettings(email, password)
      PROVIDER_YAHOO.equals(EmailUtil.getDomain(email), true) -> getYahooSettings(email, password)
      PROVIDER_TESTS.equals(EmailUtil.getDomain(email), true) -> getTestProviderSettings(email, password)

      else -> {
        null
      }
    }
  }

  private fun getOutlookSettings(email: String, password: String): AuthCredentials {
    return AuthCredentials(
        email = email,
        username = email,
        password = password,
        imapServer = IMAP_SERVER_OUTLOOK,
        imapPort = JavaEmailConstants.SSL_IMAP_PORT,
        imapOpt = SecurityType.Option.SSL_TLS,
        smtpServer = SMTP_SERVER_OUTLOOK,
        smtpPort = JavaEmailConstants.STARTTLS_SMTP_PORT,
        smtpOpt = SecurityType.Option.STARTLS,
        hasCustomSignInForSmtp = true,
        smtpSigInUsername = email,
        smtpSignInPassword = password
    )
  }

  private fun getYahooSettings(email: String, password: String): AuthCredentials {
    return AuthCredentials(
        email = email,
        username = email,
        password = password,
        imapServer = IMAP_SERVER_YAHOO,
        imapPort = JavaEmailConstants.SSL_IMAP_PORT,
        imapOpt = SecurityType.Option.SSL_TLS,
        smtpServer = SMTP_SERVER_YAHOO,
        smtpPort = JavaEmailConstants.SSL_SMTP_PORT,
        smtpOpt = SecurityType.Option.SSL_TLS,
        hasCustomSignInForSmtp = true,
        smtpSigInUsername = email,
        smtpSignInPassword = password
    )
  }

  private fun getTestProviderSettings(email: String, password: String): AuthCredentials {
    return AuthCredentials(
        email = email,
        username = email,
        password = password,
        imapServer = IMAP_SERVER_TESTS,
        imapPort = JavaEmailConstants.DEFAULT_IMAP_PORT,
        imapOpt = SecurityType.Option.NONE,
        smtpServer = SMTP_SERVER_TESTS,
        smtpPort = JavaEmailConstants.DEFAULT_SMTP_PORT,
        smtpOpt = SecurityType.Option.NONE,
        hasCustomSignInForSmtp = false
    )
  }
}