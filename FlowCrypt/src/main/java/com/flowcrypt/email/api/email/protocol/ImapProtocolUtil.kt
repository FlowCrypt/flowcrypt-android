/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.protocol

import com.flowcrypt.email.api.email.JavaEmailConstants
import com.sun.mail.imap.IMAPFolder
import java.io.IOException
import java.io.InputStream
import javax.mail.BodyPart
import javax.mail.MessagingException
import javax.mail.Multipart
import javax.mail.Part
import javax.mail.internet.InternetHeaders
import javax.mail.internet.MimePart

/**
 * This class describes custom realization of some IMAP futures, which not found in JavaMail implementation.
 *
 * @author Denis Bondarenko
 * Date: 29.09.2017
 * Time: 14:57
 * E-mail: DenBond7@gmail.com
 */

class ImapProtocolUtil {

  companion object {
    /**
     * Return the MIME format stream of headers for this body part.
     * The MIME part specifier refers to the [MIME-IMB] header for this part.
     * See details here https://tools.ietf.org/html/rfc3501#section-6.4.5.
     *
     * @param folder    The [IMAPFolder] which contains the parent message;
     * @param msgNumber This number will be used for fetching [MimePart] details;
     * @param sectionId The [Part] section id.
     * @return A [MimePart] header [InputStream]
     * @throws MessagingException
     */
    @JvmStatic
    fun getHeaderStream(folder: IMAPFolder, msgNumber: Int, sectionId: Int): InputStream? {
      return folder.doCommand { imapProtocol ->
        imapProtocol.peekBody(msgNumber, "$sectionId.MIME")?.byteArrayInputStream
      } as? InputStream
    }

    /**
     * Get [Part] which has an attachment with such attachment id.
     *
     * @param folder    The [IMAPFolder] which contains the parent message;
     * @param msgNumber This number will be used for fetching [Part] details;
     * @param part      The parent part.
     * @return [Part] which has attachment or null if message doesn't have such attachment.
     * @throws MessagingException
     * @throws IOException
     */
    @JvmStatic
    fun getAttPartById(folder: IMAPFolder, msgNumber: Int, part: Part?, attId: String): BodyPart? {
      if (part != null && part.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
        val multiPart = part.content as Multipart
        val numberOfParts = multiPart.count
        var headers: Array<String>?
        for (partCount in 0 until numberOfParts) {
          val bodyPart = multiPart.getBodyPart(partCount)
          if (bodyPart.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
            val innerPart = getAttPartById(folder, msgNumber, bodyPart, attId)
            if (innerPart != null) {
              return innerPart
            }
          } else if (Part.ATTACHMENT.equals(bodyPart.disposition, ignoreCase = true)) {
            val inputStream = getHeaderStream(folder, msgNumber, partCount + 1)
                ?: throw MessagingException("Failed to fetch headers")

            val internetHeaders = InternetHeaders(inputStream)
            headers = internetHeaders.getHeader(JavaEmailConstants.HEADER_CONTENT_ID)

            if (headers == null) {
              //try to receive custom Gmail attachments header X-Attachment-Id
              headers = internetHeaders.getHeader(JavaEmailConstants.HEADER_X_ATTACHMENT_ID)
            }

            if (headers != null && attId == headers[0]) {
              return bodyPart
            }
          }
        }
        return null
      } else {
        return null
      }
    }
  }
}
