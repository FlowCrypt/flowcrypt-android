package com.flowcrypt.email.api.email.protocol;

import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.gmail.GmailConstants;

import java.util.Properties;

/**
 * This util class help generates Properties to connect for different mail servers.
 *
 * @author DenBond7
 *         Date: 03.05.2017
 *         Time: 18:12
 *         E-mail: DenBond7@gmail.com
 */

public class PropertiesHelper {

    public static final String BOOLEAN_VALUE_TRUE = "true";

    /**
     * Generate properties for gimaps protocol.
     *
     * @return <tt>Properties</tt> New properties with setup gimaps connection;
     */
    public static Properties generatePropertiesForGimaps() {
        Properties properties = new Properties();
        properties.put(GmailConstants.PROPERTY_NAME_MAIL_GIMAPS_SSL_ENABLE, BOOLEAN_VALUE_TRUE);
        properties.put(GmailConstants.PROPERTY_NAME_MAIL_GIMAPS_AUTH_MECHANISMS, JavaEmailConstants
                .MECHANISMS_TYPE_XOAUTH2);

        return properties;
    }

    /**
     * Generate properties for gmail smtp connection.
     *
     * @return <tt>Properties</tt> New properties with setup gmail smtp connection;
     */
    public static Properties generatePropertiesForGmailSmtp() {
        Properties properties = new Properties();
        properties.put(GmailConstants.PROPERTY_NAME_MAIL_SMTP_AUTH, BOOLEAN_VALUE_TRUE);
        properties.put(GmailConstants.PROPERTY_NAME_MAIL_SMTP_SSL_ENABLE, BOOLEAN_VALUE_TRUE);
        properties.put(GmailConstants.PROPERTY_NAME_MAIL_SMTP_AUTH_MECHANISMS, JavaEmailConstants
                .MECHANISMS_TYPE_XOAUTH2);

        return properties;
    }
}
