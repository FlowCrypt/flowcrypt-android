/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.gmail.api

import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.javamail.CustomMimeMultipart
import com.flowcrypt.email.extensions.isMimeType
import com.flowcrypt.email.extensions.rawMimeType
import com.google.api.services.gmail.model.Message
import com.google.api.services.gmail.model.MessagePart
import java.util.*
import javax.mail.Flags
import javax.mail.Multipart
import javax.mail.Session
import javax.mail.internet.InternetHeaders
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage

/**
 * @author Denis Bondarenko
 *         Date: 1/6/21
 *         Time: 3:12 PM
 *         E-mail: DenBond7@gmail.com
 */
class GmaiAPIMimeMessage(session: Session = Session.getInstance(Properties()), message: Message) : MimeMessage(session) {
  private val internalDate = Date(message.internalDate ?: System.currentTimeMillis())

  init {
    message.payload?.let { payload ->
      generateMultipart(message)?.let { multipart ->
        setContent(multipart)
      } ?: message.payload?.body?.let { body ->
        setContent(body.decodeData(), message.payload.mimeType)
      }

      for (header in payload.headers ?: emptyList()) {
        setHeader(header.name, header.value)
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
        parts.add(generatedBodyPart)
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

    return MimeBodyPart(headers, part.body.decodeData() ?: byteArrayOf())
  }

  override fun getReceivedDate(): Date {
    return internalDate
  }
}