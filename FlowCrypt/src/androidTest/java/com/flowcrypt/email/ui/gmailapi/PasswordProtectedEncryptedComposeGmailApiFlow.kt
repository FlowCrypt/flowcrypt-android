/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.gmailapi

import android.os.Environment
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.EmailUtil
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
import com.flowcrypt.email.util.TestGeneralUtil
import com.flowcrypt.email.util.gson.GsonHelper
import com.google.gson.GsonBuilder
import jakarta.mail.BodyPart
import jakarta.mail.Message
import jakarta.mail.Multipart
import jakarta.mail.Part
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
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.pgpainless.PGPainless
import org.pgpainless.key.util.UserId
import org.pgpainless.util.Passphrase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Properties

@LargeTest
@RunWith(AndroidJUnit4::class)
@FlowCryptTestSettings(useCommonIdling = false, useIntents = true)
@OutgoingMessageConfiguration(
  to = [PasswordProtectedEncryptedComposeGmailApiFlow.TO_RECIPIENT_WITHOUT_PUBLIC_KEY],
  cc = [PasswordProtectedEncryptedComposeGmailApiFlow.CC_RECIPIENT_WITHOUT_PUBLIC_KEY],
  bcc = [PasswordProtectedEncryptedComposeGmailApiFlow.BCC_RECIPIENT_WITHOUT_PUBLIC_KEY],
  message = BaseComposeScreenTest.MESSAGE,
  subject = BaseComposeScreenTest.SUBJECT
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

  private val pubKeyAttachmentInfo = requireNotNull(
    EmailUtil.genAttInfoFromPubKey(
      addPrivateKeyToDatabaseRule.pgpKeyRingDetails,
      addPrivateKeyToDatabaseRule.accountEntity.email
    )
  )

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

    //enqueue outgoing message
    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())

    doAfterSendingChecks { _, mimeMessage ->
      val multipart = mimeMessage.content as MimeMultipart
      //assertEquals(1, multipart.count)
      //val encryptedMessagePart = multipart.getBodyPart(0)
      //checkEncryptedMessagePart(encryptedMessagePart)
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
      assertArrayEquals(
        arrayOf(TO_RECIPIENT_WITHOUT_PUBLIC_KEY),
        messageUploadRequest.to.toTypedArray()
      )
      assertArrayEquals(
        arrayOf(CC_RECIPIENT_WITHOUT_PUBLIC_KEY),
        messageUploadRequest.cc.toTypedArray()
      )
      assertArrayEquals(
        arrayOf(BCC_RECIPIENT_WITHOUT_PUBLIC_KEY),
        messageUploadRequest.bcc.toTypedArray()
      )
    }
  }

  private fun checkSecondPartOfRequestToMessageAPI(multipartReader: MultipartReader) {
    val secondPart = multipartReader.nextPart()
    assertNotNull(secondPart)

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
      assertTrue(
        decryptionResult.messageMetadata?.encryptionLayers
          ?.asSequence()
          ?.toList()
          ?.flatMap { it.recipients }?.isEmpty() == true
      )

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
      assertArrayEquals(
        arrayOf(TO_RECIPIENT_WITHOUT_PUBLIC_KEY),
        getEmailAddresses(mimeMessage, Message.RecipientType.TO)
      )
      assertArrayEquals(
        arrayOf(CC_RECIPIENT_WITHOUT_PUBLIC_KEY),
        getEmailAddresses(mimeMessage, Message.RecipientType.CC)
      )
      assertArrayEquals(
        emptyArray(),
        getEmailAddresses(mimeMessage, Message.RecipientType.BCC)
      )

      val multipart = mimeMessage.content as Multipart
      //this MIME message should contains 5 parts:
      //text + 4 attachments(1 and 2 - text, 3 - binary, 4 - pub key)
      assertEquals(5, multipart.count)

      val replyInfoData = HandlePasswordProtectedMsgWorker.ReplyInfoData(
        sender = addAccountToDatabaseRule.account.email.lowercase(),
        recipient = listOf(
          CC_RECIPIENT_WITHOUT_PUBLIC_KEY,
          BCC_RECIPIENT_WITHOUT_PUBLIC_KEY,
          TO_RECIPIENT_WITHOUT_PUBLIC_KEY
        ),
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
      assertEquals(bodyWithReplyToken, textBodyPart.content.toString())
      checkAttachmentPart(multipart.getBodyPart(1), 0)
      checkAttachmentPart(multipart.getBodyPart(2), 1)
      checkAttachmentPart(multipart.getBodyPart(3), 2)

      //check pub key
      val bodyPart = multipart.getBodyPart(4)
      assertEquals(Part.ATTACHMENT, bodyPart.disposition)
      assertEquals(pubKeyAttachmentInfo.name, bodyPart.fileName)
      assertArrayEquals(pubKeyAttachmentInfo.rawData, bodyPart.inputStream.readBytes())
    }
  }

  private fun getEmailAddresses(mimeMessage: MimeMessage, type: Message.RecipientType) =
    mimeMessage.getRecipients(type)
      ?.map { (it as InternetAddress).address }
      ?.toTypedArray() ?: emptyArray()

  private fun checkAttachmentPart(bodyPart: BodyPart, position: Int) {
    assertEquals(Part.ATTACHMENT, bodyPart.disposition)
    assertEquals(attachments[position].name, bodyPart.fileName)
    assertArrayEquals(attachments[position].readBytes(), bodyPart.inputStream.readBytes())
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
