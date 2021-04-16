/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock
import com.flowcrypt.email.core.msg.MsgBlockParser
import com.flowcrypt.email.extensions.kotlin.toHex
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.decrypt
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.isDecryptedFor
import org.bouncycastle.bcpg.ArmoredInputStream
import java.lang.RuntimeException
import java.nio.charset.StandardCharsets
import kotlin.experimental.and
import kotlin.math.min
import org.bouncycastle.bcpg.PacketTags
import org.bouncycastle.openpgp.PGPLiteralData
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.jcajce.JcaPGPSecretKeyRingCollection
import java.io.InputStream

object PgpMsg {

  private val messageTypes = intArrayOf(
      PacketTags.SYM_ENC_INTEGRITY_PRO,
      PacketTags.MOD_DETECTION_CODE,
      20, // SymEncryptedAEADProtected - no BouncyCastle constant for this one
      PacketTags.SYMMETRIC_KEY_ENC,
      PacketTags.COMPRESSED_DATA
  )

  private const val maxTagNumber = 20

  data class DetectBlockTypeResult(val isArmored: Boolean, val type: MsgBlock.Type?)

  @JvmStatic
  fun detectBlockType(source: ByteArray): DetectBlockTypeResult {
    if (source.isNotEmpty()) {
      val firstByte = source[0]
      if (firstByte and (0x80.toByte()) == 0x80.toByte()) {
        // 11XX XXXX - potential new pgp packet tag
        val tagNumber = if (firstByte and (0xC0.toByte()) == 0xC0.toByte()) {
          (firstByte and 0x3f).toInt()  // 11TTTTTT where T is tag number bit
        } else { // 10XX XXXX - potential old pgp packet tag
          (firstByte and 0x3c).toInt() ushr (2) // 10TTTTLL where T is tag number bit.
        }
        if (tagNumber <= maxTagNumber) {
          // Indeed a valid OpenPGP packet tag number
          // This does not 100% mean it's OpenPGP message
          // But it's a good indication that it may be
          return DetectBlockTypeResult(
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
          && MsgBlock.Type.fourBlockTypes.contains(blocks[0].type)) {
        return DetectBlockTypeResult(true, blocks[0].type)
      }

      return DetectBlockTypeResult(false, MsgBlock.Type.UNKNOWN)
    }

    return DetectBlockTypeResult(false, null)
  }

  data class Message(
      val fileName: String,
      val content: ByteArray,
      val isEncrypted: Boolean
      // val signature: VerifyResult -- TBD later
  ) {
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false
      other as Message
      return (fileName == other.fileName)
          && content.contentEquals(other.content)
          && (isEncrypted == other.isEncrypted)
    }

    override fun hashCode(): Int {
      var result = fileName.hashCode()
      result = 31 * result + content.contentHashCode()
      result = 31 * result + isEncrypted.hashCode()
      return result
    }
  }

  data class LongIds(
      val message: List<Long> = emptyList(),
      val matching: List<Long> = emptyList(),
      val chosen: List<Long> = emptyList(),
      val needPassphrase: List<Long> = emptyList()
  )

  @Suppress("unused")
  enum class DecryptionErrorType {
    KEY_MISMATCH,
    USE_PASSWORD,
    WRONG_PASSWORD,
    NO_MDC,
    BAD_MDC,
    NEED_PASSPHRASE,
    FORMAT,
    OTHER
  }

  @Suppress("unused")
  open class DecryptionError(
      val errorType: DecryptionErrorType,
      errorMessage: String,
      val longIds: LongIds = LongIds(),
      val content: ByteArray? = null,
      val packets: PgpMessage? = null,
      val isEncrypted: Boolean? = null,
      // cannot be just "message", interferes with Exception.message
      val pgpMessage: Message? = null,
      cause: Exception? = null
  ) : RuntimeException(errorMessage, cause)

  class MessageFormatError(
      cause: Exception? = null
  ) : DecryptionError(
      errorType = DecryptionErrorType.FORMAT,
      errorMessage = "Message format error",
      cause = cause
  )

  class MissingPassPhraseError(
      longIds: LongIds,
      packets: PgpMessage?
  ) : DecryptionError(
      errorType = DecryptionErrorType.NEED_PASSPHRASE,
      errorMessage = "Missing passphrase",
      longIds = longIds,
      packets = packets,
      isEncrypted = true
  )

  class UsePasswordError(
      longIds: LongIds,
  ) : DecryptionError(
      errorType = DecryptionErrorType.USE_PASSWORD,
      errorMessage = "Use message password",
      longIds = longIds,
      isEncrypted = true
  )

  class NoMdcError(
      longIds: LongIds,
      packets: PgpMessage,
  ) : DecryptionError(
      errorType = DecryptionErrorType.NO_MDC,
      errorMessage = "Security threat! Message is missing integrity checks (MDC). " +
          "The sender should update their outdated software. Display the message at your own risk.",
      longIds = longIds,
      packets = packets,
      isEncrypted = true
  )

  class OtherError(
      longIds: LongIds,
      packets: PgpMessage,
      cause: Exception
  ) : DecryptionError(
      errorType = DecryptionErrorType.OTHER,
      errorMessage = "Other decryption error",
      longIds = longIds,
      packets = packets,
      isEncrypted = true,
      cause = cause
  )

  data class SecretKeyInfo(
      val armored: String? = null,
      val keyRing: PGPSecretKeyRing = readArmored(armored!!),
      val passphrase: CharArray? = null,
      // var decrypted: PGPSecretKeyRing?
  ) {
    companion object {
      private fun readArmored(armored: String): PGPSecretKeyRing {
        return JcaPGPSecretKeyRingCollection(
            ArmoredInputStream(armored.toByteArray().inputStream())
        ).keyRings.asSequence().first()
      }
    }

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as SecretKeyInfo

      if (keyRing != other.keyRing) return false
      if (passphrase != null) {
        if (other.passphrase == null) return false
        if (!passphrase.contentEquals(other.passphrase)) return false
      } else if (other.passphrase != null) return false

      return true
    }

    override fun hashCode(): Int {
      var result = keyRing.hashCode()
      result = 31 * result + (passphrase?.contentHashCode() ?: 0)
      return result
    }
  }

  @JvmStatic
  fun decrypt(
      keysWithPassphrase: List<SecretKeyInfo>,
      encryptedData: ByteArray,
      password: CharArray?
  ): Message {
    val prepared: PgpArmor.PreparedForDecrypt
    try {
      prepared = PgpArmor.cryptoMsgPrepareForDecrypt(encryptedData)
    } catch (ex: Exception) {
      throw MessageFormatError(ex)
    }

    val keys = getSortedKeys(keysWithPassphrase, prepared)
    val longIds = LongIds(
        message = keys.encryptedFor.toList(),
        matching = keys.prvMatching.map { it.keyRing.publicKey.keyID },
        chosen = keys.prvForDecryptDecrypted.map { it.publicKey.keyID },
        needPassphrase = keys.prvForDecryptWithoutPassPhrases.map { it.publicKey.keyID },
    )

    if (prepared.isCleartext) {
      // Skip signature verification at the moment
      return Message(fileName = "", content = prepared.cleartext!!, isEncrypted = false)
    }

    if (keys.prvForDecryptDecrypted.isEmpty() && password == null) {
      throw MissingPassPhraseError(longIds = longIds, packets = prepared.message)
    }

    val isSymEncrypted = prepared.message.packets.any {
      it.packetType == PacketTags.SYMMETRIC_KEY_ENC_SESSION
    }
    val isPubEncrypted = prepared.message.packets.any {
      it.packetType == PacketTags.PUBLIC_KEY_ENC_SESSION
    }
    if (isSymEncrypted && !isPubEncrypted && password == null) {
      throw UsePasswordError(longIds = longIds)
    }

    val passwords = if (password != null) listOf(password) else null
    val decrypted: List<Pair<PgpMessage.AbstractPacketWrapper, PGPLiteralData>>
    try {
      decrypted = prepared.message.decrypt(keys.prvForDecryptDecrypted, passwords)
    } catch (ex: PgpMessage.DecryptionError) {
      // TODO: categorize error
      throw OtherError(longIds = longIds, packets = prepared.message, cause = ex)
    } catch (ex: Exception) {
      throw OtherError(longIds = longIds, packets = prepared.message, cause = ex)
    }

    // here was the check for MDC error, but not sure how to translate that into Kotlin

    // take first as decrypted message
    return Message(
        fileName = decrypted[0].second.fileName,
        content = decrypted[0].second.inputStream.readBytes(),
        isEncrypted = true
    )
  }

  private data class SortedKeysForDecrypt(
      // comment out not yet actual signature verification related stuff
      // val verificationContacts: List<Contact>,
      // val forVerification: List<PGPKeyRing>,
      val encryptedFor: Set<Long>,
      val signedBy: Set<Long>,
      val prvMatching: List<SecretKeyInfo>,
      val prvForDecrypt: List<SecretKeyInfo>,
      val prvForDecryptDecrypted: List<PGPSecretKeyRing>,
      val prvForDecryptWithoutPassPhrases: List<PGPSecretKeyRing>
  )

  private fun getSortedKeys(
      keysWithPassphrase: List<SecretKeyInfo>,
      msg: PgpArmor.PreparedForDecrypt
  ): SortedKeysForDecrypt {
    val encryptedForKeyIds = msg.message.packets
        .filter { it.packetType == PacketTags.PUBLIC_KEY_ENC_SESSION }.map {
          (it as PgpMessage.PublicKeyEncSessionPacketWrapper).encryptedData.asSequence()
        }.asSequence().flatten().map { (it as PGPPublicKeyEncryptedData).keyID }.toSet()
    val signedBy = cryptoMsgGetSignedBy(msg)
    val prvMatching = keysWithPassphrase
        .filter { encryptedForKeyIds.contains(it.keyRing.publicKey.keyID) }.toList()
    val prvForDecrypt = if (prvMatching.isNotEmpty()) prvMatching else keysWithPassphrase
    val prvForDecryptDecrypted = mutableListOf<PGPSecretKeyRing>()
    val prvForDecryptWithoutPassPhrases = mutableListOf<PGPSecretKeyRing>()

    for (keyRingWithPassphrase in prvForDecrypt) {
      val keyId = keyRingWithPassphrase.keyRing.publicKey.keyID
      val matchingKeyId: Long? = if (encryptedForKeyIds.contains(keyId)) keyId else null
      // println(
      //     "keyring ${keyRingWithPassphrase.keyRing.publicKey.keyID.toHex()}: " +
      //     "matching key id: $matchingKeyId"
      // )
      if (keyRingWithPassphrase.keyRing.isDecryptedFor(matchingKeyId)) {
          prvForDecryptDecrypted.add(keyRingWithPassphrase.keyRing)
      } else {
        try {
          prvForDecryptDecrypted.add(
              if (keyRingWithPassphrase.passphrase != null) {
                keyRingWithPassphrase.keyRing.decrypt(keyRingWithPassphrase.passphrase)
              } else {
                keyRingWithPassphrase.keyRing
              }
          )
        } catch (ex: Exception) {
          ex.printStackTrace()
          prvForDecryptWithoutPassPhrases.add(keyRingWithPassphrase.keyRing)
        }
      }
    }

    return SortedKeysForDecrypt(
        encryptedFor = encryptedForKeyIds,
        signedBy = signedBy.keyIds,
        prvMatching = prvMatching,
        prvForDecrypt = prvForDecrypt,
        prvForDecryptDecrypted = prvForDecryptDecrypted,
        prvForDecryptWithoutPassPhrases = prvForDecryptWithoutPassPhrases
    )
  }

  private data class SignedBy(val keyIds: Set<Long>)

  private fun cryptoMsgGetSignedBy(msg: PgpArmor.PreparedForDecrypt): SignedBy {
    val keyIds = if (msg.isCleartext) {
      msg.message.packets.filter { it.packetType == PacketTags.SIGNATURE }
          .map { (it as PgpMessage.SignaturePacketWrapper).signatures.asSequence() }.asSequence()
          .flatten().map { it.keyID }.toSet()
    } else {
      msg.message.packets.filter { it.packetType == PacketTags.ONE_PASS_SIGNATURE }
          .map { (it as PgpMessage.OnePassSignaturePacketWrapper).signatures.asSequence() }.asSequence()
          .flatten().map { it.keyID }.toSet()
    }.toSet()
    // skip the part with obtaining contacts
    return SignedBy(keyIds)
  }
}
