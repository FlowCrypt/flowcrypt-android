/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.gmailapi

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.adapter.GmailApiLabelsListAdapter
import com.flowcrypt.email.ui.gmailapi.base.BaseThreadDetailsGmailApiFlowTest
import com.flowcrypt.email.viewaction.CustomViewActions.swipeToRefresh
import com.google.api.client.json.Json
import com.google.api.client.json.gson.GsonFactory
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.net.HttpURLConnection

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@FlowCryptTestSettings(useCommonIdling = false)
class ThreadDetailsSwipeToRefreshGmailApiFlowTest : BaseThreadDetailsGmailApiFlowTest() {

  private var shouldReturnOnlyOneMessage = false

  override val mockWebServerRule =
    FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        return when {
          //https://flowcrypt.test/gmail/v1/users/me/threads/200000e222d6c001?format=full
          request.method == "GET" && request.path?.matches(REGEX_USER_THREADS_GET_FORMAT_FULL) == true -> {
            val path = request.path ?: ""
            val threadId =
              REGEX_USER_THREADS_GET_FORMAT_FULL.find(path)?.groups?.get(1)?.value?.trim()
            if (threadId == THREAD_ID_ONLY_STANDARD) {
              if (shouldReturnOnlyOneMessage) {
                MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
                  .setHeader("Content-Type", Json.MEDIA_TYPE)
                  .setBody(
                    com.google.api.services.gmail.model.Thread().apply {
                      factory = GsonFactory.getDefaultInstance()
                      id = THREAD_ID_ONLY_STANDARD
                      messages = listOf(
                        genStandardMessage(
                          threadId = THREAD_ID_ONLY_STANDARD,
                          messageId = MESSAGE_ID_THREAD_ONLY_STANDARD_1,
                          isFullFormat = true,
                          includeBinaryAttachment = false
                        )
                      )
                    }.toString()
                  )
              } else {
                handleCommonAPICalls(request)
              }
            } else {
              handleCommonAPICalls(request)
            }
          }

          else -> handleCommonAPICalls(request)
        }
      }
    })

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(mockWebServerRule)
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(addLabelsToDatabaseRule)
    .around(customLabelsRule)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testUpdatingThreadAfterSwipeToRefresh() {
    openThreadBasedOnPosition(3)
    checkCorrectThreadDetails(
      messagesCount = 2,
      threadSubject = SUBJECT_EXISTING_STANDARD,
      labels = listOf(
        GmailApiLabelsListAdapter.Label("Inbox")
      )
    )

    shouldReturnOnlyOneMessage = true

    waitForObjectWithText(SUBJECT_EXISTING_STANDARD)
    checkCollapsedState(1, hasPgp = false, hasAttachments = true)

    //swipe down to refresh
    onView(allOf(withId(R.id.swipeRefreshLayout), isDisplayed()))
      .perform(swipeToRefresh(swipeDown(), isDisplayingAtLeast(85)))
    Thread.sleep(1000)

    checkCorrectThreadDetails(
      messagesCount = 1,
      threadSubject = SUBJECT_EXISTING_STANDARD,
      labels = listOf(
        GmailApiLabelsListAdapter.Label("Inbox")
      )
    )
  }
}