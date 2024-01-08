/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.base

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.activityScenarioRule
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.response.api.EkmPrivateKeysResponse
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration
import com.flowcrypt.email.api.retrofit.response.model.Key
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddLabelsToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.ui.DraftsGmailAPITestCorrectSendingFlowTest
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.util.TestGeneralUtil
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.model.Label
import com.google.api.services.gmail.model.ListLabelsResponse
import com.google.api.services.gmail.model.ListMessagesResponse
import com.google.api.services.gmail.model.ListSendAsResponse
import jakarta.activation.DataSource
import jakarta.mail.internet.ContentType
import jakarta.mail.internet.InternetHeaders
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMultipart
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Before
import org.pgpainless.util.Passphrase
import rawhttp.core.RawHttp
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection

/**
 * @author Denys Bondarenko
 */
abstract class BaseComposeGmailFlow : BaseComposeScreenTest() {
  protected val sentCache = mutableListOf<com.google.api.services.gmail.model.Message>()
  abstract val mockWebServerRule: FlowCryptMockWebServerRule
  override val activityScenarioRule = activityScenarioRule<MainActivity>()

  private val accountEntity = AccountDaoManager.getDefaultAccountDao().copy(
    accountType = AccountEntity.ACCOUNT_TYPE_GOOGLE, clientConfiguration = ClientConfiguration(
      flags = listOf(
        ClientConfiguration.ConfigurationProperty.NO_PRV_CREATE,
        ClientConfiguration.ConfigurationProperty.NO_PRV_BACKUP,
        ClientConfiguration.ConfigurationProperty.NO_ATTESTER_SUBMIT,
        ClientConfiguration.ConfigurationProperty.PRV_AUTOIMPORT_OR_AUTOGEN,
        ClientConfiguration.ConfigurationProperty.FORBID_STORING_PASS_PHRASE,
        ClientConfiguration.ConfigurationProperty.RESTRICT_ANDROID_ATTACHMENT_HANDLING,
      ),
      keyManagerUrl = "https://flowcrypt.test/",
    ), useAPI = true, useCustomerFesUrl = true
  )

  final override val addAccountToDatabaseRule: AddAccountToDatabaseRule =
    AddAccountToDatabaseRule(accountEntity)

  protected val addPrivateKeyToDatabaseRule =
    AddPrivateKeyToDatabaseRule(addAccountToDatabaseRule.account)

  protected val addLabelsToDatabaseRule = AddLabelsToDatabaseRule(
    account = addAccountToDatabaseRule.account, folders = listOf(
      LocalFolder(
        account = addAccountToDatabaseRule.account.email,
        fullName = JavaEmailConstants.FOLDER_DRAFT,
        folderAlias = JavaEmailConstants.FOLDER_DRAFT,
        attributes = listOf("\\HasNoChildren", "\\Draft")
      ), LocalFolder(
        account = addAccountToDatabaseRule.account.email,
        fullName = JavaEmailConstants.FOLDER_INBOX,
        folderAlias = JavaEmailConstants.FOLDER_INBOX,
        attributes = listOf("\\HasNoChildren")
      )
    )
  )

  protected val decryptedPrivateKey = PgpKey.decryptKey(
    requireNotNull(addPrivateKeyToDatabaseRule.pgpKeyRingDetails.privateKey),
    Passphrase.fromPassword(TestConstants.DEFAULT_STRONG_PASSWORD)
  )

  @Before
  fun prepareTest() {
    openComposeScreenAndFillData()
  }

  protected fun openComposeScreenAndFillData() {
    //open the compose screen
    onView(withId(R.id.floatActionButtonCompose))
      .check(matches(isDisplayed()))
      .perform(click())

    fillInAllFields(TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER)
  }

  protected fun handleCommonAPICalls(request: RecordedRequest): MockResponse {
    when {
      request.path?.startsWith("/attester/pub", ignoreCase = true) == true -> {
        val lastSegment = request.requestUrl?.pathSegments?.lastOrNull()

        return when {
          TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER.equals(lastSegment, true) -> {
            MockResponse()
              .setResponseCode(HttpURLConnection.HTTP_OK)
              .setBody(TestGeneralUtil.readResourceAsString("3.txt"))
          }

          else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
        }
      }

      request.path == "/v1/keys/private" -> {
        return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(
          ApiHelper.getInstance(getTargetContext()).gson
            .toJson(EkmPrivateKeysResponse(privateKeys = listOf(Key(decryptedPrivateKey))))
        )
      }

      request.path == "/gmail/v1/users/me/settings/sendAs" -> {
        return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(
          ListSendAsResponse().apply {
            factory = GsonFactory.getDefaultInstance()
            sendAs = emptyList()
          }.toString()
        )
      }

      request.path == "/gmail/v1/users/me/labels" -> {
        return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(
          ListLabelsResponse().apply {
            factory = GsonFactory.getDefaultInstance()
            labels = addLabelsToDatabaseRule.folders.map {
              Label().apply {
                id = it.fullName
                name = it.folderAlias
              }
            }
          }.toString()
        )
      }

      request.path == "/gmail/v1/users/me/messages?labelIds=${JavaEmailConstants.FOLDER_INBOX}&maxResults=45" -> {
        return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(
          ListMessagesResponse().apply {
            factory = GsonFactory.getDefaultInstance()
            messages = emptyList()
          }.toString()
        )
      }

      request.method == "POST" && request.path == "/upload/gmail/v1/users/me/messages/send?uploadType=resumable" -> {
        return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setHeader("Location", BASE_URL + LOCATION_URL)
          .setBody(com.google.api.services.gmail.model.Message().apply {
            factory = GsonFactory.getDefaultInstance()
            id = MESSAGE_ID_SENT
            threadId = THREAD_ID_SENT
            labelIds = listOf(JavaEmailConstants.FOLDER_SENT)
          }.toString())
      }

      request.method == "PUT" && request.path == LOCATION_URL -> {
        val message = com.google.api.services.gmail.model.Message().apply {
          factory = GsonFactory.getDefaultInstance()
          id = DraftsGmailAPITestCorrectSendingFlowTest.MESSAGE_ID_SENT
          threadId = DraftsGmailAPITestCorrectSendingFlowTest.THREAD_ID_SENT
          labelIds = listOf(JavaEmailConstants.FOLDER_SENT)
        }

        sentCache.add(message)

        return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setBody(message.toString())
      }

      request.method == "POST" && request.path == "/batch" -> {
        val mimeMultipart = MimeMultipart(object : DataSource {
          override fun getInputStream(): InputStream = request.body.inputStream()

          override fun getOutputStream(): OutputStream {
            throw UnsupportedOperationException()
          }

          override fun getContentType(): String {
            return request.getHeader("Content-Type") ?: throw IllegalArgumentException()
          }

          override fun getName(): String = ""
        })

        val count = mimeMultipart.count
        val rawHttp = RawHttp()
        val responseMimeMultipart = MimeMultipart()
        for (i in 0 until count) {
          try {
            val bodyPart = mimeMultipart.getBodyPart(i)
            val rawHttpRequest = rawHttp.parseRequest(bodyPart.inputStream)
            val requestBody = if (rawHttpRequest.body.isPresent) {
              rawHttpRequest.body.get().asRawBytes().toRequestBody(
                contentType = bodyPart.contentType.toMediaTypeOrNull()
              )
            } else null

            val okhttp3Request = okhttp3.Request.Builder()
              .method(
                method = rawHttpRequest.method,
                body = requestBody
              )
              .url(rawHttpRequest.uri.toURL())
              .headers(Headers.Builder().build())
              .build()

            val response = ApiHelper.getInstance(getTargetContext())
              .retrofit.callFactory().newCall(okhttp3Request).execute()

            val stringBuilder = StringBuilder().apply {
              append(response.protocol.toString().uppercase())
              append(" ")
              append(response.code)
              append(" ")
              append(response.message)
              append("\n")

              response.headers.forEach {
                append(it.first + ": " + it.second + "\n")
              }
              append("\n")
              append(response.body?.string())
            }

            responseMimeMultipart.addBodyPart(
              MimeBodyPart(
                InternetHeaders(byteArrayOf().inputStream()).apply {
                  setHeader("Content-Type", "application/http")
                  setHeader("Content-ID", "response-${i + 1}")
                },
                stringBuilder.toString().toByteArray()
              )
            )
          } catch (e: Exception) {
            e.printStackTrace()
          }
        }

        val outputStream = ByteArrayOutputStream()
        responseMimeMultipart.writeTo(outputStream)
        val content = String(outputStream.toByteArray())
        val boundary = (ContentType(responseMimeMultipart.contentType)).getParameter("boundary")

        return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setHeader("Content-Type", "multipart/mixed; boundary=$boundary")
          .setBody(content)
      }

      else -> {
        return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
      }
    }
  }

  companion object {
    const val MESSAGE_ID_SENT = "5555555555555553"
    const val THREAD_ID_SENT = "1111111111111113"
    const val BASE_URL = "https://flowcrypt.test"
    const val LOCATION_URL =
      "/upload/gmail/v1/users/me/messages/send?uploadType=resumable&upload_id=Location"
  }
}