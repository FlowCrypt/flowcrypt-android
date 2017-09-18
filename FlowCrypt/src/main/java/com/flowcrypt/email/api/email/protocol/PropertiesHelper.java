/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

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

    private static final String BOOLEAN_VALUE_TRUE = "true";

    /**
     * Generate properties for gimaps protocol.
     *
     * @return <tt>Properties</tt> New properties with setup gimaps connection;
     */
    public static Properties generatePropertiesForGmail() {
        Properties properties = new Properties();
        properties.put(GmailConstants.PROPERTY_NAME_MAIL_GIMAPS_SSL_ENABLE, BOOLEAN_VALUE_TRUE);
        properties.put(GmailConstants.PROPERTY_NAME_MAIL_GIMAPS_AUTH_MECHANISMS, JavaEmailConstants
                .MECHANISMS_TYPE_XOAUTH2);
        properties.put(JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_AUTH, BOOLEAN_VALUE_TRUE);
        properties.put(JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_SSL_ENABLE, BOOLEAN_VALUE_TRUE);
        properties.put(JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_AUTH_MECHANISMS, JavaEmailConstants
                .MECHANISMS_TYPE_XOAUTH2);

        return properties;
    }

    /**
     * Generate properties for gimaps protocol which will be used for download attachment.
     *
     * @return <tt>Properties</tt> New properties with setup gimaps connection;
     */
    public static Properties generatePropertiesForDownloadGmailAttachments() {
        Properties properties = generatePropertiesForGmail();
        properties.put(GmailConstants.PROPERTY_NAME_MAIL_GIMAPS_FETCH_SIZE, 1024 * 256);

        return properties;
    }
}
