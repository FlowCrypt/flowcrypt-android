/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import android.view.InputDevice
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withEmptyRecyclerView
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withRecyclerViewItemCount
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.base.BaseDraftsGmailAPIFlowTest
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.viewaction.CustomViewActions.swipeToRefresh
import com.google.api.client.json.Json
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.model.History
import com.google.api.services.gmail.model.HistoryMessageDeleted
import com.google.api.services.gmail.model.ListDraftsResponse
import com.google.api.services.gmail.model.ListHistoryResponse
import com.google.api.services.gmail.model.Message
import com.google.api.services.gmail.model.MessagePart
import com.google.api.services.gmail.model.MessagePartBody
import com.google.api.services.gmail.model.MessagePartHeader
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.math.BigInteger
import java.net.HttpURLConnection


/**
 * https://github.com/FlowCrypt/flowcrypt-android/issues/2050
 *
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
              .setHeader("Content-Type", Json.MEDIA_TYPE)
              .setBody(
                genMessage(
                  messageId = MESSAGE_ID_FIRST,
                  messageThreadId = THREAD_ID_FIRST,
                  subject = MESSAGE_SUBJECT_FIRST,
                  historyIdValue = HISTORY_ID_FIRST
                )
              )
          }

          request.method == "GET" && request.path ==
              "/gmail/v1/users/me/messages/$MESSAGE_ID_SECOND?" +
              "fields=id,threadId,labelIds,snippet,sizeEstimate,historyId,internalDate," +
              "payload/partId,payload/mimeType,payload/filename,payload/headers," +
              "payload/body,payload/parts(partId,mimeType,filename,headers,body/size,body/attachmentId)" +
              "&format=full" -> {

            MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
              .setHeader("Content-Type", Json.MEDIA_TYPE)
              .setBody(
                genMessage(
                  messageId = MESSAGE_ID_SECOND,
                  messageThreadId = THREAD_ID_SECOND,
                  subject = MESSAGE_SUBJECT_SECOND,
                  historyIdValue = HISTORY_ID_SECOND
                )
              )
          }

          request.method == "GET" && request.path == "/gmail/v1/users/me/history?" +
              "labelId=${JavaEmailConstants.FOLDER_DRAFT}" +
              "&startHistoryId=$HISTORY_ID_FIRST" -> {
            MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
              .setBody(ListHistoryResponse().apply {
                factory = GsonFactory.getDefaultInstance()
                historyId = BigInteger("40066765")
                history = listOf(History().apply {
                  id = BigInteger("40066715")
                  messages = listOf(Message().apply {
                    id = MESSAGE_ID_SECOND
                    threadId = THREAD_ID_SECOND
                  })
                  messagesDeleted = listOf(HistoryMessageDeleted().apply {
                    message = Message().apply {
                      id = MESSAGE_ID_SECOND
                      threadId = THREAD_ID_SECOND
                      labelIds = listOf(JavaEmailConstants.FOLDER_DRAFT)
                    }
                  })
                })
              }.toString())
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

    //select the second draft
    selectDraft()

    //delete the second draft
    onView(withId(R.id.menuActionDeleteMessage))
      .check(matches(ViewMatchers.isDisplayed()))
      .perform(click())

    //swipe down to refresh
    onView(withId(R.id.swipeRefreshLayout))
      .perform(swipeToRefresh(swipeDown(), isDisplayingAtLeast(85)))
    Thread.sleep(1000)

    //check that only the first message exists in the local cache
    onView(withId(R.id.recyclerViewMsgs))
      .check(matches(withRecyclerViewItemCount(1)))
    onView(withId(R.id.recyclerViewMsgs))
      .perform(
        // scrollTo will fail the test if no item matches.
        RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
          hasDescendant(withText(MESSAGE_SUBJECT_FIRST))
        )
      )

    //check that on the server side we have only one draft
    val responseAfterDeletingSecondDraft = runBlocking {
      GmailApiHelper.loadMsgsBaseInfo(
        context = getTargetContext(),
        accountEntity = addAccountToDatabaseRule.account,
        localFolder = addLabelsToDatabaseRule.folders.first { it.isDrafts },
        fields = listOf("drafts/id", "drafts/message/id"),
        maxResult = 500
      )
    }

    assertEquals(1, (responseAfterDeletingSecondDraft as ListDraftsResponse).drafts.size)

    //############################################################################################

    //select the first draft
    selectDraft()

    //delete the first draft
    onView(withId(R.id.menuActionDeleteMessage))
      .check(matches(ViewMatchers.isDisplayed()))
      .perform(click())

    //swipe down to refresh
    onView(withId(R.id.swipeRefreshLayout))
      .perform(swipeToRefresh(swipeDown(), isDisplayingAtLeast(85)))
    Thread.sleep(1000)

    //check that there is no drafts in the local cache
    onView(withId(R.id.recyclerViewMsgs))
      .check(matches(withEmptyRecyclerView()))

    //check that on the server side we have no drafts
    val responseAfterDeletingFirstDraft = runBlocking {
      GmailApiHelper.loadMsgsBaseInfo(
        context = getTargetContext(),
        accountEntity = addAccountToDatabaseRule.account,
        localFolder = addLabelsToDatabaseRule.folders.first { it.isDrafts },
        fields = listOf("drafts/id", "drafts/message/id"),
        maxResult = 500
      )
    }

    assertEquals(0, (responseAfterDeletingFirstDraft as ListDraftsResponse).drafts.size)
  }

  private fun selectDraft(position: Int = 0) {
    onView(withId(R.id.recyclerViewMsgs))
      .perform(
        actionOnItemAtPosition<RecyclerView.ViewHolder>(
          position,
          ViewActions.actionWithAssertions(
            GeneralClickAction(
              Tap.LONG,
              GeneralLocation.CENTER,
              Press.FINGER,
              InputDevice.SOURCE_TOUCHSCREEN,
              MotionEvent.BUTTON_PRIMARY
            )
          )
        )
      )
  }

  private fun genMessage(
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
    val HISTORY_ID_FIRST = BigInteger("4444444")
    val HISTORY_ID_SECOND = BigInteger("5555555")
  }
}
