/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.core.msg

import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.PlainAttMsgBlock
import com.flowcrypt.email.extensions.kotlin.toInputStream
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.Properties
import javax.mail.Session
import javax.mail.internet.MimeMessage

object MimeUtils {
  fun resemblesMsg(msg: ByteArray?): Boolean {
    if (msg == null) return false
    val firstChars = msg.copyOfRange(0, msg.size.coerceAtMost(1000))
      .toString(StandardCharsets.US_ASCII)
      .toLowerCase(Locale.ROOT)
    val contentType = contentTypeRegex.find(firstChars) ?: return false
    return contentTransferEncodingRegex.containsMatchIn(firstChars)
      || contentDispositionRegex.containsMatchIn(firstChars)
      || firstChars.contains(kBoundary1)
      || firstChars.contains(kCharset)
      || (contentType.range.first == 0 && firstChars.contains(kBoundary2))
  }

  fun isPlainImgAtt(block: MsgBlock): Boolean {
    return (block is PlainAttMsgBlock)
        && block.attMeta.type != null
        && imageContentTypes.contains(block.attMeta.type)
  }

  fun mimeTextToMimeMessage(mimeText: String) : MimeMessage {
    return MimeMessage(Session.getInstance(Properties()), mimeText.toInputStream())
  }

  private val imageContentTypes = setOf(
    "image/jpeg", "image/jpg", "image/bmp", "image/png", "image/svg+xml"
  )
  private val contentTypeRegex = Regex("content-type: +[0-9a-z\\-/]+")
  private val contentTransferEncodingRegex = Regex("content-transfer-encoding: +[0-9a-z\\-/]+")
  private val contentDispositionRegex = Regex("content-disposition: +[0-9a-z\\-/]+")
  private const val kBoundary1 = "; boundary="
  private const val kBoundary2 = "boundary="
  private const val kCharset = "; charset="
}
