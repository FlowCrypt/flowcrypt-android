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
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.junit.annotations.OutgoingMessageConfiguration
import com.flowcrypt.email.rules.AddRecipientsToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.base.BaseComposeGmailFlow
import com.flowcrypt.email.ui.base.BaseComposeScreenTest
import jakarta.mail.internet.MimeMultipart
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@FlowCryptTestSettings(useCommonIdling = false, useIntents = true)
@OutgoingMessageConfiguration(
  to = [BaseComposeGmailFlow.DEFAULT_TO_RECIPIENT],
  cc = [BaseComposeGmailFlow.DEFAULT_CC_RECIPIENT],
  bcc = [BaseComposeGmailFlow.DEFAULT_BCC_RECIPIENT],
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
      .around(AddRecipientsToDatabaseRule(prepareRecipientsForTest()))
      .around(addLabelsToDatabaseRule)
      .around(activityScenarioRule)
      .around(ScreenshotTestRule())

  @Test
  fun testSending() {
    //add attachments
    attachments.forEach {
      addAttachment(it)
    }

    //enqueue outgoing message
    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())

    doAfterSendingChecks { _, _, mimeMessage ->
      val multipart = mimeMessage.content as MimeMultipart
      assertEquals(attachments.size + 1, multipart.count)
      val encryptedMessagePart = multipart.getBodyPart(0)
      checkEncryptedMessagePart(encryptedMessagePart)

      attachments.forEachIndexed { index, file ->
        val attachmentPart = multipart.getBodyPart(index + 1)
        checkEncryptedAttachment(attachmentPart, file.name, attachmentsDataCache[index])
      }
    }
  }
}