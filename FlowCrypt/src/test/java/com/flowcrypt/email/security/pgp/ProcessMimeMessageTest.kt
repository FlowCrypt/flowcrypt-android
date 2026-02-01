/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.api.retrofit.response.model.GenericMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.MsgBlock
import com.flowcrypt.email.core.msg.MimeUtils
import com.flowcrypt.email.extensions.kotlin.toEscapedHtml
import com.flowcrypt.email.extensions.kotlin.toInputStream
import com.flowcrypt.email.util.TestUtil
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.runBlocking
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pgpainless.key.protection.PasswordBasedSecretKeyRingProtector
import org.pgpainless.key.protection.SecretKeyRingProtector
import org.pgpainless.util.Passphrase
import java.util.Properties

/**
 * @author Denys Bondarenko
 */
class ProcessMimeMessageTest {
  @Test
  fun testProcessProtonmailPgpMimeEncrypted() {
    val processedMimeMessageResult = PgpMsg.processMimeMessage(
      MimeMessage(
        Session.getInstance(Properties()),
        TestUtil.readResourceAsByteArray("mime/protonmail_pgp_mime_encrypted.eml").inputStream()
      ),
      VERIFICATION_PUBLIC_KEYS,
      SECRET_KEYS,
      protector
    )

    assertEquals(
      "It's an encrypted message\n" +
          "\n" +
          "Sent with ProtonMail secure email.",
      processedMimeMessageResult.text
    )

    val verificationResult = processedMimeMessageResult.verificationResult
    assertTrue(verificationResult.hasEncryptedParts)
    assertTrue(verificationResult.hasSignedParts)
    assertFalse(verificationResult.hasMixedSignatures)
    assertFalse(verificationResult.isPartialSigned)
    assertFalse(verificationResult.hasBadSignatures)
    assertFalse(verificationResult.hasUnverifiedSignatures)

    val extractedBlocks = processedMimeMessageResult.blocks
    assertEquals(1, extractedBlocks.size)
    val msgBlock = extractedBlocks.first()
    assertThat(msgBlock, instanceOf(GenericMsgBlock::class.java))
    assertEquals(msgBlock.type, MsgBlock.Type.PLAIN_HTML)
    assertFalse(msgBlock.isOpenPGPMimeSigned)
  }

  @Test
  fun testGmailQuotesParsingAndHtmlManipulation() {
    val mimeMessageRaw = """
      To: default@flowcrypt.test
      From: denbond7@flowcrypt.test
      Subject: test
      Date: Sun, 17 Mar 2019 11:46:37 +0000
      Message-Id: <1552823197874-dd5800d9-54ca1a01-c548da66@flowcrypt.test>
      MIME-Version: 1.0
      Content-Type: multipart/alternative; boundary="000000000000a02340062c4d860a"

      --000000000000a02340062c4d860a
      Content-Type: text/plain; charset="UTF-8"
      Content-Transfer-Encoding: quoted-printable

      Your Android devices are always getting better thanks to new features and
      updates rolling out all the time

      On Wed, Jan 22, 2025 at 5:55=E2=80=AFPM Den <denbond7@flowcrypt.test> wrote:

      > Today, Android 15 starts rolling out to Pixel devices. These updates
      > include security features that help keep your sensitive health, financial
      > and personal information protected from theft and fraud.
      >
      > --
      > Regards,
      > Den
      >


      --=20
      Regards,
      Den

      --000000000000a02340062c4d860a
      Content-Type: text/html; charset="UTF-8"
      Content-Transfer-Encoding: quoted-printable

      <div dir=3D"ltr"> <span style=3D"color:rgb(95,99,104);font-family:&quot;Goo=
      gle Sans&quot;,roboto,arial,helvetica;font-size:16px">Your Android devices =
      are always getting better thanks to new features and updates rolling out al=
      l the time</span></div><br><div class=3D"gmail_quote gmail_quote_container"=
      > <div dir=3D"ltr" class=3D"gmail_attr">On Wed, Jan 22, 2025 at 5:55 PM Den=
       &lt;<a href=3D"mailto:denbond7@flowcrypt.test">denbond7@flowcrypt.test</a>=
      &gt; wrote:<br></div> <blockquote class=3D"gmail_quote" style=3D"margin:0px=
       0px 0px 0.8ex;border-left:1px solid rgb(204,204,204);padding-left:1ex"> <d=
      iv dir=3D"ltr"> <div><span style=3D"color:rgb(95,99,104);font-family:&quot;=
      Google Sans&quot;,roboto,arial,helvetica;font-size:16px">Today, Android 15 =
      starts rolling out to Pixel devices. These updates include security feature=
      s that help keep your sensitive health, financial and personal information =
      protected from theft and fraud.</span> </div> <div><br></div> <span class=
      =3D"gmail_signature_prefix">-- </span><br> <div dir=3D"ltr" class=3D"gmail_=
      signature"> <div dir=3D"ltr"> <div> <div dir=3D"ltr">Regards,</div> <div di=
      r=3D"ltr">Den</div> </div> </div> </div> </div> </blockquote></div><div><br=
       clear=3D"all"></div><div><br></div><span class=3D"gmail_signature_prefix">=
      -- </span><br><div dir=3D"ltr" class=3D"gmail_signature"> <div dir=3D"ltr">=
       <div>Regards,</div> <div>Den</div> </div></div>

      --000000000000a02340062c4d860a--
    """.trimIndent()

    val processedMimeMessageResult = runBlocking {
      PgpMsg.processMimeMessage(
        MimeMessage(Session.getInstance(Properties()), mimeMessageRaw.toInputStream()),
        PGPPublicKeyRingCollection(listOf()),
        PGPSecretKeyRingCollection(listOf()),
        SecretKeyRingProtector.unprotectedKeys(),
      )
    }

    assertEquals(1, processedMimeMessageResult.blocks.size)

    val plainHtmlBlock = processedMimeMessageResult.blocks.first {
      it.type == MsgBlock.Type.PLAIN_HTML
    }

    val document = Jsoup.parse(requireNotNull(plainHtmlBlock.content), "", Parser.xmlParser())
    assertNotNull(document.select("details").first())
    assertEquals(1, document.select("details").size)
    assertNotNull(document.select("summary").first())
    assertEquals(1, document.select("summary").size)

    //check that plain text was generated correctly and quotes for reply will be correct
    val documentForPlainVersion = Jsoup.parse(
      requireNotNull(PgpMsg.checkAndReturnQuotesFormatIfFound(processedMimeMessageResult.text)),
      "",
      Parser.xmlParser()
    )

    val quotesForPlainVersion = documentForPlainVersion.select("blockquote")
    assertEquals(1, quotesForPlainVersion.size)
    assertTrue(quotesForPlainVersion[0].text().startsWith("Today, Android 15"))
  }

  @Test
  fun testQuotesParsingAndHtmlManipulationForPlainMode() {
    val mimeMessageRaw = """
    MIME-Version: 1.0
    Date: Mon, 24 Feb 2025 17:02:47 +0200
    Message-ID: <messageid@flowcrypt.test>
    Subject: Re: Quotes for plain text
    From: Den at FlowCrypt <den@flowcrypt.test>
    To: DenBond7 <denbond7@flowcrypt.test>
    Content-Type: text/plain; charset="UTF-8"
    Content-Transfer-Encoding: quoted-printable
    
    reply 2
    
    The top-level build.gradle.kts file (for the Kotlin DSL) or
    build.gradle file (for the Groovy DSL) is located in the root project
    directory. It typically defines the common versions of plugins used by
    modules in your project.
    
    The following code sample describes the default settings and DSL
    elements in the top-level build script after creating a new project
    
    On Mon, Feb 24, 2025 at 5:02=E2=80=AFPM DenBond7 <denbond7@flowcrypt.tes=
    t> wrote:
    >
    > 2
    >
    > Creating custom build configurations requires you to make changes to
    > one or more build configuration files. These plain-text files use a
    > domain-specific language (DSL) to describe and manipulate the build
    > logic using Kotlin script, which is a flavor of the Kotlin language.
    > You can also use Groovy, which is a dynamic language for the Java
    > Virtual Machine (JVM), to configure your builds.
    >
    > You don't need to know Kotlin script or Groovy to start configuring
    > your build because the Android Gradle plugin introduces most of the
    > DSL elements you need. To learn more about the Android Gradle plugin
    > DSL, read the DSL reference documentation. Kotlin script also relies
    > on the underlying Gradle Kotlin DSL
    >
    > When starting a new project, Android Studio automatically creates some
    > of these files for you and populates them based on sensible defaults.
    > For an overview of the created files, see Android build structure.
    >
    > =D0=BF=D0=BD, 24 =D0=BB=D1=8E=D1=82. 2025=E2=80=AF=D1=80. =D0=BE 17:01 De=
    n at FlowCrypt <den@flowcrypt.test> =D0=BF=D0=B8=D1=88=D0=B5:
    > >
    > > reply 1
    > >
    > > Build types define certain properties that Gradle uses when building
    > > and packaging your app. Build types are typically configured for
    > > different stages of your development lifecycle.
    > >
    > > For example, the debug build type enables debug options and signs the
    > > app with the debug key, while the release build type may shrink,
    > > obfuscate, and sign your app with a release key for distribution.
    > >
    > > You must define at least one build type to build your app. Android
    > > Studio creates the debug and release build types by default. To start
    > > customizing packaging settings for your app, learn how to configure
    > > build types.
    > >
    > >
    > > On Mon, Feb 24, 2025 at 5:00=E2=80=AFPM DenBond7 <denbond7@flowcrypt.tes=
    t> wrote:
    > > >
    > > > 1
    > > >
    > > > The Android build system compiles app resources and source code and
    > > > packages them into APKs or Android App Bundles that you can test,
    > > > deploy, sign, and distribute.
    > > >
    > > > In Gradle build overview and Android build structure, we discussed
    > > > build concepts and the structure of an Android app. Now it's time to
    > > > configure the build.
    > > >
    > > >
    > > > --
    > > > Regards,
    > > > Denys Bondarenko
    > >
    > >
    > >
    > > --
    > > Regards,
    > > Den at FlowCrypt
    >
    >
    >
    > --
    > Regards,
    > Denys Bondarenko
    
    
    
    --=20
    Regards,
    Den at FlowCrypt""".trimIndent()

    val processedMimeMessageResult = runBlocking {
      PgpMsg.processMimeMessage(
        MimeMessage(Session.getInstance(Properties()), mimeMessageRaw.toInputStream()),
        PGPPublicKeyRingCollection(listOf()),
        PGPSecretKeyRingCollection(listOf()),
        SecretKeyRingProtector.unprotectedKeys(),
      )
    }

    assertEquals(1, processedMimeMessageResult.blocks.size)

    val plainHtmlBlock = processedMimeMessageResult.blocks.first {
      it.type == MsgBlock.Type.PLAIN_HTML
    }

    val document = Jsoup.parse(requireNotNull(plainHtmlBlock.content), "", Parser.xmlParser())
    assertNotNull(document.select("details").first())
    assertEquals(1, document.select("details").size)
    assertNotNull(document.select("summary").first())
    assertEquals(1, document.select("summary").size)

    val quotes = document.select("blockquote")
    assertEquals(3, quotes.size)
    assertTrue(quotes[0].text().startsWith("2 Creating custom build configurations requires"))
    assertTrue(quotes[1].text().startsWith("reply 1"))
    assertTrue(quotes[2].text().startsWith("1 The Android build system"))

    //check that plain text was generated correctly and quotes for reply will be correct
    val documentForPlainVersion = Jsoup.parse(
      requireNotNull(PgpMsg.checkAndReturnQuotesFormatIfFound(processedMimeMessageResult.text)),
      "",
      Parser.xmlParser()
    )

    val quotesForPlainVersion = documentForPlainVersion.select("blockquote")
    assertEquals(3, quotesForPlainVersion.size)
    assertTrue(quotes[0].text().startsWith("2 Creating custom build configurations requires"))
    assertTrue(quotes[1].text().startsWith("reply 1"))
    assertTrue(quotes[2].text().startsWith("1 The Android build system"))
  }

  @Test
  fun testQuotesParsingAndHtmlManipulationForEncryptedMessages() {
    val mimeMessageRaw = """
    Date: Tue, 4 Mar 2025 15:14:00 +0200 (GMT+02:00)
    From: default@flowcrypt.test
    To: default@flowcrypt.test
    Message-ID: <255254603.1.1741101240147@flowcrypt.test>
    Subject: TEst
    Mime-Version: 1.0
    Content-Type: multipart/mixed; 
      boundary="----=_Part_0_124883294.1741101240093"
    
    ------=_Part_0_124883294.1741101240093
    Content-Type: text/plain; charset=us-ascii
    Content-Transfer-Encoding: 7bit
    
    -----BEGIN PGP MESSAGE-----
    Version: PGPainless
    
    wV4DTxRYvSK3u1MSAQdA0rQBDv6Qe3gj8IWoEkn0r6W7+Uz/zyz0YI6DCLA2h3Ew
    bM+5OX93DKqTkaLWbV0VcuN4ACPOb+4nyWIhb/lQq468FO7y2rqMFah0LcTJfTWr
    wV4DTxRYvSK3u1MSAQdA/iyqemW8rY+ka18ANSph7TD8u3INnwT6Wt5BhcHzOTQw
    hPcE7cge7naa351khAsVRMgXZS4jzxR0WQw3E/truKHcprpCaQis0dgsiTxURTm+
    0sCWAdMfNsQa1GUZVredhzYCVRg6iiieU1k42vhwxYe8GMO/s+aWQgwR2EhenqKz
    0utgBCc1uP0o0fzIKdyirdCmlrSwk1yAhiU1/mR0p7Yl29vDdhRWjz7UEV1nfbwg
    1CAh7028G0YTFItwnPCCYDYYP1R0pPSffnuVV/BLuEfk0JiXUaa1ar73v5Sepfkm
    RKWX0PuM0IIY2+d78pCfr4puci2qwMGDX9hdTvp+qk+QolpFnVP7YUlDZTu5N8Q7
    GYrz0YJBXCvrkCNbu5UcssTRNyYyHB7sDT+XD+kE3uKcMat1cl190Gm6WFYB5I8x
    H3jhdxj8HyrazAyI3Lra/rtLIRg0zFbQ8Q7nb96gAzi3KU0gTdXPJzbMetlwF0cT
    Q027sUsEQ4L3PBVFNy6SYRRsQiS3o3CVaRkRlOEqsl+ix2S8bASh9bIl7Ode8+jz
    l8r4umM+zyWe
    =HJxV
    -----END PGP MESSAGE-----
    
    ------=_Part_0_124883294.1741101240093--
    """.trimIndent()

    val privateKey = "-----BEGIN PGP PRIVATE KEY BLOCK-----\n" +
        "Version: PGPainless\n" +
        "\n" +
        "lIYEYIq7phYJKwYBBAHaRw8BAQdAat45rrh+gvQwWwJw5eScq3Pdxt/8d+lWNVSm\n" +
        "kImXcRP+CQMCvWfx3mzDdd5g6c59LcPqADK0p70/7ZmTkp3ZC1YViTprg4tQt/PF\n" +
        "QJL+VPCG+BF9bWyFcfxKe+KAnXRTWml5O6xrv6ZkiNmAxoYyO1shzLQWZGVmYXVs\n" +
        "dEBmbG93Y3J5cHQudGVzdIh4BBMWCgAgBQJgirumAhsDBRYCAwEABAsJCAcFFQoJ\n" +
        "CAsCHgECGQEACgkQIl+AI8INCVcysgD/cu23M07rImuV5gIl98uOnSIR+QnHUD/M\n" +
        "I34b7iY/iTQBALMIsqO1PwYl2qKwmXb5lSoMj5SmnzRRE2RwAFW3AiMCnIsEYIq7\n" +
        "phIKKwYBBAGXVQEFAQEHQA8q7iPr+0OXqBGBSAL6WNDjzHuBsG7uiu5w8l/A6v8l\n" +
        "AwEIB/4JAwK9Z/HebMN13mCOF6Wy/9oZK4d0DW9cNLuQDeRVZejxT8oFMm7G8iGw\n" +
        "CGNjIWWcQSvctBZtHwgcMeplCW7tmzkD3Nq/ty50lCwQQd6gZSXMiHUEGBYKAB0F\n" +
        "AmCKu6YCGwwFFgIDAQAECwkIBwUVCgkICwIeAQAKCRAiX4Ajwg0JV+sbAQCv4LVM\n" +
        "0+AN54ivWa4vPRyYOfSQ1FqsipkYLJce+xwUeAD+LZpEVCypFtGWQVdeSJVxIHx3\n" +
        "k40IfHsK0fGgR+NrRAw=\n" +
        "=osuI\n" +
        "-----END PGP PRIVATE KEY BLOCK-----"

    val secretKeyRing = PgpKey.extractSecretKeyRing(privateKey)
    val processedMimeMessageResult = runBlocking {
      PgpMsg.processMimeMessage(
        MimeMessage(Session.getInstance(Properties()), mimeMessageRaw.toInputStream()),
        PGPPublicKeyRingCollection(listOf()),
        PGPSecretKeyRingCollection(listOf(secretKeyRing)),
        SecretKeyRingProtector.unlockAnyKeyWith(Passphrase.fromPassword("android")),
      )
    }

    assertEquals(1, processedMimeMessageResult.blocks.size)

    val plainHtmlBlock = processedMimeMessageResult.blocks.first {
      it.type == MsgBlock.Type.PLAIN_HTML
    }

    val document = Jsoup.parse(requireNotNull(plainHtmlBlock.content), "", Parser.xmlParser())
    assertNotNull(document.select("details").first())
    assertEquals(1, document.select("details").size)
    assertNotNull(document.select("summary").first())
    assertEquals(1, document.select("summary").size)

    val quotes = document.select("blockquote")
    assertEquals(3, quotes.size)
    assertTrue(quotes[0].text().startsWith("Sender 2"))
    assertTrue(quotes[1].text().startsWith("Reply 1"))
    assertTrue(quotes[2].text().startsWith("Sender 1"))

    //check that plain text was generated correctly and quotes for reply will be correct
    val documentForPlainVersion = Jsoup.parse(
      requireNotNull(PgpMsg.checkAndReturnQuotesFormatIfFound(processedMimeMessageResult.text)),
      "",
      Parser.xmlParser()
    )

    val quotesForPlainVersion = documentForPlainVersion.select("blockquote")
    assertEquals(3, quotesForPlainVersion.size)
    assertTrue(quotesForPlainVersion[0].text().startsWith("Sender 2"))
    assertTrue(quotesForPlainVersion[1].text().startsWith("Reply 1"))
    assertTrue(quotesForPlainVersion[2].text().startsWith("Sender 1"))
  }

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
    val keys = requireNotNull(TestKeys.KEYS["rsa1"]?.listOfKeysWithPassPhrase)
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

    checkRenderedBlock(
      block,
      listOf(RenderedBlock.normal(true, "PLAIN", HTML_SPECIAL_CHARS))
    )
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
    val keys = requireNotNull(TestKeys.KEYS["rsa1"]?.listOfKeysWithPassPhrase)
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

    checkRenderedBlock(
      block,
      listOf(RenderedBlock.normal(true, "PLAIN", HTML_SPECIAL_CHARS))
    )
  }

  @Test
  fun testParseDecryptMsgUnescapedSpecialCharactersInEncryptedPgpMime() {
    val text =
      TestUtil.readResourceAsString("pgp/messages/direct-encrypted-pgpmime-special-chars.txt")
    val keys = requireNotNull(TestKeys.KEYS["rsa1"]?.listOfKeysWithPassPhrase)
    val decryptResult = PgpDecryptAndOrVerify.decryptAndOrVerifyWithResult(
      srcInputStream = text.toInputStream(),
      publicKeys = PGPPublicKeyRingCollection(emptyList()),
      secretKeys = PGPSecretKeyRingCollection(keys.map { it.keyRing }),
      protector = TestKeys.genRingProtector(keys)
    )
    assertEquals(true, decryptResult.isEncrypted)
    val result = PgpMsg.processMimeMessage(
      requireNotNull(decryptResult.content?.toByteArray()?.inputStream()),
      secretKeys = PGPSecretKeyRingCollection(keys.map { it.keyRing }),
      protector = TestKeys.genRingProtector(keys)
    )
    assertEquals(TEXT_SPECIAL_CHARS, result.text)
    assertEquals(1, result.blocks.size)
    val block = result.blocks[0]
    assertEquals(MsgBlock.Type.PLAIN_HTML, block.type)

    checkRenderedBlock(
      block,
      listOf(RenderedBlock.normal(true, "PLAIN", HTML_SPECIAL_CHARS))
    )
  }

  @Test
  fun testParseDecryptMsgPlainInlineImage() {
    val text = TestUtil.readResourceAsString("mime/plain-inline-image.txt")
    val keys = requireNotNull(TestKeys.KEYS["rsa1"]?.listOfKeysWithPassPhrase)
    val result = PgpMsg.processMimeMessage(
      text.toInputStream(),
      secretKeys = PGPSecretKeyRingCollection(keys.map { it.keyRing }),
      protector = SecretKeyRingProtector.unprotectedKeys()
    )
    assertEquals("Below\n[image: image.png]\nAbove", result.text)
    assertEquals(false, result.verificationResult.hasEncryptedParts)
    assertEquals(2, result.blocks.size)
    val block = result.blocks[0]
    assertEquals(MsgBlock.Type.PLAIN_HTML, block.type)
    val htmlContent = TestUtil.readResourceAsString("other/plain-inline-image-html-content.txt")

    checkRenderedBlock(block, listOf(RenderedBlock.normal(true, "PLAIN", htmlContent)))
  }

  @Test
  fun testParseDecryptPlainGoogleSecurityAlertMessage() {
    val text =
      TestUtil.readResourceAsString("mime/plain-google-security-alert-20210416-084836-UTC.txt")
    val keys = requireNotNull(TestKeys.KEYS["rsa1"]?.listOfKeysWithPassPhrase)
    val result = PgpMsg.processMimeMessage(
      text.toInputStream(),
      secretKeys = PGPSecretKeyRingCollection(keys.map { it.keyRing }),
      protector = SecretKeyRingProtector.unprotectedKeys()
    )
    val textContent = TestUtil.readResourceAsString(
      "other/plain-google-security-alert-20210416-084836-UTC-text-content.txt"
    )
    assertEquals(textContent.replace("\n", "\r\n"), result.text)
    assertEquals(false, result.verificationResult.hasEncryptedParts)
    assertEquals(1, result.blocks.size)
    val block = result.blocks[0]
    assertEquals(MsgBlock.Type.PLAIN_HTML, block.type)
    val htmlContent = TestUtil.readResourceAsString(
      "other/plain-google-security-alert-20210416-084836-UTC-html-content.txt"
    )

    checkRenderedBlock(block, listOf(RenderedBlock.normal(true, "PLAIN", htmlContent)))
  }

  private fun checkRenderedBlock(block: MsgBlock, expectedRenderedBlocks: List<RenderedBlock>) {
    val parts = requireNotNull(block.content?.split("</?body>".toRegex(), 3))
    val head = parts[0]
    assertTrue(head.contains("<!DOCTYPE html>"))
    assertTrue(head.contains("<style>"))
    assertTrue(head.contains("<meta name=\"viewport\" content=\"width=device-width\">"))
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

  companion object {
    private const val NEXT_MSG_BLOCK_DELIMITER = "<!-- next MsgBlock -->\n"
    private const val TEXT_SPECIAL_CHARS = "> special <tag> & other\n> second line"

    private val VERIFICATION_PUBLIC_KEYS = PgpKey.parseKeys(
      source = TestUtil.readResourceAsByteArray("pgp/keys/default@flowcrypt.test_fisrtKey_pub.asc")
    ).pgpKeyRingCollection.pgpPublicKeyRingCollection

    private val SECRET_KEYS = PgpKey.parseKeys(
      source = TestUtil.readResourceAsByteArray("pgp/keys/default@flowcrypt.test_fisrtKey_prv_default.asc")
    ).pgpKeyRingCollection.pgpSecretKeyRingCollection

    private val protector = PasswordBasedSecretKeyRingProtector.forKey(
      SECRET_KEYS.first(),
      Passphrase.fromPassword("android")
    )

    private val HTML_SPECIAL_CHARS = TEXT_SPECIAL_CHARS.toEscapedHtml()
  }
}
