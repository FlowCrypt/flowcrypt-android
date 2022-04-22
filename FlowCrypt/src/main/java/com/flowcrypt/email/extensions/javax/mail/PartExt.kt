/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *    Ivan Pizhenko
 */

package com.flowcrypt.email.extensions.javax.mail

import com.flowcrypt.email.api.email.JavaEmailConstants
import javax.mail.Part
import javax.mail.internet.ContentType

fun Part.isInline(): Boolean {
  return (this.disposition?.lowercase() ?: "") == Part.INLINE
}

fun Part.isMultipart(): Boolean {
  return isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)
}

fun Part.isMultipartAlternative(): Boolean {
  return isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART_ALTERNATIVE)
}

fun Part.isHtmlText(): Boolean {
  return isMimeType(JavaEmailConstants.MIME_TYPE_TEXT_HTML)
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
