/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock
import com.flowcrypt.email.core.msg.MsgBlockParser
import com.flowcrypt.email.security.pgp.PgpArmor.ARMOR_HEADER_DICT
import org.bouncycastle.bcpg.ArmoredInputStream
import org.bouncycastle.bcpg.PacketTags
import org.bouncycastle.openpgp.PGPDataValidationException
import org.bouncycastle.openpgp.PGPException
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.bouncycastle.openpgp.jcajce.JcaPGPSecretKeyRingCollection
import org.pgpainless.PGPainless
import org.pgpainless.exception.MessageNotIntegrityProtectedException
import org.pgpainless.exception.ModificationDetectionException
import org.pgpainless.key.protection.PasswordBasedSecretKeyRingProtector
import org.pgpainless.key.protection.UnprotectedKeysProtector
import org.pgpainless.util.Passphrase
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
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
        if (tagNumber <= maxTagNumber) {
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
          && MsgBlock.Type.fourBlockTypes.contains(blocks[0].type)) {
        return Pair(true, blocks[0].type)
      }

      return Pair(false, MsgBlock.Type.UNKNOWN)
    } else return Pair(false, null)
  }

  private val messageTypes = intArrayOf(
      PacketTags.SYM_ENC_INTEGRITY_PRO,
      PacketTags.MOD_DETECTION_CODE,
      20, // SymEncryptedAEADProtected - no BouncyCastle constant for this one
      PacketTags.SYMMETRIC_KEY_ENC,
      PacketTags.COMPRESSED_DATA
  )

  private val maxTagNumber = 20

  enum class DecryptionErrorType {
    KEY_MISMATCH,

    // skip, effective for symmetric encryption
    // USE_PASSWORD,

    WRONG_PASSWORD,
    NO_MDC,
    BAD_MDC,

    // a passphrase was not provided for the key which requires it,
    // not sure how to detect this in the PGPainless.
    NEED_PASSPHRASE,

    FORMAT,
    OTHER
  }

  data class DecryptionError(
      val type: DecryptionErrorType,
      val message: String,
      val cause: Throwable? = null
  )

  data class DecryptionResult(
      // provided if decryption was successful
      val content: ByteArrayOutputStream?,

      // true if message was encrypted.
      // Alternatively false (because it could have also been plaintext signed,
      // or wrapped in PGP armor as plaintext packet without encrypting,
      val isEncrypted: Boolean,

      // pgp messages may include original filename in them
      val filename: String?,

      // todo later - signature verification not supported on Android yet
      val signature: Any?,

      // provided if error happens
      val error: DecryptionError?
  ) {
    companion object {
      fun withError(
          type: DecryptionErrorType,
          message: String,
          cause: Throwable? = null
      ): DecryptionResult {
        return DecryptionResult(
            content = null,
            isEncrypted = false,
            filename = null,
            signature = null,
            error = DecryptionError(type, message, cause)
        )
      }

      fun withCleartext(cleartext: ByteArrayOutputStream, signature: Any?): DecryptionResult {
        return DecryptionResult(
            content = cleartext,
            isEncrypted = false,
            filename = null,
            signature = signature,
            error = null
        )
      }

      fun withDecrypted(content: ByteArrayOutputStream, filename: String?): DecryptionResult {
        return DecryptionResult(
            content = content,
            isEncrypted = false,
            filename = null,
            signature = null,
            error = null
        )
      }
    }
  }

  @Suppress("ArrayInDataClass")
  data class SecretKeyInfo(
      val armored: String? = null,
      val keyRing: PGPSecretKeyRing = readArmored(armored!!),
      val passphrase: CharArray? = null
  ) {
    companion object {
      private fun readArmored(armored: String): PGPSecretKeyRing {
        return JcaPGPSecretKeyRingCollection(
            ArmoredInputStream(armored.toByteArray().inputStream())
        ).keyRings.asSequence().first()
      }
    }
  }

  fun decrypt(
      data: ByteArray,
      pgpSecretKeyRing: PGPSecretKeyRing, // for decryption
      passphrase: Passphrase?, // for decryption of secret keys
      pgpPublicKeyRingCollection: PGPPublicKeyRingCollection? // for verification
  ): DecryptionResult {
    if (data.isEmpty()) {
      return DecryptionResult.withError(DecryptionErrorType.FORMAT, "Can't decrypt empty message")
    }

    val chunk = data.copyOfRange(0, data.size.coerceAtMost(50)).toString(StandardCharsets.US_ASCII)
    var input = data.inputStream() as InputStream

    if (chunk.contains(ARMOR_HEADER_DICT[MsgBlock.Type.SIGNED_MSG]!!.begin)) {
      val msg = PgpArmor.readSignedClearTextMessage(input)
      return DecryptionResult.withCleartext(msg.content, msg.signature)
    }

    val isArmored = chunk.contains(ARMOR_HEADER_DICT[MsgBlock.Type.ENCRYPTED_MSG]!!.begin)
    if (isArmored) input = ArmoredInputStream(input)

    val protector = if (passphrase != null) {
      PasswordBasedSecretKeyRingProtector.forKey(pgpSecretKeyRing, passphrase)
    } else {
      UnprotectedKeysProtector()
    }

    try {
      val d = PGPainless.decryptAndOrVerify()
          .onInputStream(input)
          .decryptWith(protector, PGPSecretKeyRingCollection(listOf(pgpSecretKeyRing)))

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
      return DecryptionResult(
        content = null,
        isEncrypted = true,
        filename = null,
        signature = null,
        error = DecryptionError(
          DecryptionErrorType.NO_MDC,
          "Security threat! Message is missing integrity checks (MDC). The sender should " +
          "update their outdated software."
        )
      )
    } catch (ex: ModificationDetectionException) {
      return DecryptionResult(
          content = null,
          isEncrypted = true,
          filename = null,
          signature = null,
          error = DecryptionError(
              DecryptionErrorType.BAD_MDC,
              "Security threat! Integrity check failed."
          )
      )
    } catch (ex: Exception) {
      if (ex is PGPDataValidationException || (ex is PGPException
              && (
                ex.message?.contains("exception decrypting session info") == true
                || ex.message?.contains("encoded length out of range") == true
                || ex.message?.contains("Invalid Curve25519 public key") == true
                || ex.message?.contains("Exception recovering session info") == true
                || ex.message?.contains("ECDH requires use of PGPPrivateKey for decryption") == true
              )
          )
      ) {
        return if (passphrase != null) {
          DecryptionResult.withError(
              type = DecryptionErrorType.WRONG_PASSWORD,
              message = "Wrong password",
              cause = ex
          )
        } else {
          DecryptionResult.withError(
              type = DecryptionErrorType.KEY_MISMATCH,
              message = "There is no matching key",
              cause = ex
          )
        }
      } else {
        return DecryptionResult.withError(
            type = DecryptionErrorType.OTHER,
            message = "There is no matching key",
            cause = ex
        )
      }
    }
  }
}
