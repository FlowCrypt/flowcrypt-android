/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.enterprise

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.request.model.InitialLegacySubmitModel
import com.flowcrypt.email.api.retrofit.response.attester.InitialLegacySubmitResponse
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.ui.activity.base.BasePassphraseFlowTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.InputStreamReader
import java.net.HttpURLConnection

/**
 * @author Denis Bondarenko
 *         Date: 11/2/19
 *         Time: 12:18 PM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@Ignore("Temporary disabled due to architecture changes")
class CreatePrivateKeyFlowEnterpriseTest : BasePassphraseFlowTest() {
  override val activityScenarioRule = activityScenarioRule<MainActivity>(
    /*intent = Intent(getTargetContext(), CreatePrivateKeyActivity::class.java).apply {
      putExtra(
        CreatePrivateKeyActivity.KEY_EXTRA_ACCOUNT, AccountDaoManager
          .getAccountDao("enterprise_account_enforce_attester_submit.json")
          .copy(email = EMAIL_ENFORCE_ATTESTER_SUBMIT)
      )
    })*/
  )

  private val mockWebServerRule =
    FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        val gson =
          ApiHelper.getInstance(InstrumentationRegistry.getInstrumentation().targetContext).gson
        val model = gson.fromJson(
          InputStreamReader(request.body.inputStream()),
          InitialLegacySubmitModel::class.java
        )

        if (request.path.equals("/initial/legacy_submit")) {
          when (model.email) {
            EMAIL_ENFORCE_ATTESTER_SUBMIT -> return MockResponse().setResponseCode(
              HttpURLConnection.HTTP_OK
            )
              .setBody(gson.toJson(SUBMIT_API_ERROR_RESPONSE))
          }
        }

        return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
      }
    })

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(mockWebServerRule)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testFailAttesterSubmit() {
    onView(withId(R.id.editTextKeyPassword))
      .check(matches(isDisplayed()))
      .perform(replaceText(PERFECT_PASSWORD), closeSoftKeyboard())
    onView(withId(R.id.buttonSetPassPhrase))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withId(R.id.editTextKeyPasswordSecond))
      .check(matches(isDisplayed()))
      .perform(replaceText(PERFECT_PASSWORD), closeSoftKeyboard())
    onView(withId(R.id.buttonConfirmPassPhrases))
      .check(matches(isDisplayed()))
      .perform(click())

    checkIsSnackbarDisplayedAndClick(SUBMIT_API_ERROR_RESPONSE.apiError?.msg!!)

    checkIsSnackBarDisplayed()
    onView(withText(SUBMIT_API_ERROR_RESPONSE.apiError?.msg))
      .check(matches(isDisplayed()))
  }

  companion object {
    const val EMAIL_ENFORCE_ATTESTER_SUBMIT = "enforce_attester_submit@example.com"

    val SUBMIT_API_ERROR_RESPONSE = InitialLegacySubmitResponse(
      ApiError(
        400, "Invalid email " +
            "address", "internal_error"
      ), false
    )
  }
}
