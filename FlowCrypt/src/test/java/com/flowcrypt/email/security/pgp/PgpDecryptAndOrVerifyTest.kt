/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.core.msg.RawBlockParser
import com.flowcrypt.email.extensions.createFileWithRandomData
import com.flowcrypt.email.extensions.kotlin.toInputStream
import com.flowcrypt.email.util.TestUtil
import com.flowcrypt.email.util.exception.DecryptionException
import org.apache.commons.io.FileUtils
import org.bouncycastle.bcpg.KeyIdentifier
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.pgpainless.PGPainless
import org.pgpainless.bouncycastle.extensions.certificate
import org.pgpainless.key.protection.KeyRingProtectionSettings
import org.pgpainless.key.protection.PasswordBasedSecretKeyRingProtector
import org.pgpainless.key.protection.SecretKeyRingProtector
import org.pgpainless.key.protection.passphrase_provider.SecretKeyPassphraseProvider
import org.pgpainless.util.Passphrase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.UUID

/**
 * @author Denys Bondarenko
 */
class PgpDecryptAndOrVerifyTest {

  @get:Rule
  val temporaryFolder: TemporaryFolder = TemporaryFolder()

  @Test
  fun multipleDecryptionTest() {
    listOf(
      "decrypt - without a subject",
      "decrypt - [enigmail] encrypted iso-2022-jp pgp-mime",
      "decrypt - [enigmail] encrypted iso-2022-jp, plain text",
      "decrypt - [gpg] signed fully armored message"
    ).forEach { key ->
      val decryptionResult = processMessage(key)
      assertNotNull("Message not returned", decryptionResult.content)
      findMessage(key).run {
        checkContent(
          expected = content,
          actual = requireNotNull(decryptionResult.content?.toByteArray()),
          charset = charset
        )
      }
    }
  }

  @Test
  fun missingMdcTest() {
    val decryptionResult = processMessage("decrypt - [security] mdc - missing - error")
    assertNull("Message is returned when should not", decryptionResult.content)
    assertNotNull("Error not returned", decryptionResult.exception)
    assertTrue(
      "Missing MDC not detected",
      decryptionResult.exception?.decryptionErrorType == PgpDecryptAndOrVerify.DecryptionErrorType.NO_MDC
    )
  }

  @Test
  fun badMdcTest() {
    val decryptionResult =
      processMessage("decrypt - [security] mdc - modification detected - error")
    assertNull("Message is returned when should not", decryptionResult.content)
    assertNotNull("Error not returned", decryptionResult.exception)
    assertTrue(
      "Bad MDC not detected",
      decryptionResult.exception?.decryptionErrorType == PgpDecryptAndOrVerify.DecryptionErrorType.BAD_MDC
    )
  }

  @Test
  //https://github.com/FlowCrypt/flowcrypt-android/issues/1214
  //TODO: Should there be any error?
  fun decryptionTestIssue1214() {
    val decryptionResult = processMessage(
      "decrypt - [everdesk] message encrypted for sub but claims encryptedFor-primary,sub"
    )
    assertNotNull("Message not returned", decryptionResult.content)
    assertNull("Error returned", decryptionResult.exception)
  }

  @Test
  fun missingArmorChecksumTest() {
    // This is a test for the message with missing armor checksum - different from MDC.
    // Usually the four digits at the and like p3Fc=.
    // Such messages are still valid if this is missing,
    // and should decrypt correctly - so it's good as is.
    val decryptionResult = processMessage("decrypt - encrypted missing checksum")
    assertNotNull("Message not returned", decryptionResult.content)
    assertNull("Error returned", decryptionResult.exception)
  }

  @Test
  fun wrongArmorChecksumTest() {
    val decryptionResult = processMessage("decrypt - issue 1347 - wrong checksum")
    assertNotNull("Error not returned", decryptionResult.exception != null)
    assertEquals(
      PgpDecryptAndOrVerify.DecryptionErrorType.FORMAT,
      decryptionResult.exception?.decryptionErrorType
    )
  }

  @Test
  fun wrongPassphraseTest() {
    val messageInfo = findMessage("decrypt - without a subject")
    val wrongPassphrase = Passphrase.fromPassword("this is wrong passphrase for sure")
    val privateKeysWithWrongPassPhrases = FLOWCRYPT_COMPATIBILITY_PRIVATE_KEYS.map {
      TestKeys.KeyWithPassPhrase(keyRing = it.keyRing, passphrase = wrongPassphrase)
    }
    val decryptionResult = PgpDecryptAndOrVerify.decryptAndOrVerifyWithResult(
      srcInputStream = messageInfo.armored.byteInputStream(),
      publicKeys = PGPPublicKeyRingCollection(emptyList()),
      secretKeys = PGPSecretKeyRingCollection(privateKeysWithWrongPassPhrases.map { it.keyRing }),
      protector = SecretKeyRingProtector.unprotectedKeys()
    )
    assertNull("Message returned", decryptionResult.content)
    assertNotNull("Error not returned", decryptionResult.exception)
    assertTrue(
      "Wrong passphrase not detected",
      decryptionResult.exception?.decryptionErrorType == PgpDecryptAndOrVerify.DecryptionErrorType.WRONG_PASSPHRASE
    )
  }

  @Test
  fun missingPassphraseTest() {
    val messageInfo = findMessage("decrypt - without a subject")
    val decryptionResult = PgpDecryptAndOrVerify.decryptAndOrVerifyWithResult(
      srcInputStream = messageInfo.armored.byteInputStream(),
      publicKeys = PGPPublicKeyRingCollection(emptyList()),
      secretKeys = PGPSecretKeyRingCollection(FLOWCRYPT_COMPATIBILITY_PRIVATE_KEYS.map { it.keyRing }),
      protector = SecretKeyRingProtector.unprotectedKeys()
    )
    assertNull("Message returned", decryptionResult.content)
    assertNotNull("Error returned", decryptionResult.exception)
    assertTrue(
      "Missing passphrase not detected",
      decryptionResult.exception?.decryptionErrorType == PgpDecryptAndOrVerify.DecryptionErrorType.WRONG_PASSPHRASE
    )
  }

  @Test
  fun wrongKeyTest() {
    val messageInfo = findMessage("decrypt - without a subject")
    val wrongKey = listOf(FLOWCRYPT_COMPATIBILITY_PRIVATE_KEYS[1])
    val decryptionResult = PgpDecryptAndOrVerify.decryptAndOrVerifyWithResult(
      messageInfo.armored.byteInputStream(),
      publicKeys = PGPPublicKeyRingCollection(emptyList()),
      secretKeys = PGPSecretKeyRingCollection(wrongKey.map { it.keyRing }),
      protector = SecretKeyRingProtector.unprotectedKeys()
    )
    assertNull("Message returned", decryptionResult.content)
    assertNotNull("Error not returned", decryptionResult.exception)
    assertTrue(
      "Key mismatch not detected",
      decryptionResult.exception?.decryptionErrorType == PgpDecryptAndOrVerify.DecryptionErrorType.KEY_MISMATCH
    )
  }

  @Test
  fun singleDecryptionTest() {
    val key = "decrypt - [enigmail] encrypted iso-2022-jp, plain text"
    val decryptionResult = processMessage(key)
    assertNotNull("Message not returned", decryptionResult.content)
    findMessage(key).run {
      checkContent(
        expected = content,
        actual = requireNotNull(decryptionResult.content?.toByteArray()),
        charset = charset
      )
    }
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
    val srcFile = temporaryFolder.createFileWithRandomData(fileSizeInBytes = FileUtils.ONE_MB)
    val sourceBytes = srcFile.readBytes()
    val outputStreamForEncryptedSource = ByteArrayOutputStream()
    PgpEncryptAndOrSign.encryptAndOrSign(
      srcInputStream = ByteArrayInputStream(sourceBytes),
      destOutputStream = outputStreamForEncryptedSource,
      pgpPublicKeyRingCollection = PGPPublicKeyRingCollection(
        listOf(
          senderPGPSecretKeyRing.certificate,
          receiverPGPSecretKeyRing.certificate
        )
      ),
      doArmor = false
    )

    val encryptedBytes = outputStreamForEncryptedSource.toByteArray()
    val outputStreamWithDecryptedData = ByteArrayOutputStream()
    val randomKey = PGPainless.getInstance().generateKey()
      .simpleEcKeyRing("random@flowcrypt.test", "qwerty").pgpKeyRing

    val exception = Assert.assertThrows(DecryptionException::class.java) {
      PgpDecryptAndOrVerify.decrypt(
        srcInputStream = ByteArrayInputStream(encryptedBytes),
        destOutputStream = outputStreamWithDecryptedData,
        secretKeys = PGPSecretKeyRingCollection(listOf(randomKey)),
        protector = PasswordBasedSecretKeyRingProtector.forKey(
          randomKey,
          Passphrase.fromPassword("qwerty")
        )
      )
    }

    assertEquals(
      exception.decryptionErrorType,
      PgpDecryptAndOrVerify.DecryptionErrorType.KEY_MISMATCH
    )
  }

  @Test
  fun testDecryptionErrorWrongPassphrase() {
    val srcFile = temporaryFolder.createFileWithRandomData(fileSizeInBytes = FileUtils.ONE_MB)
    val sourceBytes = srcFile.readBytes()
    val outputStreamForEncryptedSource = ByteArrayOutputStream()
    PgpEncryptAndOrSign.encryptAndOrSign(
      srcInputStream = ByteArrayInputStream(sourceBytes),
      destOutputStream = outputStreamForEncryptedSource,
      pgpPublicKeyRingCollection = PGPPublicKeyRingCollection(
        listOf(
          senderPGPSecretKeyRing.certificate,
          receiverPGPSecretKeyRing.certificate
        )
      ),
      doArmor = false
    )

    val encryptedBytes = outputStreamForEncryptedSource.toByteArray()
    val outputStreamWithDecryptedData = ByteArrayOutputStream()
    val exception = Assert.assertThrows(DecryptionException::class.java) {
      PgpDecryptAndOrVerify.decrypt(
        srcInputStream = ByteArrayInputStream(encryptedBytes),
        destOutputStream = outputStreamWithDecryptedData,
        secretKeys = PGPSecretKeyRingCollection(listOf(receiverPGPSecretKeyRing)),
        protector = PasswordBasedSecretKeyRingProtector.forKey(
          receiverPGPSecretKeyRing,
          Passphrase.fromPassword(UUID.randomUUID().toString())
        )
      )
    }

    assertEquals(
      exception.decryptionErrorType,
      PgpDecryptAndOrVerify.DecryptionErrorType.WRONG_PASSPHRASE
    )
  }

  @Test
  fun testPatternToDetectEncryptedAtts() {
    //"(?i)(\\.pgp$)|(\\.gpg$)|(\\.[a-zA-Z0-9]{3,4}\\.asc$)"
    assertNotNull(RawBlockParser.ENCRYPTED_FILE_REGEX.find("file.pgp"))
    assertNotNull(RawBlockParser.ENCRYPTED_FILE_REGEX.find("file.PgP"))
    assertNotNull(RawBlockParser.ENCRYPTED_FILE_REGEX.find("file.gpg"))
    assertNotNull(RawBlockParser.ENCRYPTED_FILE_REGEX.find("file.gPg"))
    assertNotNull(RawBlockParser.ENCRYPTED_FILE_REGEX.find("d.fs12.asc"))
    assertNotNull(RawBlockParser.ENCRYPTED_FILE_REGEX.find("d.fs12.ASC"))
    assertNotNull(RawBlockParser.ENCRYPTED_FILE_REGEX.find("d.s12.asc"))
    assertNotNull(RawBlockParser.ENCRYPTED_FILE_REGEX.find("d.ft2.ASC"))
    assertNull(RawBlockParser.ENCRYPTED_FILE_REGEX.find("filepgp"))
    assertNull(RawBlockParser.ENCRYPTED_FILE_REGEX.find("filePgP"))
    assertNull(RawBlockParser.ENCRYPTED_FILE_REGEX.find("filegpg"))
    assertNull(RawBlockParser.ENCRYPTED_FILE_REGEX.find("filegPg"))
    assertNull(RawBlockParser.ENCRYPTED_FILE_REGEX.find("d.fs12asc"))
    assertNull(RawBlockParser.ENCRYPTED_FILE_REGEX.find("d.fs12ASC"))
    assertNull(RawBlockParser.ENCRYPTED_FILE_REGEX.find("d.s12asc"))
    assertNull(RawBlockParser.ENCRYPTED_FILE_REGEX.find("d.ft2ASC"))
  }

  @Test
  fun testDecryptMsgUnescapedSpecialCharactersInEncryptedText() {
    val text = TestUtil.readResourceAsString("pgp/messages/direct-encrypted-text-special-chars.txt")
    val keys = TestKeys.KEYS["rsa1"]!!.listOfKeysWithPassPhrase

    val decryptResult = PgpDecryptAndOrVerify.decryptAndOrVerifyWithResult(
      text.toInputStream(),
      publicKeys = PGPPublicKeyRingCollection(emptyList()),
      secretKeys = PGPSecretKeyRingCollection(keys.map { it.keyRing }),
      protector = TestKeys.genRingProtector(keys)
    )
    assertEquals(true, decryptResult.isEncrypted)
    assertEquals(
      "> special <tag> & other\n> second line",
      String(decryptResult.content?.toByteArray() ?: byteArrayOf())
    )
  }

  private fun testDecryptFileSuccess(shouldSrcBeArmored: Boolean) {
    val srcFile = temporaryFolder.createFileWithRandomData(fileSizeInBytes = FileUtils.ONE_MB)
    val sourceBytes = srcFile.readBytes()
    val outputStreamForEncryptedSource = ByteArrayOutputStream()
    PgpEncryptAndOrSign.encryptAndOrSign(
      srcInputStream = ByteArrayInputStream(sourceBytes),
      destOutputStream = outputStreamForEncryptedSource,
      pgpPublicKeyRingCollection = PGPPublicKeyRingCollection(
        listOf(
          senderPGPSecretKeyRing.certificate,
          receiverPGPSecretKeyRing.certificate
        )
      ),
      doArmor = shouldSrcBeArmored
    )

    val encryptedBytes = outputStreamForEncryptedSource.toByteArray()
    val outputStreamWithDecryptedData = ByteArrayOutputStream()
    PgpDecryptAndOrVerify.decrypt(
      srcInputStream = ByteArrayInputStream(encryptedBytes),
      destOutputStream = outputStreamWithDecryptedData,
      secretKeys = PGPSecretKeyRingCollection(listOf(receiverPGPSecretKeyRing)),
      protector = allPredefinedKeysProtector
    )

    val decryptedBytesForSender = outputStreamWithDecryptedData.toByteArray()
    Assert.assertArrayEquals(sourceBytes, decryptedBytesForSender)
  }

  private fun processMessage(messageKey: String): PgpDecryptAndOrVerify.DecryptionResult {
    val messageInfo = findMessage(messageKey)
    val result = PgpDecryptAndOrVerify.decryptAndOrVerifyWithResult(
      messageInfo.armored.byteInputStream(),
      publicKeys = PGPPublicKeyRingCollection(emptyList()),
      secretKeys = PGPSecretKeyRingCollection(FLOWCRYPT_COMPATIBILITY_PRIVATE_KEYS.map { it.keyRing }),
      protector = secretKeyRingProtectorFlowcryptCompatibility
    )
    return result
  }

  private fun checkContent(expected: List<String>, actual: ByteArray, charset: String) {
    val actualText = String(actual, Charset.forName(charset))
    for (text in expected) {
      assertTrue("Text '$text' not found", actualText.indexOf(text) != -1)
    }
  }

  private data class MessageInfo(
    val key: String,
    val content: List<String>,
    val quoted: Boolean? = null,
    val charset: String = "UTF-8"
  ) {
    val armored: String by lazy { TestUtil.readResourceAsString("pgp/messages/$key.txt") }
  }

  companion object {
    private lateinit var senderPGPSecretKeyRing: PGPSecretKeyRing
    private lateinit var receiverPGPSecretKeyRing: PGPSecretKeyRing
    private lateinit var allPredefinedKeysProtector: PasswordBasedSecretKeyRingProtector

    private const val SENDER_PASSWORD = "qwerty1234"
    private const val RECEIVER_PASSWORD = "password1234"

    private val FLOWCRYPT_COMPATIBILITY_PRIVATE_KEYS = listOf(
      TestKeys.KeyWithPassPhrase(
        passphrase = Passphrase.fromPassword("flowcrypt compatibility tests"),
        keyRing = requireNotNull(loadSecretKey("key0.txt")),
      ),

      TestKeys.KeyWithPassPhrase(
        passphrase = Passphrase.fromPassword("flowcrypt compatibility tests 2"),
        keyRing = requireNotNull(loadSecretKey("key1.txt"))
      )
    )

    private val secretKeyRingProtectorFlowcryptCompatibility =
      TestKeys.genRingProtector(FLOWCRYPT_COMPATIBILITY_PRIVATE_KEYS)

    private fun loadSecretKey(fileName: String): PGPSecretKeyRing? {
      return PGPainless.getInstance().readKey().parseKey(
        (TestUtil.readResourceAsString("pgp/keys/$fileName"))
      ).pgpKeyRing
    }

    private val MESSAGES = listOf(
      MessageInfo(
        key = "decrypt - without a subject",
        content = listOf("This is a compatibility test email")
      ),

      MessageInfo(
        key = "decrypt - [security] mdc - missing - error",
        content = listOf("Security threat!", "MDC", "Display the message at your own risk."),
      ),

      MessageInfo(
        key = "decrypt - [security] mdc - modification detected - error",
        content = listOf(
          "Security threat - opening this message is dangerous because it was modified" +
              " in transit."
        ),
      ),

      MessageInfo(
        key = "decrypt - [everdesk] message encrypted for sub but claims encryptedFor-primary,sub",
        content = listOf("this is a sample for FlowCrypt compatibility")
      ),

      MessageInfo(
        key = "decrypt - [gpg] signed fully armored message",
        content = listOf(
          "this was encrypted with gpg",
          "gpg --sign --armor -r flowcrypt.compatibility@gmail.com ./text.txt"
        ),
        quoted = false
      ),

      MessageInfo(
        key = "decrypt - encrypted missing checksum",
        content = listOf("400 library systems in 177 countries worldwide")
      ),

      MessageInfo(
        key = "decrypt - [enigmail] encrypted iso-2022-jp pgp-mime",
        content = listOf("=E3=82=BE=E3=81=97=E9=80=B8=E7=8F=BE"), // part of "ゾし逸現飲"
        charset = "ISO-2022-JP",
      ),

      MessageInfo(
        key = "decrypt - [enigmail] encrypted iso-2022-jp, plain text",
        content = listOf(
          // complete string "ゾし逸現飲"
          TestUtil.decodeString("=E3=82=BE=E3=81=97=E9=80=B8=E7=8F=BE=E9=A3=B2", "UTF-8")
        ),
        charset = "ISO-2022-JP",
      ),

      MessageInfo(
        key = "decrypt - issue 1347 - wrong checksum",
        content = listOf("")
      ),
    )

    private fun findMessage(key: String): MessageInfo {
      return MESSAGES.firstOrNull { it.key == key }
        ?: throw IllegalArgumentException("Message '$key' not found")
    }

    @BeforeClass
    @JvmStatic
    fun setUp() {
      senderPGPSecretKeyRing = PGPainless.getInstance().generateKey()
        .simpleEcKeyRing("sender@flowcrypt.test", SENDER_PASSWORD).pgpKeyRing
      receiverPGPSecretKeyRing = PGPainless.getInstance().generateKey()
        .simpleEcKeyRing("receiver@flowcrypt.test", RECEIVER_PASSWORD).pgpKeyRing

      val passphraseProvider = object : SecretKeyPassphraseProvider {
        override fun getPassphraseFor(keyId: Long): Passphrase? {
          return doGetPassphrase(keyId)
        }

        override fun getPassphraseFor(keyIdentifier: KeyIdentifier): Passphrase? {
          return getPassphraseFor(keyIdentifier.keyId)
        }

        override fun hasPassphrase(keyId: Long): Boolean {
          return doGetPassphrase(keyId) != null
        }

        override fun hasPassphrase(keyIdentifier: KeyIdentifier): Boolean {
          return hasPassphrase(keyIdentifier.keyId)
        }

        private fun doGetPassphrase(keyId: Long?): Passphrase? {
          senderPGPSecretKeyRing.publicKeys.forEach { publicKey ->
            if (publicKey.keyID == keyId) {
              return Passphrase.fromPassword(SENDER_PASSWORD)
            }
          }
          receiverPGPSecretKeyRing.publicKeys.forEach { publicKey ->
            if (publicKey.keyID == keyId) {
              return Passphrase.fromPassword(RECEIVER_PASSWORD)
            }
          }
          return null
        }
      }
      allPredefinedKeysProtector = PasswordBasedSecretKeyRingProtector(
        KeyRingProtectionSettings.secureDefaultSettings(),
        passphraseProvider
      )
    }
  }
}
