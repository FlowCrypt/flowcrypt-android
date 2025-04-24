/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.api.retrofit.response.model.DecryptedAndOrSignedContentMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.MsgBlock
import com.flowcrypt.email.extensions.kotlin.normalizeEol
import com.flowcrypt.email.extensions.kotlin.removeUtf8Bom
import com.flowcrypt.email.util.TestUtil
import com.google.gson.JsonParser
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import org.pgpainless.key.protection.UnprotectedKeysProtector
import java.util.Base64
import java.util.Properties

class PgpMsgTest {

  @Test
  @Ignore("Should be reworked after switching to a new logic that uses RawBlockParser")
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

  @Test
  @Ignore("Should be reworked after switching to a new logic that uses RawBlockParser")
  fun singleComplexMessageTest() {
    val testFile = "verify - Kraken - urldecode signature-0.json"
    checkComplexMessage(testFile)
  }

  private fun checkComplexMessage(fileName: String) {
    println("\n*** Processing '$fileName'")
    val json = TestUtil.readResourceAsString("complex_messages/$fileName")
    val rootObject = JsonParser.parseString(json).asJsonObject
    val inputMsg = Base64.getDecoder().decode(rootObject["in"].asJsonObject["mimeMsg"].asString)
    val out = rootObject["out"].asJsonObject
    val expectedBlocks = out["blocks"].asJsonArray
    val session = Session.getInstance(Properties())
    val mimeMessage = MimeMessage(session, inputMsg.inputStream())
    val extractedBlocks = PgpMsg.extractMsgBlocksFromPart(
      mimeMessage,
      PGPPublicKeyRingCollection(emptyList()),
      PGPSecretKeyRingCollection(emptyList()),
      UnprotectedKeysProtector()
    ).toList()

    assertEquals(expectedBlocks.size(), extractedBlocks.size)

    for (i in extractedBlocks.indices) {
      println("Checking block #$i of the '$fileName'")

      val expectedBlock = expectedBlocks[i].asJsonObject
      val expectedBlockType = MsgBlock.Type.ofSerializedName(expectedBlock["type"].asString)
      val expectedContent =
        if (expectedBlockType == MsgBlock.Type.DECRYPTED_AND_OR_SIGNED_CONTENT) {
          expectedBlock["blocks"].asJsonArray.first().asJsonObject["content"].asString.normalizeEol()
        } else {
          expectedBlock["content"].asString.normalizeEol()
        }
      val actualBlock = extractedBlocks[i]
      val actualContent = if (actualBlock is DecryptedAndOrSignedContentMsgBlock) {
        (actualBlock.blocks.first().content ?: "").normalizeEol().removeUtf8Bom()
      } else {
        (actualBlock.content ?: "").normalizeEol().removeUtf8Bom()
      }

      assertEquals(expectedBlockType, actualBlock.type)
      assertEquals(expectedContent, actualContent)
    }
  }
}
