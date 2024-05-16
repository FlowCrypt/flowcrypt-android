/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.gmailapi

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.retrofit.response.model.VerificationResult
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.junit.annotations.OutgoingMessageConfiguration
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.rules.AddRecipientsToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.base.BaseComposeGmailFlow
import com.flowcrypt.email.ui.base.BaseComposeScreenTest
import jakarta.mail.Message
import jakarta.mail.internet.MimeMultipart
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@FlowCryptTestSettings(useCommonIdling = false)
@OutgoingMessageConfiguration(
  to = [],
  cc = [],
  bcc = [],
  message = BaseComposeScreenTest.MESSAGE,
  subject = "",
  isNew = false
)
class EncryptedReplyComposeGmailApiFlow : BaseComposeGmailFlow() {
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
  @FlakyTest
  fun testSending() {
    //need to wait while the app loads the messages list
    Thread.sleep(2000)

    //click on a message
    onView(withId(R.id.recyclerViewMsgs))
      .perform(
        RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
          POSITION_EXISTING_STANDARD, click()
        )
      )

    //wait the message details rendering
    Thread.sleep(1000)

    //click on reply
    openReplyScreen(R.id.layoutReplyButton, SUBJECT_EXISTING_STANDARD)

    //switch to encrypted mode
    openActionBarOverflowOrOptionsMenu(getTargetContext())
    onView(withText(R.string.switch_to_secure_email))
      .check(matches(isDisplayed()))
      .perform(click())

    val outgoingMessageConfiguration =
      requireNotNull(outgoingMessageConfigurationRule.outgoingMessageConfiguration)

    fillData(outgoingMessageConfiguration)

    //enqueue outgoing message
    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())

    //back to the message details screen
    pressBack()

    doAfterSendingChecks { _, rawMime, mimeMessage ->
      //check reply subject
      assertEquals(rawMime, "Re: $SUBJECT_EXISTING_STANDARD", mimeMessage.subject)

      //check recipients
      compareAddresses(
        arrayOf(DEFAULT_FROM_RECIPIENT),
        getEmailAddresses(mimeMessage, Message.RecipientType.TO)
      )
      compareAddresses(
        arrayOf(),
        getEmailAddresses(mimeMessage, Message.RecipientType.CC)
      )
      compareAddresses(
        arrayOf(),
        getEmailAddresses(mimeMessage, Message.RecipientType.BCC)
      )

      //check reply text
      val multipart = mimeMessage.content as MimeMultipart
      assertEquals(1, multipart.count)
      val encryptedMessagePart = multipart.getBodyPart(0)
      val expectedText = MESSAGE + EmailUtil.genReplyContent(
        IncomingMessageInfo(
          msgEntity = MessageEntity(
            email = "",
            folder = "",
            uid = 0,
            fromAddress = DEFAULT_FROM_RECIPIENT,
            receivedDate = DATE_EXISTING_STANDARD

          ),
          encryptionType = MessageEncryptionType.STANDARD,
          msgBlocks = emptyList(),
          inlineSubject = SUBJECT_EXISTING_STANDARD,
          localFolder = LocalFolder(
            account = addAccountToDatabaseRule.account.email,
            fullName = JavaEmailConstants.FOLDER_INBOX
          ),
          text = MESSAGE_EXISTING_STANDARD,
          verificationResult = VerificationResult(
            hasEncryptedParts = false,
            hasSignedParts = false,
            hasMixedSignatures = false,
            isPartialSigned = false,
            keyIdOfSigningKeys = emptyList(),
            hasBadSignatures = false
          )
        )
      )
      checkEncryptedMessagePart(
        bodyPart = encryptedMessagePart,
        expectedText = expectedText,
        expectedIds = mutableListOf<Long>().apply {
          add(extractKeyId(addPrivateKeyToDatabaseRule.pgpKeyRingDetails))
          add(extractKeyId(defaultFromPgpKeyDetails))
          //need to think here. Need to arrange recipients
        }.toTypedArray().sortedArray()
      )
    }
  }
}