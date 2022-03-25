/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email

/**
 * This class described JavaEmail constants.
 *
 * @author DenBond7
 * Date: 28.04.2017
 * Time: 9:40
 * E-mail: DenBond7@gmail.com
 */
class JavaEmailConstants {
  companion object {

    const val DEFAULT_FETCH_BUFFER = 1024 * 1024
    const val ATTACHMENTS_FETCH_BUFFER = 1024 * 256
    const val COUNT_OF_LOADED_EMAILS_BY_STEP = 45

    /*IMAP*/
    const val PROPERTY_NAME_MAIL_IMAP_SSL_ENABLE = "mail.imap.ssl.enable"
    const val PROPERTY_NAME_MAIL_IMAP_SSL_CHECK_SERVER_IDENTITY =
      "mail.imap.ssl.checkserveridentity"
    const val PROPERTY_NAME_MAIL_IMAP_STARTTLS_ENABLE = "mail.imap.starttls.enable"
    const val PROPERTY_NAME_MAIL_IMAP_AUTH_MECHANISMS = "mail.imap.auth.mechanisms"
    const val PROPERTY_NAME_MAIL_IMAP_FETCH_SIZE = "mail.imap.fetchsize"
    const val PROPERTY_NAME_MAIL_IMAP_CONNECTIONTIMEOUT = "mail.imap.connectiontimeout"
    const val PROPERTY_NAME_MAIL_IMAPS_CONNECTIONTIMEOUT = "mail.imaps.connectiontimeout"
    const val PROPERTY_NAME_MAIL_IMAP_TIMEOUT = "mail.imap.timeout"
    const val PROPERTY_NAME_MAIL_IMAPS_TIMEOUT = "mail.imaps.timeout"
    const val PROPERTY_NAME_MAIL_IMAP_SSL_TRUST = "mail.imap.ssl.trust"
    const val PROPERTY_NAME_MAIL_IMAPS_SSL_TRUST = "mail.imaps.ssl.trust"
    const val DEFAULT_IMAP_PORT = 143
    const val SSL_IMAP_PORT = 993
    const val PROPERTY_NAME_MAIL_IMAPS_AUTH_LOGIN_DISABLE = "mail.imaps.auth.login.disable"
    const val PROPERTY_NAME_MAIL_IMAPS_AUTH_PLAIN_DISABLE = "mail.imaps.auth.plain.disable"
    const val PROPERTY_NAME_MAIL_IMAPS_AUTH_XOAUTH2_DISABLE = "mail.imaps.auth.xoauth2.disable"

    /*SMTP*/
    const val PROPERTY_NAME_MAIL_SMTP_AUTH = "mail.smtp.auth"
    const val PROPERTY_NAME_MAIL_SMTP_SSL_ENABLE = "mail.smtp.ssl.enable"
    const val PROPERTY_NAME_MAIL_SMTP_SSL_CHECK_SERVER_IDENTITY =
      "mail.smtp.ssl.checkserveridentity"
    const val PROPERTY_NAME_MAIL_SMTP_STARTTLS_ENABLE = "mail.smtp.starttls.enable"
    const val PROPERTY_NAME_MAIL_SMTP_AUTH_MECHANISMS = "mail.smtp.auth.mechanisms"
    const val PROPERTY_NAME_MAIL_SMTP_CONNECTIONTIMEOUT = "mail.smtp.connectiontimeout"
    const val PROPERTY_NAME_MAIL_SMTP_TIMEOUT = "mail.smtp.timeout"
    const val PROPERTY_NAME_MAIL_SMTP_SSL_TRUST = "mail.smtp.ssl.trust"
    const val DEFAULT_SMTP_PORT = 25
    const val SSL_SMTP_PORT = 465
    const val STARTTLS_SMTP_PORT = 587
    const val PROPERTY_NAME_MAIL_SMTP_AUTH_LOGIN_DISABLE = "mail.smtp.auth.login.disable"
    const val PROPERTY_NAME_MAIL_SMTP_AUTH_PLAIN_DISABLE = "mail.smtp.auth.plain.disable"
    const val PROPERTY_NAME_MAIL_SMTP_AUTH_XOAUTH2_DISABLE = "mail.smtp.auth.xoauth2.disable"

    /*AUTH MECHANISMS*/
    const val AUTH_MECHANISMS_XOAUTH2 = "XOAUTH2"
    const val AUTH_MECHANISMS_PLAIN = "PLAIN"
    const val AUTH_MECHANISMS_LOGIN = "LOGIN"

    /*PROTOCOLS*/
    const val PROTOCOL_IMAP = "imap"
    const val PROTOCOL_IMAPS = "imaps"
    const val PROTOCOL_SMTP = "smtp"
    const val PROTOCOL_GIMAPS = "gimaps"
    const val OAUTH2 = "oauth2:"

    const val MIME_TYPE_MULTIPART = "multipart/*"
    const val MIME_TYPE_MULTIPART_ALTERNATIVE = "multipart/alternative"
    const val MIME_TYPE_TEXT_PLAIN = "text/plain"
    const val MIME_TYPE_TEXT_HTML = "text/html"

    const val FOLDER_ATTRIBUTE_NO_SELECT = "\\Noselect"

    const val HEADER_X_ATTACHMENT_ID = "X-Attachment-Id"
    const val HEADER_CONTENT_ID = "Content-ID"
    const val HEADER_IN_REPLY_TO = "In-Reply-To"
    const val FOLDER_INBOX = "INBOX"
    const val FOLDER_SENT = "SENT"
    const val FOLDER_TRASH = "TRASH"
    const val FOLDER_DRAFT = "DRAFT"
    const val FOLDER_STARRED = "STARRED"
    const val FOLDER_IMPORTANT = "IMPORTANT"
    const val FOLDER_UNREAD = "UNREAD"
    const val FOLDER_SPAM = "SPAM"
    const val FOLDER_ALL_MAIL = "ALL"
    const val FOLDER_OUTBOX = "Outbox"
    const val FOLDER_SEARCH = ""
    const val FOLDER_FLAG_HAS_NO_CHILDREN = "\\HasNoChildren"

    const val EMAIL_PROVIDER_GMAIL = "gmail.com"
    const val EMAIL_PROVIDER_GOOGLEMAIL = "googlemail.com"
    const val EMAIL_PROVIDER_YAHOO = "yahoo.com"
    const val EMAIL_PROVIDER_OUTLOOK = "outlook.com"
    const val EMAIL_PROVIDER_LIVE = "live.com"
  }
}
