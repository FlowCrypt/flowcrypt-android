/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
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
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.viewaction.CustomViewActions.clickOnFolderWithName
import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.client.googleapis.json.GoogleJsonErrorContainer
import com.google.api.client.json.Json
import com.google.api.client.json.JsonObjectParser
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.model.BatchDeleteMessagesRequest
import com.google.api.services.gmail.model.Draft
import com.google.api.services.gmail.model.ListDraftsResponse
import com.google.api.services.gmail.model.ListThreadsResponse
import com.google.api.services.gmail.model.Message
import com.google.api.services.gmail.model.MessagePart
import com.google.api.services.gmail.model.MessagePartBody
import com.google.api.services.gmail.model.MessagePartHeader
import com.google.api.services.gmail.model.Thread
import jakarta.mail.Session
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Before
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.math.BigInteger
import java.net.HttpURLConnection
import java.util.Properties
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import kotlin.random.Random

/**
 * @author Denys Bondarenko
 */
abstract class BaseDraftsGmailAPIFlowTest : BaseGmailApiTest(
  accountEntity = BASE_ACCOUNT_ENTITY.copy(useConversationMode = true)
) {
  override val activityScenarioRule = activityScenarioRule<MainActivity>()
  protected val draftsCache = mutableMapOf<String, Draft>()

  @Before
  fun clearCache() {
    draftsCache.clear()
  }

  override fun handleCommonAPICalls(request: RecordedRequest): MockResponse {
    return when {
      request.method == "GET" && request.path == "/gmail/v1/users/me/threads?labelIds=${JavaEmailConstants.FOLDER_INBOX}&maxResults=45" -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(
          ListThreadsResponse().apply {
            factory = GsonFactory.getDefaultInstance()
            threads = emptyList()
          }.toString()
        )
      }

      request.method == "GET" && request.path == "/gmail/v1/users/me/threads?labelIds=${JavaEmailConstants.FOLDER_DRAFT}&maxResults=45" -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(
          ListThreadsResponse().apply {
            factory = GsonFactory.getDefaultInstance()
            threads = draftsCache.values.map {
              it.message.threadId
            }.toSet()
              .map {
                Thread().apply { id = it }
              }
          }.toString()
        )
      }

      request.method == "DELETE" && request.path?.matches("/gmail/v1/users/me/drafts/\\S*".toRegex()) == true -> {
        val draftId = request.requestUrl?.encodedPathSegments?.last()
        val cachedDraft = draftsCache[draftId]
        if (cachedDraft != null) {
          draftsCache.remove(draftId)
          MockResponse().setResponseCode(HttpURLConnection.HTTP_NO_CONTENT)
        } else {
          MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
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

      request.method == "GET" && request.path?.matches(REGEX_USER_THREADS_GET_FORMAT_FULL) == true -> {
        genThreadDetailsMockResponse(request)
      }

      request.method == "GET" && request.path?.matches(REGEX_DRAFT_BY_RFC822MSGID) == true -> {
        genListDraftsResponseForRfc822msgidSearch(request.path ?: "")
      }

      request.method == "GET" && request.path?.matches(REGEX_USER_MESSAGES_GET_FORMAT_FULL) == true -> {
        val path = request.path ?: ""
        val messageId =
          REGEX_USER_MESSAGES_GET_FORMAT_FULL.find(path)?.groups?.get(1)?.value?.trim()
        if (messageId in listOf(MESSAGE_ID_FIRST, MESSAGE_ID_SECOND)) {
          genUserMessagesGetFormatFullResponseInternal(path)
        } else {
          super.handleCommonAPICalls(request)
        }
      }

      request.method == "GET" && request.path?.matches(REGEX_USER_MESSAGES_GET_RAW) == true -> {
        genUserMessagesRawResponse(request.path ?: "")
      }

      request.method == "GET" && request.path == "/gmail/v1/users/me/drafts?fields=drafts/id,drafts/message/id&maxResults=500" -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setHeader("Content-Type", Json.MEDIA_TYPE)
          .setBody(
            ListDraftsResponse().apply {
              factory = GsonFactory.getDefaultInstance()
              drafts = draftsCache.values.map { draft ->
                Draft().apply {
                  id = draft.id
                  message = Message().apply {
                    id = draft.message.id
                  }
                }
              }
            }.toString()
          )
      }

      request.method == "POST" && request.path == "/gmail/v1/users/me/drafts" -> {
        val (draft, mimeMessage) = getDraftAndMimeMessageFromRequest(request)

        val newDraft = when (mimeMessage.subject) {
          MESSAGE_SUBJECT_FIRST -> prepareDraft(
            draftId = DRAFT_ID_FIRST,
            messageId = MESSAGE_ID_FIRST,
            messageThreadId = THREAD_ID_FIRST,
            rawMsg = draft.message.raw
          )

          MESSAGE_SUBJECT_SECOND -> prepareDraft(
            draftId = DRAFT_ID_SECOND,
            messageId = MESSAGE_ID_SECOND,
            messageThreadId = THREAD_ID_SECOND,
            rawMsg = draft.message.raw
          )

          else -> return super.handleCommonAPICalls(request)
        }

        draftsCache.put(newDraft.id, newDraft)
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setBody(newDraft.toString())
      }

      request.path == "/gmail/v1/users/me/messages/${MESSAGE_ID_FIRST}?fields=id,threadId,historyId&format=full" -> {
        genMsgDetailsMockResponse(MESSAGE_ID_FIRST, THREAD_ID_FIRST)
      }

      else -> {
        super.handleCommonAPICalls(request)
      }
    }
  }

  override fun genUserMessagesGetWithFieldsFormatFullResponse(path: String): MockResponse {
    val messageId =
      REGEX_USER_MESSAGES_GET_WITH_FIELDS_FORMAT_FULL.find(path)?.groups?.get(1)?.value?.trim()

    val message = when (messageId) {
      MESSAGE_ID_FIRST -> {
        genFirstMessage()
      }

      MESSAGE_ID_SECOND -> {
        genSecondMessage()
      }

      else -> return super.genUserMessagesGetWithFieldsFormatFullResponse(path)
    }

    return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
      .setHeader("Content-Type", Json.MEDIA_TYPE)
      .setBody(message.toString())
  }

  override fun getAllowedIdsForMessagesBatchDelete(): Collection<String> {
    return super.getAllowedIdsForMessagesBatchDelete() + listOf(
      MESSAGE_ID_FIRST,
      MESSAGE_ID_SECOND
    )
  }

  override fun getAllowedIdsForMessagesBatchModify(): Collection<String> {
    return super.getAllowedIdsForMessagesBatchModify() + listOf(
      MESSAGE_ID_FIRST,
      MESSAGE_ID_SECOND
    )
  }

  override fun handleBatchDeleteMessagesRequest(batchDeleteMessagesRequest: BatchDeleteMessagesRequest) {
    super.handleBatchDeleteMessagesRequest(batchDeleteMessagesRequest)

    for (id in batchDeleteMessagesRequest.ids) {
      draftsCache.filter {
        it.value.message.id == id
      }.forEach {
        draftsCache.remove(it.key)
      }
    }
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

  protected fun getMimeMessageFromDraft(draft: Draft?): MimeMessage? {
    if (draft?.message?.raw == null) {
      return null
    }

    val rawMimeMessageAsByteArray = Base64.decode(
      draft.message.raw, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
    )
    return MimeMessage(Session.getInstance(Properties()), rawMimeMessageAsByteArray.inputStream())
  }

  protected fun genMsgDetailsMockResponse(messageId: String, messageThreadId: String) =
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

  protected fun openComposeScreenAndTypeData(text: String) {
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
        typeText(text),
        closeSoftKeyboard()
      )

    //type some text in the message
    onView(withId(R.id.editTextEmailMessage))
      .check(matches(isDisplayed()))
      .perform(
        scrollTo(),
        click(),
        typeText(text),
        closeSoftKeyboard()
      )
  }

  protected fun moveToDraftFolder() {
    onView(withId(R.id.drawer_layout))
      .check(matches(isClosed(Gravity.LEFT)))
      .perform(open())

    onView(withId(R.id.navigationView))
      .perform(clickOnFolderWithName(JavaEmailConstants.FOLDER_DRAFT))

    waitForObjectWithText(JavaEmailConstants.FOLDER_DRAFT, TimeUnit.SECONDS.toMillis(2))
  }

  protected fun genRawMimeBase64Encoded(msgSubject: String): String {
    val raw = ByteArrayOutputStream().apply {
      this.use {
        MimeMessage(Session.getInstance(Properties())).apply {
          setFrom(accountEntity.email)
          subject = msgSubject
          setContent(MimeMultipart().apply {
            addBodyPart(MimeBodyPart().apply {
              setText(msgSubject)
            })
          })
        }.writeTo(it)
      }
    }.toByteArray()

    return Base64.encodeToString(
      raw, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
    )
  }

  protected fun getMimeMessageFromCache(draftId: String): MimeMessage {
    val raw = draftsCache[draftId]?.message?.raw ?: error("Draft not found")

    val rawMimeMessageAsByteArrayOfSecondMsg = Base64.decode(
      raw, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
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
        headers = prepareMessageHeaders(messageId, subject)
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
            body = MessagePartBody().apply {
              setSize(subject.length)
              data = java.util.Base64.getEncoder()
                .encodeToString(subject.toByteArray())
            }
          }
        )
      }
    }

  private fun prepareMessageHeaders(messageId: String, subject: String) = listOf(
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
      value = messageId
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

  private fun genThreadDetailsMockResponse(request: RecordedRequest): MockResponse {
    val path = request.path ?: ""
    val threadId =
      REGEX_USER_THREADS_GET_FORMAT_FULL.find(path)?.groups?.get(1)?.value?.trim()

    val message = when (threadId) {
      THREAD_ID_FIRST -> {
        genFirstMessage()
      }

      THREAD_ID_SECOND -> {
        genSecondMessage()
      }

      else -> return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
    }

    return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
      .setHeader("Content-Type", Json.MEDIA_TYPE)
      .setBody(
        Thread().apply {
          factory = GsonFactory.getDefaultInstance()
          id = threadId
          messages = listOf(message)
        }.toString()
      )
  }

  private fun genListDraftsResponseForRfc822msgidSearch(path: String): MockResponse {
    val messageId =
      REGEX_DRAFT_BY_RFC822MSGID.find(path)?.groups?.get(1)?.value?.trim()

    val draft = when (messageId) {
      MESSAGE_ID_FIRST -> Draft().apply {
        id = DRAFT_ID_FIRST
        message = Message().apply {
          id = MESSAGE_ID_FIRST
        }
      }

      MESSAGE_ID_SECOND -> Draft().apply {
        id = DRAFT_ID_SECOND
        message = Message().apply {
          id = MESSAGE_ID_SECOND
        }
      }

      else -> return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
    }

    return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
      .setHeader("Content-Type", Json.MEDIA_TYPE)
      .setBody(
        ListDraftsResponse().apply {
          factory = GsonFactory.getDefaultInstance()
          drafts = listOf(draft)
        }.toString()
      )
  }

  private fun genUserMessagesGetFormatFullResponseInternal(path: String): MockResponse {
    val messageId = REGEX_USER_MESSAGES_GET_FORMAT_FULL.find(path)?.groups?.get(1)?.value?.trim()
    val baseResponse = MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
      .setHeader("Content-Type", Json.MEDIA_TYPE)

    val message = when (messageId) {
      MESSAGE_ID_FIRST -> {
        genFirstMessage()
      }

      MESSAGE_ID_SECOND -> {
        genSecondMessage()
      }

      else -> return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
    }

    return baseResponse.setBody(message.toString())
  }

  private fun genUserMessagesRawResponse(path: String): MockResponse {
    val messageId =
      REGEX_USER_MESSAGES_GET_RAW.find(path)?.groups?.get(1)?.value?.trim()

    val key = when (messageId) {
      MESSAGE_ID_FIRST -> DRAFT_ID_FIRST
      MESSAGE_ID_SECOND -> DRAFT_ID_SECOND
      else -> return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
    }

    val raw = draftsCache[key]?.message?.raw ?: error("Draft not found")
    return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
      .setHeader("Content-Type", Json.MEDIA_TYPE)
      //ref com.flowcrypt.email.api.email.gmail.api.GMailRawMIMEMessageFilterInputStream
      .setBody(
        "{\n  \"raw\": \"$raw\"\n}\n"
      )
  }

  private fun genFirstMessage(): Message = genMessage(
    messageId = MESSAGE_ID_FIRST,
    messageThreadId = THREAD_ID_FIRST,
    subject = getMimeMessageFromDraft(draftsCache[DRAFT_ID_FIRST])?.subject ?: "",
    historyIdValue = HISTORY_ID_FIRST
  )

  private fun genSecondMessage(): Message = genMessage(
    messageId = MESSAGE_ID_SECOND,
    messageThreadId = THREAD_ID_SECOND,
    subject = getMimeMessageFromDraft(draftsCache[DRAFT_ID_SECOND])?.subject ?: "",
    historyIdValue = HISTORY_ID_SECOND
  )

  companion object {
    const val DRAFT_ID_FIRST = "r5555555555555500001"
    const val MESSAGE_ID_FIRST = "5555555555500001"
    const val THREAD_ID_FIRST = "1111111111100001"
    const val MESSAGE_SUBJECT_FIRST = "first"
    val HISTORY_ID_FIRST = BigInteger("1111111")

    const val DRAFT_ID_SECOND = "r5555555555555500002"
    const val MESSAGE_ID_SECOND = "5555555555500002"
    const val THREAD_ID_SECOND = "1111111111100002"
    const val MESSAGE_SUBJECT_SECOND = "second"
    val HISTORY_ID_SECOND = BigInteger("2222222")
  }
}
