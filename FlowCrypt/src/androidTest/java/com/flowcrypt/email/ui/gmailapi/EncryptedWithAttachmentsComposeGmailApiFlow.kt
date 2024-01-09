/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.gmailapi

/**
 * @author Denys Bondarenko
 */
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.extensions.kotlin.toInputStream
import com.flowcrypt.email.extensions.org.pgpainless.decryption_verification.isSigned
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.junit.annotations.OutgoingMessageConfiguration
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.ui.base.BaseComposeGmailFlow
import com.flowcrypt.email.ui.base.BaseComposeScreenTest
import jakarta.mail.Part
import jakarta.mail.internet.MimeMultipart
import jakarta.mail.internet.MimePart
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@MediumTest
@RunWith(AndroidJUnit4::class)
@FlowCryptTestSettings(useCommonIdling = false, useIntents = true)
@OutgoingMessageConfiguration(
  to = [TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER],
  message = BaseComposeScreenTest.MESSAGE,
  subject = BaseComposeScreenTest.SUBJECT
)
class EncryptedWithAttachmentsComposeGmailApiFlow : BaseComposeGmailFlow() {
  override val mockWebServerRule =
    FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        return handleCommonAPICalls(request)
      }
    })

  @get:Rule
  var ruleChain: TestRule =
    RuleChain.outerRule(RetryRule.DEFAULT)
      .around(ClearAppSettingsRule())
      .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
      .around(mockWebServerRule)
      .around(addAccountToDatabaseRule)
      .around(addPrivateKeyToDatabaseRule)
      .around(addLabelsToDatabaseRule)
      .around(activityScenarioRule)
      .around(ScreenshotTestRule())

  @Test
  fun testSending() {
    //add attachments
    atts.forEach {
      addAttachment(it)
    }

    //enqueue outgoing message
    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())

    doAfterSendingChecks { _, mimeMessage ->
      val multipart = mimeMessage.content as MimeMultipart
      assertEquals(atts.size + 1, multipart.count)
      val pgpSecretKeyRing = PgpKey.extractSecretKeyRing(
        requireNotNull(addPrivateKeyToDatabaseRule.pgpKeyRingDetails.privateKey)
      )
      val buffer = ByteArrayOutputStream()
      val messageMetadata = getMessageMetadata(
        inputStream = ((mimeMessage.content as MimeMultipart)
          .getBodyPart(0).content as String).toInputStream(),
        outputStream = buffer,
        pgpSecretKeyRing = pgpSecretKeyRing
      )
      assertEquals(true, messageMetadata.isEncrypted)
      assertEquals(true, messageMetadata.isSigned)
      assertEquals(MESSAGE, String(buffer.toByteArray()))

      atts.forEachIndexed { index, file ->
        val attachmentPart = multipart.getBodyPart(index + 1) as MimePart
        assertEquals(Part.ATTACHMENT, attachmentPart.disposition)
        assertEquals(file.name + "." + Constants.PGP_FILE_EXT, attachmentPart.fileName)

        val attachmentOutputStream = ByteArrayOutputStream()
        val attachmentMessageMetadata = getMessageMetadata(
          inputStream = attachmentPart.inputStream,
          outputStream = attachmentOutputStream,
          pgpSecretKeyRing = pgpSecretKeyRing
        )

        assertEquals(file.name, attachmentMessageMetadata.filename)
        assertEquals(true, attachmentMessageMetadata.isEncrypted)
        assertEquals(false, attachmentMessageMetadata.isSigned)
        assertEquals(genFileContent(index), String(attachmentOutputStream.toByteArray()))
      }
    }
  }
}