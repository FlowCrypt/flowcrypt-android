/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 */

package com.flowcrypt.email.security.pgp

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.retrofit.response.model.node.AttMeta
import com.flowcrypt.email.api.retrofit.response.model.node.AttMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.DecryptErrorMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.EncryptedAttLinkMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.EncryptedAttMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlockFactory
import com.flowcrypt.email.api.retrofit.response.model.node.PublicKeyMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.SignedMsgBlock
import com.flowcrypt.email.core.msg.MimeUtils
import com.flowcrypt.email.core.msg.MsgBlockParser
import com.flowcrypt.email.extensions.java.io.readText
import com.flowcrypt.email.extensions.javax.mail.internet.hasFileName
import com.flowcrypt.email.extensions.javax.mail.isInline
import com.flowcrypt.email.extensions.kotlin.decodeFcHtmlAttr
import com.flowcrypt.email.extensions.kotlin.escapeHtmlAttr
import com.flowcrypt.email.extensions.kotlin.stripHtmlRootTags
import com.flowcrypt.email.extensions.kotlin.toEscapedHtml
import com.flowcrypt.email.extensions.kotlin.unescapeHtml
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.armor
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.toPgpKeyDetails
import com.flowcrypt.email.extensions.org.owasp.html.allowAttributesOnElementsExt
import com.flowcrypt.email.security.pgp.PgpArmor.ARMOR_HEADER_DICT
import org.bouncycastle.bcpg.ArmoredInputStream
import org.bouncycastle.bcpg.PacketTags
import org.bouncycastle.openpgp.PGPDataValidationException
import org.bouncycastle.openpgp.PGPException
import org.bouncycastle.openpgp.PGPKeyRing
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.json.JSONObject
import org.jsoup.Jsoup
import org.owasp.html.HtmlPolicyBuilder
import org.pgpainless.PGPainless
import org.pgpainless.decryption_verification.ConsumerOptions
import org.pgpainless.exception.MessageNotIntegrityProtectedException
import org.pgpainless.exception.ModificationDetectionException
import org.pgpainless.key.info.KeyRingInfo
import org.pgpainless.key.protection.UnprotectedKeysProtector
import org.pgpainless.util.Passphrase
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.Locale
import javax.mail.Address
import javax.mail.Message
import javax.mail.Multipart
import javax.mail.Part
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimePart
import kotlin.experimental.and
import kotlin.math.min
import kotlin.random.Random

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
            if (MESSAGE_TYPES.contains(tagNumber)) {
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
      ).blocks
      if (blocks.size == 1 && !blocks[0].complete
        && MsgBlock.Type.WELL_KNOWN_BLOCK_TYPES.contains(blocks[0].type)
      ) {
        return Pair(true, blocks[0].type)
      }
      return Pair(false, MsgBlock.Type.UNKNOWN)
    }
    return Pair(false, null)
  }

  private val MESSAGE_TYPES = intArrayOf(
    PacketTags.SYM_ENC_INTEGRITY_PRO,
    PacketTags.MOD_DETECTION_CODE,
    20, // SymEncryptedAEADProtected - no BouncyCastle constant for this one
    PacketTags.SYMMETRIC_KEY_ENC,
    PacketTags.COMPRESSED_DATA
  )

  private const val MAX_TAG_NUMBER = 20

  data class DecryptionError(
    val type: PgpDecrypt.DecryptionErrorType,
    val message: String? = null
  ) : Parcelable {
    constructor(parcel: Parcel) : this(
      parcel.readParcelable<PgpDecrypt.DecryptionErrorType>(
        PgpDecrypt.DecryptionErrorType::class.java.classLoader
      )!!,
      parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
      parcel.writeParcelable(type, flags)
      parcel.writeString(message)
    }

    override fun describeContents(): Int {
      return 0
    }

    companion object CREATOR : Parcelable.Creator<DecryptionError> {
      override fun createFromParcel(parcel: Parcel) = DecryptionError(parcel)
      override fun newArray(size: Int): Array<DecryptionError?> = arrayOfNulls(size)
    }
  }

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
    val signature: String? = null,

    // provided if error happens
    val error: DecryptionError? = null
  ) {
    companion object {
      fun withError(
        type: PgpDecrypt.DecryptionErrorType,
        message: String
      ): DecryptionResult {
        return DecryptionResult(error = DecryptionError(type, message))
      }

      fun withCleartext(cleartext: ByteArrayOutputStream, signature: String?): DecryptionResult {
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
    data: ByteArray?,
    keys: List<KeyWithPassPhrase>,
    pgpPublicKeyRingCollection: PGPPublicKeyRingCollection? = null // for verification
  ): DecryptionResult {
    if (data == null || data.isEmpty()) {
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
            message = ex.message!!
          )
        } else {
          DecryptionResult.withError(
            type = PgpDecrypt.DecryptionErrorType.OTHER,
            message = "Decode cleartext error"
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
        message = "Wrong passphrase"
      )
    }

    val exception: Exception?
    try {
      val consumerOptions = ConsumerOptions()
        .addDecryptionKeys(PGPSecretKeyRingCollection(keyList), UnprotectedKeysProtector())
      pgpPublicKeyRingCollection?.let { consumerOptions.addVerificationCerts(it) }

      val decryptionStream = PGPainless.decryptAndOrVerify()
        .onInputStream(input)
        .withOptions(consumerOptions)

      val output = ByteArrayOutputStream()
      decryptionStream.use { it.copyTo(output) }

      val streamResult = decryptionStream.result
      return DecryptionResult.withDecrypted(output, streamResult.fileInfo?.fileName)
    } catch (ex: MessageNotIntegrityProtectedException) {
      return DecryptionResult.withError(
        type = PgpDecrypt.DecryptionErrorType.NO_MDC,
        message = "Security threat! Message is missing integrity checks (MDC)." +
            " The sender should update their outdated software."
      )
    } catch (ex: ModificationDetectionException) {
      return DecryptionResult.withError(
        type = PgpDecrypt.DecryptionErrorType.BAD_MDC,
        message = "Security threat! Integrity check failed."
      )
    } catch (ex: PGPDataValidationException) {
      return DecryptionResult.withError(
        type = PgpDecrypt.DecryptionErrorType.KEY_MISMATCH,
        message = "There is no matching key"
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
          message = "There is no suitable decryption key"
        )
      }
    }
    return DecryptionResult.withError(
      type = PgpDecrypt.DecryptionErrorType.OTHER,
      message = "Decryption failed"
    )
  }

  @Suppress("ArrayInDataClass")
  data class MimeContent(
    val attachments: List<MimePart>,
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
    val from: Array<Address>? = null,
    val to: Array<Address>? = null,
    val subject: String? = null
  )

  // Typescript: public static decode = async (mimeMsg: Uint8Array): Promise<MimeContent>
  // invoke in the test as:
  // val session = Session.getInstance(Properties())
  // val msg = MimeMessage(session, stream)
  fun decodeMimeMessage(msg: MimeMessage): MimeContent {
    var signature: String? = null
    var html: StringBuilder? = null
    var text: StringBuilder? = null
    val attachments = mutableListOf<MimePart>()

    val stack = ArrayDeque<Part>()
    stack.addFirst(msg)

    while (stack.isNotEmpty()) {
      val part = stack.removeFirst()
      if (part.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
        // multi-part, break down into separate parts
        val multipart = part.content as Multipart
        val list = mutableListOf<Part>()
        val n = multipart.count
        for (i in 0 until n) {
          list.add(multipart.getBodyPart(i))
        }
        for (i in 0 until n) {
          stack.addFirst(list[n - i - 1])
        }
      } else {
        // single part, analyze content type and extract some data
        part as MimePart
        // println("parse: '${part.contentType}' $contentType '${part.fileName}' ${part.size}")
        when (part.contentType.split(';').first().trim()) {
          "application/pgp-signature" -> {
            signature = String(part.inputStream.readBytes(), StandardCharsets.US_ASCII)
          }

          // this one was not in the Typescript, but I had to add it to pass some tests
          "message/rfc822" -> {
            stack.addFirst(part.content as Part)
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

          "text/rfc822-headers" -> {
          } // skip

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
        TreatAs.HIDDEN -> {
        } // ignore

        TreatAs.ENCRYPTED_MSG -> {
          if (content is InputStream) content = content.readText(StandardCharsets.US_ASCII)
          if (content is String) {
            val armored = PgpArmor.clip(content)
            if (armored != null) {
              blocks.add(MsgBlockFactory.fromContent(MsgBlock.Type.ENCRYPTED_MSG, armored))
            }
          }
        }

        TreatAs.SIGNATURE -> {
          if (content is InputStream) content = content.readText(StandardCharsets.US_ASCII)
          if (content is String && signature == null) signature = content
        }

        TreatAs.PUBLIC_KEY -> {
          if (content is InputStream) content = content.readText(StandardCharsets.US_ASCII)
          if (content is String) blocks.addAll(MsgBlockParser.detectBlocks(content).blocks)
        }

        TreatAs.PRIVATE_KEY -> {
          if (content is InputStream) content = content.readText(StandardCharsets.US_ASCII)
          if (content is String) blocks.addAll(MsgBlockParser.detectBlocks(content).blocks)
        }

        TreatAs.ENCRYPTED_FILE -> {
          blocks.add(MsgBlockFactory.fromAttachment(MsgBlock.Type.ENCRYPTED_ATT, att))
        }

        TreatAs.PLAIN_FILE -> {
          blocks.add(MsgBlockFactory.fromAttachment(MsgBlock.Type.PLAIN_ATT, att))
        }
      }
    }

    if (signature != null) fixSignedBlocks(blocks, signature)

    return MimeProcessedMsg(blocks, decoded.from, decoded.to, decoded.subject)
  }

  private fun analyzeDecodedTextAndHtml(decoded: MimeContent): MutableList<MsgBlock> {
    val blocks = mutableListOf<MsgBlock>()
    if (decoded.text != null) {
      val blocksFromTextPart = MsgBlockParser.detectBlocks(decoded.text).blocks
      val suitableBlock = blocksFromTextPart.firstOrNull {
        it.type in MsgBlock.Type.WELL_KNOWN_BLOCK_TYPES
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
        else -> {
        }
      }
    }
  }

  enum class TreatAs {
    HIDDEN,
    ENCRYPTED_MSG,
    SIGNATURE,
    PUBLIC_KEY,
    PRIVATE_KEY,
    ENCRYPTED_FILE,
    PLAIN_FILE
  }

  fun treatAs(att: MimePart): TreatAs {
    val name = att.fileName ?: ""
    val type = att.contentType
    val length = att.size
    if (HIDDEN_FILE_NAMES.contains(name)) {
      // PGPexch.htm.pgp is html alternative of textual body content produced
      // by the PGP Desktop and GPG4o
      return TreatAs.HIDDEN
    } else if (name == "signature.asc" || type == "application/pgp-signature") {
      return TreatAs.SIGNATURE
    } else if (name == "" && !type.startsWith("image/")) {
      return if (length < 100) TreatAs.SIGNATURE else TreatAs.ENCRYPTED_MSG
    } else if (name == "msg.asc" && length < 100 && type == "application/pgp-encrypted") {
      // mail.ch does this - although it looks like encrypted msg,
      // it will just contain PGP version eg "Version: 1"
      return TreatAs.SIGNATURE
    } else if (ENCRYPTED_MSG_NAMES.contains(name)) {
      return TreatAs.ENCRYPTED_MSG
    } else if (ENCRYPTED_FILE_REGEX.containsMatchIn(name)) {
      // ends with one of .gpg, .pgp, .???.asc, .????.asc
      return TreatAs.ENCRYPTED_FILE
    } else if (PRIVATE_KEY_REGEX.containsMatchIn(name)) {
      return TreatAs.PRIVATE_KEY
    } else if (type == "application/pgp-keys") {
      return TreatAs.PUBLIC_KEY
    } else if (PUBLIC_KEY_REGEX_1.containsMatchIn(name)) {
      // name starts with a key id
      return TreatAs.PUBLIC_KEY
    } else if (
      name.toLowerCase(Locale.ROOT).contains("public") &&
      PUBLIC_KEY_REGEX_2.containsMatchIn(name)
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

  private val HIDDEN_FILE_NAMES = setOf(
    "PGPexch.htm.pgp",
    "PGPMIME version identification",
    "Version.txt",
    "PGPMIME Versions Identification"
  )

  private val ENCRYPTED_MSG_NAMES = setOf(
    "message", "msg.asc", "message.asc", "encrypted.asc", "encrypted.eml.pgp",
    "Message.pgp", "openpgp-encrypted-message.asc"
  )

  private val ENCRYPTED_FILE_REGEX = Regex("(\\.pgp\$)|(\\.gpg\$)|(\\.[a-zA-Z0-9]{3,4}\\.asc\$)")
  private val PRIVATE_KEY_REGEX = Regex("(cryptup|flowcrypt)-backup-[a-z0-9]+\\.(key|asc)\$")
  private val PUBLIC_KEY_REGEX_1 = Regex("^(0|0x)?[A-F0-9]{8}([A-F0-9]{8})?.*\\.asc\$")
  private val PUBLIC_KEY_REGEX_2 = Regex("[A-F0-9]{8}.*\\.asc\$")
  private val PUBLIC_KEY_REGEX_3 = Regex("^(0x)?[A-Fa-f0-9]{16,40}\\.asc\\.pgp$")

  data class ParseDecryptResult(
    val subject: String?,
    val isReplyEncrypted: Boolean,
    val text: String,
    val blocks: List<MsgBlock>
  )

  fun parseDecryptMsg(
    content: String,
    isEmail: Boolean,
    keys: List<KeyWithPassPhrase>
  ): ParseDecryptResult {
    return if (isEmail) {
      parseDecryptMsg(MimeUtils.mimeTextToMimeMessage(content), keys)
    } else {
      val blocks = listOf(MsgBlockFactory.fromContent(MsgBlock.Type.ENCRYPTED_MSG, content))
      parseDecryptProcessedMsg(MimeProcessedMsg(blocks), keys)
    }
  }

  fun parseDecryptMsg(msg: MimeMessage, keys: List<KeyWithPassPhrase>): ParseDecryptResult {
    val decoded = decodeMimeMessage(msg)
    val processed = processDecodedMimeMessage(decoded)
    return parseDecryptProcessedMsg(processed, keys)
  }

  private fun parseDecryptProcessedMsg(
    msg: MimeProcessedMsg,
    keys: List<KeyWithPassPhrase>
  ): ParseDecryptResult {
    var subject = msg.subject
    val sequentialProcessedBlocks = mutableListOf<MsgBlock>()
    for (rawBlock in msg.blocks) {
      if (
        (rawBlock.type == MsgBlock.Type.SIGNED_MSG || rawBlock.type == MsgBlock.Type.SIGNED_HTML)
        && (rawBlock as SignedMsgBlock).signature != null
      ) {
        when (rawBlock.type) {
          MsgBlock.Type.SIGNED_MSG -> {
            // skip verification for now
            sequentialProcessedBlocks.add(
              MsgBlockFactory.fromContent(
                type = MsgBlock.Type.VERIFIED_MSG,
                content = rawBlock.content?.toEscapedHtml(),
                signature = rawBlock.signature
              )
            )
          }

          MsgBlock.Type.SIGNED_HTML -> {
            // skip verification for now
            sequentialProcessedBlocks.add(
              MsgBlockFactory.fromContent(
                type = MsgBlock.Type.VERIFIED_MSG,
                content = sanitizeHtmlKeepBasicTags(rawBlock.content),
                signature = rawBlock.signature
              )
            )
          }

          else -> {
          } // make IntelliJ happy
        } // when
      } else if (
        rawBlock.type == MsgBlock.Type.SIGNED_MSG || rawBlock.type == MsgBlock.Type.ENCRYPTED_MSG
      ) {
        val decryptionResult = decrypt(rawBlock.content?.toByteArray(), keys)
        if (decryptionResult.error == null) {
          if (decryptionResult.isEncrypted) {
            val decrypted = decryptionResult.content?.toByteArray()
            val formatted = MsgBlockParser.fmtDecryptedAsSanitizedHtmlBlocks(decrypted)
            if (subject == null) subject = formatted.subject
            sequentialProcessedBlocks.addAll(formatted.blocks)
          } else {
            // ------------------------------------------------------------------------------------
            // Comment from TS code:
            // ------------------------------------------------------------------------------------
            // treating as text, converting to html - what about plain signed html?
            // This could produce html tags although hopefully, that would, typically, result in
            // the `(rawBlock.type === 'signedMsg' || rawBlock.type === 'signedHtml')` block above
            // the only time I can imagine it screwing up down here is if it was a signed-only
            // message that was actually fully armored (text not visible) with a mime msg inside
            // ... -> in which case the user would I think see full mime content?
            // ------------------------------------------------------------------------------------
            sequentialProcessedBlocks.add(
              MsgBlockFactory.fromContent(
                type = MsgBlock.Type.VERIFIED_MSG,
                content = decryptionResult.content?.toString("UTF-8")?.toEscapedHtml(),
                signature = decryptionResult.signature
              )
            )
          }
        } else {
          sequentialProcessedBlocks.add(
            DecryptErrorMsgBlock(
              content = null,
              complete = true,
              error = null,
              kotlinError = decryptionResult.error
            )
          )
        }
      } else if (
        rawBlock.type == MsgBlock.Type.ENCRYPTED_ATT
        && (rawBlock as EncryptedAttMsgBlock).attMeta.name != null
        && PUBLIC_KEY_REGEX_3.matches(rawBlock.attMeta.name!!)
      ) {
        // encrypted public key attached
        val decryptionResult = decrypt(
          data = rawBlock.content?.toByteArray(StandardCharsets.UTF_8),
          keys = keys
        )
        if (decryptionResult.content != null) {
          val content = decryptionResult.content.toString("UTF-8")
          sequentialProcessedBlocks.add(
            MsgBlockFactory.fromContent(MsgBlock.Type.PUBLIC_KEY, content)
          )
        } else {
          // will show as encrypted attachment
          sequentialProcessedBlocks.add(rawBlock)
        }
      } else {
        sequentialProcessedBlocks.add(rawBlock)
      }
    }

    var isReplyEncrypted = false
    val contentBlocks = mutableListOf<MsgBlock>()
    val resultBlocks = mutableListOf<MsgBlock>()

    for (block in sequentialProcessedBlocks) {
      // We don't need Base64 correction here, fromAttachment() does this for us
      // We also seem to don't need to make correction between raw and utf8
      // But I'd prefer MsgBlock.content to be ByteArray
      // So, at least meanwhile, not porting this:
      // block.content = isContentBlock(block.type)
      //     ? block.content.toUtfStr() : block.content.toRawBytesStr();

      if (
        block.type == MsgBlock.Type.DECRYPTED_HTML
        || block.type == MsgBlock.Type.DECRYPTED_TEXT
        || block.type == MsgBlock.Type.DECRYPTED_ATT
      ) {
        isReplyEncrypted = true
      }

      if (block.type == MsgBlock.Type.PUBLIC_KEY) {
        var keyRings: List<PGPKeyRing>? = null
        try {
          keyRings = PgpKey.parseAndNormalizeKeyRings(block.content!!)
        } catch (ex: Exception) {
          ex.printStackTrace()
        }
        if (keyRings != null && keyRings.isNotEmpty()) {
          resultBlocks.addAll(
            keyRings.map { PublicKeyMsgBlock(it.armor(null), true, it.toPgpKeyDetails()) }
          )
        } else {
          resultBlocks.add(
            DecryptErrorMsgBlock(
              block.content,
              true,
              null,
              DecryptionError(PgpDecrypt.DecryptionErrorType.FORMAT, "Badly formatted public key")
            )
          )
        }
      } else if (block.type.isContentBlockType() || MimeUtils.isPlainImgAtt(block)) {
        contentBlocks.add(block)
      } else if (block.type != MsgBlock.Type.PLAIN_ATT) {
        resultBlocks.add(block)
      }
    }

    val fmtRes = fmtContentBlock(contentBlocks)
    resultBlocks.add(0, fmtRes.contentBlock)

    return ParseDecryptResult(
      subject = subject,
      isReplyEncrypted = isReplyEncrypted,
      text = fmtRes.text,
      blocks = resultBlocks
    )
  }

  private data class FormatContentBlockResult(
    val text: String,
    val contentBlock: MsgBlock
  )

  private fun fmtContentBlock(allContentBlocks: List<MsgBlock>): FormatContentBlockResult {
    val inlineImagesByCid = mutableMapOf<String, MsgBlock>()
    val imagesAtTheBottom = mutableListOf<MsgBlock>()
    for (plainImageBlock in allContentBlocks.filter { MimeUtils.isPlainImgAtt(it) }) {
      var contentId = (plainImageBlock as AttMsgBlock).attMeta.contentId ?: ""
      if (contentId.isNotEmpty()) {
        contentId =
          contentId.replace(CID_CORRECTION_REGEX_1, "").replace(CID_CORRECTION_REGEX_2, "")
        inlineImagesByCid[contentId] = plainImageBlock
      } else {
        imagesAtTheBottom.add(plainImageBlock)
      }
    }

    val msgContentAsHtml = StringBuilder()
    val msgContentAsText = StringBuilder()
    for (block in allContentBlocks.filterNot { MimeUtils.isPlainImgAtt(it) }) {
      if (block.content != null) {
        when (block.type) {
          MsgBlock.Type.DECRYPTED_TEXT -> {
            val html = fmtMsgContentBlockAsHtml(block.content?.toEscapedHtml(), FrameColor.GREEN)
            msgContentAsHtml.append(html)
            msgContentAsText.append(block.content ?: "").append('\n')
          }

          MsgBlock.Type.DECRYPTED_HTML -> {
            // Typescript comment: todo: add support for inline imgs? when included using cid
            var html = block.content!!.stripHtmlRootTags()
            html = fmtMsgContentBlockAsHtml(html, FrameColor.GREEN)
            msgContentAsHtml.append(html)
            msgContentAsText
              .append(sanitizeHtmlStripAllTags(block.content)?.unescapeHtml())
              .append('\n')
          }

          MsgBlock.Type.PLAIN_TEXT -> {
            val html =
              fmtMsgContentBlockAsHtml(block.content.toString().toEscapedHtml(), FrameColor.PLAIN)
            msgContentAsHtml.append(html)
            msgContentAsText.append(block.content).append('\n')
          }

          MsgBlock.Type.PLAIN_HTML -> {
            val stripped = block.content!!.stripHtmlRootTags()
            val dirtyHtmlWithImgs = fillInlineHtmlImages(stripped, inlineImagesByCid)
            msgContentAsHtml.append(fmtMsgContentBlockAsHtml(dirtyHtmlWithImgs, FrameColor.PLAIN))
            val text = sanitizeHtmlStripAllTags(dirtyHtmlWithImgs)?.unescapeHtml()
            msgContentAsText.append(text).append('\n')
          }

          MsgBlock.Type.VERIFIED_MSG -> {
            msgContentAsHtml.append(fmtMsgContentBlockAsHtml(block.content, FrameColor.GRAY))
            msgContentAsText.append(sanitizeHtmlStripAllTags(block.content)).append('\n')
          }

          else -> {
            msgContentAsHtml.append(fmtMsgContentBlockAsHtml(block.content, FrameColor.PLAIN))
            msgContentAsText.append(block.content).append('\n')
          }
        }
      }
    }

    imagesAtTheBottom.addAll(inlineImagesByCid.values)
    for (inlineImg in imagesAtTheBottom) {
      inlineImg as AttMsgBlock
      val imageName = inlineImg.attMeta.name ?: "(unnamed image)"
      val imageLengthKb = inlineImg.attMeta.length / 1024
      val alt = "$imageName - $imageLengthKb Kb"
      val inlineImgTag = "<img src=\"data:${inlineImg.attMeta.type ?: ""};base64," +
          "${inlineImg.attMeta.data ?: ""}\" alt=\"${alt.escapeHtmlAttr()}\" />"
      msgContentAsHtml.append(fmtMsgContentBlockAsHtml(inlineImgTag, FrameColor.PLAIN))
      msgContentAsText.append("[image: ${alt}]\n")
    }

    return FormatContentBlockResult(
      text = msgContentAsText.toString().trim(),
      contentBlock = MsgBlockFactory.fromContent(
        type = MsgBlock.Type.PLAIN_HTML,
        """<!DOCTYPE html><html>
  <head>
    <meta name="viewport" content="width=device-width" />
    <style>
      body { word-wrap: break-word; word-break: break-word; hyphens: auto; margin-left: 0px; padding-left: 0px; }
      body img { display: inline !important; height: auto !important; max-width: 95% !important; }
      body pre { white-space: pre-wrap !important; }
      body > div.MsgBlock > table { zoom: 75% } /* table layouts tend to overflow - eg emails from fb */
    </style>
  </head>
  <body>$msgContentAsHtml</body>
</html>"""
      )
    )
  }

  private val CID_CORRECTION_REGEX_1 = Regex(">$")
  private val CID_CORRECTION_REGEX_2 = Regex("^<")

  /**
   * replace content of images: <img src="cid:16c7a8c3c6a8d4ab1e01">
   */
  private fun fillInlineHtmlImages(
    htmlContent: String,
    inlineImagesByCid: MutableMap<String, MsgBlock>
  ): String {
    val usedCids = mutableSetOf<String>()
    val result = StringBuilder()
    var startPos = 0
    while (true) {
      val match = IMG_SRC_WITH_CID_REGEX.find(htmlContent, startPos)
      if (match == null) {
        result.append(htmlContent.substring(startPos, htmlContent.length))
        break
      }
      if (match.range.first > startPos) {
        result.append(htmlContent.substring(startPos, match.range.first))
      }
      val cid = match.groupValues[0]
      val img = inlineImagesByCid[cid]
      if (img != null) {
        img as AttMsgBlock
        // Typescript comment:
        // in current usage, as used by `endpoints.ts`: `block.attMeta!.data`
        // actually contains base64 encoded data, not Uint8Array as the type claims
        result.append("src=\"data:${img.attMeta.type ?: ""};base64,${img.attMeta.data ?: ""}\"")
        // Typescript comment:
        // Delete to find out if any imgs were unused. Later we can add the unused ones
        // at the bottom (though as implemented will cause issues if the same cid is reused
        // in several places in html - which is theoretically valid - only first will get replaced)
        // Kotlin:
        // Collect used CIDs and delete later
        usedCids.add(cid)
      } else {
        result.append(htmlContent.substring(match.range))
      }
      startPos = match.range.last + 1
    }
    for (cid in usedCids) {
      inlineImagesByCid.remove(cid)
    }
    return result.toString()
  }

  private val IMG_SRC_WITH_CID_REGEX = Regex("src=\"cid:([^\"]+)\"")

  private enum class FrameColor {
    GREEN,
    GRAY,
    RED,
    PLAIN
  }

  private const val GENERAL_CSS =
    "background: white;padding-left: 8px;min-height: 50px;padding-top: 4px;" +
        "padding-bottom: 4px;width: 100%;"

  private const val SEAMLESS_LOCK_BG = "iVBORw0KGgoAAAANSUhEUgAAAFoAAABaCAMAAAAPdrEwAAAAh1BMVEXw" +
      "8PD////w8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PD" +
      "w8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8P" +
      "Dw8PD7MuHIAAAALXRSTlMAAAECBAcICw4QEhUZIyYqMTtGTV5kdn2Ii5mfoKOqrbG0uL6/xcnM0NTX2t1l7cN4A" +
      "AAB0UlEQVR4Ae3Y3Y4SQRCG4bdHweFHRBTBH1FRFLXv//qsA8kmvbMdXhh2Q0KfknpSCQc130c67s22+e9+v/+d" +
      "84fxkSPH0m/+5P9vN7vRV0vPfx7or1NB23e99KAHuoXOOc6moQsBwNN1Q9g4Wdh1uq3MA7Qn0+2ylAt7WbWpyT+" +
      "Wo8roKH6v2QhZ2ghZ2ghZ2ghZ2ghZ2ghZ2ghZ2ghZ2ghZ2ghZ2ghZ2ghZ2ghZ2gjZ2AUNOLmwgQdogEJ2dnF3UJ" +
      "dU3WjqO/u96aYtVd/7jqvIyu76G5se6GaY7tNNcy5d7se7eWVnDz87fMkuVuS8epF6f9NPObPY5re9y4N1/vya9" +
      "Gr3se2bfvl9M0mkyZdv077p+a/3z4Meby5Br4NWiV51BaiUqfLro9I3WiR61RVcffwfXI7u5zZ20EOA82Uu8x3S" +
      "lrSwXQuBSvSqK0AletUVoBK96gpIwlZy0MJWctDCVnLQwlZy0MJWctDCVnLQwlZy0MJWctDCVnLQwlZy0MJWctD" +
      "CVnLQwlZy0MJWckIletUVIJJxITN6wtZd2EI+0NquyIJOnUpFVvRpcwmV6FVXgEr0qitAJXrVFaASveoKUIledQ" +
      "WoRK+6AlSiV13BP+/VVbky7Xq1AAAAAElFTkSuQmCC"

  private val FRAME_CSS_MAP = mapOf(
    FrameColor.GREEN to "border: 1px solid #f0f0f0;border-left: 8px solid #31A217;" +
        "border-right: none;background-image: url(data:image/png;base64,${SEAMLESS_LOCK_BG});",
    FrameColor.GRAY to "border: 1px solid #f0f0f0;border-left: 8px solid #989898;" +
        "border-right: none;",
    FrameColor.RED to "border: 1px solid #f0f0f0;border-left: 8px solid #d14836;" +
        "border-right: none;",
    FrameColor.PLAIN to "border: none;"
  )

  private fun fmtMsgContentBlockAsHtml(dirtyContent: String?, frameColor: FrameColor): String {
    return if (dirtyContent == null) ""
    else "<div class=\"MsgBlock ${frameColor}\" style=" +
        "\"${GENERAL_CSS}${FRAME_CSS_MAP[frameColor]!!}\">" +
        "${sanitizeHtmlKeepBasicTags(dirtyContent)}</div><!-- next MsgBlock -->\n"
  }

  /**
   * Used whenever untrusted remote content (eg html email) is rendered, but we still want to
   * preserve HTML. "imgToLink" is ignored. Remote links are replaced with <a>, and local images
   * are preserved.
   */
  fun sanitizeHtmlKeepBasicTags(dirtyHtml: String?): String? {
    if (dirtyHtml == null) return null
    val imgContentReplaceable = "IMG_ICON_${generateRandomSuffix()}"
    var remoteContentReplacedWithLink = false
    val policyFactory = HtmlPolicyBuilder()
      .allowElements(
        { elementName, attrs ->
          // Remove tiny elements (often contain hidden content, tracking pixels, etc)
          for (i in 0 until attrs.size / 2) {
            val j = i * 2
            if (
              (attrs[j] == "width" && attrs[j + 1] == "1")
              || (attrs[j] == "height" && attrs[j + 1] == "1" && elementName != "hr")
            ) {
              return@allowElements null
            }
          }

          // let the browser/web view decide how big should elements be, based on their content
          var i = 0
          while (i < attrs.size) {
            if (
              (attrs[i] == "width" && attrs[i + 1] != "1" && elementName != "img")
              || (attrs[i] == "height" && attrs[i + 1] != "1" && elementName != "img")
            ) {
              attrs.removeAt(i)
              attrs.removeAt(i)
            } else {
              i += 2
            }
          }

          var newElementName = elementName
          if (elementName == "img") {
            val srcAttr = getAttribute(attrs, "src", "")!!
            val altAttr = getAttribute(attrs, "alt")
            when {
              srcAttr.startsWith("data:") -> {
                attrs.clear()
                attrs.add("src")
                attrs.add(srcAttr)
                if (altAttr != null) {
                  attrs.add("alt")
                  attrs.add(altAttr)
                }
              }

              (srcAttr.startsWith("http://") || srcAttr.startsWith("https://")) -> {
                // Orignal typecript:
                // return { tagName: 'a', attribs: { href: String(attribs.src), target: "_blank" },
                //   text: imgContentReplaceable };
                // Github: https://github.com/OWASP/java-html-sanitizer/issues/230
                // SO: https://stackoverflow.com/questions/67976114
                // There is no way to achieve this with OWASP sanitizer, so we do it with Jsoup
                // as post-processing step
                remoteContentReplacedWithLink = true
                newElementName = "a"
                attrs.clear()
                attrs.add("href")
                attrs.add(srcAttr)
                attrs.add("target")
                attrs.add("_blank")
                attrs.add(INNER_TEXT_TYPE_ATTR)
                attrs.add("1")
              }

              else -> {
                newElementName = "a"
                val titleAttr = getAttribute(attrs, "title")
                attrs.clear()
                if (altAttr != null) {
                  attrs.add("alt")
                  attrs.add(altAttr)
                }
                if (titleAttr != null) {
                  attrs.add("title")
                  attrs.add(titleAttr)
                }
                attrs.add(INNER_TEXT_TYPE_ATTR)
                attrs.add("2")
              }
            }
            attrs.add(FROM_IMAGE_ATTR)
            attrs.add(true.toString())
          }

          return@allowElements newElementName
        },
        *ALLOWED_ELEMENTS
      )
      .allowUrlProtocols(*ALLOWED_PROTOCOLS)
      .allowAttributesOnElementsExt(ALLOWED_ATTRS)
      .toFactory()

    val cleanHtml = policyFactory.sanitize(dirtyHtml)
    val doc = Jsoup.parse(cleanHtml)
    doc.outputSettings().prettyPrint(false)
    for (element in doc.select("a")) {
      if (element.hasAttr(INNER_TEXT_TYPE_ATTR)) {
        val innerTextType = element.attr(INNER_TEXT_TYPE_ATTR)
        element.attributes().remove(INNER_TEXT_TYPE_ATTR)
        var innerText: String? = null
        when (innerTextType) {
          "1" -> innerText = imgContentReplaceable
          "2" -> innerText = "[image]"
        }
        if (innerText != null) element.html(innerText)
      }
    }
    var cleanHtml2 = doc.outerHtml()

    if (remoteContentReplacedWithLink) {
      cleanHtml2 = htmlPolicyWithBasicTagsOnlyFactory.sanitize(
        "<font size=\"-1\" color=\"#31a217\" face=\"monospace\">[remote content blocked " +
            "for your privacy]</font><br /><br />$cleanHtml2"
      )
    }

    return cleanHtml2.replace(
      imgContentReplaceable,
      "<font color=\"#D14836\" face=\"monospace\">[img]</font>"
    )
  }

  private const val INNER_TEXT_TYPE_ATTR = "data-fc-inner-text-type"
  private const val FROM_IMAGE_ATTR = "data-fc-is-from-image"

  fun sanitizeHtmlStripAllTags(dirtyHtml: String?, outputNl: String = "\n"): String? {
    val html = sanitizeHtmlKeepBasicTags(dirtyHtml) ?: return null
    val randomSuffix = generateRandomSuffix()
    val br = "CU_BR_$randomSuffix"
    val blockStart = "CU_BS_$randomSuffix"
    val blockEnd = "CU_BE_$randomSuffix"

    var text = html.replace(HTML_BR_REGEX, br)
      .replace("\n", "")
      .replace(BLOCK_END_REGEX, blockEnd)
      .replace(BLOCK_START_REGEX, blockStart)
      .replace(Regex("($blockStart)+"), blockStart)
      .replace(Regex("($blockEnd)+"), blockEnd)

    val policyFactory = HtmlPolicyBuilder()
      .allowUrlProtocols(*ALLOWED_PROTOCOLS)
      .allowElements(
        { elementName, attrs ->
          when (elementName) {
            "img" -> {
              var innerText = "no name"
              val alt = getAttribute(attrs, "alt")
              if (alt != null) {
                innerText = alt
              } else {
                val title = getAttribute(attrs, "title")
                if (title != null) innerText = title
              }
              attrs.clear()
              attrs.add(INNER_TEXT_TYPE_ATTR)
              attrs.add(innerText)
              return@allowElements "span"
            }
            "a" -> {
              val fromImage = getAttribute(attrs, FROM_IMAGE_ATTR)
              if (fromImage == true.toString()) {
                var innerText = "[image]"
                val alt = getAttribute(attrs, "alt")
                if (alt != null) {
                  innerText = "[image: $alt]"
                }
                attrs.clear()
                attrs.add(INNER_TEXT_TYPE_ATTR)
                attrs.add(innerText)
                return@allowElements "span"
              } else {
                return@allowElements elementName
              }
            }
            else -> return@allowElements elementName
          }
        },
        "img",
        "span",
        "a"
      )
      .allowAttributes("src", "alt", "title").onElements("img")
      .allowAttributes(INNER_TEXT_TYPE_ATTR).onElements("span")
      .allowAttributes("src", "alt", "title", FROM_IMAGE_ATTR).onElements("a")
      .toFactory()

    text = policyFactory.sanitize(text)
    val doc = Jsoup.parse(text)
    doc.outputSettings().prettyPrint(false)
    for (element in doc.select("span")) {
      if (element.hasAttr(INNER_TEXT_TYPE_ATTR)) {
        val innerText = element.attr(INNER_TEXT_TYPE_ATTR)
        element.attributes().remove(INNER_TEXT_TYPE_ATTR)
        element.html(innerText)
      }
    }

    text = HtmlPolicyBuilder().toFactory().sanitize(doc.outerHtml())
    text = text.split(br + blockEnd + blockStart)
      .joinToString(br)
      .split(blockEnd + blockStart)
      .joinToString(br)
      .split(br + blockEnd)
      .joinToString(br)
      .split(br)
      .joinToString("\n")
      .split(blockStart)
      .filter { it != "" }
      .joinToString("\n")
      .split(blockEnd)
      .filter { it != "" }
      .joinToString("\n")
      .replace(MULTI_NEW_LINE_REGEX, "\n\n")

    if (outputNl != "\n") text = text.replace("\n", outputNl)
    return text
  }

  private val BLOCK_START_REGEX = Regex(
    "<(p|h1|h2|h3|h4|h5|h6|ol|ul|pre|address|blockquote|dl|div|fieldset|form|hr|table)[^>]*>"
  )
  private val BLOCK_END_REGEX = Regex(
    "</(p|h1|h2|h3|h4|h5|h6|ol|ul|pre|address|blockquote|dl|div|fieldset|form|hr|table)[^>]*>"
  )
  private val MULTI_NEW_LINE_REGEX = Regex("\\n{2,}")
  private val HTML_BR_REGEX = Regex("<br[^>]*>")

  private fun getAttribute(
    attrs: List<String>,
    attrName: String,
    defaultValue: String? = null
  ): String? {
    val srcAttrIndex = attrs.withIndex().indexOfFirst {
      it.index % 2 == 0 && it.value == attrName
    }
    return if (srcAttrIndex != -1) attrs[srcAttrIndex + 1] else defaultValue
  }

  private fun generateRandomSuffix(length: Int = 5): String {
    val rnd = Random(System.currentTimeMillis())
    var s = rnd.nextInt().toString(16)
    while (s.length < length) s += rnd.nextInt().toString(16)
    return s.substring(0, length)
  }

  private val ALLOWED_ELEMENTS = arrayOf(
    "p",
    "div",
    "br",
    "u",
    "i",
    "em",
    "b",
    "ol",
    "ul",
    "pre",
    "li",
    "table",
    "thead",
    "tbody",
    "tfoot",
    "tr",
    "td",
    "th",
    "h1",
    "h2",
    "h3",
    "h4",
    "h5",
    "h6",
    "hr",
    "address",
    "blockquote",
    "dl",
    "fieldset",
    "a",
    "font",
    "strong",
    "strike",
    "code",
    "img"
  )

  private val ALLOWED_ATTRS = mapOf(
    "a" to arrayOf("href", "name", "target", "data-x-fc-inner-text-type"),
    "img" to arrayOf("src", "width", "height", "alt"),
    "font" to arrayOf("size", "color", "face"),
    "span" to arrayOf("color"),
    "div" to arrayOf("color"),
    "p" to arrayOf("color"),
    "em" to arrayOf("style"), // Typescript: tests rely on this, could potentially remove
    "td" to arrayOf("width", "height"),
    "hr" to arrayOf("color", "height")
  )

  private val ALLOWED_PROTOCOLS = arrayOf("data", "http", "https", "mailto")

  private val htmlPolicyWithBasicTagsOnlyFactory = HtmlPolicyBuilder()
    .allowElements(*ALLOWED_ELEMENTS)
    .allowUrlProtocols(*ALLOWED_PROTOCOLS)
    .allowAttributesOnElementsExt(ALLOWED_ATTRS)
    .toFactory()

  fun extractFcAttachments(decryptedContent: String, blocks: MutableList<MsgBlock>): String {
    // these tags were created by FlowCrypt exclusively, so the structure is fairly rigid
    // `<a href="${att.url}" class="cryptup_file" cryptup-data="${fcData}">${linkText}</a>\n`
    // thus we can use Regex
    if (!decryptedContent.contains("class=\"cryptup_file\"")) return decryptedContent
    var i = 0
    val result = java.lang.StringBuilder()
    for (match in FC_ATT_REGEX.findAll(decryptedContent)) {
      if (match.range.first > i) {
        result.append(decryptedContent.substring(i, match.range.first))
        i = match.range.last + 1
      }
      val url = match.groups[1]!!.value
      val attr = match.groups[2]!!.value.decodeFcHtmlAttr()
      if (isFcAttachmentLinkData(attr)) {
        attr!!.put("url", url)
        blocks.add(EncryptedAttLinkMsgBlock(AttMeta(attr)))
      }
    }
    if (i < decryptedContent.length) result.append(decryptedContent.substring(i))
    return result.toString()
  }

  fun isFcAttachmentLinkData(obj: JSONObject?): Boolean {
    return obj != null && obj.has("name") && obj.has("size") && obj.has("type")
  }

  private val FC_ATT_REGEX = Regex(
    "<a\\s+href=\"([^\"]+)\"\\s+class=\"cryptup_file\"\\s+cryptup-data=" +
        "\"([^\"]+)\"\\s*>[^<]+</a>\\n?"
  )

  fun stripFcReplyToken(decryptedContent: String): String {
    return decryptedContent.replace(FC_REPLY_TOKEN_REGEX, "")
  }

  private val FC_REPLY_TOKEN_REGEX = Regex("<div[^>]+class=\"cryptup_reply\"[^>]+></div>")

  fun stripPublicKeys(decryptedContent: String, foundPublicKeys: MutableList<String>): String {
    val normalizedTextAndBlocks = MsgBlockParser.detectBlocks(decryptedContent)
    var result = normalizedTextAndBlocks.normalized
    for (block in normalizedTextAndBlocks.blocks) {
      if (block.type == MsgBlock.Type.PUBLIC_KEY && block.content != null) {
        val content = block.content!!
        foundPublicKeys.add(content)
        result = result.replace(content, "")
      }
    }
    return result
  }
}
