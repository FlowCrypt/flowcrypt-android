/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security.pgp

import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.bouncycastle.util.io.Streams
import org.junit.Assert.assertArrayEquals
import org.junit.BeforeClass
import org.junit.Test
import org.pgpainless.PGPainless
import org.pgpainless.key.generation.type.rsa.RsaLength
import org.pgpainless.key.protection.PasswordBasedSecretKeyRingProtector
import org.pgpainless.key.util.KeyRingUtils
import org.pgpainless.util.Passphrase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream


/**
 * @author Denis Bondarenko
 * Date: 3/17/21
 * Time: 3:59 PM
 * E-mail: DenBond7@gmail.com
 */
class PgpEncryptTest {

  @Test
  fun testEncryptFile() {
    val senderPgpPublicKeyRing = KeyRingUtils.publicKeyRingFrom(senderPGPSecretKeyRing)
    val recipientPgpPublicKeyRing = KeyRingUtils.publicKeyRingFrom(recipientPGPSecretKeyRing)

    val sourceBytes = testMessage.toByteArray()
    val outputStreamForEncryptedSource = ByteArrayOutputStream()
    PgpEncrypt.encryptFile(
        srcInputStream = ByteArrayInputStream(sourceBytes),
        destOutputStream = outputStreamForEncryptedSource,
        pgpPublicKeyRingCollection = PGPPublicKeyRingCollection(listOf(senderPgpPublicKeyRing, recipientPgpPublicKeyRing)))
    val encryptedBytes = outputStreamForEncryptedSource.toByteArray()

    val decryptedBytesForSender = decrypt(
        inputStream = ByteArrayInputStream(encryptedBytes),
        pgpPublicKeyRingCollection = PGPPublicKeyRingCollection(listOf(senderPgpPublicKeyRing, recipientPgpPublicKeyRing)),
        pgpSecretKeyRing = senderPGPSecretKeyRing,
        passphrase = Passphrase.fromPassword(SENDER_PASSWORD))
        .toByteArray()
    assertArrayEquals(sourceBytes, decryptedBytesForSender)

    val decryptedBytesForReceiver = decrypt(
        inputStream = ByteArrayInputStream(encryptedBytes),
        pgpPublicKeyRingCollection = PGPPublicKeyRingCollection(listOf(senderPgpPublicKeyRing, recipientPgpPublicKeyRing)),
        pgpSecretKeyRing = recipientPGPSecretKeyRing,
        passphrase = Passphrase.fromPassword(RECEIVER_PASSWORD))
        .toByteArray()
    assertArrayEquals(sourceBytes, decryptedBytesForReceiver)
  }

  //todo-denbond7 Replace it when we will have PgpDecrypt.decryptFile()
  private fun decrypt(inputStream: ByteArrayInputStream,
                      pgpPublicKeyRingCollection: PGPPublicKeyRingCollection,
                      pgpSecretKeyRing: PGPSecretKeyRing,
                      passphrase: Passphrase): ByteArrayOutputStream {
    val protector = PasswordBasedSecretKeyRingProtector.forKey(pgpSecretKeyRing, passphrase)

    val decryptionStream = PGPainless.decryptAndOrVerify()
        .onInputStream(inputStream)
        .decryptWith(protector, PGPSecretKeyRingCollection(listOf(pgpSecretKeyRing)))
        .verifyWith(pgpPublicKeyRingCollection)
        .ignoreMissingPublicKeys()
        .build()

    val decryptedSecretMessage = ByteArrayOutputStream()
    decryptionStream.use {
      Streams.pipeAll(it, decryptedSecretMessage)
    }

    return decryptedSecretMessage
  }

  companion object {
    private lateinit var senderPGPSecretKeyRing: PGPSecretKeyRing
    private lateinit var recipientPGPSecretKeyRing: PGPSecretKeyRing

    private const val SENDER_PASSWORD = "qwerty1234"
    private const val RECEIVER_PASSWORD = "password1234"

    private val testMessage = """
    Meet the OS that’s optimized for how you use your phone.
    Helping you manage conversations. And organize your day.
    With even more tools and privacy controls that put you in charge.
    """.trimIndent()

    @BeforeClass
    @JvmStatic
    fun setUp() {
      senderPGPSecretKeyRing = PGPainless.generateKeyRing().simpleRsaKeyRing("sender@encrypted.key", RsaLength._4096, SENDER_PASSWORD)
      recipientPGPSecretKeyRing = PGPainless.generateKeyRing().simpleRsaKeyRing("juliet@encrypted.key", RsaLength._4096, RECEIVER_PASSWORD)
    }
  }
}