/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org). Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.protocol;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.sun.mail.gimap.GmailSSLStore;

import javax.mail.MessagingException;
import javax.mail.Session;

/**
 * This util class help generate Store classes.
 *
 * @author DenBond7
 *         Date: 03.05.2017
 *         Time: 18:00
 *         E-mail: DenBond7@gmail.com
 */

public class OpenStoreHelper {

    /**
     * Open and connect to the store using gimaps protocol.
     *
     * @param token       An OAuth2 access token;
     * @param accountName An account name which use to create connection;
     * @return <tt>GmailSSLStore</tt> A GmailSSLStore object based on properties for
     * gimaps.
     */

    public static GmailSSLStore openAndConnectToGimapsStore(String token, String accountName) throws
            MessagingException {

        Session session = Session.getInstance(PropertiesHelper.generatePropertiesForGimaps());
        session.setDebug(BuildConfig.DEBUG);

        GmailSSLStore gmailSSLStore = (GmailSSLStore) session.getStore(JavaEmailConstants
                .PROTOCOL_GIMAPS);
        gmailSSLStore.connect(JavaEmailConstants.GMAIL_IMAP_SERVER, accountName, token);
        return gmailSSLStore;
    }

    /**
     * Generate a session for gimaps protocol.
     *
     * @return <tt>Session</tt> A new session for gimaps protocol based on properties for gimaps.
     */
    public static Session getGmailSession() {
        Session session = Session.getInstance(PropertiesHelper.generatePropertiesForGimaps());
        session.setDebug(BuildConfig.DEBUG);
        return session;
    }
}
