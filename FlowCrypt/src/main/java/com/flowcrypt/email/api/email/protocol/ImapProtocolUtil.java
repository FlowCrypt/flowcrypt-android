/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.protocol;

import com.flowcrypt.email.database.dao.source.AccountDao;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.protocol.BODY;
import com.sun.mail.imap.protocol.IMAPProtocol;

import java.io.InputStream;

import javax.mail.MessagingException;
import javax.mail.Part;

/**
 * This class describes custom realization of some IMAP futures, which not found in JavaMail implementation.
 *
 * @author Denis Bondarenko
 *         Date: 29.09.2017
 *         Time: 14:57
 *         E-mail: DenBond7@gmail.com
 */

public class ImapProtocolUtil {
    /**
     * Return the MIME format stream of headers for this body part.
     *
     * @param accountDao    The object which contains information about an email account.
     * @param imapFolder    The {@link IMAPFolder} which contains the parent message;
     * @param messageNumber This number will be used for fetching {@link Part} details;
     * @param sectionId     The {@link Part} section id.
     * @return
     * @throws MessagingException
     */
    public static InputStream getHeaderStream(AccountDao accountDao, IMAPFolder imapFolder, final int messageNumber,
                                              int sectionId) throws MessagingException {

        final String section;

        switch (accountDao.getAccountType()) {
            case AccountDao.ACCOUNT_TYPE_GOOGLE:
            case AccountDao.ACCOUNT_TYPE_OUTLOOK:
                section = sectionId + ".MIME";
                break;

            default:
                section = sectionId + ".HEADER";
                break;
        }

        return (InputStream) imapFolder.doCommand(new IMAPFolder.ProtocolCommand() {
            public Object doCommand(IMAPProtocol imapProtocol)
                    throws ProtocolException {

                BODY body = imapProtocol.peekBody(messageNumber, section);
                if (body != null) {
                    return body.getByteArrayInputStream();
                } else return null;
            }
        });
    }
}
