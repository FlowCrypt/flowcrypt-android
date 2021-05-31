/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *     Ivan Pizhenko
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.api.retrofit.response.model.node.GenericMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock
import com.flowcrypt.email.extensions.kotlin.normalizeEol
import com.flowcrypt.email.extensions.kotlin.removeUtf8Bom
import com.google.gson.JsonParser
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pgpainless.PGPainless
import org.pgpainless.util.Passphrase
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

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

    @Suppress("SameParameterValue")
    private fun decodeString(s: String, charsetName: String): String {
      val bytes = s.substring(1).split('=').map { Integer.parseInt(it, 16).toByte() }.toByteArray()
      return String(bytes, Charset.forName(charsetName))
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

    private fun loadComplexMessage(file: String): String {
      return loadResourceAsString("complex_messages/$file")
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
    assertTrue("Missing MDC not detected", r.error!!.type == PgpDecrypt.DecryptionErrorType.NO_MDC)
  }

  @Test // ok
  fun badMdcTest() {
    val r = processMessage("decrypt - [security] mdc - modification detected - error")
    assertTrue("Message is returned when should not", r.content == null)
    assertTrue("Error not returned", r.error != null)
    assertTrue("Bad MDC not detected", r.error!!.type == PgpDecrypt.DecryptionErrorType.BAD_MDC)
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
      r.error!!.type == PgpDecrypt.DecryptionErrorType.WRONG_PASSPHRASE
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
      r.error!!.type == PgpDecrypt.DecryptionErrorType.NEED_PASSPHRASE
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
      r.error!!.type == PgpDecrypt.DecryptionErrorType.KEY_MISMATCH
    )
  }

  // -------------------------------------------------------------------------------------------

  // Use this one for debugging
  @Test
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

  // -------------------------------------------------------------------------------------------

  @Test
  fun multipleComplexMessagesTest() {
    val testFiles = listOf(
      "decrypt - [enigmail] basic html-0.json",
      "decrypt - [gnupg v2] thai text-0.json",
      "decrypt - [gnupg v2] thai text in html-0.json",
      "decrypt - [gpgmail] signed message will get parsed and rendered " +
          "(though verification fails, enigmail does the same)-0.json",
      "decrypt - protonmail - auto TOFU load matching pubkey first time-0.json",
      "decrypt - protonmail - load pubkey into contact + verify detached msg-0.json",
      "decrypt - protonmail - load pubkey into contact + verify detached msg-1.json",
      "decrypt - protonmail - load pubkey into contact + verify detached msg-2.json",
      "decrypt - [symantec] base64 german umlauts-0.json",
      "decrypt - [thunderbird] unicode chinese-0.json",
      "decrypt - verify encrypted+signed message-0.json",
      "verify - Kraken - urldecode signature-0.json"
    )

    for (testFile in testFiles) {
      checkComplexMessage(testFile)
    }
  }

  // Use this one for debugging
  @Test
  fun singleComplexMessageTest() {
    val testFile = "verify - Kraken - urldecode signature-0.json"
    checkComplexMessage(testFile)
  }

  private fun checkComplexMessage(fileName: String) {
    println("\n*** Processing '$fileName'")

    val rootObject = JsonParser.parseString(loadComplexMessage(fileName)).asJsonObject
    val inputMsg = Base64.getDecoder().decode(rootObject["in"].asJsonObject["mimeMsg"].asString)
    val out = rootObject["out"].asJsonObject
    val expectedBlocks = out["blocks"].asJsonArray
    val from = InternetAddress(out["from"].asString)
    val to = out["to"].asJsonArray.map { InternetAddress(it.asString) }.toTypedArray()
    val session = Session.getInstance(Properties())
    val mimeMessage = MimeMessage(session, inputMsg.inputStream())
    val mimeContent = PgpMsg.decodeMimeMessage(mimeMessage)
    val processed = PgpMsg.processDecodedMimeMessage(mimeContent)

    assertEquals(1, processed.from?.size ?: 0)
    assertEquals(from, processed.from!![0])
    assertArrayEquals(to, processed.to)
    assertEquals(expectedBlocks.size(), processed.blocks.size)

    for (i in processed.blocks.indices) {
      println("Checking block #$i of the '$fileName'")

      val expectedBlock = expectedBlocks[i].asJsonObject
      val expectedBlockType = MsgBlock.Type.ofSerializedName(expectedBlock["type"].asString)
      val expectedContent = expectedBlock["content"].asString.normalizeEol()
      val expectedComplete = expectedBlock["complete"].asBoolean
      val actualBlock = processed.blocks[i]
      val actualContent = (actualBlock.content ?: "").normalizeEol().removeUtf8Bom()

      assertEquals(expectedBlockType, actualBlock.type)
      assertEquals(expectedComplete, actualBlock.complete)
      assertEquals(expectedContent, actualContent)

      if (
        actualBlock.type == MsgBlock.Type.SIGNED_TEXT
        || actualBlock.type == MsgBlock.Type.SIGNED_HTML
      ) {
        val expectedSignature = expectedBlock["signature"].asString.normalizeEol()
        val actualSignature = ((actualBlock as GenericMsgBlock).signature ?: "").normalizeEol()
        assertEquals(expectedSignature, actualSignature)
      }
    }
  }
}
