/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.gmail

/**
 * This class described Gmail constants.
 *
 * @author DenBond7
 * Date: 28.04.2017
 * Time: 9:47
 * E-mail: DenBond7@gmail.com
 */
class GmailConstants {
  companion object {
    const val PROPERTY_NAME_MAIL_GIMAPS_SSL_ENABLE = "mail.gimaps.ssl.enable"
    const val PROPERTY_NAME_MAIL_GIMAPS_SSL_CHECK_SERVER_IDENTITY =
      "mail.gimaps.ssl.checkserveridentity"
    const val PROPERTY_NAME_MAIL_GIMAPS_AUTH_MECHANISMS = "mail.gimaps.auth.mechanisms"
    const val PROPERTY_NAME_MAIL_GIMAPS_FETCH_SIZE = "mail.gimaps.fetchsize"
    const val GMAIL_ALERT_MESSAGE_WHEN_LESS_SECURE_NOT_ALLOWED =
      "[ALERT] Please log in via your web browser"
    const val PROPERTY_NAME_MAIL_GIMAPS_CONNECTIONTIMEOUT = "mail.gimaps.connectiontimeout"
    const val PROPERTY_NAME_MAIL_GIMAPS_TIMEOUT = "mail.gimaps.timeout"

    const val GMAIL_IMAP_SERVER = "imap.gmail.com"
    const val GMAIL_SMTP_SERVER = "smtp.gmail.com"
    const val GMAIL_SMTP_PORT = 587
    const val GMAIL_IMAP_PORT = 993
  }
}
