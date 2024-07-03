/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.protocol

import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.extensions.jakarta.mail.isAttachment
import org.eclipse.angus.mail.imap.IMAPFolder
import jakarta.mail.BodyPart
import jakarta.mail.MessagingException
import jakarta.mail.Multipart
import jakarta.mail.Part
import jakarta.mail.internet.MimePart
import java.io.InputStream

/**
 * This class describes custom realization of some IMAP futures, which not found in JavaMail implementation.
 *
 * @author Denys Bondarenko
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
     * @param part         The parent part.
     * @param currentPath  The current path of MIME hierarchy.
     * @param neededPath   The path where the needed attachment exists.
     * @return [Part] which has attachment or null if message doesn't have such attachment.
     */
    fun getAttPartByPath(part: Part?, currentPath: String = "0/", neededPath: String): BodyPart? {
      if (part != null && part.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
        val neededParentPath =
          neededPath.substringBeforeLast(AttachmentInfo.DEPTH_SEPARATOR) + AttachmentInfo.DEPTH_SEPARATOR
        val multiPart = part.content as Multipart
        val partsCount = multiPart.count

        if (currentPath == neededParentPath) {
          val position = neededPath.substringAfterLast(AttachmentInfo.DEPTH_SEPARATOR).toInt()

          if (partsCount > position) {
            val bodyPart = multiPart.getBodyPart(position)
            if (bodyPart.isAttachment()) {
              return bodyPart
            }
          }
        } else {
          val nextDepth = neededParentPath.replaceFirst(currentPath, "")
            .split(AttachmentInfo.DEPTH_SEPARATOR).first().toInt()
          val bodyPart = multiPart.getBodyPart(nextDepth)
          return getAttPartByPath(
            bodyPart,
            currentPath + nextDepth + AttachmentInfo.DEPTH_SEPARATOR,
            neededPath
          )
        }
        return null
      } else {
        return null
      }
    }
  }
}
