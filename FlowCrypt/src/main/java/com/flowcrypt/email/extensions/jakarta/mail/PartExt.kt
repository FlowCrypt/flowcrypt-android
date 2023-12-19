/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *  Ivan Pizhenko
 */

package com.flowcrypt.email.extensions.jakarta.mail

import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.core.msg.RawBlockParser
import jakarta.mail.Part
import jakarta.mail.internet.ContentType
import java.util.regex.Matcher
import java.util.regex.Pattern

fun Part.isAttachment(): Boolean = isDisposition(Part.ATTACHMENT)

fun Part.isInline(): Boolean = isDisposition(Part.INLINE)

/**
 * https://www.ietf.org/rfc/rfc2183.txt
 *
 * disposition-type := "inline"
 *                        / "attachment"
 *                        / extension-token
 *                        ; values are not case-sensitive
 */
fun Part.isDisposition(predictedDisposition: String): Boolean {
  return try {
    predictedDisposition.equals(this.disposition, ignoreCase = true)
  } catch (e: Exception) {
    //https://github.com/FlowCrypt/flowcrypt-android/issues/2425
    allHeaders.toList().firstOrNull {
      it.name.equals("Content-Disposition", true)
    }?.value?.startsWith(prefix = predictedDisposition, ignoreCase = true) == true
  }
}

fun Part.getFileNameWithCarefully(): String? {
  return try {
    fileName
  } catch (e: Exception) {
    val undefined = "undefined"
    val contentDispositionValue = allHeaders.toList().firstOrNull {
      it.name.equals("Content-Disposition", true)
    }?.value ?: return undefined

    val pattern: Pattern = Pattern.compile(
      "(filename.*=)(.*)",
      Pattern.CASE_INSENSITIVE or Pattern.MULTILINE
    )
    val matcher: Matcher = pattern.matcher(contentDispositionValue)
    if (matcher.find()) {
      return matcher.group(2)
    } else return undefined
  }
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
