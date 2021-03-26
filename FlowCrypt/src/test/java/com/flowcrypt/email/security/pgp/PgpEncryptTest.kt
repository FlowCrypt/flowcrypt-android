/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security.pgp

import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.junit.Assert.assertArrayEquals
import org.junit.BeforeClass
import org.junit.Test
import org.pgpainless.PGPainless
import org.pgpainless.key.protection.KeyRingProtectionSettings
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

    val sourceBytes = source.toByteArray()
    val outputStreamForEncryptedSource = ByteArrayOutputStream()
    PgpEncrypt.encrypt(
        srcInputStream = ByteArrayInputStream(sourceBytes),
        destOutputStream = outputStreamForEncryptedSource,
        pgpPublicKeyRingCollection = PGPPublicKeyRingCollection(listOf(
            senderPgpPublicKeyRing,
            recipientPgpPublicKeyRing)
        ))
    val encryptedBytes = outputStreamForEncryptedSource.toByteArray()

    val decryptedBytesForSender = decrypt(
        inputStream = ByteArrayInputStream(encryptedBytes),
        pgpPublicKeyRingCollection = PGPPublicKeyRingCollection(listOf(
            senderPgpPublicKeyRing,
            recipientPgpPublicKeyRing)
        ),
        pgpSecretKeyRing = senderPGPSecretKeyRing,
        passphrase = Passphrase.fromPassword(SENDER_PASSWORD))
        .toByteArray()
    assertArrayEquals(sourceBytes, decryptedBytesForSender)

    val decryptedBytesForReceiver = decrypt(
        inputStream = ByteArrayInputStream(encryptedBytes),
        pgpPublicKeyRingCollection = PGPPublicKeyRingCollection(listOf(
            senderPgpPublicKeyRing,
            recipientPgpPublicKeyRing)
        ),
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

    val protector = PasswordBasedSecretKeyRingProtector(
        KeyRingProtectionSettings.secureDefaultSettings()) { keyId ->
      pgpSecretKeyRing.publicKeys.forEach { publicKey ->
        if (publicKey.keyID == keyId) {
          return@PasswordBasedSecretKeyRingProtector passphrase
        }
      }
      return@PasswordBasedSecretKeyRingProtector null
    }

    val decryptionStream = PGPainless.decryptAndOrVerify()
        .onInputStream(inputStream)
        .decryptWith(protector, PGPSecretKeyRingCollection(listOf(pgpSecretKeyRing)))
        .verifyWith(pgpPublicKeyRingCollection)
        .ignoreMissingPublicKeys()
        .build()

    val outputStreamWithDecryptedData = ByteArrayOutputStream()
    decryptionStream.use { it.copyTo(outputStreamWithDecryptedData) }

    return outputStreamWithDecryptedData
  }

  companion object {
    private lateinit var senderPGPSecretKeyRing: PGPSecretKeyRing
    private lateinit var recipientPGPSecretKeyRing: PGPSecretKeyRing

    private const val SENDER_PASSWORD = "qwerty1234"
    private const val RECEIVER_PASSWORD = "password1234"

    private val source = """
    Meet the OS that’s optimized for how you use your phone.
    Helping you manage conversations. And organize your day.
    With even more tools and privacy controls that put you in charge.
    """.trimIndent()

    @BeforeClass
    @JvmStatic
    fun setUp() {
      senderPGPSecretKeyRing = PGPainless.generateKeyRing()
          .simpleEcKeyRing("sender@encrypted.key", SENDER_PASSWORD)
      recipientPGPSecretKeyRing = PGPainless.generateKeyRing()
          .simpleEcKeyRing("juliet@encrypted.key", RECEIVER_PASSWORD)
    }
  }
}