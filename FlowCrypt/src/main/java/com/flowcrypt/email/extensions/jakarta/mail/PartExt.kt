/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.jakarta.mail

import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.core.msg.RawBlockParser
import jakarta.mail.Part
import jakarta.mail.internet.ContentType

fun Part.isInline(): Boolean {
  return (this.disposition?.lowercase() ?: "") == Part.INLINE
}

fun Part.isMultipart(): Boolean {
  return isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)
}

fun Part.isMultipartAlternative(): Boolean {
  return isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART_ALTERNATIVE)
}

fun Part.isPlainText(): Boolean {
  return isMimeType(JavaEmailConstants.MIME_TYPE_TEXT_PLAIN)
}

fun Part.baseContentType(): String? {
  return try {
    ContentType(contentType).baseType
  } catch (e: Exception) {
    null
  }
}

fun Part.isOpenPGPMimeSigned(): Boolean {
  val type = try {
    ContentType(contentType)
  } catch (e: Exception) {
    null
  }
  return isMimeType("multipart/signed") && type?.getParameter("protocol")
    ?.lowercase() == "application/pgp-signature"
}

fun Part.hasPgpThings(): Boolean {
  val detectedBlocks = RawBlockParser.detectBlocks(this)
  return detectedBlocks.any { it.type in RawBlockParser.PGP_BLOCK_TYPES }
}
