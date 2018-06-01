/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.protocol;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPSSLStore;

import javax.mail.Session;
import javax.mail.URLName;

/**
 * A custom implementation of the {@link IMAPSSLStore}
 *
 * @author Denis Bondarenko
 * Date: 30.05.2018
 * Time: 15:00
 * E-mail: DenBond7@gmail.com
 */
public class CustomIMAPSSLStore extends IMAPSSLStore {
    /**
     * Constructor that takes a Session object and a URLName that
     * represents a specific IMAP server.
     *
     * @param session the Session
     * @param url     the URLName of this store
     */
    public CustomIMAPSSLStore(Session session, URLName url) {
        super(session, url);
    }

    @Override
    protected IMAPFolder newIMAPFolder(String fullName, char separator, Boolean isNamespace) {
        return new CustomIMAPFolder(fullName, separator, this, isNamespace);
    }
}
