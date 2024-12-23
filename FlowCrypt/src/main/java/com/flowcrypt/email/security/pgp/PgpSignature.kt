/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.security.pgp


import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.pgpainless.PGPainless
import org.pgpainless.decryption_verification.ConsumerOptions
import org.pgpainless.decryption_verification.MessageMetadata
import org.pgpainless.decryption_verification.cleartext_signatures.ClearsignedMessageUtil
import org.pgpainless.decryption_verification.cleartext_signatures.InMemoryMultiPassStrategy
import org.pgpainless.decryption_verification.cleartext_signatures.MultiPassStrategy
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * @author Denys Bondarenko
 */
object PgpSignature {
  fun extractClearText(source: String?, isSilent: Boolean = true): String? {
    return extractClearText(ByteArrayInputStream(source?.toByteArray()), isSilent)
  }

  @Suppress("MemberVisibilityCanBePrivate")
  fun extractClearText(srcInputStream: InputStream, isSilent: Boolean = true): String? {
    srcInputStream.use { srcStream ->
      return try {
        val multiPassStrategy = MultiPassStrategy.keepMessageInMemory()
        ClearsignedMessageUtil.detachSignaturesFromInbandClearsignedMessage(
          srcStream,
          multiPassStrategy.messageOutputStream
        )
        String(multiPassStrategy.getBytes())
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
    ByteArrayOutputStream().use { outStream ->
      return try {
        val verificationStream = PGPainless.decryptAndOrVerify()
          .onInputStream(srcInputStream)
          .withOptions(
            ConsumerOptions()
              .addVerificationCerts(publicKeys)
              .setMultiPassStrategy(InMemoryMultiPassStrategy())
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
    ByteArrayOutputStream().use { outStream ->
      return try {
        val verificationStream = PGPainless.decryptAndOrVerify()
          .onInputStream(srcInputStream)
          .withOptions(
            ConsumerOptions()
              .addVerificationOfDetachedSignatures(signatureInputStream)
              .addVerificationCerts(publicKeys)
              .setMultiPassStrategy(InMemoryMultiPassStrategy())
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
