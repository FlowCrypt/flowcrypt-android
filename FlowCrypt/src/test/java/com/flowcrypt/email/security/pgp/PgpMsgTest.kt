/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *     Ivan Pizhenko
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.api.retrofit.response.model.MsgBlock
import com.flowcrypt.email.core.msg.MimeUtils
import com.flowcrypt.email.core.msg.RawBlockParser
import com.flowcrypt.email.extensions.kotlin.toEscapedHtml
import com.flowcrypt.email.extensions.kotlin.toInputStream
import com.flowcrypt.email.util.TestUtil
import com.google.gson.JsonParser
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pgpainless.PGPainless
import org.pgpainless.key.protection.SecretKeyRingProtector
import org.pgpainless.util.Passphrase
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Properties
import javax.mail.Session
import javax.mail.internet.MimeMessage

class PgpMsgTest {
  private data class MessageInfo(
    val key: String,
    val content: List<String>,
    val quoted: Boolean? = null,
    val charset: String = "UTF-8"
  ) {
    val armored: String by lazy { loadResourceAsString("messages/$key.txt") }
  }

  companion object {
    private const val NEXT_MSG_BLOCK_DELIMITER = "<!-- next MsgBlock -->\n"

    private val BODY_SPLIT_REGEX = Regex("</?body>")

    private const val TEXT_SPECIAL_CHARS = "> special <tag> & other\n> second line"

    private val HTML_SPECIAL_CHARS = TEXT_SPECIAL_CHARS.toEscapedHtml()

    private val PRIVATE_KEYS = listOf(
      TestKeys.KeyWithPassPhrase(
        passphrase = Passphrase.fromPassword("flowcrypt compatibility tests"),
        keyRing = loadSecretKey("key0.txt"),
      ),

      TestKeys.KeyWithPassPhrase(
        passphrase = Passphrase.fromPassword("flowcrypt compatibility tests 2"),
        keyRing = loadSecretKey("key1.txt")
      )
    )

    private val secretKeyRingProtector = TestKeys.genRingProtector(PRIVATE_KEYS)

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

    private fun loadResourceAsString(
      path: String,
      charset: Charset = StandardCharsets.UTF_8
    ): String {
      return TestUtil.readResourceAsString(
        path = "${PgpMsgTest::class.java.simpleName}/$path",
        charset = charset
      )
    }

    private fun loadSecretKey(keyFile: String): PGPSecretKeyRing {
      return PGPainless.readKeyRing().secretKeyRing(loadResourceAsString("keys/$keyFile"))
    }
  }

  @Test
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

  @Test
  fun missingMdcTest() {
    val r = processMessage("decrypt - [security] mdc - missing - error")
    assertTrue("Message is returned when should not", r.content == null)
    assertTrue("Error not returned", r.exception != null)
    assertTrue(
      "Missing MDC not detected",
      r.exception?.decryptionErrorType == PgpDecryptAndOrVerify.DecryptionErrorType.NO_MDC
    )
  }

  @Test
  fun badMdcTest() {
    val r = processMessage("decrypt - [security] mdc - modification detected - error")
    assertTrue("Message is returned when should not", r.content == null)
    assertTrue("Error not returned", r.exception != null)
    assertTrue(
      "Bad MDC not detected",
      r.exception?.decryptionErrorType == PgpDecryptAndOrVerify.DecryptionErrorType.BAD_MDC
    )
  }

  @Test
  fun decryptionTest3() {
    val r = processMessage(
      "decrypt - [everdesk] message encrypted for sub but claims encryptedFor-primary,sub"
    )
    assertTrue("Message not returned", r.content != null)
    assertTrue("Error returned", r.exception == null)
    // TODO: Should there be any error?
    // https://github.com/FlowCrypt/flowcrypt-android/issues/1214
  }

  @Test
  fun missingArmorChecksumTest() {
    // This is a test for the message with missing armor checksum - different from MDC.
    // Usually the four digits at the and like p3Fc=.
    // Such messages are still valid if this is missing,
    // and should decrypt correctly - so it's good as is.
    val r = processMessage("decrypt - encrypted missing checksum")
    assertTrue("Message not returned", r.content != null)
    assertTrue("Error returned", r.exception == null)
  }

  @Test
  // @Ignore("BC ArmoredInputStream issue")
  fun wrongArmorChecksumTest() {
    val r = processMessage("decrypt - issue 1347 - wrong checksum")
    assertTrue("Error not returned", r.exception != null)
    assertEquals(PgpDecryptAndOrVerify.DecryptionErrorType.FORMAT, r.exception?.decryptionErrorType)
  }

  @Test
  fun wrongPassphraseTest() {
    val messageInfo = findMessage("decrypt - without a subject")
    val wrongPassphrase = Passphrase.fromPassword("this is wrong passphrase for sure")
    val privateKeysWithWrongPassPhrases = PRIVATE_KEYS.map {
      TestKeys.KeyWithPassPhrase(keyRing = it.keyRing, passphrase = wrongPassphrase)
    }
    val r = PgpDecryptAndOrVerify.decryptAndOrVerifyWithResult(
      srcInputStream = messageInfo.armored.byteInputStream(),
      publicKeys = PGPPublicKeyRingCollection(emptyList()),
      secretKeys = PGPSecretKeyRingCollection(privateKeysWithWrongPassPhrases.map { it.keyRing }),
      protector = SecretKeyRingProtector.unprotectedKeys()
    )
    assertTrue("Message returned", r.content == null)
    assertTrue("Error not returned", r.exception != null)
    assertTrue(
      "Wrong passphrase not detected",
      r.exception?.decryptionErrorType == PgpDecryptAndOrVerify.DecryptionErrorType.WRONG_PASSPHRASE
    )
  }

  @Test
  fun missingPassphraseTest() {
    val messageInfo = findMessage("decrypt - without a subject")
    val r = PgpDecryptAndOrVerify.decryptAndOrVerifyWithResult(
      srcInputStream = messageInfo.armored.byteInputStream(),
      publicKeys = PGPPublicKeyRingCollection(emptyList()),
      secretKeys = PGPSecretKeyRingCollection(PRIVATE_KEYS.map { it.keyRing }),
      protector = SecretKeyRingProtector.unprotectedKeys()
    )
    assertTrue("Message returned", r.content == null)
    assertTrue("Error returned", r.exception != null)
    assertTrue(
      "Missing passphrase not detected",
      r.exception?.decryptionErrorType == PgpDecryptAndOrVerify.DecryptionErrorType.WRONG_PASSPHRASE
    )
  }

  @Test
  fun wrongKeyTest() {
    val messageInfo = findMessage("decrypt - without a subject")
    val wrongKey = listOf(PRIVATE_KEYS[1])
    val r = PgpDecryptAndOrVerify.decryptAndOrVerifyWithResult(
      messageInfo.armored.byteInputStream(),
      publicKeys = PGPPublicKeyRingCollection(emptyList()),
      secretKeys = PGPSecretKeyRingCollection(wrongKey.map { it.keyRing }),
      protector = SecretKeyRingProtector.unprotectedKeys()
    )
    assertTrue("Message returned", r.content == null)
    assertTrue("Error not returned", r.exception != null)
    assertTrue(
      "Key mismatch not detected",
      r.exception?.decryptionErrorType == PgpDecryptAndOrVerify.DecryptionErrorType.KEY_MISMATCH
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

  private fun processMessage(messageKey: String): PgpDecryptAndOrVerify.DecryptionResult {
    val messageInfo = findMessage(messageKey)
    val result = PgpDecryptAndOrVerify.decryptAndOrVerifyWithResult(
      messageInfo.armored.byteInputStream(),
      publicKeys = PGPPublicKeyRingCollection(emptyList()),
      secretKeys = PGPSecretKeyRingCollection(PRIVATE_KEYS.map { it.keyRing }),
      protector = secretKeyRingProtector
    )
    if (result.content != null) {
      val s = String(result.content!!.toByteArray(), Charset.forName(messageInfo.charset))
      println("=========\n$s\n=========")
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
    val json = loadResourceAsString("complex_messages/$fileName")
    val rootObject = JsonParser.parseString(json).asJsonObject
    val inputMsg = Base64.getDecoder().decode(rootObject["in"].asJsonObject["mimeMsg"].asString)
    val out = rootObject["out"].asJsonObject
    val expectedBlocks = out["blocks"].asJsonArray
    val session = Session.getInstance(Properties())
    val mimeMessage = MimeMessage(session, inputMsg.inputStream())
    ///val mimeContent = PgpMsg.extractMimeContent(mimeMessage)
    //val processed = PgpMsg.extractMsgBlocksFromPart(mimeContent)

    /*assertEquals(expectedBlocks.size(), processed.size)

    for (i in processed.indices) {
      println("Checking block #$i of the '$fileName'")

      val expectedBlock = expectedBlocks[i].asJsonObject
      val expectedBlockType = MsgBlock.Type.ofSerializedName(expectedBlock["type"].asString)
      val expectedContent = expectedBlock["content"].asString.normalizeEol()
      val expectedComplete = expectedBlock["complete"].asBoolean
      val actualBlock = processed[i]
      val actualContent = (actualBlock.content ?: "").normalizeEol().removeUtf8Bom()

      assertEquals(expectedBlockType, actualBlock.type)
      assertEquals(expectedComplete, actualBlock.complete)
      assertEquals(expectedContent, actualContent)

      if (actualBlock.type in MsgBlock.Type.SIGNED_BLOCK_TYPES) {
        val expectedSignature = expectedBlock["signature"].asString.normalizeEol()
        val actualSignature = ((actualBlock as SignedMsgBlock).signature ?: "").normalizeEol()
        assertEquals(expectedSignature, actualSignature)
      }
    }*/
  }

  // -------------------------------------------------------------------------------------------

  @Test
  fun testParseDecryptMsgUnescapedSpecialCharactersInTextOriginallyTextPlain() {
    val mimeText = "MIME-Version: 1.0\n" +
        "Date: Fri, 6 Sep 2019 10:48:25 +0000\n" +
        "Message-ID: <some@mail.gmail.com>\n" +
        "Subject: plain text with special chars\n" +
        "From: Human at FlowCrypt <human@flowcrypt.com>\n" +
        "To: FlowCrypt Compatibility <flowcrypt.compatibility@gmail.com>\n" +
        "Content-Type: text/plain; charset=\"UTF-8\"\n" +
        "\n" + TEXT_SPECIAL_CHARS
    val keys = TestKeys.KEYS["rsa1"]!!.listOfKeysWithPassPhrase
    val result = PgpMsg.processMimeMessage(
      msg = MimeUtils.mimeTextToMimeMessage(mimeText),
      secretKeys = PGPSecretKeyRingCollection(keys.map { it.keyRing }),
      protector = SecretKeyRingProtector.unprotectedKeys()
    )
    assertEquals(TEXT_SPECIAL_CHARS, result.text)
    assertEquals(false, result.verificationResult.hasEncryptedParts)
    assertEquals(1, result.blocks.size)
    val block = result.blocks[0]
    assertEquals(MsgBlock.Type.PLAIN_HTML, block.type)
    checkRenderedBlock(block, listOf(RenderedBlock.normal(true, "PLAIN", TEXT_SPECIAL_CHARS)))
  }

  @Test
  fun testParseDecryptMsgUnescapedSpecialCharactersInTextOriginallyTextHtml() {
    val mimeText = "MIME-Version: 1.0\n" +
        "Date: Fri, 6 Sep 2019 10:48:25 +0000\n" +
        "Message-ID: <some@mail.gmail.com>\n" +
        "Subject: plain text with special chars\n" +
        "From: Human at FlowCrypt <human@flowcrypt.com>\n" +
        "To: FlowCrypt Compatibility <flowcrypt.compatibility@gmail.com>\n" +
        "Content-Type: text/html; charset=\"UTF-8\"\n" +
        "\n" + HTML_SPECIAL_CHARS
    val keys = TestKeys.KEYS["rsa1"]!!.listOfKeysWithPassPhrase
    val result = PgpMsg.processMimeMessage(
      msg = MimeUtils.mimeTextToMimeMessage(mimeText),
      secretKeys = PGPSecretKeyRingCollection(keys.map { it.keyRing }),
      protector = SecretKeyRingProtector.unprotectedKeys()
    )
    assertEquals(TEXT_SPECIAL_CHARS, result.text)
    assertEquals(false, result.verificationResult.hasEncryptedParts)
    assertEquals(1, result.blocks.size)
    val block = result.blocks[0]
    assertEquals(MsgBlock.Type.PLAIN_HTML, block.type)
    checkRenderedBlock(block, listOf(RenderedBlock.normal(true, "PLAIN", HTML_SPECIAL_CHARS)))
  }

  @Test
  fun testParseDecryptMsgUnescapedSpecialCharactersInEncryptedPgpMime() {
    val text = loadResourceAsString("compat/direct-encrypted-pgpmime-special-chars.txt")
    val keys = TestKeys.KEYS["rsa1"]!!.listOfKeysWithPassPhrase
    val decryptResult = PgpDecryptAndOrVerify.decryptAndOrVerifyWithResult(
      srcInputStream = text.toInputStream(),
      publicKeys = PGPPublicKeyRingCollection(emptyList()),
      secretKeys = PGPSecretKeyRingCollection(keys.map { it.keyRing }),
      protector = TestKeys.genRingProtector(keys)
    )
    assertEquals(true, decryptResult.isEncrypted)
    val result = PgpMsg.processMimeMessage(
      decryptResult.content?.toByteArray()?.inputStream()!!,
      secretKeys = PGPSecretKeyRingCollection(keys.map { it.keyRing }),
      protector = TestKeys.genRingProtector(keys)
    )
    assertEquals(TEXT_SPECIAL_CHARS, result.text)
    assertEquals(1, result.blocks.size)
    val block = result.blocks[0]
    assertEquals(MsgBlock.Type.PLAIN_HTML, block.type)
    checkRenderedBlock(block, listOf(RenderedBlock.normal(true, "GREEN", HTML_SPECIAL_CHARS)))
  }

  @Test
  fun testDecryptMsgUnescapedSpecialCharactersInEncryptedText() {
    val text = loadResourceAsString("compat/direct-encrypted-text-special-chars.txt")
    val keys = TestKeys.KEYS["rsa1"]!!.listOfKeysWithPassPhrase

    val decryptResult = PgpDecryptAndOrVerify.decryptAndOrVerifyWithResult(
      text.toInputStream(),
      publicKeys = PGPPublicKeyRingCollection(emptyList()),
      secretKeys = PGPSecretKeyRingCollection(keys.map { it.keyRing }),
      protector = TestKeys.genRingProtector(keys)
    )
    assertEquals(true, decryptResult.isEncrypted)
    assertEquals(TEXT_SPECIAL_CHARS, String(decryptResult.content?.toByteArray() ?: byteArrayOf()))
  }

  @Test
  fun testParseDecryptMsgPlainInlineImage() {
    val text = loadResourceAsString("other/plain-inline-image.txt")
    val keys = TestKeys.KEYS["rsa1"]!!.listOfKeysWithPassPhrase
    val result = PgpMsg.processMimeMessage(
      text.toInputStream(),
      secretKeys = PGPSecretKeyRingCollection(keys.map { it.keyRing }),
      protector = SecretKeyRingProtector.unprotectedKeys()
    )
    assertEquals("Below\n\n[image: image.png]\nAbove", result.text)
    assertEquals(false, result.verificationResult.hasEncryptedParts)
    assertEquals(2, result.blocks.size)
    val block = result.blocks[0]
    assertEquals(MsgBlock.Type.PLAIN_HTML, block.type)
    val htmlContent = loadResourceAsString("other/plain-inline-image-html-content.txt")
    checkRenderedBlock(block, listOf(RenderedBlock.normal(true, "PLAIN", htmlContent)))
  }

  @Test
  fun testExtractClearTextFromMsgSignedMessagePreserveNewlines() {
    val text = loadResourceAsString("other/signed-message-preserve-newlines.txt")
    val blocks = RawBlockParser.detectBlocks(text).toList()
    val clearText = PgpSignature.extractClearText(text)
    assertEquals(
      "Standard message\n\nsigned inline\n\nshould easily verify\nThis is email footer",
      clearText
    )
    assertEquals(1, blocks.size)
    assertEquals(RawBlockParser.RawBlockType.PGP_CLEARSIGN_MSG, blocks[0].type)
  }

  @Test
  fun testParseDecryptPlainGoogleSecurityAlertMessage() {
    val text = loadResourceAsString("other/plain-google-security-alert-20210416-084836-UTC.txt")
    val keys = TestKeys.KEYS["rsa1"]!!.listOfKeysWithPassPhrase
    val result = PgpMsg.processMimeMessage(
      text.toInputStream(),
      secretKeys = PGPSecretKeyRingCollection(keys.map { it.keyRing }),
      protector = SecretKeyRingProtector.unprotectedKeys()
    )
    val textContent = loadResourceAsString(
      "other/plain-google-security-alert-20210416-084836-UTC-text-content.txt"
    )
    assertEquals(textContent, result.text)
    assertEquals(false, result.verificationResult.hasEncryptedParts)
    assertEquals(1, result.blocks.size)
    val block = result.blocks[0]
    assertEquals(MsgBlock.Type.PLAIN_HTML, block.type)
    val htmlContent = loadResourceAsString(
      "other/plain-google-security-alert-20210416-084836-UTC-html-content.txt"
    )
    checkRenderedBlock(block, listOf(RenderedBlock.normal(true, "PLAIN", htmlContent)))
  }

  private data class RenderedBlock(
    val rendered: Boolean,
    val frameColor: String?,
    val htmlContent: String?,
    val content: String?,
    val error: String?
  ) {
    companion object {
      fun normal(rendered: Boolean, frameColor: String?, htmlContent: String?): RenderedBlock {
        return RenderedBlock(
          rendered = rendered,
          frameColor = frameColor,
          htmlContent = htmlContent,
          content = null,
          error = null
        )
      }

      fun error(error: String, content: String): RenderedBlock {
        return RenderedBlock(
          rendered = false,
          frameColor = null,
          htmlContent = null,
          content = content,
          error = error
        )
      }
    }
  }

  private fun checkRenderedBlock(block: MsgBlock, expectedRenderedBlocks: List<RenderedBlock>) {
    val parts = block.content!!.split(BODY_SPLIT_REGEX, 3)
    val head = parts[0]
    assertTrue(head.contains("<!DOCTYPE html>"))
    assertTrue(head.contains("<style>"))
    assertTrue(head.contains("<meta name=\"viewport\" content=\"width=device-width\"/>"))
    val foot = parts[2]
    assertTrue(foot.contains("</html>"))
    val body = parts[1]
    if (body.contains(NEXT_MSG_BLOCK_DELIMITER)) {
      val renderedContentBlocks = body.split(NEXT_MSG_BLOCK_DELIMITER)
      val lastEmpty = renderedContentBlocks.last()
      assertEquals("", lastEmpty)
      val actualRenderedBlocks = renderedContentBlocks.subList(0, renderedContentBlocks.size - 1)
        .map {
          // Regex didn't work well for all test cases, so I have switched to jsoup
          val document = Jsoup.parse(it)
          document.outputSettings().prettyPrint(false)
          val htmlBody = document.body()
          if (
            htmlBody.childrenSize() == 1 && htmlBody.child(0).normalName() == "div"
            && htmlBody.child(0).attributes().size() == 2 && htmlBody.child(0).hasAttr("class")
            && htmlBody.child(0).hasAttr("style")
          ) {
            val blockDiv = htmlBody.child(0)
            val frameColor = blockDiv.attr("class").split(" ").last()
            val content = blockDiv.html()
            RenderedBlock.normal(true, frameColor, content)
          } else {
            RenderedBlock.error("TEST VALIDATION ERROR - MISMATCHING CONTENT BLOCK FORMAT", it)
          }
        }.toList()
      assertEquals(expectedRenderedBlocks, actualRenderedBlocks)
    }
  }
}
