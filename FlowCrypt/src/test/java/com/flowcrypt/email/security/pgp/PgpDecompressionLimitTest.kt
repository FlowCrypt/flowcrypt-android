/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.api.retrofit.response.model.DecryptErrorMsgBlock
import jakarta.mail.Session
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import jakarta.mail.util.ByteArrayDataSource
import org.bouncycastle.bcpg.CompressionAlgorithmTags
import org.bouncycastle.openpgp.PGPCompressedDataGenerator
import org.bouncycastle.openpgp.PGPLiteralData
import org.bouncycastle.openpgp.PGPLiteralDataGenerator
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.pgpainless.key.protection.SecretKeyRingProtector
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.util.Date
import java.util.Properties

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [BuildConfig.MIN_SDK_VERSION])
class PgpDecompressionLimitTest {
  @Test
  fun testBombAttachmentProducesBoundedErrorBlock() {
    val message = MimeMessage(Session.getInstance(Properties()))
    val multipart = MimeMultipart()
    val body = MimeBodyPart()
    body.setText("message")
    multipart.addBodyPart(body)
    val attachment = MimeBodyPart()
    attachment.setDataHandler(
      jakarta.activation.DataHandler(
        ByteArrayDataSource(compressLiteralData(ByteArray(DECOMPRESSED_SIZE)), "application/octet-stream")
      )
    )
    attachment.fileName = "message.asc"
    multipart.addBodyPart(attachment)
    message.setContent(multipart)
    message.saveChanges()

    val result = PgpMsg.processMimeMessage(
      msg = message,
      secretKeys = PGPSecretKeyRingCollection(emptyList()),
      protector = SecretKeyRingProtector.unprotectedKeys()
    )
    assertTrue(
      "The oversized attachment was not rejected",
      result.blocks.filterIsInstance<DecryptErrorMsgBlock>().any {
        it.decryptErr?.details?.message?.contains("exceeds the limit") == true
      }
    )
    assertTrue(
      "Rendered block grew beyond the decompression limit",
      result.blocks.all { (it.content?.length ?: 0) < DECOMPRESSED_SIZE }
    )
  }

  @Test
  fun testDecompressionBeyondLimitIsRejected() {
    val compressed = compressLiteralData(ByteArray(DECOMPRESSED_SIZE))
    val result = PgpDecryptAndOrVerify.decryptAndOrVerifyWithResult(
      srcInputStream = compressed.inputStream()
    )
    assertNull("Unbounded decompression result was returned", result.content)
    assertNotNull("No error reported", result.exception)
  }

  @Test
  fun testDecompressionWithinLimitSucceeds() {
    val compressed = compressLiteralData(ByteArray(ONE_MB))
    val result = PgpDecryptAndOrVerify.decryptAndOrVerifyWithResult(
      srcInputStream = compressed.inputStream()
    )
    assertNull("Unexpected error", result.exception)
    assertNotNull("Content not returned", result.content)
  }

  private fun compressLiteralData(data: ByteArray): ByteArray {
    val compressedOutput = ByteArrayOutputStream()
    val compressedGenerator = PGPCompressedDataGenerator(CompressionAlgorithmTags.ZIP)
    val compressedStream = compressedGenerator.open(compressedOutput)
    val literalGenerator = PGPLiteralDataGenerator()
    val literalStream = literalGenerator.open(
      compressedStream,
      PGPLiteralData.BINARY,
      "message.txt",
      data.size.toLong(),
      Date()
    )
    literalStream.write(data)
    literalStream.close()
    literalGenerator.close()
    compressedGenerator.close()
    return compressedOutput.toByteArray()
  }

  private companion object {
    const val ONE_MB = 1024 * 1024
    const val DECOMPRESSED_SIZE = 9 * ONE_MB
  }
}
