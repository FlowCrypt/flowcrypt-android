/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.gmailapi.passwordprotected

import android.Manifest
import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.openLinkWithText
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.response.api.ClientConfigurationResponse
import com.flowcrypt.email.junit.annotations.EnterpriseTest
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.junit.annotations.OutgoingMessageConfiguration
import com.flowcrypt.email.rules.AddRecipientsToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
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
import java.util.concurrent.TimeUnit

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@FlowCryptTestSettings(useCommonIdling = false, useIntents = true)
@EnterpriseTest
@OutgoingMessageConfiguration(
  to = [],
  cc = [],
  bcc = [],
  message = "",
  subject = "",
  isNew = true
)
class ComposeScreenPasswordProtectedDisallowedTermsReFetchConfigurationFlowTest :
  BaseComposeScreenPasswordProtectedDisallowedTermsTest(
    ACCOUNT_ENTITY_WITH_EXISTING_OPTIONAL_PARAMETERS
  ) {

  private var attemptsToUseDefaultBehavior = 2

  override val mockWebServerRule =
    FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        return when {
          request.path.equals("/api/v1/client-configuration?domain=flowcrypt.test") -> {
            if (attemptsToUseDefaultBehavior == 0) {
              MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(
                  ApiHelper.getInstance(getTargetContext()).gson.toJson(
                    ClientConfigurationResponse(
                      clientConfiguration = ACCOUNT_ENTITY_WITH_EXISTING_OPTIONAL_PARAMETERS.clientConfiguration?.copy(
                        disallowPasswordMessagesErrorText = UPDATED_ERROR_TEXT,
                        disallowPasswordMessagesForTerms = TERMS
                      )
                    )
                  )
                )
            } else {
              attemptsToUseDefaultBehavior--
              handleCommonAPICalls(request)
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
      .around(GrantPermissionRuleChooser.grant(Manifest.permission.POST_NOTIFICATIONS))
      .around(mockWebServerRule)
      .around(addAccountToDatabaseRule)
      .around(addPrivateKeyToDatabaseRule)
      .around(AddRecipientsToDatabaseRule(prepareRecipientsForTest()))
      .around(addLabelsToDatabaseRule)
      .around(activityScenarioRule)
      .around(ScreenshotTestRule())

  @Test
  //@Ignore("temp")
  fun testDialogWithErrorText() {
    Thread.sleep(60000)

    intentsRelease()

    //do checking before updating client configuration
    MATCHING_SUBJECTS.forEach { subject ->
      onView(withId(R.id.editTextEmailSubject))
        .perform(scrollTo(), click(), replaceText(subject))

      waitForObjectWithText(subject, TimeUnit.SECONDS.toMillis(5))

      onView(withId(R.id.menuActionSend))
        .check(matches(isDisplayed()))
        .perform(click())

      isDialogWithTextDisplayed(
        decorView,
        ERROR_TEXT
      )

      Intents.init()
      //test that URL is openable
      val expectingIntent = allOf(hasAction(Intent.ACTION_VIEW), hasData(URL))
      //mocking intent to prevent actual navigation during test
      Intents.intending(expectingIntent).respondWith(Instrumentation.ActivityResult(0, null))
      //performing action
      onView(withText(ERROR_TEXT))
        .perform(openLinkWithText(URL))
      //asserting our expected intent was recorded
      Intents.intended(expectingIntent)
      Intents.release()

      onView(withId(android.R.id.button1))
        .check(matches(isDisplayed()))
        .perform(click())
    }

    //go to ProvidePasswordToProtectMsgFragment and go back to trigger updating
    onView(withId(R.id.btnSetWebPortalPassword))
      .perform(click())
    Espresso.pressBack()
    //need to wait while threads will be synced
    Thread.sleep(2000)

    //check that client configuration was updated
    MATCHING_SUBJECTS.first().let { subject ->
      onView(withId(R.id.editTextEmailSubject))
        .perform(scrollTo(), click(), replaceText(subject))

      waitForObjectWithText(subject, TimeUnit.SECONDS.toMillis(5))

      onView(withId(R.id.menuActionSend))
        .check(matches(isDisplayed()))
        .perform(click())

      isDialogWithTextDisplayed(
        decorView,
        UPDATED_ERROR_TEXT
      )

      onView(withId(android.R.id.button1))
        .check(matches(isDisplayed()))
        .perform(click())
    }
  }

  companion object {
    const val UPDATED_ERROR_TEXT = "Updated: Password-protected messages are disabled"

    val ACCOUNT_ENTITY_WITH_EXISTING_OPTIONAL_PARAMETERS =
      BASE_ACCOUNT_ENTITY.copy(
        clientConfiguration = BASE_ACCOUNT_ENTITY.clientConfiguration?.copy(
          disallowPasswordMessagesErrorText = ERROR_TEXT,
          disallowPasswordMessagesForTerms = TERMS
        )
      )
  }
}
