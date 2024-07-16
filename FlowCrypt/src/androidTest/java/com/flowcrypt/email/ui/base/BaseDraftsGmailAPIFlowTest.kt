/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.base

import android.util.Base64
import android.view.Gravity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions.open
import androidx.test.espresso.contrib.DrawerMatchers.isClosed
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.activityScenarioRule
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.EmailUtil
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
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.viewaction.CustomViewActions.clickOnFolderWithName
import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.client.googleapis.json.GoogleJsonErrorContainer
import com.google.api.client.json.Json
import com.google.api.client.json.JsonObjectParser
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.model.Draft
import com.google.api.services.gmail.model.Label
import com.google.api.services.gmail.model.ListDraftsResponse
import com.google.api.services.gmail.model.ListLabelsResponse
import com.google.api.services.gmail.model.ListMessagesResponse
import com.google.api.services.gmail.model.ListSendAsResponse
import com.google.api.services.gmail.model.Message
import com.google.api.services.gmail.model.MessagePart
import com.google.api.services.gmail.model.MessagePartBody
import com.google.api.services.gmail.model.MessagePartHeader
import jakarta.activation.DataSource
import jakarta.mail.Session
import jakarta.mail.internet.ContentType
import jakarta.mail.internet.InternetHeaders
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
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
import java.io.InputStreamReader
import java.io.OutputStream
import java.math.BigInteger
import java.net.HttpURLConnection
import java.util.Properties
import java.util.zip.GZIPInputStream
import kotlin.random.Random

/**
 * @author Denys Bondarenko
 */
abstract class BaseDraftsGmailAPIFlowTest : BaseComposeScreenTest() {
  abstract val mockWebServerRule: FlowCryptMockWebServerRule
  override val activityScenarioRule = activityScenarioRule<MainActivity>()
  protected val draftsCache = mutableListOf<Draft>()

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
  fun clearCache() {
    draftsCache.clear()
  }

  protected fun getDraftAndMimeMessageFromRequest(request: RecordedRequest): Pair<Draft, MimeMessage> {
    val gzipInputStream = GZIPInputStream(request.body.inputStream())
    val draft = JsonObjectParser(GsonFactory.getDefaultInstance()).parseAndClose(
      InputStreamReader(gzipInputStream), Draft::class.java
    )
    val rawMimeMessageAsByteArray = Base64.decode(
      draft.message.raw, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
    )
    val mimeMessage = MimeMessage(
      Session.getInstance(Properties()), rawMimeMessageAsByteArray.inputStream()
    )
    return Pair(draft, mimeMessage)
  }

  protected fun genMsgDetailsMockResponse(
    messageId: String,
    messageThreadId: String,
  ) =
    MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
      .setBody(Message().apply {
        factory = GsonFactory.getDefaultInstance()
        id = messageId
        threadId = messageThreadId
        labelIds = listOf(JavaEmailConstants.FOLDER_DRAFT)
        historyId = BigInteger.valueOf(Random.nextLong())
      }.toString())

  protected fun prepareDraft(
    draftId: String,
    messageId: String,
    messageThreadId: String,
    rawMsg: String
  ): Draft {
    return Draft().apply {
      factory = GsonFactory.getDefaultInstance()
      id = draftId
      message = Message().apply {
        id = messageId
        threadId = messageThreadId
        labelIds = listOf(JavaEmailConstants.FOLDER_DRAFT)
        raw = rawMsg
      }
    }
  }

  protected fun openComposeScreenAndTypeSubject(subject: String) {
    //open the compose screen
    onView(withId(R.id.floatActionButtonCompose))
      .check(matches(isDisplayed()))
      .perform(click())

    //type some text in the subject
    onView(withId(R.id.editTextEmailSubject))
      .check(matches(isDisplayed()))
      .perform(
        scrollTo(),
        click(),
        typeText(subject),
        closeSoftKeyboard()
      )
  }

  protected fun moveToDraftFolder() {
    onView(withId(R.id.drawer_layout))
      .check(matches(isClosed(Gravity.LEFT)))
      .perform(open())

    onView(withId(R.id.navigationView))
      .perform(clickOnFolderWithName(JavaEmailConstants.FOLDER_DRAFT))

    Thread.sleep(1000)
  }

  protected fun handleCommonAPICalls(request: RecordedRequest): MockResponse {
    when {
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

      request.method == "GET" && request.path?.matches("/gmail/v1/users/me/drafts\\S*".toRegex()) == true -> {
        return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(
          ListDraftsResponse().apply {
            factory = GsonFactory.getDefaultInstance()
            val requestUrl = requireNotNull(request.requestUrl)
            val queryParameterField = requestUrl.queryParameter("fields")
            val isThreadIdAllowed = queryParameterField == null ||
                queryParameterField.contains("drafts/message/threadId")
            drafts = draftsCache.map { draft ->
              Draft().apply {
                id = draft.id
                message = Message().apply {
                  id = draft.message.id
                  if (isThreadIdAllowed) {
                    threadId = draft.message.threadId
                  }
                }
              }
            }
          }.toString()
        )
      }

      request.method == "DELETE" && request.path?.matches("/gmail/v1/users/me/drafts/\\S*".toRegex()) == true -> {
        val draftId = request.requestUrl?.encodedPathSegments?.last()
        val cachedDraft = draftsCache.firstOrNull { it.id == draftId }
        if (cachedDraft != null) {
          draftsCache.remove(cachedDraft)
          return MockResponse().setResponseCode(HttpURLConnection.HTTP_NO_CONTENT)
        } else {
          return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
            .setHeader("Content-Type", Json.MEDIA_TYPE)
            .setBody(GoogleJsonErrorContainer().apply {
              factory = GsonFactory.getDefaultInstance()
              error = GoogleJsonError().apply {
                code = HttpURLConnection.HTTP_NOT_FOUND
                message = "Requested entity was not found."
                errors = listOf(GoogleJsonError.ErrorInfo().apply {
                  message = "Requested entity was not found."
                  domain = "local"
                  reason = "notFound"
                })
              }
            }.toString())
        }
      }

      request.method == "POST" && request.path == "/batch" -> {
        val mimeMultipart = MimeMultipart(object : DataSource {
          override fun getInputStream(): InputStream = request.body.inputStream()

          override fun getOutputStream(): OutputStream {
            throw java.lang.UnsupportedOperationException()
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

  protected fun genRawMimeWithSubject(msgSubject: String) = String(
    ByteArrayOutputStream().apply {
      this.use {
        MimeMessage(Session.getInstance(Properties())).apply {
          subject = msgSubject
          setContent(MimeMultipart().apply { addBodyPart(MimeBodyPart().apply { setText("") }) })
        }.writeTo(it)
      }
    }.toByteArray()
  )

  protected fun getMimeMessageFromCache(msgPosition: Int): MimeMessage {
    val rawMimeMessageAsByteArrayOfSecondMsg = Base64.decode(
      draftsCache[msgPosition].message.raw, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
    )
    return MimeMessage(
      Session.getInstance(Properties()), rawMimeMessageAsByteArrayOfSecondMsg.inputStream()
    )
  }

  protected fun genMessage(
    messageId: String,
    messageThreadId: String,
    subject: String,
    historyIdValue: BigInteger
  ) =
    Message().apply {
      factory = GsonFactory.getDefaultInstance()
      id = messageId
      threadId = messageThreadId
      labelIds = listOf(JavaEmailConstants.FOLDER_DRAFT)
      snippet = subject
      historyId = historyIdValue
      payload = MessagePart().apply {
        partId = ""
        mimeType = "multipart/alternative"
        filename = ""
        headers = prepareMessageHeaders(subject)
        body = MessagePartBody().apply {
          setSize(0)
        }
        parts = listOf(
          MessagePart().apply {
            partId = "0"
            mimeType = "text/plain"
            filename = ""
            headers = listOf(MessagePartHeader().apply {
              name = "Content-Type"
              value = "text/plain"
            })
            body = MessagePartBody().apply { setSize(130) }
          }
        )
      }
    }.toString()

  protected fun genPathForGmailMessages(subPath: String) = "/gmail/v1/users/me/messages/$subPath?" +
      "fields=id,threadId,labelIds,snippet,sizeEstimate,historyId,internalDate," +
      "payload/partId,payload/mimeType,payload/filename,payload/headers," +
      "payload/body,payload/parts(partId,mimeType,filename,headers,body/size,body/attachmentId)" +
      "&format=full"

  private fun prepareMessageHeaders(subject: String) = listOf(
    MessagePartHeader().apply {
      name = "MIME-Version"
      value = "1.0"
    },
    MessagePartHeader().apply {
      name = "Date"
      value = "Tue, 29 Nov 2022 14:30:15 +0200"
    },
    MessagePartHeader().apply {
      name = "Message-ID"
      value = EmailUtil.generateContentId()
    },
    MessagePartHeader().apply {
      name = "Subject"
      value = subject
    },
    MessagePartHeader().apply {
      name = "From"
      value = AccountDaoManager.getDefaultAccountDao().email
    },
    MessagePartHeader().apply {
      name = "Content-Type"
      value = "text/plain"
    },
  )

  companion object {
    const val DRAFT_ID_FIRST = "r5555555555555555551"
    const val MESSAGE_ID_FIRST = "5555555555555551"
    const val THREAD_ID_FIRST = "1111111111111111"
    const val MESSAGE_SUBJECT_FIRST = "first"

    const val DRAFT_ID_SECOND = "r5555555555555555552"
    const val MESSAGE_ID_SECOND = "5555555555555552"
    const val THREAD_ID_SECOND = "11111111111111112"
    const val MESSAGE_SUBJECT_SECOND = "second"
  }
}
