/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.extensions.createFileWithGivenSizeAndRandomData
import com.flowcrypt.email.util.exception.DecryptionException
import org.apache.commons.io.FileUtils
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.junit.Assert
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.BeforeClass
import org.junit.Ignore
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
import java.util.*

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
  @Ignore("need to add realization")
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
  @Ignore("need to add realization")
  fun testDecryptionErrorKeyMismatch() {

  }

  @Test
  fun testDecryptionErrorWrongPassphrase() {
    val srcFile = temporaryFolder.createFileWithGivenSizeAndRandomData(FileUtils.ONE_MB)
    val sourceBytes = srcFile.readBytes()
    val outputStreamForEncryptedSource = ByteArrayOutputStream()
    PgpEncrypt.encryptAndOrSign(
      srcInputStream = ByteArrayInputStream(sourceBytes),
      destOutputStream = outputStreamForEncryptedSource,
      pgpPublicKeyRingCollection = PGPPublicKeyRingCollection(
        listOf(
          KeyRingUtils.publicKeyRingFrom(senderPGPSecretKeyRing),
          KeyRingUtils.publicKeyRingFrom(receiverPGPSecretKeyRing)
        )
      ),
      doArmor = false
    )

    val encryptedBytes = outputStreamForEncryptedSource.toByteArray()
    val outputStreamWithDecryptedData = ByteArrayOutputStream()
    val exception = Assert.assertThrows(DecryptionException::class.java) {
      PgpDecrypt.decrypt(
        srcInputStream = ByteArrayInputStream(encryptedBytes),
        destOutputStream = outputStreamWithDecryptedData,
        pgpSecretKeyRingCollection = PGPSecretKeyRingCollection(listOf(receiverPGPSecretKeyRing)),
        protector = PasswordBasedSecretKeyRingProtector.forKey(
          receiverPGPSecretKeyRing,
          Passphrase.fromPassword(UUID.randomUUID().toString())
        )
      )
    }

    Assert.assertEquals(
      exception.decryptionErrorType,
      PgpDecrypt.DecryptionErrorType.WRONG_PASSPHRASE
    )
  }

  @Test
  @Ignore("need to add realization")
  fun testDecryptionErrorNoMdc() {

  }

  @Test
  @Ignore("need to add realization")
  fun testDecryptionErrorBadMdc() {

  }

  @Test
  @Ignore("need to add realization")
  fun testDecryptionErrorFormat() {

  }

  @Test
  @Ignore("need to add realization")
  fun testDecryptionErrorOther() {

  }

  @Test
  fun testPatternToDetectEncryptedAtts() {
    //"(?i)(\\.pgp$)|(\\.gpg$)|(\\.[a-zA-Z0-9]{3,4}\\.asc$)"
    assertNotNull(PgpDecrypt.DETECT_SEPARATE_ENCRYPTED_ATTACHMENTS_PATTERN.find("file.pgp"))
    assertNotNull(PgpDecrypt.DETECT_SEPARATE_ENCRYPTED_ATTACHMENTS_PATTERN.find("file.PgP"))
    assertNotNull(PgpDecrypt.DETECT_SEPARATE_ENCRYPTED_ATTACHMENTS_PATTERN.find("file.gpg"))
    assertNotNull(PgpDecrypt.DETECT_SEPARATE_ENCRYPTED_ATTACHMENTS_PATTERN.find("file.gPg"))
    assertNotNull(PgpDecrypt.DETECT_SEPARATE_ENCRYPTED_ATTACHMENTS_PATTERN.find("d.fs12.asc"))
    assertNotNull(PgpDecrypt.DETECT_SEPARATE_ENCRYPTED_ATTACHMENTS_PATTERN.find("d.fs12.ASC"))
    assertNotNull(PgpDecrypt.DETECT_SEPARATE_ENCRYPTED_ATTACHMENTS_PATTERN.find("d.s12.asc"))
    assertNotNull(PgpDecrypt.DETECT_SEPARATE_ENCRYPTED_ATTACHMENTS_PATTERN.find("d.ft2.ASC"))
    assertNull(PgpDecrypt.DETECT_SEPARATE_ENCRYPTED_ATTACHMENTS_PATTERN.find("filepgp"))
    assertNull(PgpDecrypt.DETECT_SEPARATE_ENCRYPTED_ATTACHMENTS_PATTERN.find("filePgP"))
    assertNull(PgpDecrypt.DETECT_SEPARATE_ENCRYPTED_ATTACHMENTS_PATTERN.find("filegpg"))
    assertNull(PgpDecrypt.DETECT_SEPARATE_ENCRYPTED_ATTACHMENTS_PATTERN.find("filegPg"))
    assertNull(PgpDecrypt.DETECT_SEPARATE_ENCRYPTED_ATTACHMENTS_PATTERN.find("d.fs12asc"))
    assertNull(PgpDecrypt.DETECT_SEPARATE_ENCRYPTED_ATTACHMENTS_PATTERN.find("d.fs12ASC"))
    assertNull(PgpDecrypt.DETECT_SEPARATE_ENCRYPTED_ATTACHMENTS_PATTERN.find("d.s12asc"))
    assertNull(PgpDecrypt.DETECT_SEPARATE_ENCRYPTED_ATTACHMENTS_PATTERN.find("d.ft2ASC"))
  }

  private fun testDecryptFileSuccess(shouldSrcBeArmored: Boolean) {
    val srcFile = temporaryFolder.createFileWithGivenSizeAndRandomData(FileUtils.ONE_MB)
    val sourceBytes = srcFile.readBytes()
    val outputStreamForEncryptedSource = ByteArrayOutputStream()
    PgpEncrypt.encryptAndOrSign(
      srcInputStream = ByteArrayInputStream(sourceBytes),
      destOutputStream = outputStreamForEncryptedSource,
      pgpPublicKeyRingCollection = PGPPublicKeyRingCollection(
        listOf(
          KeyRingUtils.publicKeyRingFrom(senderPGPSecretKeyRing),
          KeyRingUtils.publicKeyRingFrom(receiverPGPSecretKeyRing)
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