/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.gmailapi

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.request.model.MessageUploadRequest
import com.flowcrypt.email.api.retrofit.response.api.MessageReplyTokenResponse
import com.flowcrypt.email.api.retrofit.response.api.MessageUploadResponse
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.extensions.kotlin.asInternetAddress
import com.flowcrypt.email.extensions.kotlin.toInputStream
import com.flowcrypt.email.jetpack.workmanager.HandlePasswordProtectedMsgWorker
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.junit.annotations.OutgoingMessageConfiguration
import com.flowcrypt.email.rules.AddRecipientsToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.security.pgp.PgpDecryptAndOrVerify
import com.flowcrypt.email.ui.base.BaseComposeGmailFlow
import com.flowcrypt.email.ui.base.BaseComposeScreenTest
import com.flowcrypt.email.util.gson.GsonHelper
import com.google.gson.GsonBuilder
import jakarta.mail.Message
import jakarta.mail.Multipart
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartReader
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.pgpainless.util.Passphrase
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Properties

@LargeTest
@RunWith(AndroidJUnit4::class)
@FlowCryptTestSettings(useCommonIdling = false, useIntents = true)
@OutgoingMessageConfiguration(
  to = [
    BaseComposeGmailFlow.TO_RECIPIENT,
    PasswordProtectedEncryptedComposeGmailApiFlow.TO_RECIPIENT_WITHOUT_PUBLIC_KEY
  ],
  cc = [
    BaseComposeGmailFlow.CC_RECIPIENT,
    PasswordProtectedEncryptedComposeGmailApiFlow.CC_RECIPIENT_WITHOUT_PUBLIC_KEY
  ],
  bcc = [
    BaseComposeGmailFlow.BCC_RECIPIENT,
    PasswordProtectedEncryptedComposeGmailApiFlow.BCC_RECIPIENT_WITHOUT_PUBLIC_KEY
  ],
  message = BaseComposeScreenTest.MESSAGE,
  subject = BaseComposeScreenTest.SUBJECT,
  timeoutToWaitSendingInMilliseconds = 10000
)
class PasswordProtectedEncryptedComposeGmailApiFlow : BaseComposeGmailFlow() {

  override val mockWebServerRule =
    FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        val gson = ApiHelper.getInstance(getTargetContext()).gson
        return when {
          request.path.equals("/api/v1/message/new-reply-token") -> {
            MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
              .setBody(gson.toJson(MessageReplyTokenResponse(replyToken = REPLY_TOKEN)))
          }

          request.path.equals("/api/v1/message") -> {
            //Analyze the request before proceeding
            checkRequestToMessageApi(request)
            isRequestToMessageAPITested = true
            MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
              .setBody(gson.toJson(MessageUploadResponse(url = WEB_PORTAL_URL)))
          }

          else -> handleCommonAPICalls(request)
        }
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

  private var isRequestToMessageAPITested = false

  override fun prepareRecipientsForTest(): List<RecipientWithPubKeys> {
    return super.prepareRecipientsForTest().toMutableList().apply {
      addAll(
        listOf(
          RecipientWithPubKeys(
            RecipientEntity(
              email = TO_RECIPIENT_WITHOUT_PUBLIC_KEY,
              name = null
            ),
            emptyList()
          ), RecipientWithPubKeys(
            RecipientEntity(
              email = CC_RECIPIENT_WITHOUT_PUBLIC_KEY,
              name = null
            ),
            emptyList()
          ),
          RecipientWithPubKeys(
            RecipientEntity(
              email = BCC_RECIPIENT_WITHOUT_PUBLIC_KEY,
              name = null
            ),
            emptyList()
          )
        )
      )
    }
  }

  @Test
  fun testSendPasswordProtectedMessageWithFewAttachments() {
    isRequestToMessageAPITested = false

    //add attachments
    attachments.forEach {
      addAttachment(it)
    }

    //attach a public key
    openActionBarOverflowOrOptionsMenu(getTargetContext())
    onView(withText(R.string.include_public_key))
      .check(matches(isDisplayed()))
      .perform(click())

    //add password-protected logic
    onView(withId(R.id.btnSetWebPortalPassword))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withId(R.id.eTPassphrase))
      .perform(
        replaceText(WEB_PORTAL_PASSWORD),
        closeSoftKeyboard()
      )
    onView(withId(R.id.btSetPassword))
      .perform(click())

    Thread.sleep(2000)

    //enqueue outgoing message
    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())

    doAfterSendingChecks { _, mimeMessage ->
      val multipart = mimeMessage.content as MimeMultipart
      assertEquals(
        1 // Part with FES message link
            + 1   // Encrypted by default method message
            + 3   // Attachments
            + 1,  // Pub key
        multipart.count
      )

      //check the FES message link part
      val fesLinkMessagePart = multipart.getBodyPart(0)
      assertEquals(
        getResString(
          R.string.password_protected_msg_promo,
          addAccountToDatabaseRule.account.email,
          WEB_PORTAL_URL
        ),
        fesLinkMessagePart.content as String
      )

      //check encrypted by default method message
      val encryptedMessagePart = multipart.getBodyPart(1)
      checkEncryptedMessagePart(encryptedMessagePart)

      //check encrypted attachments
      attachments.forEachIndexed { index, file ->
        val attachmentPart = multipart.getBodyPart(index + 2)
        checkEncryptedAttachment(attachmentPart, file.name, attachmentsDataCache[index])
      }

      //check public key
      val publicKeyPart = multipart.getBodyPart(attachments.size + 2)
      checkAttachedPublicKey(publicKeyPart)
    }

    assertTrue(isRequestToMessageAPITested)
  }

  /**
   * This method checks that we create a right structure when create a request to
   * "https://fes.{domain}/api/v1/message". Also, it checks that we create a right MIME message
   * in the encrypted data in the second part of the request. The right MIME message means
   * we can read all data and all additional info looks well(headers, names and so on).
   */
  private fun checkRequestToMessageApi(request: RecordedRequest) {
    val mediaType = request.headers["Content-Type"]?.toMediaType()
    val boundary = mediaType?.parameter("boundary")
    assertNotNull(boundary)
    val multipartReader = MultipartReader(request.body, requireNotNull(boundary))

    multipartReader.use {
      checkFirstPartOfRequestToMessageAPI(multipartReader)
      checkSecondPartOfRequestToMessageAPI(multipartReader)

      //we should not have more than 2 parts
      assertNull(multipartReader.nextPart())
    }
  }

  private fun checkFirstPartOfRequestToMessageAPI(multipartReader: MultipartReader) {
    val firstPart = multipartReader.nextPart()
    assertNotNull(firstPart)

    firstPart?.use {
      val messageUploadRequest = GsonHelper.gson.fromJson(
        firstPart.body.readString(StandardCharsets.UTF_8),
        MessageUploadRequest::class.java
      )

      assertNotNull(messageUploadRequest)
      assertEquals(REPLY_TOKEN, messageUploadRequest.associateReplyToken)
      assertEquals(
        addAccountToDatabaseRule.account.email.asInternetAddress(),
        messageUploadRequest.from.asInternetAddress()
      )
      val outgoingMessageConfiguration =
        requireNotNull(outgoingMessageConfigurationRule.outgoingMessageConfiguration)

      compareAddresses(outgoingMessageConfiguration.to, messageUploadRequest.to.toTypedArray())
      compareAddresses(outgoingMessageConfiguration.cc, messageUploadRequest.cc.toTypedArray())
      compareAddresses(outgoingMessageConfiguration.bcc, messageUploadRequest.bcc.toTypedArray())
    }
  }

  private fun checkSecondPartOfRequestToMessageAPI(multipartReader: MultipartReader) {
    val secondPart = multipartReader.nextPart()
    assertNotNull(secondPart)
    val outgoingMessageConfiguration =
      requireNotNull(outgoingMessageConfigurationRule.outgoingMessageConfiguration)

    secondPart?.use {
      val sentPgpMessage = secondPart.body.readString(StandardCharsets.UTF_8)
      val decryptionResult = PgpDecryptAndOrVerify.decryptAndOrVerifyWithResult(
        srcInputStream = sentPgpMessage.toInputStream(),
        passphrase = Passphrase.fromPassword(WEB_PORTAL_PASSWORD)
      )
      //check that message was encrypted
      assertTrue(decryptionResult.isEncrypted)
      //check that message was not signed
      assertFalse(decryptionResult.isSigned)
      //check that message was not encrypted by public keys
      //ref https://github.com/pgpainless/pgpainless/issues/376
      assertTrue(decryptionResult.messageMetadata?.recipientKeyIds?.isEmpty() == true)

      val decryptedContent = decryptionResult.content
      assertNotNull(decryptedContent)

      //parse decrypted content to MIME message
      val mimeMessage = MimeMessage(
        Session.getInstance(Properties()),
        ByteArrayInputStream(decryptedContent?.toByteArray())
      )

      //we should be sure that me have right recipients in MIME message. BCC should be empty here.
      assertEquals(
        addAccountToDatabaseRule.account.email,
        (mimeMessage.from.first() as InternetAddress).address
      )
      compareAddresses(
        outgoingMessageConfiguration.to,
        getEmailAddresses(mimeMessage, Message.RecipientType.TO)
      )
      compareAddresses(
        outgoingMessageConfiguration.cc,
        getEmailAddresses(mimeMessage, Message.RecipientType.CC)
      )
      compareAddresses(
        emptyArray(),
        getEmailAddresses(mimeMessage, Message.RecipientType.BCC)
      )

      val multipart = mimeMessage.content as Multipart
      //this MIME message should contains 5 parts:
      //text + 4 attachments(1 and 2 - text, 3 - binary, 4 - pub key)
      assertEquals(5, multipart.count)

      val replyInfoData = HandlePasswordProtectedMsgWorker.ReplyInfoData(
        sender = addAccountToDatabaseRule.account.email.lowercase(),
        recipient = (outgoingMessageConfiguration.to
            + outgoingMessageConfiguration.cc
            + outgoingMessageConfiguration.bcc)
          .mapNotNull { it.asInternetAddress()?.address?.lowercase() }
          .filterNot {
            it.equals(addAccountToDatabaseRule.account.email.lowercase(), true)
          }.toHashSet()
          .sorted(),
        subject = SUBJECT,
        token = REPLY_TOKEN
      )

      val replyInfo = Base64.getEncoder().encodeToString(
        GsonBuilder().create().toJson(replyInfoData).toByteArray()
      )

      val infoDiv = HandlePasswordProtectedMsgWorker.genInfoDiv(replyInfo)
      val bodyWithReplyToken = MESSAGE + "\n\n" + infoDiv

      //check parts content. Check that content is the same as source
      val textBodyPart = multipart.getBodyPart(0)
      assertEquals(
        textBodyPart.content.toString(),
        bodyWithReplyToken,
        textBodyPart.content.toString()
      )

      //check attachments
      attachments.forEachIndexed { index, file ->
        val attachmentPart = multipart.getBodyPart(index + 1)
        checkStandardAttachment(attachmentPart, file.name, attachmentsDataCache[index])
      }

      //check pub key
      val publicKeyPart = multipart.getBodyPart(4)
      checkAttachedPublicKey(publicKeyPart)
    }
  }

  companion object {
    const val TO_RECIPIENT_WITHOUT_PUBLIC_KEY = "to_no_key@flowcrypt.test"
    const val CC_RECIPIENT_WITHOUT_PUBLIC_KEY = "cc_no_key@flowcrypt.test"
    const val BCC_RECIPIENT_WITHOUT_PUBLIC_KEY = "bcc_no_key@flowcrypt.test"
    private const val WEB_PORTAL_PASSWORD = "Qwerty1234@"
    private const val REPLY_TOKEN = "some_reply_token"
    private const val WEB_PORTAL_URL = "https://fes.flowcrypt.test/message/some_id"
  }
}
