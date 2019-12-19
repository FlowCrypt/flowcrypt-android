/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
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

    const val COUNT_OF_LOADED_EMAILS_BY_STEP = 45

    /*IMAP*/
    const val PROPERTY_NAME_MAIL_IMAP_SSL_ENABLE = "mail.imap.ssl.enable"
    const val PROPERTY_NAME_MAIL_IMAP_STARTTLS_ENABLE = "mail.imap.starttls.enable"
    const val PROPERTY_NAME_MAIL_IMAP_AUTH_MECHANISMS = "mail.imap.auth.mechanisms"
    const val PROPERTY_NAME_MAIL_IMAP_FETCH_SIZE = "mail.imap.fetchsize"
    const val DEFAULT_IMAP_PORT = 143
    const val SSL_IMAP_PORT = 993

    /*SMTP*/
    const val PROPERTY_NAME_MAIL_SMTP_AUTH = "mail.smtp.auth"
    const val PROPERTY_NAME_MAIL_SMTP_SSL_ENABLE = "mail.smtp.ssl.enable"
    const val PROPERTY_NAME_MAIL_SMTP_STARTTLS_ENABLE = "mail.smtp.starttls.enable"
    const val PROPERTY_NAME_MAIL_SMTP_AUTH_MECHANISMS = "mail.smtp.auth.mechanisms"
    const val DEFAULT_SMTP_PORT = 25
    const val SSL_SMTP_PORT = 465
    const val STARTTLS_SMTP_PORT = 587

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
    const val MIME_TYPE_TEXT_PLAIN = "text/plain"
    const val MIME_TYPE_TEXT_HTML = "text/html"

    const val FOLDER_ATTRIBUTE_NO_SELECT = "\\Noselect"

    const val HEADER_X_ATTACHMENT_ID = "X-Attachment-Id"
    const val HEADER_CONTENT_ID = "Content-ID"
    const val HEADER_IN_REPLY_TO = "In-Reply-To"
    const val FOLDER_INBOX = "INBOX"
    const val FOLDER_OUTBOX = "Outbox"
    const val FOLDER_FLAG_HAS_NO_CHILDREN = "\\HasNoChildren"

    const val EMAIL_PROVIDER_GMAIL = "gmail.com"
  }
}
