/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions

import com.google.api.services.gmail.model.MessagePart
import jakarta.mail.internet.ContentDisposition
import jakarta.mail.internet.ContentType
import jakarta.mail.internet.ParseException

/**
 * @author Denys Bondarenko
 */
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
