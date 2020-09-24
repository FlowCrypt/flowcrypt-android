/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.protocol

import com.flowcrypt.email.Constants
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailConstants
import com.flowcrypt.email.api.email.model.AuthCredentials
import com.flowcrypt.email.api.email.model.SecurityType
import com.flowcrypt.email.database.entity.AccountEntity
import java.util.*

/**
 * This util class help generates Properties to connect for different mail servers.
 *
 * @author DenBond7
 * Date: 03.05.2017
 * Time: 18:12
 * E-mail: DenBond7@gmail.com
 */
class PropertiesHelper {

  // todo-denbond7 - this class should be refactored to have less repetition:
  //  genProps(authCreds: AuthCredentials?) and genProps(accountEntity: AccountEntity?)
  //  are very similar

  companion object {
    private val BOOLEAN_VALUE_TRUE = "true"

    /**
     * Generate properties for gimaps protocol.
     *
     * @return <tt>Properties</tt> New properties with setup gimaps connection;
     */
    fun generateGmailProperties(): Properties {
      val prop = Properties()
      prop[GmailConstants.PROPERTY_NAME_MAIL_GIMAPS_SSL_ENABLE] = BOOLEAN_VALUE_TRUE
      prop[GmailConstants.PROPERTY_NAME_MAIL_GIMAPS_AUTH_MECHANISMS] = JavaEmailConstants.AUTH_MECHANISMS_XOAUTH2
      prop[GmailConstants.PROPERTY_NAME_MAIL_GIMAPS_SSL_CHECK_SERVER_IDENTITY] = BOOLEAN_VALUE_TRUE

      prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_AUTH] = BOOLEAN_VALUE_TRUE
      prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_SSL_ENABLE] = BOOLEAN_VALUE_TRUE
      prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_AUTH_MECHANISMS] = JavaEmailConstants.AUTH_MECHANISMS_XOAUTH2
      prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_SSL_CHECK_SERVER_IDENTITY] = BOOLEAN_VALUE_TRUE
      return prop
    }

    /**
     * Generate properties for gimaps protocol which will be used for download attachment.
     *
     * @return <tt>Properties</tt> New properties with setup gimaps connection;
     */
    fun genGmailAttsProperties(): Properties {
      val properties = generateGmailProperties()
      properties[GmailConstants.PROPERTY_NAME_MAIL_GIMAPS_FETCH_SIZE] = 1024 * 256
      return properties
    }

    /**
     * Generate properties for imap protocol which will be used for download attachment.
     *
     * @return <tt>Properties</tt> New properties with setup imap connection;
     */
    fun genDownloadAttsProps(accountEntity: AccountEntity): Properties {
      val properties = genProps(accountEntity)
      properties[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAP_FETCH_SIZE] = 1024 * 256
      return properties
    }

    /**
     * Generate properties.
     *
     * @param authCreds The object which contains information about settings for a connection.
     * @return <tt>Properties</tt> New properties.
     */
    fun genProps(authCreds: AuthCredentials?): Properties {
      val prop = Properties()
      authCreds?.let {
        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAP_SSL_ENABLE] = it.imapOpt === SecurityType.Option.SSL_TLS
        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAP_STARTTLS_ENABLE] = it.imapOpt === SecurityType.Option.STARTLS
        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAP_SSL_CHECK_SERVER_IDENTITY] = it.imapOpt === SecurityType.Option.SSL_TLS
        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAP_FETCH_SIZE] = 1024 * 128

        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_AUTH] = it.hasCustomSignInForSmtp
        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_SSL_ENABLE] = it.smtpOpt === SecurityType.Option.SSL_TLS
        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_STARTTLS_ENABLE] = it.smtpOpt === SecurityType.Option.STARTLS
        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_SSL_CHECK_SERVER_IDENTITY] = it.smtpOpt === SecurityType.Option.SSL_TLS

        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAPS_SSL_TRUST] = Constants.HOST_MACHINE_IP
        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAP_SSL_TRUST] = Constants.HOST_MACHINE_IP
        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_SSL_TRUST] = Constants.HOST_MACHINE_IP

        if (authCreds.useOAuth2) {
          prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAP_AUTH_MECHANISMS] = JavaEmailConstants.AUTH_MECHANISMS_XOAUTH2
          prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAPS_AUTH_LOGIN_DISABLE] = "true"
          prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAPS_AUTH_PLAIN_DISABLE] = "true"
          prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAPS_AUTH_XOAUTH2_DISABLE] = "false"
          prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_AUTH_MECHANISMS] = JavaEmailConstants.AUTH_MECHANISMS_XOAUTH2
          prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_AUTH_LOGIN_DISABLE] = "true"
          prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_AUTH_PLAIN_DISABLE] = "true"
          prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_AUTH_XOAUTH2_DISABLE] = "false"
        }
      }

      return prop
    }

    /**
     * Generate properties.
     *
     * @param accountEntity The object which contains information about settings for a connection.
     * @return <tt>Properties</tt> New properties.
     */
    fun genProps(accountEntity: AccountEntity?): Properties {
      val prop = Properties()
      accountEntity?.let {
        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAP_SSL_ENABLE] = it.imapOpt() === SecurityType.Option.SSL_TLS
        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAP_STARTTLS_ENABLE] = it.imapOpt() === SecurityType.Option.STARTLS
        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAP_FETCH_SIZE] = 1024 * 128
        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAP_SSL_CHECK_SERVER_IDENTITY] = it.imapOpt() === SecurityType.Option.SSL_TLS

        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_AUTH] = it.useCustomSignForSmtp
        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_SSL_ENABLE] = it.smtpOpt() === SecurityType.Option.SSL_TLS
        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_STARTTLS_ENABLE] = it.smtpOpt() === SecurityType.Option.STARTLS
        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_SSL_CHECK_SERVER_IDENTITY] = it.smtpOpt() === SecurityType.Option.SSL_TLS

        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAPS_SSL_TRUST] = Constants.HOST_MACHINE_IP
        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAP_SSL_TRUST] = Constants.HOST_MACHINE_IP
        prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_SSL_TRUST] = Constants.HOST_MACHINE_IP

        if (accountEntity.imapAuthMechanisms == JavaEmailConstants.AUTH_MECHANISMS_XOAUTH2) {
          prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAP_AUTH_MECHANISMS] = JavaEmailConstants.AUTH_MECHANISMS_XOAUTH2
          prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAPS_AUTH_LOGIN_DISABLE] = "true"
          prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAPS_AUTH_PLAIN_DISABLE] = "true"
          prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAPS_AUTH_XOAUTH2_DISABLE] = "false"
        }

        if (accountEntity.smtpAuthMechanisms == JavaEmailConstants.AUTH_MECHANISMS_XOAUTH2) {
          prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_AUTH_MECHANISMS] = JavaEmailConstants.AUTH_MECHANISMS_XOAUTH2
          prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_AUTH_LOGIN_DISABLE] = "true"
          prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_AUTH_PLAIN_DISABLE] = "true"
          prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_AUTH_XOAUTH2_DISABLE] = "false"
        }
      }

      return prop
    }
  }
}
