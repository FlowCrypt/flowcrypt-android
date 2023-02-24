/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.response.api.PostHelpFeedbackResponse
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.model.Screenshot
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.ui.activity.fragment.FeedbackFragmentArgs
import com.flowcrypt.email.ui.base.BaseFeedbackFragmentTest
import com.flowcrypt.email.util.TestGeneralUtil
import com.flowcrypt.email.util.exception.ApiException
import com.google.gson.Gson
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestName
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.util.UUID

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class SendFeedbackHasAccountFlowTest : BaseFeedbackFragmentTest() {
  override val activityScenarioRule = activityScenarioRule<MainActivity>(
    TestGeneralUtil.genIntentForNavigationComponent(
      destinationId = R.id.feedbackFragment,
      extras = FeedbackFragmentArgs(
        screenshot = Screenshot(SCREENSHOT_BYTE_ARRAY)
      ).toBundle()
    )
  )

  @get:Rule
  val testNameRule = TestName()

  private val mockWebServerRule = FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT,
    object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        val gson = ApiHelper.getInstance(getTargetContext()).gson

        if (request.path?.startsWith("/help/feedback") == true) {
          return handlePostFeedbackRequest(gson)
        }

        return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
      }
    })

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(mockWebServerRule)
    .around(AddAccountToDatabaseRule())
    .around(AddPrivateKeyToDatabaseRule())
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testHandleApiErrorWhenSendingFeedback() {
    onView(withId(R.id.editTextUserMessage))
      .perform(typeText(UUID.randomUUID().toString()))

    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())

    val exception = ApiException(API_ERROR)
    val errorMsg = if (exception.message.isNullOrEmpty()) {
      exception.javaClass.simpleName
    } else exception.message

    val dialogText = getResString(
      R.string.send_feedback_failed_hint,
      getResString(R.string.support_email),
      errorMsg ?: ""
    )

    onView(withText(dialogText))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testSendingFeedbackSuccess() {
    onView(withId(R.id.editTextUserMessage))
      .perform(typeText(UUID.randomUUID().toString()))

    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())

    onView(withId(R.id.tVStatusMessage))
      .check(doesNotExist())
  }

  @Test
  fun testNavigateToImageEditor() {
    onView(withId(R.id.checkBoxScreenshot))
      .check(matches(isDisplayed()))
      .perform(click())

    onView(withId(R.id.imageButtonScreenshot))
      .check(matches(isDisplayed()))
      .perform(click())

    onView(withId(R.id.photoEditorView))
      .check(matches(isDisplayed()))
  }

  private fun handlePostFeedbackRequest(gson: Gson): MockResponse {
    return when (testNameRule.methodName) {
      "testHandleApiErrorWhenSendingFeedback" -> {
        MockResponse()
          .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
          .setBody(gson.toJson(PostHelpFeedbackResponse(apiError = API_ERROR)))
      }

      "testSendingFeedbackSuccess" -> {
        MockResponse()
          .setResponseCode(HttpURLConnection.HTTP_OK)
          .setBody(gson.toJson(PostHelpFeedbackResponse(isSent = true, text = "text")))
      }

      else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
    }
  }

  companion object {
    private val API_ERROR = ApiError(
      code = HttpURLConnection.HTTP_BAD_REQUEST,
      msg = "Wrong request received"
    )
  }
}
