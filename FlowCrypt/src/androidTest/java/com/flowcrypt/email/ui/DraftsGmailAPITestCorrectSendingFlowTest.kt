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
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.jetpack.viewmodel.DraftViewModel
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.base.BaseDraftsGmailAPIFlowTest
import com.flowcrypt.email.util.TestGeneralUtil
import com.flowcrypt.email.viewaction.ClickOnViewInRecyclerViewItem
import com.google.api.client.json.JsonObjectParser
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.model.Draft
import jakarta.mail.internet.MimeMultipart
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.core.AllOf.allOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

/**
 * https://github.com/FlowCrypt/flowcrypt-android/issues/2050
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@FlowCryptTestSettings(useCommonIdling = false)
class DraftsGmailAPITestCorrectSendingFlowTest : BaseDraftsGmailAPIFlowTest() {
  private val sentCache = mutableListOf<com.google.api.services.gmail.model.Message>()

  override val mockWebServerRule: FlowCryptMockWebServerRule = FlowCryptMockWebServerRule(
    TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        return when {
          request.path?.startsWith("/attester/pub", ignoreCase = true) == true -> {
            val lastSegment = request.requestUrl?.pathSegments?.lastOrNull()

            when {
              TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER.equals(lastSegment, true) -> {
                MockResponse()
                  .setResponseCode(HttpURLConnection.HTTP_OK)
                  .setBody(TestGeneralUtil.readResourceAsString("3.txt"))
              }

              else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
            }
          }

          request.method == "PUT" && request.path == "/upload/gmail/v1/users/me/messages/send?uploadType=resumable&upload_id=Location" -> {
            val message = com.google.api.services.gmail.model.Message().apply {
              factory = GsonFactory.getDefaultInstance()
              id = MESSAGE_ID_SENT
              threadId = THREAD_ID_SENT
              labelIds = listOf(JavaEmailConstants.FOLDER_SENT)
            }

            sentCache.add(message)

            MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
              .setBody(message.toString())
          }

          request.path == "/gmail/v1/users/me/messages/${MESSAGE_ID_FIRST}?fields=id,threadId,historyId&format=full" -> {
            return genMsgDetailsMockResponse(MESSAGE_ID_FIRST, THREAD_ID_FIRST)
          }

          request.method == "POST" && request.path == "/upload/gmail/v1/users/me/drafts/send?uploadType=resumable" -> {
            val gzipInputStream = GZIPInputStream(request.body.inputStream())
            val draft = JsonObjectParser(GsonFactory.getDefaultInstance()).parseAndClose(
              InputStreamReader(gzipInputStream), Draft::class.java
            )

            if (draft.id == DRAFT_ID_FIRST) {
              MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
                .setHeader("Location", LOCATION_URL)
                .setBody(com.google.api.services.gmail.model.Message().apply {
                  factory = GsonFactory.getDefaultInstance()
                  id = MESSAGE_ID_SENT
                  threadId = THREAD_ID_SENT
                  labelIds = listOf(JavaEmailConstants.FOLDER_SENT)
                }.toString())
            } else {
              MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
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
  @FlakyTest
  fun testCorrectDraftsSending() {
    sentCache.clear()
    moveToDraftFolder()

    //create a new draft
    openComposeScreenAndTypeData(MESSAGE_SUBJECT_FIRST)

    openActionBarOverflowOrOptionsMenu(getTargetContext())
    onView(withText(R.string.switch_to_standard_email))
      .check(matches(isDisplayed()))
      .perform(click())
    waitUntil(DraftViewModel.DELAY_TIMEOUT * 2) {
      draftsCache.isNotEmpty()
    }

    //check that draft was created
    assertEquals(1, draftsCache.size)
    val mimeMessage = getMimeMessageFromCache(DRAFT_ID_FIRST)
    assertEquals(MESSAGE_SUBJECT_FIRST, mimeMessage.subject)
    assertEquals(
      MESSAGE_SUBJECT_FIRST,
      (mimeMessage.content as MimeMultipart).getBodyPart(0).content as String
    )

    //move to the drafts list
    pressBack()
    waitForObjectWithText(MESSAGE_SUBJECT_FIRST, TimeUnit.SECONDS.toMillis(10))

    //open created draft and send
    onView(allOf(withId(R.id.recyclerViewMsgs), isDisplayed())).perform(
      actionOnItemAtPosition<ViewHolder>(0, click())
    )
    waitForObjectWithText(MESSAGE_SUBJECT_FIRST, TimeUnit.SECONDS.toMillis(10))

    //click to edit a draft
    onView(allOf(withId(R.id.recyclerViewMessages), isDisplayed()))
      .perform(
        actionOnItemAtPosition<ViewHolder>(
          1,
          ClickOnViewInRecyclerViewItem(R.id.imageButtonEditDraft)
        )
      )

    //wait rendering a draft on the compose message screen
    waitForObjectWithText(MESSAGE_SUBJECT_FIRST, TimeUnit.SECONDS.toMillis(10))

    onView(withId(R.id.editTextEmailAddress))
      .perform(
        replaceText(TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER),
        pressImeActionButton()
      )

    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())

    //need to wait while a message will be sent
    waitUntil {
      val countOfOutgoingMessages = runBlocking {
        roomDatabase.msgDao().getOutboxMsgsSuspend(addAccountToDatabaseRule.account.email).size
      }
      countOfOutgoingMessages == 0
    }

    assertEquals(0, runBlocking {
      roomDatabase.msgDao().getOutboxMsgsSuspend(addAccountToDatabaseRule.account.email).size
    })

    //check that we have a new sent message in the cache
    assertEquals(1, sentCache.size)
    assertEquals(MESSAGE_ID_SENT, sentCache[0].id)
  }

  companion object {
    const val MESSAGE = "Some message"
    const val MESSAGE_ID_SENT = "5555555555555553"
    const val THREAD_ID_SENT = "1111111111111113"
    const val LOCATION_URL =
      "https://flowcrypt.test/upload/gmail/v1/users/me/messages/send?uploadType=resumable&upload_id=Location"
  }
}
