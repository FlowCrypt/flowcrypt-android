/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.retrofit.response.model.node.AttMeta
import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlockFactory
import com.flowcrypt.email.core.msg.MsgBlockParser
import com.flowcrypt.email.extensions.javax.mail.hasFileName
import com.flowcrypt.email.extensions.javax.mail.isInline
import com.flowcrypt.email.security.pgp.PgpArmor.ARMOR_HEADER_DICT
import org.bouncycastle.bcpg.ArmoredInputStream
import org.bouncycastle.bcpg.PacketTags
import org.bouncycastle.openpgp.PGPDataValidationException
import org.bouncycastle.openpgp.PGPException
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.pgpainless.PGPainless
import org.pgpainless.exception.MessageNotIntegrityProtectedException
import org.pgpainless.exception.ModificationDetectionException
import org.pgpainless.key.info.KeyRingInfo
import org.pgpainless.key.protection.UnprotectedKeysProtector
import org.pgpainless.util.Passphrase
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.*
import javax.mail.Address
import javax.mail.BodyPart
import javax.mail.Message
import javax.mail.Multipart
import javax.mail.Part
import javax.mail.internet.MimeMessage
import kotlin.experimental.and
import kotlin.math.min

object PgpMsg {
  /**
   * @return Pair.first  indicates armored (true) or binary (false) format
   *         Pair.second block type or null if the source is empty
   */
  fun detectBlockType(source: ByteArray): Pair<Boolean, MsgBlock.Type?> {
    if (source.isNotEmpty()) {
      val firstByte = source[0]
      if (firstByte and (0x80.toByte()) == 0x80.toByte()) {
        // 11XX XXXX - potential new pgp packet tag
        val tagNumber = if (firstByte and (0xC0.toByte()) == 0xC0.toByte()) {
          (firstByte and 0x3f).toInt()  // 11TTTTTT where T is tag number bit
        } else { // 10XX XXXX - potential old pgp packet tag
          (firstByte and 0x3c).toInt() ushr (2) // 10TTTTLL where T is tag number bit.
        }
        if (tagNumber <= MAX_TAG_NUMBER) {
          // Indeed a valid OpenPGP packet tag number
          // This does not 100% mean it's OpenPGP message
          // But it's a good indication that it may be
          return Pair(
            false,
            if (messageTypes.contains(tagNumber)) {
              MsgBlock.Type.ENCRYPTED_MSG
            } else {
              MsgBlock.Type.PUBLIC_KEY
            }
          )
        }
      }

      val blocks = MsgBlockParser.detectBlocks(
        // only interested in the first 50 bytes
        // use ASCII, it never fails
        String(source.copyOfRange(0, min(50, source.size)), StandardCharsets.US_ASCII).trim()
      )
      if (blocks.size == 1 && !blocks[0].complete
        && MsgBlock.Type.wellKnownBlockTypes.contains(blocks[0].type)
      ) {
        return Pair(true, blocks[0].type)
      }
      return Pair(false, MsgBlock.Type.UNKNOWN)
    }
    return Pair(false, null)
  }

  private val messageTypes = intArrayOf(
    PacketTags.SYM_ENC_INTEGRITY_PRO,
    PacketTags.MOD_DETECTION_CODE,
    20, // SymEncryptedAEADProtected - no BouncyCastle constant for this one
    PacketTags.SYMMETRIC_KEY_ENC,
    PacketTags.COMPRESSED_DATA
  )

  private const val MAX_TAG_NUMBER = 20

  data class DecryptionError(
    val type: PgpDecrypt.DecryptionErrorType,
    val message: String,
    val cause: Throwable? = null
  )

  data class DecryptionResult(
    // provided if decryption was successful
    val content: ByteArrayOutputStream? = null,

    // true if message was encrypted.
    // Alternatively false (because it could have also been plaintext signed,
    // or wrapped in PGP armor as plaintext packet without encrypting)
    // also false when error happens.
    val isEncrypted: Boolean = false,

    // pgp messages may include original filename in them
    val filename: String? = null,

    // todo later - signature verification not supported on Android yet
    val signature: Any? = null,

    // provided if error happens
    val error: DecryptionError? = null
  ) {
    companion object {
      fun withError(
        type: PgpDecrypt.DecryptionErrorType,
        message: String,
        cause: Throwable? = null
      ): DecryptionResult {
        return DecryptionResult(error = DecryptionError(type, message, cause))
      }

      fun withCleartext(cleartext: ByteArrayOutputStream, signature: Any?): DecryptionResult {
        return DecryptionResult(content = cleartext, signature = signature)
      }

      fun withDecrypted(content: ByteArrayOutputStream, filename: String?): DecryptionResult {
        return DecryptionResult(content = content, isEncrypted = true, filename = filename)
      }
    }
  }

  data class KeyWithPassPhrase(
    val keyRing: PGPSecretKeyRing,
    val passphrase: Passphrase?
  )

  fun decrypt(
    data: ByteArray,
    keys: List<KeyWithPassPhrase>,
    pgpPublicKeyRingCollection: PGPPublicKeyRingCollection? // for verification
  ): DecryptionResult {
    if (data.isEmpty()) {
      return DecryptionResult.withError(
        type = PgpDecrypt.DecryptionErrorType.FORMAT,
        message = "Can't decrypt empty message"
      )
    }

    val chunk = data.copyOfRange(0, data.size.coerceAtMost(50)).toString(StandardCharsets.US_ASCII)
    var input = data.inputStream() as InputStream

    if (chunk.contains(ARMOR_HEADER_DICT[MsgBlock.Type.SIGNED_MSG]!!.begin)) {
      val msg: PgpArmor.CleartextSignedMessage
      try {
        msg = PgpArmor.readSignedClearTextMessage(input)
      } catch (ex: Exception) {
        return if (
          ex is PGPException && ex.message != null && ex.message == "Cleartext format error"
        ) {
          DecryptionResult.withError(
            type = PgpDecrypt.DecryptionErrorType.FORMAT,
            message = ex.message!!,
            cause = ex.cause
          )
        } else {
          DecryptionResult.withError(
            type = PgpDecrypt.DecryptionErrorType.OTHER,
            message = "Decode cleartext error",
            cause = ex
          )
        }
      }
      return DecryptionResult.withCleartext(msg.content, msg.signature)
    }

    val isArmored = chunk.contains(ARMOR_HEADER_DICT[MsgBlock.Type.ENCRYPTED_MSG]!!.begin)
    if (isArmored) input = ArmoredInputStream(input)

    val keyList: List<PGPSecretKeyRing>
    try {
      keyList = keys.map {
        when {
          KeyRingInfo(it.keyRing).isFullyDecrypted -> it.keyRing
          it.passphrase == null -> throw PGPException("flowcrypt: need passphrase")
          else -> PgpKey.decryptKey(it.keyRing, it.passphrase) // may throw PGPException
        }
      }.toList()
    } catch (ex: PGPException) {
      if (ex.message == "flowcrypt: need passphrase") {
        return DecryptionResult.withError(
          type = PgpDecrypt.DecryptionErrorType.NEED_PASSPHRASE,
          message = "Need passphrase"
        )
      }
      return DecryptionResult.withError(
        type = PgpDecrypt.DecryptionErrorType.WRONG_PASSPHRASE,
        message = "Wrong passphrase",
        cause = ex
      )
    }

    val exception: Exception?
    try {
      val d = PGPainless.decryptAndOrVerify()
        .onInputStream(input)
        .decryptWith(UnprotectedKeysProtector(), PGPSecretKeyRingCollection(keyList))

      val decryptionStream = if (pgpPublicKeyRingCollection != null) {
        d.verifyWith(pgpPublicKeyRingCollection).ignoreMissingPublicKeys().build()
      } else {
        d.doNotVerify().build()
      }

      val output = ByteArrayOutputStream()
      decryptionStream.use { it.copyTo(output) }

      val streamResult = decryptionStream.result
      return DecryptionResult.withDecrypted(output, streamResult.fileInfo?.fileName)
    } catch (ex: MessageNotIntegrityProtectedException) {
      return DecryptionResult.withError(
        type = PgpDecrypt.DecryptionErrorType.NO_MDC,
        message = "Security threat! Message is missing integrity checks (MDC)." +
            " The sender should update their outdated software.",
        cause = ex
      )
    } catch (ex: ModificationDetectionException) {
      return DecryptionResult.withError(
        type = PgpDecrypt.DecryptionErrorType.BAD_MDC,
        message = "Security threat! Integrity check failed.",
        cause = ex
      )
    } catch (ex: PGPDataValidationException) {
      return DecryptionResult.withError(
        type = PgpDecrypt.DecryptionErrorType.KEY_MISMATCH,
        message = "There is no matching key",
        cause = ex
      )
    } catch (ex: PGPException) {
      if (
        ex.message?.contains("exception decrypting session info") == true
        || ex.message?.contains("encoded length out of range") == true
        || ex.message?.contains("Exception recovering session info") == true
        || ex.message?.contains("No suitable decryption key") == true
      ) {
        return DecryptionResult.withError(
          type = PgpDecrypt.DecryptionErrorType.KEY_MISMATCH,
          message = "There is no suitable decryption key",
          cause = ex
        )
      } else {
        exception = ex
      }
    } catch (ex: Exception) {
      exception = ex
    }
    return DecryptionResult.withError(
      type = PgpDecrypt.DecryptionErrorType.OTHER,
      message = "Decryption failed",
      cause = exception
    )
  }

  @Suppress("ArrayInDataClass")
  data class MimeContent(
    val attachments: List<BodyPart>,
    var signature: String?,
    val subject: String,
    val html: String?,
    val text: String?,
    val from: Array<Address>?,
    val to: Array<Address>?,
    val cc: Array<Address>?,
    val bcc: Array<Address>?
  )

  @Suppress("ArrayInDataClass")
  data class MimeProcessedMsg(
    val blocks: List<MsgBlock>,
    val from: Array<Address>?,
    val to: Array<Address>?
  )

  // Typescript: public static decode = async (mimeMsg: Uint8Array): Promise<MimeContent>
  // invoke in the test as:
  // val session = Session.getInstance(Properties())
  // val msg = MimeMessage(session, stream)
  fun decodeMimeMessage(msg: MimeMessage): MimeContent {
    var signature: String? = null
    var html: StringBuilder? = null
    var text: StringBuilder? = null
    val attachments = mutableListOf<BodyPart>()

    val stack = ArrayDeque<Part>()
    stack.push(msg)

    while (stack.isNotEmpty()) {
      val part = stack.pop()
      if (part.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
        // multi-part, break down into separate parts
        val multipart = part.content as Multipart
        val list = mutableListOf<Part>()
        val n = multipart.count
        for (i in 0 until n) {
          list.add(multipart.getBodyPart(i))
        }
        for (i in 0 until n) {
          stack.push(list[n - i - 1])
        }
      } else {
        // single part, analyze content type and extract some data
        part as BodyPart
        // println("parse: '${part.contentType}' $contentType '${part.fileName}' ${part.size}")
        when (part.contentType.split(';').first().trim()) {
          "application/pgp-signature" -> {
            signature = String(part.inputStream.readBytes(), StandardCharsets.US_ASCII)
          }

          // this one was not in the Typescript, but I had to add it to pass some tests
          "message/rfc822" -> {
            stack.push(part.content as Part)
          }

          "text/html" -> {
            if (!part.hasFileName()) {
              if (html == null) html = StringBuilder()
              html.append(part.content)
            }
          }

          "text/plain" -> {
            if (!part.hasFileName() || part.isInline()) {
              if (text == null) {
                text = StringBuilder()
              } else {
                text.append("\n\n")
              }
              text.append(part.content)
            }
          }

          "text/rfc822-headers" -> {} // skip

          else -> {
            attachments.add(part)
          }
        }
      }
    }
    return MimeContent(
      attachments = attachments,
      signature = signature,
      subject = msg.subject,
      html = html?.toString(),
      text = text?.toString(),
      from = msg.from,
      to = msg.getRecipients(Message.RecipientType.TO),
      cc = msg.getRecipients(Message.RecipientType.CC),
      bcc = msg.getRecipients(Message.RecipientType.BCC)
    )
  }

  // Typescript:  public static processDecoded = (decoded: MimeContent): MimeProcessedMsg
  fun processDecodedMimeMessage(decoded: MimeContent): MimeProcessedMsg {
    val blocks = analyzeDecodedTextAndHtml(decoded)
    var signature: String? = decoded.signature

    for (att in decoded.attachments) {
      var content = att.content
      // println("att: ${att.contentType} '${att.fileName}' ${att.size} -> $treatedAs")
      when (treatAs(att)) {
        TreatAs.HIDDEN -> {} // ignore

        TreatAs.ENCRYPTED_MSG -> {
          if (content is InputStream) {
            content = String(content.readBytes(), StandardCharsets.US_ASCII)
          }
          if (content is String) {
            val armored = PgpArmor.clip(content)
            if (armored != null) {
              blocks.add(MsgBlockFactory.fromContent(MsgBlock.Type.ENCRYPTED_MSG, armored))
            }
          }
        }

        TreatAs.SIGNATURE -> {
          if (content is InputStream) {
            content = String(content.readBytes(), StandardCharsets.US_ASCII)
          }
          if (content is String && signature == null) {
            signature = content
          }
        }

        TreatAs.PUBLIC_KEY -> {
          if (content is InputStream) {
            content = String(content.readBytes(), StandardCharsets.US_ASCII)
          }
          if (content is String) {
            blocks.addAll(MsgBlockParser.detectBlocks(content))
          }
        }

        TreatAs.PRIVATE_KEY -> {
          if (content is InputStream) {
            content = String(content.readBytes(), StandardCharsets.US_ASCII)
          }
          if (content is String) {
            blocks.addAll(MsgBlockParser.detectBlocks(content))
          }
        }

        TreatAs.ENCRYPTED_FILE -> {
          if (content is String) {
            blocks.add(
              MsgBlockFactory.fromAttachment(
                MsgBlock.Type.ENCRYPTED_ATT,
                null,
                AttMeta(att.fileName, content, att.size.toLong(), att.contentType)
              )
            )
          }
        }

        TreatAs.PLAIN_FILE -> {
          if (content is String) {
            blocks.add(
              MsgBlockFactory.fromAttachment(
                MsgBlock.Type.PLAIN_ATT,
                null,
                AttMeta(att.fileName, content, att.size.toLong(), att.contentType)
              )
            )
          }
        }
      }
    }

    if (signature != null) {
      fixSignedBlocks(blocks, signature)
    }

    return MimeProcessedMsg(blocks, decoded.from, decoded.to)
  }

  private fun analyzeDecodedTextAndHtml(decoded: MimeContent): MutableList<MsgBlock> {
    val blocks = mutableListOf<MsgBlock>()
    if (decoded.text != null) {
      val blocksFromTextPart = MsgBlockParser.detectBlocks(decoded.text)
      val suitableBlock = blocksFromTextPart.firstOrNull {
        it.type in MsgBlock.Type.wellKnownBlockTypes
      }
      when {
        suitableBlock != null -> {
          // if there are some encryption-related blocks found in the text section,
          // which we can use, and not look at the html section, because the html most likely
          // contains the same thing, just harder to parse pgp sections cause it's html
          blocks.addAll(blocksFromTextPart)
        }
        decoded.html != null -> {
          // if no pgp blocks found in text part and there is html part, prefer html
          blocks.add(MsgBlockFactory.fromContent(MsgBlock.Type.PLAIN_HTML, decoded.html))
        }
        else -> {
          // else if no html and just a plain text message, use that
          blocks.addAll(blocksFromTextPart)
        }
      }
    } else if (decoded.html != null) {
      blocks.add(MsgBlockFactory.fromContent(MsgBlock.Type.PLAIN_HTML, decoded.html))
    }
    return blocks
  }

  private fun fixSignedBlocks(blocks: MutableList<MsgBlock>, signature: String) {
    for (i in 0 until blocks.size) {
      val block = blocks[i]
      when (block.type) {
        MsgBlock.Type.PLAIN_TEXT -> {
          blocks[i] = MsgBlockFactory.fromContent(
            type = MsgBlock.Type.SIGNED_TEXT,
            content = block.content,
            missingEnd = !block.complete,
            signature = signature
          )
        }
        MsgBlock.Type.PLAIN_HTML -> {
          blocks[i] = MsgBlockFactory.fromContent(
            type = MsgBlock.Type.SIGNED_HTML,
            content = block.content,
            missingEnd = !block.complete,
            signature = signature
          )
        }
        else -> {}
      }
    }
  }

  private enum class TreatAs {
    HIDDEN,
    ENCRYPTED_MSG,
    SIGNATURE,
    PUBLIC_KEY,
    PRIVATE_KEY,
    ENCRYPTED_FILE,
    PLAIN_FILE
  }

  private fun treatAs(att: BodyPart): TreatAs {
    val name = att.fileName ?: ""
    val type = att.contentType
    val length = att.size
    if (hiddenFileNames.contains(name)) {
      // PGPexch.htm.pgp is html alternative of textual body content produced
      // by the PGP Desktop and GPG4o
      return TreatAs.HIDDEN
    } else if (name == "signature.asc" || type == "application/pgp-signature") {
      return TreatAs.SIGNATURE
    } else if (name == "" && !type.startsWith("image/")) {
      return if (length < 100)  TreatAs.SIGNATURE else TreatAs.ENCRYPTED_MSG
    } else if (name == "msg.asc" && length < 100 && type == "application/pgp-encrypted") {
      // mail.ch does this - although it looks like encrypted msg,
      // it will just contain PGP version eg "Version: 1"
      return TreatAs.SIGNATURE
    } else if (encryptedMsgNames.contains(name)) {
      return TreatAs.ENCRYPTED_MSG
    } else if (encryptedFileRegex.containsMatchIn(name)) {
      // ends with one of .gpg, .pgp, .???.asc, .????.asc
      return TreatAs.ENCRYPTED_FILE
    } else if (privateKeyRegex.containsMatchIn(name)) {
      return TreatAs.PRIVATE_KEY
    } else if (type == "application/pgp-keys") {
      return TreatAs.PUBLIC_KEY
    } else if (publicKeyRegex1.containsMatchIn(name)) {
      // name starts with a key id
      return TreatAs.PUBLIC_KEY
    } else if (
      name.toLowerCase(Locale.ROOT).contains("public") &&
      publicKeyRegex2.containsMatchIn(name)
    ) {
      // name contains the word "public", any key id and ends with .asc
      return TreatAs.PUBLIC_KEY
    } else if (name.endsWith(".asc") && length > 0 && checkForPublicKeyBlock(att.inputStream)) {
      return TreatAs.PUBLIC_KEY
    } else if (name.endsWith(".asc") && length < 100000 && !att.isInline()) {
      return TreatAs.ENCRYPTED_MSG
    } else {
      return TreatAs.PLAIN_FILE
    }
  }

  @JvmStatic
  private fun checkForPublicKeyBlock(stream: InputStream): Boolean {
    val a = ByteArray(100)
    val r = stream.read(a)
    if (r < 1) return false
    val s = String(if (r == a.size) a else a.copyOf(r), StandardCharsets.US_ASCII)
    return s.contains("-----BEGIN PGP PUBLIC KEY BLOCK-----")
  }

  private val hiddenFileNames = setOf(
    "PGPexch.htm.pgp",
    "PGPMIME version identification",
    "Version.txt",
    "PGPMIME Versions Identification"
  )

  private val encryptedMsgNames = setOf(
    "message", "msg.asc", "message.asc", "encrypted.asc", "encrypted.eml.pgp",
    "Message.pgp", "openpgp-encrypted-message.asc"
  )

  private val encryptedFileRegex = Regex("(\\.pgp\$)|(\\.gpg\$)|(\\.[a-zA-Z0-9]{3,4}\\.asc\$)")
  private val privateKeyRegex = Regex("(cryptup|flowcrypt)-backup-[a-z0-9]+\\.(key|asc)\$")
  private val publicKeyRegex1 = Regex("^(0|0x)?[A-F0-9]{8}([A-F0-9]{8})?.*\\.asc\$")
  private val publicKeyRegex2 = Regex("[A-F0-9]{8}.*\\.asc\$")
}
