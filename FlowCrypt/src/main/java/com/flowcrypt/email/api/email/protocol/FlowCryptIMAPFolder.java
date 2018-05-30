/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.protocol;


import com.sun.mail.imap.IMAPFolder;

import javax.mail.FetchProfile;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 * An abstract implementation of the {@link IMAPFolder} for our purposes
 *
 * @author Denis Bondarenko
 *         Date: 30.05.2018
 *         Time: 9:50
 *         E-mail: DenBond7@gmail.com
 */
public interface FlowCryptIMAPFolder {
    void fetchGeneralInfo(Message[] messages, FetchProfile fetchProfile) throws MessagingException;
}
