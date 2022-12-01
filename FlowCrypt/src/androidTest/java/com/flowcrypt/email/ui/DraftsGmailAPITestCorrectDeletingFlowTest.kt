/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.base.BaseDraftsGmailAPIFlowTest
import com.flowcrypt.email.util.AccountDaoManager
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.model.Message
import com.google.api.services.gmail.model.MessagePart
import com.google.api.services.gmail.model.MessagePartBody
import com.google.api.services.gmail.model.MessagePartHeader
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.math.BigInteger
import java.net.HttpURLConnection
import kotlin.random.Random

/**
 * @author Denis Bondarenko
 *         Date: 11/28/22
 *         Time: 6:23 PM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class DraftsGmailAPITestCorrectDeletingFlowTest : BaseDraftsGmailAPIFlowTest() {
  override val mockWebServerRule: FlowCryptMockWebServerRule = FlowCryptMockWebServerRule(
    TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        return when {
          request.method == "GET" && request.path ==
              "/gmail/v1/users/me/messages/$MESSAGE_ID_FIRST?" +
              "fields=id,threadId,labelIds,snippet,sizeEstimate,historyId,internalDate," +
              "payload/partId,payload/mimeType,payload/filename,payload/headers," +
              "payload/body,payload/parts(partId,mimeType,filename,headers,body/size,body/attachmentId)" +
              "&format=full" -> {

            MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
              .setHeader("Content-Type", "application/json; charset=UTF-8")
              .setBody(genMessage(MESSAGE_ID_FIRST, THREAD_ID_FIRST, MESSAGE_SUBJECT_FIRST))
          }

          request.method == "GET" && request.path ==
              "/gmail/v1/users/me/messages/$MESSAGE_ID_SECOND?" +
              "fields=id,threadId,labelIds,snippet,sizeEstimate,historyId,internalDate," +
              "payload/partId,payload/mimeType,payload/filename,payload/headers," +
              "payload/body,payload/parts(partId,mimeType,filename,headers,body/size,body/attachmentId)" +
              "&format=full" -> {

            MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
              .setHeader("Content-Type", "application/json; charset=UTF-8")
              .setBody(genMessage(MESSAGE_ID_SECOND, THREAD_ID_SECOND, MESSAGE_SUBJECT_SECOND))
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
      .around(addLabelsToDatabaseRule)
      .around(activityScenarioRule)
      .around(ScreenshotTestRule())

  @Before
  fun prepareDrafts() {
    val firstDraft = prepareDraft(
      draftId = DRAFT_ID_FIRST,
      messageId = MESSAGE_ID_FIRST,
      messageThreadId = THREAD_ID_FIRST,
      rawMsg = genRawMimeWithSubject(MESSAGE_SUBJECT_FIRST)
    )
    draftsCache.add(firstDraft)

    val secondDraft = prepareDraft(
      draftId = DRAFT_ID_SECOND,
      messageId = MESSAGE_ID_SECOND,
      messageThreadId = THREAD_ID_SECOND,
      rawMsg = genRawMimeWithSubject(MESSAGE_SUBJECT_SECOND)
    )
    draftsCache.add(secondDraft)
  }

  @Test
  fun testCorrectDraftsDeleting() {
    moveToDraftFolder()
    Thread.sleep(5000)
  }

  private fun genMessage(messageId: String, messageThreadId: String, subject: String) =
    Message().apply {
      factory = GsonFactory.getDefaultInstance()
      id = messageId
      threadId = messageThreadId
      labelIds = listOf(JavaEmailConstants.FOLDER_DRAFT)
      snippet = subject
      historyId = BigInteger.valueOf(Random.nextLong())
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
}
