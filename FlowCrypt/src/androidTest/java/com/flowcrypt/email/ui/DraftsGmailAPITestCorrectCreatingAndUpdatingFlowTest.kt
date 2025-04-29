/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui

import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.jetpack.viewmodel.DraftViewModel
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withRecyclerViewItemCount
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.base.BaseDraftsGmailAPIFlowTest
import com.flowcrypt.email.viewaction.ClickOnViewInRecyclerViewItem
import com.google.api.client.json.Json
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.model.BatchModifyMessagesRequest
import com.google.api.services.gmail.model.Draft
import com.google.api.services.gmail.model.ListDraftsResponse
import com.google.api.services.gmail.model.Message
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okio.GzipSource
import okio.buffer
import org.hamcrest.Matchers
import org.hamcrest.core.AllOf.allOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit

/**
 * https://github.com/FlowCrypt/flowcrypt-android/issues/2050
 */
@MediumTest
@FlowCryptTestSettings(useCommonIdling = false)
@RunWith(AndroidJUnit4::class)
class DraftsGmailAPITestCorrectCreatingAndUpdatingFlowTest : BaseDraftsGmailAPIFlowTest() {
  override val mockWebServerRule =
    FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        return when {
          request.method == "PUT" && request.path == "/gmail/v1/users/me/drafts/$DRAFT_ID_FIRST" -> {
            val (draft, mimeMessage) = getDraftAndMimeMessageFromRequest(request)
            val existingDraftInCache = draftsCache[DRAFT_ID_FIRST]

            if (existingDraftInCache != null && mimeMessage.subject == MESSAGE_SUBJECT_FIRST_EDITED) {
              val updatedDraft = draft.clone().apply {
                id = existingDraftInCache.id
                message = Message().apply {
                  id = existingDraftInCache.message.id
                  threadId = existingDraftInCache.message.threadId
                  labelIds = existingDraftInCache.message.labelIds
                  raw = message.raw
                }
              }
              draftsCache.put(DRAFT_ID_FIRST, updatedDraft)
              MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(updatedDraft.toString())
            } else {
              MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
            }
          }

          request.method == "POST" && request.path == "/gmail/v1/users/me/drafts" -> {
            val (draft, mimeMessage) = getDraftAndMimeMessageFromRequest(request)

            when (mimeMessage.subject) {
              MESSAGE_SUBJECT_FIRST -> {
                val newDraft = prepareDraft(
                  draftId = DRAFT_ID_FIRST,
                  messageId = MESSAGE_ID_FIRST,
                  messageThreadId = THREAD_ID_FIRST,
                  rawMsg = draft.message.raw
                )
                draftsCache.put(DRAFT_ID_FIRST, newDraft)

                MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
                  .setBody(newDraft.toString())
              }

              else -> {
                MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
              }
            }
          }

          request.path == "/gmail/v1/users/me/messages/${MESSAGE_ID_FIRST}?fields=id,threadId,historyId&format=full" -> {
            genMsgDetailsMockResponse(MESSAGE_ID_FIRST, THREAD_ID_FIRST)
          }

          request.method == "GET" && request.path?.matches(REGEX_USER_THREADS_GET_FORMAT_FULL) == true -> {
            val path = request.path ?: ""
            val threadId =
              REGEX_USER_THREADS_GET_FORMAT_FULL.find(path)?.groups?.get(1)?.value?.trim()
            when (threadId) {
              THREAD_ID_FIRST -> {
                MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
                  .setHeader("Content-Type", Json.MEDIA_TYPE)
                  .setBody(
                    com.google.api.services.gmail.model.Thread().apply {
                      factory = GsonFactory.getDefaultInstance()
                      id = THREAD_ID_FIRST
                      messages = listOf(
                        genMessage(
                          messageId = MESSAGE_ID_FIRST,
                          messageThreadId = THREAD_ID_FIRST,
                          subject = getMimeMessageFromDraft(draftsCache[DRAFT_ID_FIRST])?.subject
                            ?: "",
                          historyIdValue = HISTORY_ID_FIRST
                        )
                      )
                    }.toString()
                  )
              }

              else -> {
                MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
              }
            }
          }

          request.method == "GET" && request.path == genPathForGmailMessages(MESSAGE_ID_FIRST) -> {

            MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
              .setHeader("Content-Type", Json.MEDIA_TYPE)
              .setBody(
                genMessage(
                  messageId = MESSAGE_ID_FIRST,
                  messageThreadId = THREAD_ID_FIRST,
                  subject = getMimeMessageFromDraft(draftsCache[DRAFT_ID_FIRST])?.subject ?: "",
                  historyIdValue = HISTORY_ID_FIRST
                ).toString()
              )
          }

          request.method == "GET" && request.path?.matches(REGEX_DRAFT_BY_RFC822MSGID) == true -> {
            genListDraftsResponseForRfc822msgidSearch(request.path ?: "")
          }

          request.method == "GET" && request.path?.matches(REGEX_USER_MESSAGES_GET_FORMAT_FULL) == true -> {
            val path = request.path ?: ""
            val messageId =
              REGEX_USER_MESSAGES_GET_FORMAT_FULL.find(path)?.groups?.get(1)?.value?.trim()
            if (messageId in listOf(MESSAGE_ID_FIRST)) {
              genUserMessagesGetFormatFullResponseInternal(path)
            } else {
              handleCommonAPICalls(request)
            }
          }

          request.path == "/gmail/v1/users/me/messages/5555555555500001?fields=raw&format=raw" -> {
            MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
              .setHeader("Content-Type", Json.MEDIA_TYPE)
              .setBody(draftsCache[DRAFT_ID_FIRST]?.message?.raw ?: error("Draft not found"))
          }

          request.method == "POST" && request.path == "/gmail/v1/users/me/messages/batchModify" -> {
            val source = GzipSource(request.body)
            val batchModifyMessagesRequest = GsonFactory.getDefaultInstance().fromInputStream(
              source.buffer().inputStream(),
              BatchModifyMessagesRequest::class.java
            )

            val handledIds = arrayOf(MESSAGE_ID_FIRST)

            if (handledIds.any { batchModifyMessagesRequest.ids.contains(it) }) {
              MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
            } else {
              MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
            }
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

  @Test
  fun testCorrectCreatingAndUpdating() {
    moveToDraftFolder()

    onView(withId(R.id.recyclerViewMsgs))
      .check(matches(withRecyclerViewItemCount(0)))

    //create a draft
    openComposeScreenAndTypeData(MESSAGE_SUBJECT_FIRST)
    //switch to standard mode
    openActionBarOverflowOrOptionsMenu(getTargetContext())
    onView(withText(R.string.switch_to_standard_email))
      .check(matches(isDisplayed()))
      .perform(click())
    waitUntil(DraftViewModel.DELAY_TIMEOUT * 2) { draftsCache.isNotEmpty() }
    pressBack()
    waitForObjectWithText(MESSAGE_SUBJECT_FIRST, TimeUnit.SECONDS.toMillis(10))

    //check that the a thread with a single draft was created
    assertEquals(1, draftsCache.size)
    onView(withId(R.id.recyclerViewMsgs))
      .check(matches(withRecyclerViewItemCount(1)))
    val mimeMessageFirst = getMimeMessageFromCache(DRAFT_ID_FIRST)
    assertEquals(MESSAGE_SUBJECT_FIRST, mimeMessageFirst.subject)

    //open the created thread and wait rendering
    onView(allOf(withId(R.id.recyclerViewMsgs), isDisplayed())).perform(
      actionOnItemAtPosition<ViewHolder>(0, click())
    )
    waitForObjectWithText(MESSAGE_SUBJECT_FIRST, TimeUnit.SECONDS.toMillis(10))

    onView(Matchers.allOf(withId(R.id.recyclerViewMessages), isDisplayed()))
      .perform(
        RecyclerViewActions.scrollTo<ViewHolder>(
          hasDescendant(
            allOf(
              withId(R.id.textViewSubject),
              withText(MESSAGE_SUBJECT_FIRST)
            )
          )
        )
      )

    //click to edit a draft
    onView(Matchers.allOf(withId(R.id.recyclerViewMessages), isDisplayed()))
      .perform(
        actionOnItemAtPosition<ViewHolder>(
          1,
          ClickOnViewInRecyclerViewItem(R.id.imageButtonEditDraft)
        )
      )

    //wait rendering a draft on the compose message screen
    waitForObjectWithText(MESSAGE_SUBJECT_FIRST, TimeUnit.SECONDS.toMillis(10))

    //update the draft subject
    onView(withId(R.id.editTextEmailSubject))
      .check(matches(isDisplayed()))
      .perform(
        scrollTo(),
        click(),
        replaceText(MESSAGE_SUBJECT_FIRST_EDITED),
        closeSoftKeyboard()
      )

    //back to the message details screen and check if content was updated
    pressBack()
    waitForObjectWithText(MESSAGE_SUBJECT_FIRST_EDITED, TimeUnit.SECONDS.toMillis(10))
    onView(Matchers.allOf(withId(R.id.recyclerViewMessages), isDisplayed()))
      .perform(
        RecyclerViewActions.scrollTo<ViewHolder>(
          hasDescendant(
            allOf(
              withId(R.id.textViewSubject),
              withText(MESSAGE_SUBJECT_FIRST_EDITED)
            )
          )
        )
      )

    //back to the messages list screen and check if content was updated
    pressBack()
    assertEquals(1, draftsCache.size)
    onView(withId(R.id.recyclerViewMsgs))
      .check(matches(withRecyclerViewItemCount(1)))
    val mimeMessageFirstEdited = getMimeMessageFromCache(DRAFT_ID_FIRST)
    assertEquals(MESSAGE_SUBJECT_FIRST_EDITED, mimeMessageFirstEdited.subject)
    onView(withText(MESSAGE_SUBJECT_FIRST_EDITED))
      .check(matches(isDisplayed()))
  }

  private fun genListDraftsResponseForRfc822msgidSearch(path: String): MockResponse {
    val messageId =
      REGEX_DRAFT_BY_RFC822MSGID.find(path)?.groups?.get(1)?.value?.trim()

    val draft = when (messageId) {
      MESSAGE_ID_FIRST ->
        Draft().apply {
          id = DRAFT_ID_FIRST
          message = Message().apply {
            id = MESSAGE_ID_FIRST
          }
        }

      else -> null
    }

    return if (draft != null) {
      MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
        .setHeader("Content-Type", Json.MEDIA_TYPE)
        .setBody(
          ListDraftsResponse().apply {
            factory = GsonFactory.getDefaultInstance()
            drafts = listOf(draft)
          }.toString()
        )
    } else {
      MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
    }
  }

  private fun genUserMessagesGetFormatFullResponseInternal(path: String): MockResponse {
    val messageId = REGEX_USER_MESSAGES_GET_FORMAT_FULL.find(path)?.groups?.get(1)?.value?.trim()
    val baseResponse = MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
      .setHeader("Content-Type", Json.MEDIA_TYPE)

    return when (messageId) {
      MESSAGE_ID_FIRST -> {
        baseResponse.setBody(
          genMessage(
            messageId = MESSAGE_ID_FIRST,
            messageThreadId = THREAD_ID_FIRST,
            subject = getMimeMessageFromDraft(draftsCache[DRAFT_ID_FIRST])?.subject ?: "",
            historyIdValue = HISTORY_ID_FIRST
          ).toString()
        )
      }

      else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
    }
  }

  companion object {
    const val MESSAGE_SUBJECT_FIRST_EDITED = "first edited"
  }
}
