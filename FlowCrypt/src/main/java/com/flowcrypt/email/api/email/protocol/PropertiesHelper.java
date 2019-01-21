/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.protocol;

import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.gmail.GmailConstants;
import com.flowcrypt.email.api.email.model.AuthCredentials;
import com.flowcrypt.email.api.email.model.SecurityType;

import java.util.Properties;

/**
 * This util class help generates Properties to connect for different mail servers.
 *
 * @author DenBond7
 * Date: 03.05.2017
 * Time: 18:12
 * E-mail: DenBond7@gmail.com
 */

public class PropertiesHelper {

  private static final String BOOLEAN_VALUE_TRUE = "true";

  /**
   * Generate properties for gimaps protocol.
   *
   * @return <tt>Properties</tt> New properties with setup gimaps connection;
   */
  public static Properties generateGmailProperties() {
    Properties properties = new Properties();
    properties.put(GmailConstants.PROPERTY_NAME_MAIL_GIMAPS_SSL_ENABLE, BOOLEAN_VALUE_TRUE);
    properties.put(GmailConstants.PROPERTY_NAME_MAIL_GIMAPS_AUTH_MECHANISMS, JavaEmailConstants
        .AUTH_MECHANISMS_XOAUTH2);
    properties.put(JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_AUTH, BOOLEAN_VALUE_TRUE);
    properties.put(JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_SSL_ENABLE, BOOLEAN_VALUE_TRUE);
    properties.put(JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_AUTH_MECHANISMS, JavaEmailConstants
        .AUTH_MECHANISMS_XOAUTH2);

    return properties;
  }

  /**
   * Generate properties for gimaps protocol which will be used for download attachment.
   *
   * @return <tt>Properties</tt> New properties with setup gimaps connection;
   */
  public static Properties genGmailAttsProperties() {
    Properties properties = generateGmailProperties();
    properties.put(GmailConstants.PROPERTY_NAME_MAIL_GIMAPS_FETCH_SIZE, 1024 * 256);
    return properties;
  }

  /**
   * Generate properties for imap protocol which will be used for download attachment.
   *
   * @return <tt>Properties</tt> New properties with setup imap connection;
   */
  public static Properties genDownloadAttsProps(AuthCredentials authCreds) {
    Properties properties = genProps(authCreds);
    properties.put(JavaEmailConstants.PROPERTY_NAME_MAIL_IMAP_FETCH_SIZE, 1024 * 256);
    return properties;
  }

  /**
   * Generate properties.
   *
   * @param authCreds The object which contains information about settings for a connection.
   * @return <tt>Properties</tt> New properties.
   */
  public static Properties genProps(AuthCredentials authCreds) {
    Properties properties = new Properties();
    if (authCreds != null) {
      properties.put(JavaEmailConstants.PROPERTY_NAME_MAIL_IMAP_SSL_ENABLE,
          authCreds.getImapOpt() == SecurityType.Option.SSL_TLS);
      properties.put(JavaEmailConstants.PROPERTY_NAME_MAIL_IMAP_STARTTLS_ENABLE,
          authCreds.getImapOpt() == SecurityType.Option.STARTLS);
      properties.put(JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_AUTH,
          authCreds.hasCustomSignInForSmtp());
      properties.put(JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_SSL_ENABLE,
          authCreds.getSmtpOpt() == SecurityType.Option.SSL_TLS);
      properties.put(JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_STARTTLS_ENABLE,
          authCreds.getSmtpOpt() == SecurityType.Option.STARTLS);
    }

    return properties;
  }
}
