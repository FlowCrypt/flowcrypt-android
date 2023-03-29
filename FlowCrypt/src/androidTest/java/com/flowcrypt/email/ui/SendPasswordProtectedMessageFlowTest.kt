/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui

import android.content.Context
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.flowcrypt.email.Constants
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.request.model.MessageUploadRequest
import com.flowcrypt.email.api.retrofit.response.api.MessageReplyTokenResponse
import com.flowcrypt.email.api.retrofit.response.api.MessageUploadResponse
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.extensions.kotlin.toInputStream
import com.flowcrypt.email.jetpack.workmanager.HandlePasswordProtectedMsgWorker
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.rules.AddRecipientsToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.security.pgp.PgpDecryptAndOrVerify
import com.flowcrypt.email.service.ProcessingOutgoingMessageInfoHelper
import com.flowcrypt.email.ui.base.BaseDraftsGmailAPIFlowTest
import com.flowcrypt.email.util.TestGeneralUtil
import com.flowcrypt.email.util.gson.GsonHelper
import com.google.gson.GsonBuilder
import jakarta.mail.BodyPart
import jakarta.mail.Multipart
import jakarta.mail.Part
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
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
class SendPasswordProtectedMessageFlowTest : BaseDraftsGmailAPIFlowTest() {

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

  private val recipientWithPubKeys = listOf(
    RecipientWithPubKeys(
      RecipientEntity(
        email = RECIPIENT_WITHOUT_PUBLIC_KEY,
        name = null
      ),
      emptyList()
    )
  )

  private val pubKeyAttachmentInfo = requireNotNull(
    EmailUtil.genAttInfoFromPubKey(
      addPrivateKeyToDatabaseRule.pgpKeyDetails,
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
      .around(addLabelsToDatabaseRule)
      .around(AddRecipientsToDatabaseRule(recipientWithPubKeys))
      .around(ScreenshotTestRule())

  private var isRequestToMessageAPITested = false

  @Test
  fun testSendPasswordProtectedMessageWithFewAttachments() {
    isRequestToMessageAPITested = false

    val context: Context = ApplicationProvider.getApplicationContext()
    val uid = EmailUtil.genOutboxUID(context)
    val outgoingMessageInfo = OutgoingMessageInfo(
      account = addAccountToDatabaseRule.account.email,
      subject = MESSAGE_SUBJECT,
      msg = MESSAGE_TEXT,
      toRecipients = listOf(InternetAddress(RECIPIENT_WITHOUT_PUBLIC_KEY)),
      from = InternetAddress(addAccountToDatabaseRule.account.email),
      encryptionType = MessageEncryptionType.ENCRYPTED,
      messageType = MessageType.NEW,
      uid = uid,
      password = WEB_PORTAL_PASSWORD.toCharArray(),
      atts = attachments.mapIndexedNotNull { index, file ->
        EmailUtil.getAttInfoFromUri(
          context = context,
          uri = FileProvider.getUriForFile(context, Constants.FILE_PROVIDER_AUTHORITY, file)
        )?.copy(
          email = addAccountToDatabaseRule.account.email,
          uid = uid,
          folder = JavaEmailConstants.FOLDER_OUTBOX,
          path = index.toString()
        )
      }.toMutableList().apply {
        add(pubKeyAttachmentInfo.copy(path = 3.toString()))
      }.toList()
    )

    ProcessingOutgoingMessageInfoHelper.process(context, outgoingMessageInfo)
    //need to wait sometime until all processes will be completed
    Thread.sleep(5000)
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
      assertEquals(setOf<Long>(), decryptionResult.openPgpMetadata?.recipientKeyIds)

      val decryptedContent = decryptionResult.content
      assertNotNull(decryptedContent)

      //parse decrypted content to MIME message
      val mimeMessage = MimeMessage(
        Session.getInstance(Properties()),
        ByteArrayInputStream(decryptedContent?.toByteArray())
      )

      val multipart = mimeMessage.content as Multipart
      //this MIME message should contains 5 parts:
      //text + 4 attachments(1 and 2 - text, 3 - binary, 4 - pub key)
      assertEquals(5, multipart.count)

      val replyInfoData = HandlePasswordProtectedMsgWorker.ReplyInfoData(
        sender = addAccountToDatabaseRule.account.email.lowercase(),
        recipient = listOf(RECIPIENT_WITHOUT_PUBLIC_KEY),
        subject = MESSAGE_SUBJECT,
        token = REPLY_TOKEN
      )

      val replyInfo = Base64.getEncoder().encodeToString(
        GsonBuilder().create().toJson(replyInfoData).toByteArray()
      )

      val infoDiv = HandlePasswordProtectedMsgWorker.genInfoDiv(replyInfo)
      val bodyWithReplyToken = MESSAGE_TEXT + "\n\n" + infoDiv

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

  private fun checkAttachmentPart(bodyPart: BodyPart, position: Int) {
    assertEquals(Part.ATTACHMENT, bodyPart.disposition)
    assertEquals(attachments[position].name, bodyPart.fileName)
    assertArrayEquals(attachmentsDataCache[position], bodyPart.inputStream.readBytes())
  }

  companion object {
    private const val RECIPIENT_WITHOUT_PUBLIC_KEY = "no_key@flowcrypt.test"
    private const val WEB_PORTAL_PASSWORD = "Qwerty1234@"
    private const val MESSAGE_SUBJECT = "Subject"
    private const val MESSAGE_TEXT = "Some text"
    private const val ATTACHMENT_NAME_1 = "text.txt"
    private const val ATTACHMENT_NAME_2 = "text1.txt"
    private const val ATTACHMENT_NAME_3 = "binary_key.key"
    private const val REPLY_TOKEN = "some_reply_token"
    private const val WEB_PORTAL_URL = "https://fes.flowcrypt.test/message/some_id"
    private var attachmentsDataCache: MutableList<ByteArray> = mutableListOf()
    private var attachments: MutableList<File> = mutableListOf()
    private val pgpSecretKeyRing = PGPainless.generateKeyRing().simpleEcKeyRing(
      UserId.nameAndEmail(RECIPIENT_WITHOUT_PUBLIC_KEY, RECIPIENT_WITHOUT_PUBLIC_KEY),
      TestConstants.DEFAULT_PASSWORD
    )

    @BeforeClass
    @JvmStatic
    fun setUp() {
      val directory = InstrumentationRegistry.getInstrumentation().targetContext
        .getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        ?: File(Environment.DIRECTORY_DOCUMENTS)

      attachmentsDataCache.addAll(
        listOf(
          "Text attachment 1".toByteArray(),//text data
          "Text attachment 2".toByteArray(),//text data
          ByteArrayOutputStream().apply { pgpSecretKeyRing.encode(this) }.toByteArray(),
          //binary data
        )
      )

      attachments.addAll(
        listOf(
          TestGeneralUtil.createFileWithContent(
            directory = directory,
            fileName = ATTACHMENT_NAME_1,
            inputStream = attachmentsDataCache[0].inputStream()
          ),
          TestGeneralUtil.createFileWithContent(
            directory = directory,
            fileName = ATTACHMENT_NAME_2,
            inputStream = attachmentsDataCache[1].inputStream()
          ),
          TestGeneralUtil.createFileWithContent(
            directory = directory,
            fileName = ATTACHMENT_NAME_3,
            inputStream = attachmentsDataCache[2].inputStream()
          )
        )
      )
    }
  }
}
