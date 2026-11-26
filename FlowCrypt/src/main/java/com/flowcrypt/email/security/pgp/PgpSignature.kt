/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.security.pgp


import com.flowcrypt.email.Constants
import com.flowcrypt.email.extensions.java.io.LimitedOutputStream
import com.flowcrypt.email.extensions.java.io.readBytesWithLimit
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.pgpainless.PGPainless
import org.pgpainless.decryption_verification.ConsumerOptions
import org.pgpainless.decryption_verification.MessageMetadata
import org.pgpainless.decryption_verification.cleartext_signatures.ClearsignedMessageUtil
import org.pgpainless.decryption_verification.cleartext_signatures.MultiPassStrategy
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * @author Denys Bondarenko
 */
object PgpSignature {
  fun extractClearText(source: String?, isSilent: Boolean = true): String? {
    return extractClearText(source?.byteInputStream() ?: ByteArrayInputStream(ByteArray(0)), isSilent)
  }

  @Suppress("MemberVisibilityCanBePrivate")
  fun extractClearText(srcInputStream: InputStream, isSilent: Boolean = true): String? {
    srcInputStream.use { srcStream ->
      return try {
        val messageOutputStream = LimitedOutputStream(Constants.MAX_PGP_MESSAGE_SIZE_IN_BYTES)
        ClearsignedMessageUtil.detachSignaturesFromInbandClearsignedMessage(
          ByteArrayInputStream(srcStream.readBytesWithLimit(Constants.MAX_PGP_MESSAGE_SIZE_IN_BYTES)),
          messageOutputStream
        )
        String(messageOutputStream.toByteArray())
      } catch (e: Exception) {
        if (isSilent) {
          e.printStackTrace()
          null
        } else throw e
      }
    }
  }

  fun verifyClearTextSignature(
    srcInputStream: InputStream,
    publicKeys: PGPPublicKeyRingCollection
  ): ClearTextVerificationResult {
    LimitedOutputStream(Constants.MAX_PGP_MESSAGE_SIZE_IN_BYTES).use { outStream ->
      return try {
        val api = PGPainless.getInstance()
        val verificationStream = api.processMessage()
          .onInputStream(
            ByteArrayInputStream(srcInputStream.readBytesWithLimit(Constants.MAX_PGP_MESSAGE_SIZE_IN_BYTES))
          )
          .withOptions(
            ConsumerOptions.get(api)
              .addVerificationCerts(publicKeys.map { api.toCertificate(it) })
              .setMultiPassStrategy(LimitedMultiPassStrategy())
          )

        verificationStream.use { it.copyTo(outStream) }
        ClearTextVerificationResult(
          messageMetadata = verificationStream.metadata,
          clearText = String(outStream.toByteArray())
        )
      } catch (e: Exception) {
        ClearTextVerificationResult(exception = e)
      }
    }
  }

  fun verifyDetachedSignature(
    srcInputStream: InputStream,
    signatureInputStream: InputStream,
    publicKeys: PGPPublicKeyRingCollection
  ): DetachedSignatureVerificationResult {
    LimitedOutputStream(Constants.MAX_PGP_MESSAGE_SIZE_IN_BYTES).use { outStream ->
      return try {
        val api = PGPainless.getInstance()
        val verificationStream = api.processMessage()
          .onInputStream(
            ByteArrayInputStream(srcInputStream.readBytesWithLimit(Constants.MAX_PGP_MESSAGE_SIZE_IN_BYTES))
          )
          .withOptions(
            ConsumerOptions.get(api)
              .addVerificationOfDetachedSignatures(
                ByteArrayInputStream(
                  signatureInputStream.readBytesWithLimit(Constants.MAX_PGP_MESSAGE_SIZE_IN_BYTES)
                )
              )
              .addVerificationCerts(publicKeys.map { api.toCertificate(it) })
              .setMultiPassStrategy(LimitedMultiPassStrategy())
          )

        verificationStream.use { it.copyTo(outStream) }
        DetachedSignatureVerificationResult(
          messageMetadata = verificationStream.metadata
        )
      } catch (e: Exception) {
        e.printStackTrace()
        DetachedSignatureVerificationResult(exception = e)
      }
    }
  }

  private class LimitedMultiPassStrategy : MultiPassStrategy {
    private val output = LimitedOutputStream(Constants.MAX_PGP_MESSAGE_SIZE_IN_BYTES)

    override val messageOutputStream: java.io.OutputStream = output

    override val messageInputStream: InputStream
      get() = ByteArrayInputStream(output.toByteArray())
  }

  data class ClearTextVerificationResult(
    override val messageMetadata: MessageMetadata? = null,
    override val exception: Exception? = null,
    val clearText: String? = null,
  ) : VerificationResult

  data class DetachedSignatureVerificationResult(
    override val messageMetadata: MessageMetadata? = null,
    override val exception: Exception? = null
  ) : VerificationResult

  interface VerificationResult {
    val messageMetadata: MessageMetadata?
    val exception: Exception?
  }
}
