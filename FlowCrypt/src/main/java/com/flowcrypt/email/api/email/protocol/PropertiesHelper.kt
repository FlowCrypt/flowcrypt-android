/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.protocol

import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailConstants
import com.flowcrypt.email.api.email.model.AuthCredentials
import com.flowcrypt.email.api.email.model.SecurityType
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.util.FlavorSettings
import java.util.Properties

/**
 * This util class help generates Properties to connect for different mail servers.
 *
 * @author DenBond7
 * Date: 03.05.2017
 * Time: 18:12
 * E-mail: DenBond7@gmail.com
 */
class PropertiesHelper {
  companion object {
    private const val BOOLEAN_VALUE_TRUE = "true"

    /**
     * Generate properties for imap protocol which will be used for download attachment.
     *
     * @return <tt>Properties</tt> New properties with setup imap connection;
     */
    fun genDownloadAttsProps(accountEntity: AccountEntity): Properties {
      return when (accountEntity.accountType) {
        AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
          genProps(accountEntity).apply {
            put(
              GmailConstants.PROPERTY_NAME_MAIL_GIMAPS_FETCH_SIZE,
              JavaEmailConstants.ATTACHMENTS_FETCH_BUFFER
            )
          }
        }

        else -> {
          genProps(accountEntity).apply {
            put(
              JavaEmailConstants.PROPERTY_NAME_MAIL_IMAP_FETCH_SIZE,
              JavaEmailConstants.ATTACHMENTS_FETCH_BUFFER
            )
          }
        }
      }
    }

    /**
     * Generate properties.
     *
     * @param accountEntity The object which contains information about settings for a connection.
     * @return <tt>Properties</tt> New properties.
     */
    fun genProps(accountEntity: AccountEntity?): Properties {
      accountEntity ?: return Properties()
      return when (accountEntity.accountType) {
        AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
          generateGmailProperties()
        }

        else -> {
          genPropsFromAuthCredentials(AuthCredentials.from(accountEntity))
        }
      }
    }

    /**
     * Generate properties.
     *
     * @param authCreds The object which contains information about settings for a connection.
     * @return <tt>Properties</tt> New properties.
     */
    private fun genPropsFromAuthCredentials(authCreds: AuthCredentials?): Properties {
      val prop = Properties()
      authCreds?.let {
        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAP_SSL_ENABLE] =
          it.imapOpt === SecurityType.Option.SSL_TLS
        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAP_STARTTLS_ENABLE] =
          it.imapOpt === SecurityType.Option.STARTLS
        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAP_SSL_CHECK_SERVER_IDENTITY] =
          it.imapOpt === SecurityType.Option.SSL_TLS
        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAP_FETCH_SIZE] =
          JavaEmailConstants.DEFAULT_FETCH_BUFFER
        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAP_CONNECTIONTIMEOUT] = 1000 * 30
        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAPS_CONNECTIONTIMEOUT] = 1000 * 30
        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAP_TIMEOUT] = 1000 * 20
        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAPS_TIMEOUT] = 1000 * 20

        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_AUTH] = it.hasCustomSignInForSmtp
        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_SSL_ENABLE] =
          it.smtpOpt === SecurityType.Option.SSL_TLS
        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_STARTTLS_ENABLE] =
          it.smtpOpt === SecurityType.Option.STARTLS
        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_SSL_CHECK_SERVER_IDENTITY] =
          it.smtpOpt === SecurityType.Option.SSL_TLS
        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_CONNECTIONTIMEOUT] = 1000 * 30
        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_TIMEOUT] = 1000 * 30

        if (authCreds.useOAuth2) {
          prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAP_AUTH_MECHANISMS] =
            JavaEmailConstants.AUTH_MECHANISMS_XOAUTH2
          prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAPS_AUTH_LOGIN_DISABLE] = "true"
          prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAPS_AUTH_PLAIN_DISABLE] = "true"
          prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAPS_AUTH_XOAUTH2_DISABLE] = "false"
          prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_AUTH_MECHANISMS] =
            JavaEmailConstants.AUTH_MECHANISMS_XOAUTH2
          prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_AUTH_LOGIN_DISABLE] = "true"
          prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_AUTH_PLAIN_DISABLE] = "true"
          prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_AUTH_XOAUTH2_DISABLE] = "false"
        }

        //apply flavor settings
        prop.putAll(FlavorSettings.getFlavorPropertiesForSession())
      }

      return prop
    }

    private fun generateGmailProperties(): Properties {
      val prop = Properties()
      prop[GmailConstants.PROPERTY_NAME_MAIL_GIMAPS_FETCH_SIZE] =
        JavaEmailConstants.DEFAULT_FETCH_BUFFER
      prop[GmailConstants.PROPERTY_NAME_MAIL_GIMAPS_SSL_ENABLE] = BOOLEAN_VALUE_TRUE
      prop[GmailConstants.PROPERTY_NAME_MAIL_GIMAPS_AUTH_MECHANISMS] =
        JavaEmailConstants.AUTH_MECHANISMS_XOAUTH2
      prop[GmailConstants.PROPERTY_NAME_MAIL_GIMAPS_SSL_CHECK_SERVER_IDENTITY] = BOOLEAN_VALUE_TRUE
      prop[GmailConstants.PROPERTY_NAME_MAIL_GIMAPS_CONNECTIONTIMEOUT] = 1000 * 30
      prop[GmailConstants.PROPERTY_NAME_MAIL_GIMAPS_TIMEOUT] = 1000 * 20

      prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_AUTH] = BOOLEAN_VALUE_TRUE
      prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_SSL_ENABLE] = "false"
      prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_STARTTLS_ENABLE] = BOOLEAN_VALUE_TRUE
      prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_AUTH_MECHANISMS] =
        JavaEmailConstants.AUTH_MECHANISMS_XOAUTH2
      prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_SSL_CHECK_SERVER_IDENTITY] =
        BOOLEAN_VALUE_TRUE
      prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_CONNECTIONTIMEOUT] = 1000 * 30
      prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_TIMEOUT] = 1000 * 30
      return prop
    }
  }
}
