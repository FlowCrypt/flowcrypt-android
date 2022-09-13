/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.gmail.api

import android.content.Context
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.javamail.CustomMimeBodyPart
import com.flowcrypt.email.api.email.javamail.CustomMimeMultipart
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.isMimeType
import com.flowcrypt.email.extensions.rawMimeType
import com.google.api.services.gmail.model.Message
import com.google.api.services.gmail.model.MessagePart
import jakarta.mail.Flags
import jakarta.mail.Multipart
import jakarta.mail.Session
import jakarta.mail.internet.InternetHeaders
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.SharedInputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.util.Date
import java.util.Properties

/**
 * @author Denis Bondarenko
 *         Date: 1/6/21
 *         Time: 3:12 PM
 *         E-mail: DenBond7@gmail.com
 */
class GmaiAPIMimeMessage(
  session: Session = Session.getInstance(Properties()),
  message: Message,
  private val context: Context? = null,
  private val accountEntity: AccountEntity? = null
) : MimeMessage(session) {
  private val internalDate = Date(message.internalDate ?: System.currentTimeMillis())
  private val msgId = message.id

  init {
    message.payload?.let { payload ->
      var removeContentTransferEncoding = false
      generateMultipart(message)?.let { multipart ->
        setContent(multipart)
      } ?: message.payload?.body?.let { body ->
        setContent(body.decodeData(), message.payload.mimeType)
        removeContentTransferEncoding = true
      }

      for (header in payload.headers ?: emptyList()) {
        setHeader(header.name, header.value)
      }

      if (removeContentTransferEncoding) {
        removeHeader("Content-Transfer-Encoding")
      }
    }

    if (message.labelIds?.contains(GmailApiHelper.LABEL_UNREAD) != true) {
      setFlag(Flags.Flag.SEEN, true)
    }
  }

  private fun generateMultipart(message: Message): Multipart? {
    return message.payload?.let { mainMessagePart ->
      parseBodyPart(mainMessagePart)?.content as Multipart
    }
  }

  private fun parseBodyPart(messagePart: MessagePart?): MimeBodyPart? {
    messagePart ?: return null
    val parts = mutableListOf<MimeBodyPart>()
    for (part in messagePart.parts ?: emptyList()) {
      if (part.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
        parseBodyPart(part)?.let { multipart ->
          parts.add(multipart)
        }
      } else {
        val generatedBodyPart = genMimeBodyPart(part)
        if (EmailUtil.isPartAllowed(generatedBodyPart)) {
          parts.add(generatedBodyPart)
        }
      }
    }

    val multipart = CustomMimeMultipart(messagePart.rawMimeType() ?: messagePart.mimeType)
    parts.forEach {
      multipart.addBodyPart(it)
    }

    return MimeBodyPart().apply {
      setContent(multipart)

      for (header in messagePart.headers ?: emptyList()) {
        addHeader(header.name, header.value)
      }
    }
  }

  private fun genMimeBodyPart(part: MessagePart): MimeBodyPart {
    val headers = InternetHeaders()
    for (header in part.headers ?: emptyList()) {
      headers.addHeader(header.name, header.value)
    }

    return if (part.body?.attachmentId?.isNotEmpty() == true) {
      if (context != null && accountEntity != null) {
        //because we use users.messages.attachments.get that returns the body data of a MIME message part as a base64url encoded string
        headers.setHeader("Content-Transfer-Encoding", "base64")
        val attInputStream = GmailApiHelper.getAttInputStream(
          context = context,
          accountEntity = accountEntity,
          msgId = msgId,
          attId = part.body.attachmentId,
          decodeBase64 = false
        )
        CustomMimeBodyPart(CustomSharedInputStream(attInputStream), headers)
      } else MimeBodyPart(headers, byteArrayOf())
    } else {
      //because we use body.decodeData() that returns decoded base64url string
      headers.removeHeader("Content-Transfer-Encoding")
      MimeBodyPart(headers, part.body.decodeData() ?: byteArrayOf())
    }
  }

  class CustomSharedInputStream(inputStream: InputStream) : FilterInputStream(inputStream),
    SharedInputStream {
    override fun getPosition(): Long = 0
    override fun newStream(start: Long, end: Long) = this
  }

  override fun getReceivedDate(): Date {
    return internalDate
  }
}
