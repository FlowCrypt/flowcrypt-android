/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.core.msg

import com.flowcrypt.email.api.retrofit.response.model.InlinePlaneAttMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.MsgBlock
import com.flowcrypt.email.extensions.kotlin.toInputStream
import jakarta.mail.Session
import jakarta.mail.internet.ContentType
import jakarta.mail.internet.MimeMessage
import java.nio.charset.StandardCharsets
import java.util.Properties

object MimeUtils {
  fun resemblesMsg(msg: ByteArray?): Boolean {
    if (msg == null) return false
    val firstChars = msg.copyOfRange(0, msg.size.coerceAtMost(1000))
      .toString(StandardCharsets.US_ASCII)
      .lowercase()
    val contentType = CONTENT_TYPE_REGEX.find(firstChars) ?: return false
    return CONTENT_TRANSFER_ENCODING_REGEX.containsMatchIn(firstChars)
        || CONTENT_DISPOSITION_REGEX.containsMatchIn(firstChars)
        || firstChars.contains(BOUNDARY_1)
        || firstChars.contains(CHARSET)
        || (contentType.range.first == 0 && firstChars.contains(BOUNDARY_2))
  }

  fun isPlainImgAtt(block: MsgBlock): Boolean {
    return try {
      ((block is InlinePlaneAttMsgBlock)
          && ContentType(block.attMeta.type ?: "").match("image/*"))
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  fun mimeTextToMimeMessage(mimeText: String): MimeMessage {
    return MimeMessage(Session.getInstance(Properties()), mimeText.toInputStream())
  }

  private val CONTENT_TYPE_REGEX = Regex("content-type: +[0-9a-z\\-/]+")
  private val CONTENT_TRANSFER_ENCODING_REGEX = Regex("content-transfer-encoding: +[0-9a-z\\-/]+")
  private val CONTENT_DISPOSITION_REGEX = Regex("content-disposition: +[0-9a-z\\-/]+")
  private const val BOUNDARY_1 = "; boundary="
  private const val BOUNDARY_2 = "boundary="
  private const val CHARSET = "; charset="
}
