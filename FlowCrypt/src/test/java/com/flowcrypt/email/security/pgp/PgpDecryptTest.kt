/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.extensions.createFileWithGivenSizeAndRandomData
import org.apache.commons.io.FileUtils
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.pgpainless.PGPainless
import org.pgpainless.key.protection.KeyRingProtectionSettings
import org.pgpainless.key.protection.PasswordBasedSecretKeyRingProtector
import org.pgpainless.key.util.KeyRingUtils
import org.pgpainless.util.Passphrase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * @author Denis Bondarenko
 * Date: 5/12/21
 * Time: 9:28 AM
 * E-mail: DenBond7@gmail.com
 */
class PgpDecryptTest {

  @get:Rule
  val temporaryFolder: TemporaryFolder = TemporaryFolder()

  @Test
  fun testDecryptJustSignedFile() {

  }

  @Test
  fun testDecryptArmoredFileSuccess() {
    testDecryptFileSuccess(true)
  }

  @Test
  fun testDecryptBinaryFileSuccess() {
    testDecryptFileSuccess(false)
  }

  @Test
  fun testDecryptionErrorKeyMismatch() {

  }

  @Test
  fun testDecryptionErrorWrongPassphrase() {

  }

  @Test
  fun testDecryptionErrorNoMdc() {

  }

  @Test
  fun testDecryptionErrorBadMdc() {

  }

  @Test
  fun testDecryptionErrorFormat() {

  }

  @Test
  fun testDecryptionErrorOther() {

  }

  private fun testDecryptFileSuccess(shouldSrcBeArmored: Boolean) {
    val senderPgpPublicKeyRing = KeyRingUtils.publicKeyRingFrom(senderPGPSecretKeyRing)
    val receiverPgpPublicKeyRing = KeyRingUtils.publicKeyRingFrom(receiverPGPSecretKeyRing)
    val srcFile = temporaryFolder.createFileWithGivenSizeAndRandomData(FileUtils.ONE_MB)
    val sourceBytes = srcFile.readBytes()
    val outputStreamForEncryptedSource = ByteArrayOutputStream()
    PgpEncrypt.encryptAndOrSign(
      srcInputStream = ByteArrayInputStream(sourceBytes),
      destOutputStream = outputStreamForEncryptedSource,
      pgpPublicKeyRingCollection = PGPPublicKeyRingCollection(
        listOf(
          senderPgpPublicKeyRing,
          receiverPgpPublicKeyRing
        )
      ),
      doArmor = shouldSrcBeArmored
    )

    val encryptedBytes = outputStreamForEncryptedSource.toByteArray()
    val outputStreamWithDecryptedData = ByteArrayOutputStream()
    PgpDecrypt.decrypt(
      srcInputStream = ByteArrayInputStream(encryptedBytes),
      destOutputStream = outputStreamWithDecryptedData,
      pgpSecretKeyRingCollection = PGPSecretKeyRingCollection(listOf(receiverPGPSecretKeyRing)),
      protector = allPredefinedKeysProtector
    )

    val decryptedBytesForSender = outputStreamWithDecryptedData.toByteArray()
    Assert.assertArrayEquals(sourceBytes, decryptedBytesForSender)
  }

  companion object {
    private lateinit var senderPGPSecretKeyRing: PGPSecretKeyRing
    private lateinit var receiverPGPSecretKeyRing: PGPSecretKeyRing
    private lateinit var allPredefinedKeysProtector: PasswordBasedSecretKeyRingProtector

    private const val SENDER_PASSWORD = "qwerty1234"
    private const val RECEIVER_PASSWORD = "password1234"

    @BeforeClass
    @JvmStatic
    fun setUp() {
      senderPGPSecretKeyRing = PGPainless.generateKeyRing()
        .simpleEcKeyRing("sender@flowcrypt.test", SENDER_PASSWORD)
      receiverPGPSecretKeyRing = PGPainless.generateKeyRing()
        .simpleEcKeyRing("receiver@flowcrypt.test", RECEIVER_PASSWORD)

      allPredefinedKeysProtector = PasswordBasedSecretKeyRingProtector(
        KeyRingProtectionSettings.secureDefaultSettings()
      ) { keyId ->
        senderPGPSecretKeyRing.publicKeys.forEach { publicKey ->
          if (publicKey.keyID == keyId) {
            return@PasswordBasedSecretKeyRingProtector Passphrase.fromPassword(SENDER_PASSWORD)
          }
        }
        receiverPGPSecretKeyRing.publicKeys.forEach { publicKey ->
          if (publicKey.keyID == keyId) {
            return@PasswordBasedSecretKeyRingProtector Passphrase.fromPassword(RECEIVER_PASSWORD)
          }
        }
        return@PasswordBasedSecretKeyRingProtector null
      }
    }
  }
}