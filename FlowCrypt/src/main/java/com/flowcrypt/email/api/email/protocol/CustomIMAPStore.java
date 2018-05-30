/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.protocol;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;

import javax.mail.Session;
import javax.mail.URLName;

/**
 * A custom implementation of the {@link IMAPStore}
 *
 * @author Denis Bondarenko
 * Date: 30.05.2018
 * Time: 15:00
 * E-mail: DenBond7@gmail.com
 */
public class CustomIMAPStore extends IMAPStore {

    public CustomIMAPStore(Session session, URLName url) {
        super(session, url);
    }

    protected CustomIMAPStore(Session session, URLName url, String name, boolean isSSL) {
        super(session, url, name, isSSL);
    }

    @Override
    protected IMAPFolder newIMAPFolder(String fullName, char separator, Boolean isNamespace) {
        return new CustomIMAPFolder(fullName, separator, this, isNamespace);
    }
}
