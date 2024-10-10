/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.com.google.api.services.gmail.model

import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.extensions.java.lang.printStackTraceIfDebugOnly
import com.google.api.services.gmail.model.MessagePart
import jakarta.mail.Part
import jakarta.mail.internet.ContentDisposition
import jakarta.mail.internet.ContentType
import jakarta.mail.internet.ParseException

/**
 * @author Denys Bondarenko
 */

fun MessagePart.hasAttachments(): Boolean {
  return when {
    parts?.isNotEmpty() == true -> {
      parts?.any { part -> part.hasAttachments() } == true
    }

    Part.ATTACHMENT.equals(disposition(), ignoreCase = true) -> {
      true
    }

    else -> false
  }
}

/**
 * Get information about attachments from the given [MessagePart]
 *
 * @param depth  The depth of the given [MessagePart]
 * @return       a list of found attachments
 */
fun MessagePart.getAttachmentInfoList(depth: String = "0"): List<AttachmentInfo> {
  val attachmentInfoList = mutableListOf<AttachmentInfo>()
  if (isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
    for ((index, part) in (parts ?: emptyList()).withIndex()) {
      attachmentInfoList.addAll(
        part.getAttachmentInfoList("$depth${AttachmentInfo.DEPTH_SEPARATOR}${index}")
      )
    }
  } else if (Part.ATTACHMENT.equals(disposition(), ignoreCase = true)) {
    val attachmentInfoBuilder = AttachmentInfo.Builder()
    attachmentInfoBuilder.name = filename ?: depth
    attachmentInfoBuilder.encodedSize = body?.getSize()?.toLong() ?: 0
    attachmentInfoBuilder.type = mimeType ?: ""
    attachmentInfoBuilder.id = contentId()
      ?: EmailUtil.generateContentId(AttachmentInfo.INNER_ATTACHMENT_PREFIX)
    attachmentInfoBuilder.path = depth
    attachmentInfoList.add(attachmentInfoBuilder.build())
  }

  return attachmentInfoList
}

/**
 * This method is similar to [jakarta.mail.internet.MimeBodyPart.isMimeType]
 */
fun MessagePart.isMimeType(inputMimeType: String): Boolean {
  val type: String = mimeType
  return try {
    ContentType(type).match(inputMimeType)
  } catch (ex: ParseException) {
    // we only need the type and subtype so throw away the rest
    try {
      val i = type.indexOf(';')
      if (i > 0) return ContentType(type.substring(0, i)).match(inputMimeType)
    } catch (pex2: ParseException) {
      pex2.printStackTraceIfDebugOnly()
    }
    type.equals(inputMimeType, ignoreCase = true)
  }
}

/**
 * This method is similar to [jakarta.mail.internet.MimeBodyPart.getDisposition]
 */
fun MessagePart.disposition(): String? {
  try {
    val value = headers?.firstOrNull { it.name.equals("Content-Disposition", true) }?.value
      ?: return null
    val cd = ContentDisposition(value)
    return cd.disposition
  } catch (e: Exception) {
    e.printStackTraceIfDebugOnly()
    //we can drop the exception and just return null
    return null
  }
}

fun MessagePart.contentId(): String? {
  return headers?.firstOrNull { it.name.equals("Content-ID", true) }?.value
}

fun MessagePart.rawMimeType(): String? {
  return headers?.firstOrNull { it.name.equals("Content-Type", true) }?.value
}
