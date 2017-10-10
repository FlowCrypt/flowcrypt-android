/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
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
    public static final String SCOPE_MAIL_GOOGLE_COM = "https://mail.google.com/";
    public static final String PROPERTY_NAME_MAIL_GIMAPS_SSL_ENABLE = "mail.gimaps.ssl.enable";
    public static final String PROPERTY_NAME_MAIL_GIMAPS_AUTH_MECHANISMS = "mail.gimaps.auth.mechanisms";
    public static final String PROPERTY_NAME_MAIL_GIMAPS_FETCH_SIZE = "mail.gimaps.fetchsize";


    public static final String GMAIL_IMAP_SERVER = "imap.gmail.com";
    public static final String GMAIL_SMTP_SERVER = "smtp.gmail.com";
    public static final int GMAIL_SMTP_PORT = 465;
    public static final int GMAIL_IMAP_PORT = 993;
}
