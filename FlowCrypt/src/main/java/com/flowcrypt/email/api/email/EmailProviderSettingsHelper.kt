/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email

import com.flowcrypt.email.api.email.model.AuthCredentials
import com.flowcrypt.email.api.email.model.SecurityType
import com.flowcrypt.email.api.oauth.OAuth2Helper
import com.flowcrypt.email.util.GeneralUtil

/**
 * This class describes the base settings for added email providers
 *
 * @author Denys Bondarenko
 */
object EmailProviderSettingsHelper {
  /*********************** Microsoft **********************/
  private const val PROVIDER_OUTLOOK = "outlook.com"
  private const val PROVIDER_HOTMAIL = "hotmail.com"
  private const val PROVIDER_LIVE = "live.com"
  private const val IMAP_SERVER_MICROSOFT = "outlook.office365.com"
  private const val SMTP_SERVER_MICROSOFT = "smtp.office365.com"
  private const val FAQ_URL_MICROSOFT =
    "https://support.microsoft.com/en-us/office/pop-imap-and-smtp-settings-for-outlook-com-d088b986-291d-42b8-9564-9c414e2aa040"

  /*********************** Yahoo **********************/
  private const val PROVIDER_YAHOO = "yahoo.com"
  private const val IMAP_SERVER_YAHOO = "imap.mail.yahoo.com"
  private const val SMTP_SERVER_YAHOO = "smtp.mail.yahoo.com"
  private const val FAQ_URL_YAHOO = "https://help.yahoo.com/kb/sln4075.html"

  /*********************** iCloud **********************/
  private const val PROVIDER_ICLOUD = "icloud.com"
  private const val IMAP_SERVER_ICLOUD = "imap.mail.me.com"
  private const val SMTP_SERVER_ICLOUD = "smtp.mail.me.com"
  private const val FAQ_URL_ICLOUD = "https://support.apple.com/en-us/HT202304"

  /*********************** AOL **********************/
  private const val PROVIDER_AOL = "aol.com"
  private const val IMAP_SERVER_AOL = "imap.aol.com"
  private const val SMTP_SERVER_AOL = "smtp.aol.com"
  private const val FAQ_URL_AOL =
    "https://help.aol.com/articles/how-do-i-use-other-email-applications-to-send-and-receive-my-aol-mail"

  /*********************** UKR-NET **********************/
  private const val PROVIDER_UKR_NET = "ukr.net"
  private const val IMAP_SERVER_UKR_NET = "imap.ukr.net"
  private const val SMTP_SERVER_UKR_NET = "smtp.ukr.net"
  private const val FAQ_URL_UKR_NET = "https://wiki.ukr.net/IMAP_enable_ua"

  /*********************** Account for internal testing **********************/
  private const val PROVIDER_TESTS = "flowcrypt.test"
  private const val IMAP_SERVER_TESTS = "10.0.2.2"//local machine where an emulator works
  private const val SMTP_SERVER_TESTS = "10.0.2.2"//local machine where an emulator works

  /**
   * Get the base settings for the given account.
   */
  fun getBaseSettings(email: String, password: String): AuthCredentials? {
    if (!GeneralUtil.isEmailValid(email)) {
      return null
    }

    return when {
      PROVIDER_OUTLOOK.equals(EmailUtil.getDomain(email), true) -> getOutlookSettings(
        email,
        password
      )
      PROVIDER_HOTMAIL.equals(EmailUtil.getDomain(email), true) -> getOutlookSettings(
        email,
        password
      )
      PROVIDER_LIVE.equals(EmailUtil.getDomain(email), true) -> getOutlookSettings(email, password)
      PROVIDER_YAHOO.equals(EmailUtil.getDomain(email), true) -> getYahooSettings(email, password)
      PROVIDER_ICLOUD.equals(EmailUtil.getDomain(email), true) -> getICloudSettings(email, password)
      PROVIDER_AOL.equals(EmailUtil.getDomain(email), true) -> getAolSettings(email, password)
      PROVIDER_UKR_NET.equals(EmailUtil.getDomain(email), true) -> getUkrNetSettings(
        email,
        password
      )
      PROVIDER_TESTS.equals(EmailUtil.getDomain(email), true) -> getTestProviderSettings(
        email,
        password
      )

      else -> {
        null
      }
    }
  }

  fun getBaseSettingsForProvider(email: String, provider: OAuth2Helper.Provider): AuthCredentials {
    return when (provider) {
      OAuth2Helper.Provider.MICROSOFT -> getOutlookSettings(email, "")
    }
  }

  private fun getOutlookSettings(email: String, password: String): AuthCredentials {
    return AuthCredentials(
      email = email,
      username = email,
      password = password,
      imapServer = IMAP_SERVER_MICROSOFT,
      imapPort = JavaEmailConstants.SSL_IMAP_PORT,
      imapOpt = SecurityType.Option.SSL_TLS,
      smtpServer = SMTP_SERVER_MICROSOFT,
      smtpPort = JavaEmailConstants.STARTTLS_SMTP_PORT,
      smtpOpt = SecurityType.Option.STARTLS,
      hasCustomSignInForSmtp = true,
      smtpSigInUsername = email,
      smtpSignInPassword = password,
      faqUrl = FAQ_URL_MICROSOFT
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
      smtpSignInPassword = password,
      faqUrl = FAQ_URL_YAHOO
    )
  }

  private fun getICloudSettings(email: String, password: String): AuthCredentials {
    return AuthCredentials(
      email = email,
      username = email,
      password = password,
      imapServer = IMAP_SERVER_ICLOUD,
      imapPort = JavaEmailConstants.SSL_IMAP_PORT,
      imapOpt = SecurityType.Option.SSL_TLS,
      smtpServer = SMTP_SERVER_ICLOUD,
      smtpPort = JavaEmailConstants.STARTTLS_SMTP_PORT,
      smtpOpt = SecurityType.Option.STARTLS,
      hasCustomSignInForSmtp = true,
      smtpSigInUsername = email,
      smtpSignInPassword = password,
      faqUrl = FAQ_URL_ICLOUD
    )
  }

  private fun getAolSettings(email: String, password: String): AuthCredentials {
    return AuthCredentials(
      email = email,
      username = email,
      password = password,
      imapServer = IMAP_SERVER_AOL,
      imapPort = JavaEmailConstants.SSL_IMAP_PORT,
      imapOpt = SecurityType.Option.SSL_TLS,
      smtpServer = SMTP_SERVER_AOL,
      smtpPort = JavaEmailConstants.SSL_SMTP_PORT,
      smtpOpt = SecurityType.Option.SSL_TLS,
      hasCustomSignInForSmtp = true,
      smtpSigInUsername = email,
      smtpSignInPassword = password,
      faqUrl = FAQ_URL_AOL
    )
  }

  private fun getUkrNetSettings(email: String, password: String): AuthCredentials {
    return AuthCredentials(
      email = email,
      username = email,
      password = password,
      imapServer = IMAP_SERVER_UKR_NET,
      imapPort = JavaEmailConstants.SSL_IMAP_PORT,
      imapOpt = SecurityType.Option.SSL_TLS,
      smtpServer = SMTP_SERVER_UKR_NET,
      smtpPort = JavaEmailConstants.SSL_SMTP_PORT,
      smtpOpt = SecurityType.Option.SSL_TLS,
      hasCustomSignInForSmtp = true,
      smtpSigInUsername = email,
      smtpSignInPassword = password,
      faqUrl = FAQ_URL_UKR_NET
    )
  }

  private fun getTestProviderSettings(email: String, password: String): AuthCredentials {
    return AuthCredentials(
      email = email,
      username = email,
      password = password,
      imapServer = IMAP_SERVER_TESTS,
      imapPort = JavaEmailConstants.SSL_IMAP_PORT,
      imapOpt = SecurityType.Option.SSL_TLS,
      smtpServer = SMTP_SERVER_TESTS,
      smtpPort = JavaEmailConstants.STARTTLS_SMTP_PORT,
      smtpOpt = SecurityType.Option.STARTLS,
      hasCustomSignInForSmtp = true,
      smtpSigInUsername = email,
      smtpSignInPassword = password,
    )
  }
}
