/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.protocol;

import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.protocol.BODY;
import com.sun.mail.imap.protocol.IMAPProtocol;

import java.io.IOException;
import java.io.InputStream;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimePart;

/**
 * This class describes custom realization of some IMAP futures, which not found in JavaMail implementation.
 *
 * @author Denis Bondarenko
 * Date: 29.09.2017
 * Time: 14:57
 * E-mail: DenBond7@gmail.com
 */

public class ImapProtocolUtil {
  /**
   * Return the MIME format stream of headers for this body part.
   * The MIME part specifier refers to the [MIME-IMB] header for this part.
   * See details here https://tools.ietf.org/html/rfc3501#section-6.4.5.
   *
   * @param folder    The {@link IMAPFolder} which contains the parent message;
   * @param msgNumber This number will be used for fetching {@link MimePart} details;
   * @param sectionId The {@link Part} section id.
   * @return A {@link MimePart} header {@link InputStream}
   * @throws MessagingException
   */
  public static InputStream getHeaderStream(IMAPFolder folder, final int msgNumber,
                                            final int sectionId) throws MessagingException {

    return (InputStream) folder.doCommand(new IMAPFolder.ProtocolCommand() {
      public Object doCommand(IMAPProtocol imapProtocol) throws ProtocolException {
        BODY body = imapProtocol.peekBody(msgNumber, sectionId + ".MIME");
        if (body != null) {
          return body.getByteArrayInputStream();
        } else {
          return null;
        }
      }
    });
  }

  /**
   * Get {@link Part} which has an attachment with such attachment id.
   *
   * @param folder    The {@link IMAPFolder} which contains the parent message;
   * @param msgNumber This number will be used for fetching {@link Part} details;
   * @param part      The parent part.
   * @return {@link Part} which has attachment or null if message doesn't have such attachment.
   * @throws MessagingException
   * @throws IOException
   */
  public static BodyPart getAttPartById(IMAPFolder folder, int msgNumber, Part part, String attId)
      throws MessagingException, IOException {
    if (part != null && part.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
      Multipart multiPart = (Multipart) part.getContent();
      int numberOfParts = multiPart.getCount();
      String[] headers;
      for (int partCount = 0; partCount < numberOfParts; partCount++) {
        BodyPart bodyPart = multiPart.getBodyPart(partCount);
        if (bodyPart.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
          BodyPart innerPart = getAttPartById(folder, msgNumber, bodyPart, attId);
          if (innerPart != null) {
            return innerPart;
          }
        } else if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
          InputStream inputStream = ImapProtocolUtil.getHeaderStream(folder, msgNumber,
              partCount + 1);

          if (inputStream == null) {
            throw new MessagingException("Failed to fetch headers");
          }

          InternetHeaders internetHeaders = new InternetHeaders(inputStream);
          headers = internetHeaders.getHeader(JavaEmailConstants.HEADER_CONTENT_ID);

          if (headers == null) {
            //try to receive custom Gmail attachments header X-Attachment-Id
            headers = internetHeaders.getHeader(JavaEmailConstants.HEADER_X_ATTACHMENT_ID);
          }

          if (headers != null && attId.equals(headers[0])) {
            return bodyPart;
          }
        }
      }
      return null;
    } else {
      return null;
    }
  }
}
