/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.base

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasTextColor
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.extensions.kotlin.asInternetAddress
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.util.TestGeneralUtil
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.CoreMatchers
import org.junit.ClassRule
import java.net.HttpURLConnection

/**
 * @author Denys Bondarenko
 */
abstract class BaseComposeScreenNoKeyAvailableTest : BaseComposeScreenTest() {

  protected fun doTestAddEmailToExistingKey(action: () -> Unit) {
    doBaseActions {
      onView(withText(R.string.add_email_to_existing_key))
        .check(matches(isDisplayed()))
        .perform(click())

      action.invoke()
    }
  }

  protected fun doBaseActions(action: () -> Unit) {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()
    fillInAllFields(
      to = setOf(
        requireNotNull(TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER.asInternetAddress())
      )
    )

    //check that editTextFrom has gray text color. It means a sender doesn't have a private key
    onView(withId(R.id.editTextFrom))
      .check(matches(isDisplayed()))
      .check(matches(hasTextColor(R.color.gray)))

    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())

    isDialogWithTextDisplayed(
      decorView,
      getResString(R.string.no_key_available, addAccountToDatabaseRule.account.email)
    )

    action.invoke()

    waitForObjectWithText(getResString(R.string.compose), 5000)

    //check that editTextFrom doesn't have gray text color. It means a sender has a private key.
    onView(withId(R.id.editTextFrom))
      .check(matches(isDisplayed()))
      .check(matches(CoreMatchers.not(hasTextColor(R.color.gray))))
  }

  companion object {
    @get:ClassRule
    @JvmStatic
    val mockWebServerRule = FlowCryptMockWebServerRule(
      TestConstants.MOCK_WEB_SERVER_PORT,
      object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
          if (request.path?.startsWith("/attester/pub", ignoreCase = true) == true) {
            val lastSegment = request.requestUrl?.pathSegments?.lastOrNull()

            when {
              TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER.equals(
                lastSegment, true
              ) -> {
                return MockResponse()
                  .setResponseCode(HttpURLConnection.HTTP_OK)
                  .setBody(TestGeneralUtil.readResourceAsString("3.txt"))
              }
            }
          }

          return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
        }
      })
  }
}