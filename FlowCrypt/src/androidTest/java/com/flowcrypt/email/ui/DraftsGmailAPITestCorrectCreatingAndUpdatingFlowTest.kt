/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.jetpack.viewmodel.DraftViewModel
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withRecyclerViewItemCount
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.DraftsGmailAPITestCorrectDeletingFlowTest.Companion.HISTORY_ID_FIRST
import com.flowcrypt.email.ui.base.BaseDraftsGmailAPIFlowTest
import com.google.api.client.json.Json
import com.google.api.services.gmail.model.Message
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.CoreMatchers.allOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.net.HttpURLConnection

/**
 * https://github.com/FlowCrypt/flowcrypt-android/issues/2050
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class DraftsGmailAPITestCorrectCreatingAndUpdatingFlowTest : BaseDraftsGmailAPIFlowTest() {
  override val mockWebServerRule =
    FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        when {
          request.method == "PUT" && request.path == "/gmail/v1/users/me/drafts/$DRAFT_ID_FIRST" -> {
            val (draft, mimeMessage) = getDraftAndMimeMessageFromRequest(request)
            val existingDraftInCache = draftsCache.firstOrNull { it.id == DRAFT_ID_FIRST }

            return if (existingDraftInCache != null && mimeMessage.subject == MESSAGE_SUBJECT_FIRST_EDITED) {
              val existingMessage = existingDraftInCache.message
              existingDraftInCache.message = Message().apply {
                id = existingMessage.id
                threadId = existingMessage.threadId
                labelIds = existingMessage.labelIds
                raw = draft.message.raw
              }

              MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(existingDraftInCache.toString())
            } else {
              MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
            }
          }

          request.method == "POST" && request.path == "/gmail/v1/users/me/drafts" -> {
            val (draft, mimeMessage) = getDraftAndMimeMessageFromRequest(request)

            return when (mimeMessage.subject) {
              MESSAGE_SUBJECT_FIRST -> {
                val newDraft = prepareDraft(
                  draftId = DRAFT_ID_FIRST,
                  messageId = MESSAGE_ID_FIRST,
                  messageThreadId = THREAD_ID_FIRST,
                  rawMsg = draft.message.raw
                )
                draftsCache.add(newDraft)

                MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
                  .setBody(newDraft.toString())
              }

              MESSAGE_SUBJECT_SECOND -> {
                val newDraft = prepareDraft(
                  draftId = DRAFT_ID_SECOND,
                  messageId = MESSAGE_ID_SECOND,
                  messageThreadId = THREAD_ID_SECOND,
                  rawMsg = draft.message.raw
                )
                draftsCache.add(newDraft)

                MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
                  .setBody(newDraft.toString())
              }

              else -> {
                MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
              }
            }
          }

          request.path == "/gmail/v1/users/me/messages/${MESSAGE_ID_FIRST}?fields=id,threadId,historyId&format=full" -> {
            return genMsgDetailsMockResponse(MESSAGE_ID_FIRST, THREAD_ID_FIRST)
          }

          request.path == "/gmail/v1/users/me/messages/${MESSAGE_ID_SECOND}?fields=id,threadId,historyId&format=full" -> {
            return genMsgDetailsMockResponse(MESSAGE_ID_SECOND, THREAD_ID_SECOND)
          }

          request.method == "GET" && request.path == genPathForGmailMessages(MESSAGE_ID_FIRST) -> {

            return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
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

          else -> return handleCommonAPICalls(request)
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

  @Test
  fun testCorrectCreatingAndUpdating() {
    moveToDraftFolder()

    onView(withId(R.id.recyclerViewMsgs))
      .check(matches(withRecyclerViewItemCount(0)))

    openComposeScreenAndTypeSubject(MESSAGE_SUBJECT_FIRST)
    Thread.sleep(DraftViewModel.DELAY_TIMEOUT * 2)
    pressBack()

    //check that the first draft was created
    assertEquals(1, draftsCache.size)
    onView(withId(R.id.recyclerViewMsgs))
      .check(matches(withRecyclerViewItemCount(1)))
    val mimeMessageFirst = getMimeMessageFromCache(0)
    assertEquals(MESSAGE_SUBJECT_FIRST, mimeMessageFirst.subject)

    openComposeScreenAndTypeSubject(MESSAGE_SUBJECT_SECOND)
    Thread.sleep(DraftViewModel.DELAY_TIMEOUT * 2)
    pressBack()

    //check that the second draft was created
    assertEquals(2, draftsCache.size)
    onView(withId(R.id.recyclerViewMsgs))
      .check(matches(withRecyclerViewItemCount(2)))
    val mimeMessageSecond = getMimeMessageFromCache(1)
    assertEquals(MESSAGE_SUBJECT_SECOND, mimeMessageSecond.subject)

    //open the first draft and modify it
    onView(withId(R.id.recyclerViewMsgs))
      .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))
    //wait for the message details
    Thread.sleep(2000)
    onView(
      allOf(
        //as we have viewpager at this stage need to add additional selector
        hasSibling(allOf(withId(R.id.textViewSubject), withText(MESSAGE_SUBJECT_FIRST))),
        withId(R.id.imageButtonEditDraft)
      )
    ).check(matches(isDisplayed()))
      .perform(click())
    onView(withId(R.id.editTextEmailSubject))
      .check(matches(isDisplayed()))
      .perform(
        scrollTo(),
        click(),
        replaceText(MESSAGE_SUBJECT_FIRST_EDITED),
        closeSoftKeyboard()
      )

    Thread.sleep(DraftViewModel.DELAY_TIMEOUT * 2)
    pressBack()//back to the message details screen
    pressBack()//back to the messages list screen

    //check if 1st draft is updated correctly and 2nd draft remains same
    assertEquals(2, draftsCache.size)
    onView(withId(R.id.recyclerViewMsgs))
      .check(matches(withRecyclerViewItemCount(2)))
    val mimeMessageFirstEdited = getMimeMessageFromCache(0)
    assertEquals(MESSAGE_SUBJECT_FIRST_EDITED, mimeMessageFirstEdited.subject)
    onView(withText(MESSAGE_SUBJECT_FIRST_EDITED))
      .check(matches(isDisplayed()))
    val mimeMessageSecondAfterEditingFirst = getMimeMessageFromCache(1)
    assertEquals(MESSAGE_SUBJECT_SECOND, mimeMessageSecondAfterEditingFirst.subject)
  }

  companion object {
    const val MESSAGE_SUBJECT_FIRST_EDITED = "first edited"
  }
}
