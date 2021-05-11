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
import org.pgpainless.PGPainless
import org.pgpainless.exception.MessageNotIntegrityProtectedException
import org.pgpainless.exception.ModificationDetectionException
import org.pgpainless.key.info.KeyRingInfo
import org.pgpainless.key.protection.UnprotectedKeysProtector
import org.pgpainless.util.Passphrase
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import kotlin.experimental.and
import kotlin.math.min

object PgpMsg {
  private const val MAX_TAG_NUMBER = 20

  /**
   * @return Pair.first  indicates armored (true) or binary (false) format
   *         Pair.second block type or null if the source is empty
   */
  fun detectBlockType(source: ByteArray): Pair<Boolean, MsgBlock.Type?> {
    val messageTypes = intArrayOf(
      PacketTags.SYM_ENC_INTEGRITY_PRO,
      PacketTags.MOD_DETECTION_CODE,
      20, // SymEncryptedAEADProtected - no BouncyCastle constant for this one
      PacketTags.SYMMETRIC_KEY_ENC,
      PacketTags.COMPRESSED_DATA
    )

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

  fun decrypt(
    data: ByteArray, keys: List<KeyWithPassPhrase>,
    pgpPublicKeyRingCollection: PGPPublicKeyRingCollection?
  ): DecryptionResult {
    if (data.isEmpty()) {
      return DecryptionResult.withError(DecryptionErrorType.FORMAT, "Can't decrypt empty message")
    }

    val chunk = data.copyOfRange(0, data.size.coerceAtMost(50)).toString(StandardCharsets.US_ASCII)
    var input = data.inputStream() as InputStream

    if (chunk.contains(ARMOR_HEADER_DICT[MsgBlock.Type.SIGNED_MSG]!!.begin)) {
      return handleSignedClearTextMsg(input)
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
          type = DecryptionErrorType.NEED_PASSPHRASE,
          message = "Need passphrase"
        )
      }
      return DecryptionResult.withError(
        type = DecryptionErrorType.WRONG_PASSPHRASE,
        message = "Wrong passphrase",
        cause = ex
      )
    }

    var exception: Exception? = null
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
        type = DecryptionErrorType.NO_MDC,
        message = "Security threat! Message is missing integrity checks (MDC)." +
            " The sender should update their outdated software.",
        cause = ex
      )
    } catch (ex: ModificationDetectionException) {
      return DecryptionResult.withError(
        type = DecryptionErrorType.BAD_MDC,
        message = "Security threat! Integrity check failed.",
        cause = ex
      )
    } catch (ex: PGPDataValidationException) {
      return DecryptionResult.withError(
        type = DecryptionErrorType.KEY_MISMATCH,
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
          type = DecryptionErrorType.KEY_MISMATCH,
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
      type = DecryptionErrorType.OTHER,
      message = "Decryption failed",
      cause = exception
    )
  }

  private fun handleSignedClearTextMsg(input: InputStream): DecryptionResult {
    val msg: PgpArmor.CleartextSignedMessage
    try {
      msg = PgpArmor.readSignedClearTextMessage(input)
    } catch (ex: Exception) {
      return if (
        ex is PGPException && ex.message != null && ex.message == "Cleartext format error"
      ) {
        DecryptionResult.withError(
          type = DecryptionErrorType.FORMAT,
          message = ex.message!!,
          cause = ex.cause
        )
      } else {
        DecryptionResult.withError(
          type = DecryptionErrorType.OTHER,
          message = "Decode cleartext error",
          cause = ex
        )
      }
    }
    return DecryptionResult.withCleartext(msg.content, msg.signature)
  }

  data class KeyWithPassPhrase(
    val keyRing: PGPSecretKeyRing,
    val passphrase: Passphrase?
  )

  enum class DecryptionErrorType {
    KEY_MISMATCH,
    WRONG_PASSPHRASE,
    NO_MDC,
    BAD_MDC,
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
    /**
     * provided if decryption was successful
     */
    val content: ByteArrayOutputStream? = null,

    /**
     * true if message was encrypted.
     * Alternatively false (because it could have also been plaintext signed,
     * or wrapped in PGP armor as plaintext packet without encrypting)
     * also false when error happens.
     */
    val isEncrypted: Boolean = false,

    /**
     * pgp messages may include original filename in them
     */
    val filename: String? = null,

    // todo later - signature verification not supported on Android yet
    val signature: Any? = null,

    /**
     * provided if error happens
     */
    val error: DecryptionError? = null
  ) {
    companion object {
      fun withError(
        type: DecryptionErrorType,
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
}
