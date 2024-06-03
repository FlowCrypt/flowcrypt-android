/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import com.flowcrypt.email.viewaction.CustomViewActions.waitUntilGone
import com.google.api.client.json.gson.GsonFactory
import jakarta.mail.Message
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMultipart
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
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit

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

          request.method == "PUT" && request.path == "/gmail/v1/users/me/drafts/$DRAFT_ID_FIRST" -> {
            val (draft, _) = getDraftAndMimeMessageFromRequest(request)
            val existingDraftInCache = draftsCache.firstOrNull { it.id == DRAFT_ID_FIRST }

            return if (existingDraftInCache != null) {
              val existingMessage = existingDraftInCache.message
              existingDraftInCache.message = com.google.api.services.gmail.model.Message().apply {
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
            if (mimeMessage.subject == MESSAGE_SUBJECT_FIRST) {
              val newDraft = prepareDraft(
                draftId = DRAFT_ID_FIRST,
                messageId = MESSAGE_ID_FIRST,
                messageThreadId = THREAD_ID_FIRST,
                rawMsg = draft.message.raw
              )
              draftsCache.add(newDraft)
              MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(newDraft.toString())
            } else {
              MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
            }
          }

          request.path == "/gmail/v1/users/me/messages/${MESSAGE_ID_FIRST}?fields=id,threadId,historyId&format=full" -> {
            return genMsgDetailsMockResponse(MESSAGE_ID_FIRST, THREAD_ID_FIRST)
          }

          request.method == "POST" && request.path == "/upload/gmail/v1/users/me/messages/send?uploadType=resumable" -> {
            MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
              .setHeader("Location", LOCATION_URL)
              .setBody(com.google.api.services.gmail.model.Message().apply {
                factory = GsonFactory.getDefaultInstance()
                id = MESSAGE_ID_SENT
                threadId = THREAD_ID_SENT
                labelIds = listOf(JavaEmailConstants.FOLDER_SENT)
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
  fun clearSentCache() {
    sentCache.clear()
  }

  @Test
  fun testCorrectDraftsSending() {
    moveToDraftFolder()

    //create a new draft
    openComposeScreenAndTypeSubject(MESSAGE_SUBJECT_FIRST)

    openActionBarOverflowOrOptionsMenu(getTargetContext())
    onView(withText(R.string.switch_to_standard_email))
      .check(matches(isDisplayed()))
      .perform(click())

    onView(withId(R.id.editTextEmailMessage))
      .perform(
        scrollTo(),
        click(),
        typeText(MESSAGE),
        closeSoftKeyboard()
      )
    onView(withId(R.id.editTextEmailAddress))
      .perform(
        typeText(TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER),
        pressImeActionButton()
      )
    Thread.sleep(DraftViewModel.DELAY_TIMEOUT * 2)

    //check that draft was created
    assertEquals(1, draftsCache.size)
    val mimeMessage = getMimeMessageFromCache(0)
    assertEquals(MESSAGE_SUBJECT_FIRST, mimeMessage.subject)
    assertEquals(
      TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER,
      (mimeMessage.getRecipients(Message.RecipientType.TO).first() as InternetAddress).address
    )
    assertEquals(
      MESSAGE,
      (mimeMessage.content as MimeMultipart).getBodyPart(0).content as String
    )

    //move to the drafts list
    Espresso.pressBack()

    //open created draft and send
    onView(withId(R.id.recyclerViewMsgs))
      .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
    waitForObjectWithText(MESSAGE_SUBJECT_FIRST, TimeUnit.SECONDS.toMillis(2))
    onView(withId(R.id.imageButtonEditDraft))
      .check(matches(isDisplayed()))
      .perform(click())
    //need to wait while the message details will be rendered
    Thread.sleep(1000)
    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())

    //need to wait while a message will be sent
    onView(withText(R.string.sending_message)).perform(waitUntilGone(TimeUnit.SECONDS.toMillis(10)))

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
