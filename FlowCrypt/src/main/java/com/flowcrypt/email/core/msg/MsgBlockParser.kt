/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.core.msg

import com.flowcrypt.email.api.retrofit.response.model.MsgBlock
import com.flowcrypt.email.api.retrofit.response.model.MsgBlockFactory
import com.flowcrypt.email.extensions.kotlin.normalize
import com.flowcrypt.email.extensions.kotlin.toEscapedHtml
import com.flowcrypt.email.security.pgp.PgpArmor
import com.flowcrypt.email.security.pgp.PgpMsg
import java.util.Properties
import javax.mail.Session
import javax.mail.internet.MimeMessage

object MsgBlockParser {
  private const val ARMOR_HEADER_MAX_LENGTH = 50

  fun detectBlocks(text: String): NormalizedTextAndBlocks {
    val normalized = text.normalize()
    val blocks = mutableListOf<MsgBlock>()
    var startAt = 0
    while (true) {
      val continueAt = detectNextBlock(normalized, startAt, blocks)
      if (startAt >= continueAt) return NormalizedTextAndBlocks(normalized, blocks)
      startAt = continueAt
    }
  }

  fun fmtDecryptedAsSanitizedHtmlBlocks(decryptedContent: ByteArray?): SanitizedBlocks {
    if (decryptedContent == null) return SanitizedBlocks(emptyList(), false)
    val blocks = mutableListOf<MsgBlock>()
    if (MimeUtils.resemblesMsg(decryptedContent)) {
      val decoded = PgpMsg.extractMimeContent(
        MimeMessage(Session.getInstance(Properties()), decryptedContent.inputStream())
      )
      var isRichText = false
      when {
        decoded.html != null -> {
          // sanitized html
          val sanitizedHtml = PgpMsg.sanitizeHtmlKeepBasicTags(decoded.html)
          blocks.add(MsgBlockFactory.fromContent(MsgBlock.Type.DECRYPTED_HTML, sanitizedHtml))
          isRichText = true
        }
        decoded.text != null -> {
          // escaped text as html
          val html = decoded.text.toEscapedHtml()
          blocks.add(MsgBlockFactory.fromContent(MsgBlock.Type.DECRYPTED_HTML, html))
        }
        else -> {
          // escaped mime text as html
          val html = String(decryptedContent).toEscapedHtml()
          blocks.add(MsgBlockFactory.fromContent(MsgBlock.Type.DECRYPTED_HTML, html))
        }
      }

      for (attachment in decoded.attachments) {
        blocks.add(
          if (PgpMsg.treatAs(attachment) == PgpMsg.TreatAs.PUBLIC_KEY) {
            val content = String(attachment.inputStream.readBytes())
            MsgBlockFactory.fromContent(MsgBlock.Type.PUBLIC_KEY, content)
          } else {
            MsgBlockFactory.fromAttachment(MsgBlock.Type.DECRYPTED_ATT, attachment)
          }
        )
      }

      return SanitizedBlocks(blocks, isRichText)
    } else {
      val armoredKeys = mutableListOf<String>()
      val content = PgpMsg.stripPublicKeys(
        PgpMsg.stripFcReplyToken(PgpMsg.extractFcAttachments(String(decryptedContent), blocks)),
        armoredKeys
      ).toEscapedHtml()
      blocks.add(MsgBlockFactory.fromContent(MsgBlock.Type.DECRYPTED_HTML, content))
      for (armoredKey in armoredKeys) {
        blocks.add(MsgBlockFactory.fromContent(MsgBlock.Type.PUBLIC_KEY, armoredKey))
      }
      return SanitizedBlocks(blocks, false)
    }
  }

  private fun detectNextBlock(text: String, startAt: Int, blocks: MutableList<MsgBlock>): Int {
    val initialBlockCount = blocks.size
    var continueAt = -1
    val beginIndex = text.indexOf(
      PgpArmor.ARMOR_HEADER_DICT[MsgBlock.Type.UNKNOWN]!!.begin, startAt
    )
    if (beginIndex != -1) { // found
      val endIndex = (beginIndex + ARMOR_HEADER_MAX_LENGTH).coerceAtMost(text.length)
      val potentialHeaderBegin = text.substring(beginIndex, endIndex)
      for (blockHeaderKvp in PgpArmor.ARMOR_HEADER_DICT) {
        val blockHeaderDef = blockHeaderKvp.value
        if (!blockHeaderDef.replace || potentialHeaderBegin.indexOf(blockHeaderDef.begin) != 0) {
          continue
        }

        if (beginIndex > startAt) {
          // only replace blocks if they begin on their own line
          // contains deliberate block: `-----BEGIN PGP PUBLIC KEY BLOCK-----\n...`
          // contains deliberate block: `Hello\n-----BEGIN PGP PUBLIC KEY BLOCK-----\n...`
          // just plaintext (accidental block): `Hello -----BEGIN PGP PUBLIC KEY BLOCK-----\n...`
          // block treated as plaintext, not on dedicated line - considered accidental
          // this will actually cause potential deliberate blocks
          // that follow accidental block to be ignored
          // but if the message already contains accidental
          // (not on dedicated line) blocks, it's probably a good thing to ignore the rest
          var textBeforeBlock = text.substring(startAt, beginIndex)
          if (!textBeforeBlock.endsWith('\n')) continue
          textBeforeBlock = textBeforeBlock.trim()
          if (textBeforeBlock.isNotEmpty()) {
            blocks.add(MsgBlockFactory.fromContent(MsgBlock.Type.PLAIN_TEXT, textBeforeBlock))
          }
        }

        val end = blockHeaderDef.end
        val endHeaderIndex = text.indexOf(end, beginIndex + blockHeaderDef.begin.length)
        val endHeaderLength = if (endHeaderIndex == -1) 0 else end.length

        if (endHeaderIndex != -1) {
          // identified end of the same block
          continueAt = endHeaderIndex + endHeaderLength
          val content = text.substring(beginIndex, continueAt).trim()
          blocks.add(MsgBlockFactory.fromContent(blockHeaderKvp.key, content))
        } else {
          // corresponding end not found
          val content = text.substring(beginIndex)
          blocks.add(MsgBlockFactory.fromContent(blockHeaderKvp.key, content, true))
        }
        break
      }
    }

    if (text.isNotEmpty() && blocks.size == initialBlockCount) {
      // didn't find any blocks, but input is non-empty
      val remainingText = text.substring(startAt).trim()
      if (remainingText.isNotEmpty()) {
        blocks.add(MsgBlockFactory.fromContent(MsgBlock.Type.PLAIN_TEXT, remainingText))
      }
    }

    return continueAt
  }

  data class NormalizedTextAndBlocks(
    val normalized: String,
    val blocks: List<MsgBlock>
  )

  data class SanitizedBlocks(
    val blocks: List<MsgBlock>,
    val isRichText: Boolean
  )
}
