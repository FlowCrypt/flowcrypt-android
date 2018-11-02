/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.protocol;

import com.sun.mail.gimap.GmailSSLStore;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.protocol.ListInfo;

import javax.mail.Session;
import javax.mail.URLName;

/**
 * A custom implementation of the {@link GmailSSLStore}
 *
 * @author Denis Bondarenko
 * Date: 29.05.2018
 * Time: 12:27
 * E-mail: DenBond7@gmail.com
 */
public class CustomGmailSSLStore extends GmailSSLStore {

  /**
   * Constructor that takes a Session object and a URLName that
   * represents a specific IMAP server.
   *
   * @param session the Session
   * @param url     the URLName of this store
   */
  public CustomGmailSSLStore(Session session, URLName url) {
    super(session, url);
  }

  @Override
  protected IMAPFolder newIMAPFolder(ListInfo li) {
    return new CustomGmailFolder(li, this);
  }

  @Override
  protected IMAPFolder newIMAPFolder(String fullName, char separator, Boolean isNamespace) {
    return new CustomGmailFolder(fullName, separator, this, isNamespace);
  }
}
