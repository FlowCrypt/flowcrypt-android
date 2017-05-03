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
    public static GmailSSLStore openAndConnectToGimapsStore(String token, String accountName) throws
            MessagingException {

        Session session = Session.getInstance(PropertiesHelper.generatePropertiesForGimaps());
        session.setDebug(BuildConfig.DEBUG);

        GmailSSLStore gmailSSLStore = (GmailSSLStore) session.getStore(JavaEmailConstants
                .PROTOCOL_GIMAPS);
        gmailSSLStore.connect(JavaEmailConstants.GMAIL_IMAP_SERVER, accountName, token);
        return gmailSSLStore;
    }
}
