/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org). Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/tree/master/src/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.gmail;

/**
 * This class described Gmail constants.
 *
 * @author DenBond7
 *         Date: 28.04.2017
 *         Time: 9:47
 *         E-mail: DenBond7@gmail.com
 */

public class GmailConstants {
    public static final String FOLDER_NAME_INBOX = "INBOX";
    public static final String SCOPE_MAIL_GOOGLE_COM = "https://mail.google.com/";
    public static final String PROPERTY_NAME_MAIL_GIMAPS_SSL_ENABLE = "mail.gimaps.ssl.enable";
    public static final String PROPERTY_NAME_MAIL_GIMAPS_AUTH_MECHANISMS = "mail.gimaps.auth" +
            ".mechanisms";

    public static final String PROPERTY_NAME_MAIL_SMTP_AUTH = "mail.smtp.auth";
    public static final String PROPERTY_NAME_MAIL_SMTP_SSL_ENABLE = "mail.smtp.ssl.enable";
    public static final String PROPERTY_NAME_MAIL_SMTP_AUTH_MECHANISMS = "mail.smtp.auth" +
            ".mechanisms";


    public static final String HOST_SMTP_GMAIL_COM = "smtp.gmail.com";
    public static final int PORT_SMTP_GMAIL_COM = 465;
}
