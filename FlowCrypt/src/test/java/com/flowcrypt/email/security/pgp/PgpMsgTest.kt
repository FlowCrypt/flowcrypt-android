/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *     Ivan Pizhenko
 */

package com.flowcrypt.email.security.pgp

import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.junit.Test
import org.junit.Assert.assertTrue
import org.pgpainless.PGPainless
import org.pgpainless.util.Passphrase
import java.lang.IllegalArgumentException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class PgpMsgTest {

  private data class MessageInfo(
    val key: String,
    val content: List<String>,
    val quoted: Boolean? = null,
    val charset: String = "UTF-8"
  ) {
    val armored: String = loadMessage("$key.txt")
  }

  companion object {
    private val privateKeys = listOf(
      PgpMsg.KeyWithPassPhrase(
        passphrase = Passphrase.fromPassword("flowcrypt compatibility tests"),
        keyRing = loadSecretKey("key0.txt"),
      ),

      PgpMsg.KeyWithPassPhrase(
        passphrase = Passphrase.fromPassword("flowcrypt compatibility tests 2"),
        keyRing = loadSecretKey("key1.txt")
      )
    )

    private val messages = listOf(
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
          decodeString("=E3=82=BE=E3=81=97=E9=80=B8=E7=8F=BE=E9=A3=B2", "UTF-8")
        ),
        charset = "ISO-2022-JP",
      )
    )

    private fun decodeString(s: String, charset: String): String {
      val bytes = s.substring(1).split('=').map { Integer.parseInt(it, 16).toByte() }.toByteArray()
      return String(bytes, Charset.forName(charset))
    }

    private fun loadResource(path: String): ByteArray {
      return PgpMsgTest::class.java.classLoader!!
        .getResourceAsStream("${PgpMsgTest::class.simpleName}/$path")
        .readBytes()
    }

    private fun loadResourceAsString(path: String): String {
      return String(loadResource(path), StandardCharsets.UTF_8)
    }

    private fun loadSecretKey(file: String): PGPSecretKeyRing {
      return PGPainless.readKeyRing().secretKeyRing(loadResourceAsString("keys/$file"))
    }

    private fun loadMessage(file: String): String {
      return loadResourceAsString("messages/$file")
    }

    private fun findMessage(key: String): MessageInfo {
      return messages.firstOrNull { it.key == key }
        ?: throw IllegalArgumentException("Message '$key' not found")
    }
  }

  @Test // ok
  fun multipleDecryptionTest() {
    val keys = listOf(
      "decrypt - without a subject",
      "decrypt - [enigmail] encrypted iso-2022-jp pgp-mime",
      "decrypt - [enigmail] encrypted iso-2022-jp, plain text",
      "decrypt - [gpg] signed fully armored message"
    )
    for (key in keys) {
      println("Decrypt: '$key'")
      val r = processMessage(key)
      assertTrue("Message not returned", r.content != null)
      val messageInfo = findMessage(key)
      checkContent(
        expected = messageInfo.content,
        actual = r.content!!.toByteArray(),
        charset = messageInfo.charset
      )
    }
  }

  @Test // ok
  fun missingMdcTest() {
    val r = processMessage("decrypt - [security] mdc - missing - error")
    assertTrue("Message is returned when should not", r.content == null)
    assertTrue("Error not returned", r.error != null)
    assertTrue("Missing MDC not detected", r.error!!.type == PgpMsg.DecryptionErrorType.NO_MDC)
  }

  @Test // ok
  fun badMdcTest() {
    val r = processMessage("decrypt - [security] mdc - modification detected - error")
    assertTrue("Message is returned when should not", r.content == null)
    assertTrue("Error not returned", r.error != null)
    assertTrue("Bad MDC not detected", r.error!!.type == PgpMsg.DecryptionErrorType.BAD_MDC)
  }

  // TODO: Should there be any error?
  // https://github.com/FlowCrypt/flowcrypt-android/issues/1214
  @Test
  fun decryptionTest3() {
    val r = processMessage(
      "decrypt - [everdesk] message encrypted for sub but claims encryptedFor-primary,sub"
    )
    assertTrue("Message not returned", r.content != null)
    assertTrue("Error returned", r.error == null)
  }

  @Test
  fun missingArmorChecksumTest() {
    // This is a test for the message with missing armor checksum - different from MDC.
    // Usually the four digits at the and like p3Fc=.
    // Such messages are still valid if this is missing,
    // and should decrypt correctly - so it's good as is.
    val r = processMessage("decrypt - encrypted missing checksum")
    assertTrue("Message not returned", r.content != null)
    assertTrue("Error returned", r.error == null)
  }

  @Test
  fun wrongPassphraseTest() {
    val messageInfo = findMessage("decrypt - without a subject")
    val wrongPassphrase = Passphrase.fromPassword("this is wrong passphrase for sure")
    val privateKeysWithWrongPassPhrases = privateKeys.map {
      PgpMsg.KeyWithPassPhrase(keyRing = it.keyRing, passphrase = wrongPassphrase)
    }
    val r = PgpMsg.decrypt(
      messageInfo.armored.toByteArray(), privateKeysWithWrongPassPhrases, null
    )
    assertTrue("Message returned", r.content == null)
    assertTrue("Error not returned", r.error != null)
    assertTrue(
      "Wrong passphrase not detected",
      r.error!!.type == PgpMsg.DecryptionErrorType.WRONG_PASSPHRASE
    )
  }

  @Test
  fun missingPassphraseTest() {
    val messageInfo = findMessage("decrypt - without a subject")
    val privateKeysWithMissingPassphrases = privateKeys.map {
      PgpMsg.KeyWithPassPhrase(keyRing = it.keyRing, passphrase = null)
    }
    val r = PgpMsg.decrypt(
      messageInfo.armored.toByteArray(), privateKeysWithMissingPassphrases, null
    )
    assertTrue("Message returned", r.content == null)
    assertTrue("Error not returned", r.error != null)
    assertTrue(
      "Missing passphrase not detected",
      r.error!!.type == PgpMsg.DecryptionErrorType.NEED_PASSPHRASE
    )
  }

  @Test
  fun wrongKeyTest() {
    val messageInfo = findMessage("decrypt - without a subject")
    val wrongKey = listOf(privateKeys[1])
    val r = PgpMsg.decrypt(
      messageInfo.armored.toByteArray(), wrongKey, null
    )
    assertTrue("Message returned", r.content == null)
    assertTrue("Error not returned", r.error != null)
    assertTrue(
      "Key mismatch not detected",
      r.error!!.type == PgpMsg.DecryptionErrorType.KEY_MISMATCH
    )
  }

  // -------------------------------------------------------------------------------------------

  @Test // use this for debugging
  fun singleDecryptionTest() {
    val key = "decrypt - [enigmail] encrypted iso-2022-jp, plain text"
    val r = processMessage(key)
    assertTrue("Message not returned", r.content != null)
    val messageInfo = findMessage(key)
    checkContent(
      expected = messageInfo.content,
      actual = r.content!!.toByteArray(),
      charset = messageInfo.charset
    )
  }

  private fun processMessage(messageKey: String): PgpMsg.DecryptionResult {
    val messageInfo = findMessage(messageKey)
    val result = PgpMsg.decrypt(messageInfo.armored.toByteArray(), privateKeys, null)
    if (result.content != null) {
      val s = String(result.content!!.toByteArray(), Charset.forName(messageInfo.charset))
      println("=========\n$s\n=========")
    }
    if (result.error != null && result.error!!.cause != null) {
      println("CAUSE:")
      result.error!!.cause!!.printStackTrace(System.out)
    }
    return result
  }

  private fun checkContent(expected: List<String>, actual: ByteArray, charset: String) {
    val z = String(actual, Charset.forName(charset))
    for (s in expected) {
      assertTrue("Text '$s' not found", z.indexOf(s) != -1)
    }
  }
}
