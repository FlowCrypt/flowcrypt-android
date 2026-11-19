/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.api.retrofit.response.model.MsgBlock
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.bcpg.CompressionAlgorithmTags
import org.bouncycastle.bcpg.PacketTags
import org.bouncycastle.openpgp.PGPCompressedDataGenerator
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.pgpainless.key.protection.SecretKeyRingProtector
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Properties

class PgpPacketBudgetGuardTest {
  @Test
  fun compressedSignatureBombIsRejectedByDecrypt() {
    val armored = armoredMessage(signatureCount = 2_000, literalText = "secret")

    val result = PgpDecryptAndOrVerify.decryptAndOrVerifyWithResult(
      srcInputStream = armored.byteInputStream(),
      publicKeys = PGPPublicKeyRingCollection(emptyList()),
      secretKeys = PGPSecretKeyRingCollection(emptyList()),
      protector = SecretKeyRingProtector.unprotectedKeys()
    )

    assertNull("Content must not be produced", result.content)
    assertNotNull("Over-budget message must be rejected", result.exception)
    assertEquals(
      PgpDecryptAndOrVerify.DecryptionErrorType.FORMAT,
      result.exception?.decryptionErrorType
    )
    assertTrue(
      "Budget error expected but was: ${result.exception?.message}",
      result.exception?.message?.contains("signature packets") == true
    )
  }

  @Test
  fun reportedScaleSignatureBombIsRejected() {
    val armored = armoredMessage(signatureCount = 120_000, literalText = "secret")
    assertRejected(armored, "signature packets")
  }

  @Test
  fun armorMarkerInsideBinaryPacketIsNotMistakenForArmor() {
    val inner = ByteArrayOutputStream()
    writePacket(
      inner, 60, "-----BEGIN PGP MESSAGE----- noise".toByteArray(StandardCharsets.US_ASCII)
    )
    repeat(2_000) { writePacket(inner, PacketTags.SIGNATURE, byteArrayOf(0, 1, 2, 3)) }

    try {
      PgpPacketBudgetGuard.guard(ByteArrayInputStream(inner.toByteArray()))
      fail("Expected an over-budget rejection")
    } catch (e: PgpPacketBudgetGuard.BudgetExceededException) {
      assertTrue("Unexpected message: ${e.message}", e.message!!.contains("signature packets"))
    }
  }

  @Test
  fun binarySignatureBombIsRejected() {
    val inner = ByteArrayOutputStream()
    repeat(2_000) { writePacket(inner, PacketTags.SIGNATURE, byteArrayOf(0, 1, 2, 3)) }
    try {
      PgpPacketBudgetGuard.guard(ByteArrayInputStream(inner.toByteArray()))
      fail("Expected an over-budget rejection")
    } catch (e: PgpPacketBudgetGuard.BudgetExceededException) {
      assertTrue("Unexpected message: ${e.message}", e.message!!.contains("signature packets"))
    }
  }

  @Test
  fun uncompressedSignatureBombIsRejected() {
    val armored = armoredMessage(signatureCount = 2_000, compress = false)
    assertRejected(armored, "signature packets")
  }

  @Test
  fun nestedCompressedSignatureBombIsRejected() {
    val armored = armoredMessage(signatureCount = 2_000, nestedCompression = true)
    assertRejected(armored, "signature packets")
  }

  @Test
  fun tooManyPacketsAreRejected() {
    val inner = ByteArrayOutputStream()
    repeat(12_000) { writePacket(inner, 60, byteArrayOf(1)) }
    assertRejected(armoredRaw(inner.toByteArray(), compress = false), "too many packets")
  }

  @Test
  fun oversizedDecompressedStreamIsRejected() {
    val output = ByteArrayOutputStream()
    val generator = PGPCompressedDataGenerator(CompressionAlgorithmTags.ZIP)
    val compressor = generator.open(output)
    // 1,100 packets of 8 KiB each decompress to about 8.6 MiB.
    repeat(1_100) { writePacket(compressor, 60, ByteArray(8 * 1024)) }
    compressor.close()
    generator.close()
    assertRejected(armorRaw(output.toByteArray()), "Decompressed")
  }

  @Test
  fun underBudgetMessageIsAccepted() {
    val armored = armoredMessage(signatureCount = 100, literalText = "hello")
    val guarded = PgpPacketBudgetGuard.guard(armored.byteInputStream())
    assertNotNull(guarded)

    val result = PgpDecryptAndOrVerify.decryptAndOrVerifyWithResult(
      srcInputStream = armored.byteInputStream(),
      publicKeys = PGPPublicKeyRingCollection(emptyList()),
      secretKeys = PGPSecretKeyRingCollection(emptyList()),
      protector = SecretKeyRingProtector.unprotectedKeys()
    )
    assertFalse(
      "Legitimate message wrongly rejected: ${result.exception?.message}",
      result.exception?.message?.contains("packets") == true
    )
  }

  @Test
  fun processMimeMessageRendersBoundedErrorForBomb() {
    val armored = armoredMessage(signatureCount = 2_000, literalText = "secret")
    val rawMessage = (
      "From: attacker@example.com\r\nTo: victim@example.com\r\n" +
        "Subject: test\r\nContent-Type: text/plain; charset=utf-8\r\n\r\n$armored"
      ).toByteArray(StandardCharsets.UTF_8)
    val mime = MimeMessage(Session.getInstance(Properties()), ByteArrayInputStream(rawMessage))

    val result = PgpMsg.processMimeMessage(
      mime,
      verificationPublicKeys = PGPPublicKeyRingCollection(emptyList()),
      secretKeys = PGPSecretKeyRingCollection(emptyList()),
      protector = SecretKeyRingProtector.unprotectedKeys()
    )

    assertTrue(
      "Expected a decrypt-error block, got ${result.blocks.map { it.type }}",
      result.blocks.any { it.type == MsgBlock.Type.DECRYPT_ERROR }
    )
    assertFalse(
      "Bomb plaintext must not be rendered",
      result.text.contains("secret")
    )
  }

  private fun assertRejected(armored: String, expectedMessage: String) {
    try {
      PgpPacketBudgetGuard.guard(armored.byteInputStream())
      fail("Expected an over-budget rejection")
    } catch (e: PgpPacketBudgetGuard.BudgetExceededException) {
      assertTrue(
        "Unexpected message: ${e.message}",
        e.message?.contains(expectedMessage, ignoreCase = true) == true
      )
    }
  }

  private fun armoredMessage(
    signatureCount: Int = 0,
    literalText: String? = null,
    compress: Boolean = true,
    nestedCompression: Boolean = false
  ): String {
    val inner = ByteArrayOutputStream()
    repeat(signatureCount) { writePacket(inner, PacketTags.SIGNATURE, byteArrayOf(0, 1, 2, 3)) }
    literalText?.let { text ->
      val body = ByteArrayOutputStream().apply {
        write('b'.code) // 'b' = binary literal data
        write(0) // no file name
        repeat(4) { write(0) } // timestamp
        write(text.toByteArray(StandardCharsets.UTF_8))
      }
      writePacket(inner, PacketTags.LITERAL_DATA, body.toByteArray())
    }
    return armoredRaw(inner.toByteArray(), compress, nestedCompression)
  }

  private fun armoredRaw(
    data: ByteArray,
    compress: Boolean,
    nestedCompression: Boolean = false
  ): String {
    var payload = data
    if (nestedCompression) payload = compress(payload)
    if (compress) payload = compress(payload)

    return armorRaw(payload)
  }

  private fun armorRaw(payload: ByteArray): String {
    val armored = ByteArrayOutputStream()
    ArmoredOutputStream(armored).use { it.write(payload) }
    return String(armored.toByteArray(), StandardCharsets.US_ASCII)
  }

  private fun compress(data: ByteArray): ByteArray {
    val output = ByteArrayOutputStream()
    val generator = PGPCompressedDataGenerator(CompressionAlgorithmTags.ZIP)
    generator.open(output).use { it.write(data) }
    generator.close()
    return output.toByteArray()
  }

  private fun writePacket(output: java.io.OutputStream, tag: Int, body: ByteArray) {
    output.write(0xC0 or tag)
    when {
      body.size < 192 -> output.write(body.size)
      body.size < 8384 -> {
        val length = body.size - 192
        output.write((length shr 8) + 192)
        output.write(length and 0xFF)
      }

      else -> {
        output.write(0xFF)
        output.write(body.size shr 24)
        output.write(body.size shr 16)
        output.write(body.size shr 8)
        output.write(body.size)
      }
    }
    output.write(body)
  }
}
