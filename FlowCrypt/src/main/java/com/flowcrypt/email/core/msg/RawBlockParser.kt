/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.core.msg

import com.flowcrypt.email.core.msg.RawBlockParser.RawBlock
import com.flowcrypt.email.extensions.jakarta.mail.baseContentType
import com.flowcrypt.email.extensions.jakarta.mail.getFileNameWithCarefully
import com.flowcrypt.email.extensions.jakarta.mail.isAttachment
import com.flowcrypt.email.extensions.jakarta.mail.isInline
import com.flowcrypt.email.extensions.java.io.readText
import com.flowcrypt.email.extensions.kotlin.normalize
import com.flowcrypt.email.security.pgp.PgpArmor
import com.sun.mail.util.BASE64DecoderStream
import jakarta.mail.Part
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMultipart
import jakarta.mail.internet.MimePart
import java.io.FilterInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

/**
 * This class is responsible for analyzing input source and extract [RawBlock] that can contain
 * some useful data like PGP message, plain text and etc.
 */
object RawBlockParser {
  val ENCRYPTED_FILE_REGEX = Regex(
    pattern = "(\\.pgp\$)|(\\.gpg\$)|(\\.[a-zA-Z0-9]{3,4}\\.asc\$)",
    option = RegexOption.IGNORE_CASE
  )

  val PGP_BLOCK_TYPES = setOf(
    RawBlockType.PGP_PUBLIC_KEY,
    RawBlockType.PGP_PRIVATE_KEY,
    RawBlockType.PGP_CLEARSIGN_MSG,
    RawBlockType.PGP_MSG
  )

  private const val ARMOR_HEADER_MAX_LENGTH = 50
  private val PRIVATE_KEY_REGEX = Regex(
    pattern = "(cryptup|flowcrypt)-backup-[a-z0-9]+\\.(key|asc)\$",
    option = RegexOption.IGNORE_CASE
  )
  private val PUBLIC_KEY_REGEX_1 = Regex(
    pattern = "^(0|0x)?[A-F0-9]{8}([A-F0-9]{8})?.*\\.asc\$",
    option = RegexOption.IGNORE_CASE
  )
  private val PUBLIC_KEY_REGEX_2 = Regex(
    pattern = "[A-F0-9]{8}.*\\.asc\$",
    option = RegexOption.IGNORE_CASE
  )
  private val HIDDEN_FILE_NAMES = setOf(
    "PGPexch.htm.pgp",
    "PGPMIME version identification",
    "Version.txt",
    "PGPMIME Versions Identification"
  )
  private val ENCRYPTED_MSG_NAMES = setOf(
    "message",
    "msg.asc",
    "message.asc",
    "encrypted.asc",
    "encrypted.eml.pgp",
    "Message.pgp",
    "openpgp-encrypted-message.asc"
  )

  fun detectBlocks(
    part: Part,
    isOpenPGPMimeSigned: Boolean = false,
    isOpenPGPMimeEncrypted: Boolean = false
  ): Collection<RawBlock> {
    val mimePart = (part as? MimePart) ?: return emptyList()
    return when {
      isOpenPGPMimeEncrypted -> {
        listOf(
          RawBlock(
            RawBlockType.PGP_MSG,
            mimePart.inputStream.readBytes(),
            isOpenPGPMimeSigned
          )
        )
      }

      mimePart.isAttachment() -> {
        when (treatAs(mimePart)) {
          TreatAs.HIDDEN -> emptyList()

          TreatAs.PGP_MSG -> {
            listOf(
              RawBlock(
                RawBlockType.PGP_MSG,
                mimePart.inputStream.readBytes(),
                isOpenPGPMimeSigned
              )
            )
          }

          TreatAs.PUBLIC_KEY -> {
            listOf(
              RawBlock(
                RawBlockType.PGP_PUBLIC_KEY,
                mimePart.inputStream.readBytes(),
                isOpenPGPMimeSigned
              )
            )
          }

          TreatAs.PRIVATE_KEY -> {
            listOf(
              RawBlock(
                RawBlockType.PGP_PRIVATE_KEY,
                mimePart.inputStream.readBytes(),
                isOpenPGPMimeSigned
              )
            )
          }

          else -> {
            listOf(
              RawBlock(
                RawBlockType.ATTACHMENT,
                mimePart.inputStream.readBytes(),
                isOpenPGPMimeSigned
              )
            )
          }
        }
      }

      mimePart.isInline() && mimePart.contentID != null -> {
        val inputStream = if (part.encoding == "base64") {
          //We have to use FilterInputStream to replace '-' with '+' and '_' with '/' as Gmail API
          //uses URL-safe implementation of Base64 encoding.
          //https://datatracker.ietf.org/doc/html/rfc4648#section-5
          val filterInputStream =
            object : FilterInputStream((mimePart as MimeBodyPart).rawInputStream) {
              override fun read(b: ByteArray?): Int {
                val byteArray = ByteArray(b?.size ?: 0)
                val count = super.read(byteArray)
                byteArray.forEachIndexed { index, byte ->
                  when (byte.toInt().toChar()) {
                    '-' -> byteArray[index] = '+'.code.toByte()
                    '_' -> byteArray[index] = '/'.code.toByte()
                  }
                }
                b?.let { byteArray.copyInto(it) }
                return count
              }
            }

          BASE64DecoderStream(filterInputStream)
        } else mimePart.inputStream

        listOf(
          RawBlock(
            RawBlockType.INLINE_ATTACHMENT,
            inputStream.readBytes(),
            isOpenPGPMimeSigned
          )
        )
      }

      else -> when {
        "text/rfc822-headers" == mimePart.baseContentType() -> {
          emptyList() //we skip this type of content
        }

        //https://github.com/FlowCrypt/flowcrypt-android/issues/2402
        "application/pgp-encrypted" == mimePart.baseContentType() &&
            "multipart/encrypted" == ((mimePart as? MimeBodyPart)?.parent as? MimeMultipart)?.baseContentType()
        -> {
          //we skip part with content type "application/pgp-encrypted" as not informative for a user
          emptyList()
        }

        "text/html" == mimePart.baseContentType() -> listOf(
          RawBlock(
            RawBlockType.HTML_TEXT,
            mimePart.inputStream.readBytes(),
            isOpenPGPMimeSigned
          )
        )

        else -> detectBlocks(mimePart.inputStream.readText()).map {
          it.copy(isOpenPGPMimeSigned = isOpenPGPMimeSigned)
        }
      }
    }
  }

  fun detectBlocks(source: String): Collection<RawBlock> {
    val normalized = source.normalize()
    val blocks = mutableListOf<RawBlock>()
    var startAt = 0
    while (true) {
      val continueAt = detectNextBlock(normalized, startAt, blocks)
      if (startAt >= continueAt) return blocks
      startAt = continueAt
    }
  }

  private fun treatAs(mimePart: MimePart): TreatAs {
    val name = mimePart.getFileNameWithCarefully()?.lowercase() ?: ""
    val baseContentType = mimePart.baseContentType()
    val length = mimePart.size
    when {
      name in HIDDEN_FILE_NAMES -> {
        // PGPexch.htm.pgp is html alternative of textual body content produced
        // by the PGP Desktop and GPG4o
        return TreatAs.HIDDEN
      }

      "" == name && baseContentType?.startsWith("image/") != true -> {
        return if (length < 100) TreatAs.SIGNATURE else TreatAs.PGP_MSG
      }

      "signature.asc" == name || "application/pgp-signature" == baseContentType -> {
        return TreatAs.SIGNATURE
      }

      "msg.asc" == name && length < 100 && "application/pgp-encrypted" == baseContentType -> {
        // mail.ch does this - although it looks like encrypted msg,
        // it will just contain PGP version eg "Version: 1"
        return TreatAs.SIGNATURE
      }

      "Message.pgp" == mimePart.fileName || name in ENCRYPTED_MSG_NAMES -> {
        return TreatAs.PGP_MSG
      }

      ENCRYPTED_FILE_REGEX.containsMatchIn(name) -> {
        // ends with one of .gpg, .pgp, .???.asc, .????.asc
        return TreatAs.ENCRYPTED_FILE
      }

      PRIVATE_KEY_REGEX.containsMatchIn(name) -> {
        return TreatAs.PRIVATE_KEY
      }

      "application/pgp-keys" == baseContentType -> {
        return TreatAs.PUBLIC_KEY
      }

      PUBLIC_KEY_REGEX_1.containsMatchIn(name) -> {
        // name starts with a key id
        return TreatAs.PUBLIC_KEY
      }

      name.contains("public") && PUBLIC_KEY_REGEX_2.containsMatchIn(name) -> {
        // name contains the word "public", any key id and ends with .asc
        return TreatAs.PUBLIC_KEY
      }

      name.endsWith(".asc") && length > 0 && checkForPublicKeyBlock(mimePart.inputStream) -> {
        return TreatAs.PUBLIC_KEY
      }

      name.endsWith(".asc") && length < 100000 && !mimePart.isInline() -> {
        return TreatAs.PGP_MSG
      }

      else -> {
        return TreatAs.PLAIN_FILE
      }
    }
  }

  private fun checkForPublicKeyBlock(stream: InputStream): Boolean {
    val a = ByteArray(100)
    val r = stream.read(a)
    if (r < 1) return false
    val s = String(if (r == a.size) a else a.copyOf(r), StandardCharsets.US_ASCII)
    return s.contains("-----BEGIN PGP PUBLIC KEY BLOCK-----")
  }

  private fun detectNextBlock(text: String, startAt: Int, blocks: MutableList<RawBlock>): Int {
    val initialBlockCount = blocks.size
    var continueAt = -1
    val beginIndex = text.indexOf(
      PgpArmor.ARMOR_HEADER_DICT.getValue(RawBlockType.UNKNOWN).begin, startAt
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
            blocks.add(RawBlock(RawBlockType.PLAIN_TEXT, textBeforeBlock.toByteArray()))
          }
        }

        val end = blockHeaderDef.end
        val endHeaderIndex = text.indexOf(end, beginIndex + blockHeaderDef.begin.length)
        val endHeaderLength = if (endHeaderIndex == -1) 0 else end.length

        if (endHeaderIndex != -1) {
          // identified end of the same block
          continueAt = endHeaderIndex + endHeaderLength
          val content = text.substring(beginIndex, continueAt).trim()
          blocks.add(RawBlock(blockHeaderKvp.key, content.toByteArray()))
        } else {
          // corresponding end not found
          val content = text.substring(beginIndex)
          blocks.add(RawBlock(blockHeaderKvp.key, content.toByteArray()))
        }
        break
      }
    }

    if (text.isNotEmpty() && blocks.size == initialBlockCount) {
      // didn't find any blocks, but input is non-empty
      val remainingText = text.substring(startAt).trim()
      if (remainingText.isNotEmpty()) {
        blocks.add(RawBlock(RawBlockType.PLAIN_TEXT, remainingText.toByteArray()))
      }
    }

    return continueAt
  }

  data class RawBlock(
    val type: RawBlockType,
    val content: ByteArray,
    val isOpenPGPMimeSigned: Boolean = false
  ) {
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as RawBlock

      if (type != other.type) return false
      if (!content.contentEquals(other.content)) return false
      return isOpenPGPMimeSigned == other.isOpenPGPMimeSigned
    }

    override fun hashCode(): Int {
      var result = type.hashCode()
      result = 31 * result + content.contentHashCode()
      result = 31 * result + isOpenPGPMimeSigned.hashCode()
      return result
    }
  }

  enum class RawBlockType {
    UNKNOWN,
    HTML_TEXT,
    PLAIN_TEXT,
    ATTACHMENT,
    PGP_MSG,
    PGP_PUBLIC_KEY,
    PGP_CLEARSIGN_MSG,
    PGP_PRIVATE_KEY,
    CERTIFICATE,
    SIGNATURE,
    INLINE_ATTACHMENT;
  }

  enum class TreatAs {
    HIDDEN,
    PGP_MSG,
    SIGNATURE,
    PUBLIC_KEY,
    PRIVATE_KEY,
    ENCRYPTED_FILE,
    PLAIN_FILE
  }
}
