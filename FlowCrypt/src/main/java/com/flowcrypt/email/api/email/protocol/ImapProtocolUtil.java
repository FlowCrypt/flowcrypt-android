/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.protocol;

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.protocol.BODY;
import com.sun.mail.imap.protocol.IMAPProtocol;

import java.io.InputStream;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimePart;

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
     * The MIME part specifier refers to the [MIME-IMB] header for this part.
     * See details here https://tools.ietf.org/html/rfc3501#section-6.4.5.
     *
     * @param imapFolder    The {@link IMAPFolder} which contains the parent message;
     * @param messageNumber This number will be used for fetching {@link MimePart} details;
     * @param sectionId     The {@link Part} section id.
     * @return A {@link MimePart} header {@link InputStream}
     * @throws MessagingException
     */
    public static InputStream getHeaderStream(IMAPFolder imapFolder, final int messageNumber,
                                              final int sectionId) throws MessagingException {

        return (InputStream) imapFolder.doCommand(new IMAPFolder.ProtocolCommand() {
            public Object doCommand(IMAPProtocol imapProtocol) throws ProtocolException {
                BODY body = imapProtocol.peekBody(messageNumber, sectionId + ".MIME");
                if (body != null) {
                    return body.getByteArrayInputStream();
                } else {
                    return null;
                }
            }
        });
    }
}
