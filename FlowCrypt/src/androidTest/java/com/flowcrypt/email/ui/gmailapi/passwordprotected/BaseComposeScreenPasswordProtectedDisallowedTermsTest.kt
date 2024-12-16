/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.gmailapi.passwordprotected

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withTextViewDrawable
import com.flowcrypt.email.matchers.TextViewDrawableMatcher
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.ui.base.BaseComposeGmailFlow
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Before

/**
 * @author Denys Bondarenko
 */
open class BaseComposeScreenPasswordProtectedDisallowedTermsTest(
  accountEntity: AccountEntity = BASE_ACCOUNT_ENTITY
) : BaseComposeGmailFlow(accountEntity) {
  override val mockWebServerRule =
    FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        return handleCommonAPICalls(request)
      }
    })

  @Before
  fun setUp() {
    onView(withId(R.id.editTextEmailAddress))
      .perform(
        typeText(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER),
        pressImeActionButton(),
        closeSoftKeyboard()
      )
    //need to leave focus from 'To' field. move the focus to the next view
    onView(withId(R.id.editTextEmailSubject))
      .perform(scrollTo(), click(), replaceText(MATCHING_SUBJECTS.first()))

    onView(withId(R.id.btnSetWebPortalPassword))
      .check(matches(isDisplayed()))
      .check(matches(withText(getResString(R.string.tap_to_protect_with_web_portal_password))))
      .check(
        matches(
          withTextViewDrawable(
            resourceId = R.drawable.ic_password_not_protected_white_24,
            drawablePosition = TextViewDrawableMatcher.DrawablePosition.LEFT
          )
        )
      ).perform(click())

    onView(withId(R.id.eTPassphrase))
      .perform(
        replaceText(PASSWORD),
        closeSoftKeyboard()
      )

    onView(withId(R.id.btSetPassword))
      .perform(click())
  }

  companion object {
    private const val PASSWORD = "Qwerty1234@"
    const val URL = "https://flowcrypt.com"

    val MATCHING_SUBJECTS = listOf(
      "[Classification: Data Control: Internal Data Control] Quarter results",
      "Conference information [Classification: Data Control: Internal Data Control]",
      "[Classification: Data Control: Internal Data Control]",
      "aaaa[Classification: Data Control: Internal Data Control]bbb",
      "[droid]",
      "check -droid- case",
    )

    val NON_MATCHING_SUBJECTS = listOf(
      "[1Classification: Data Control: Internal Data Control] Quarter results",
      "Conference information [1Classification: Data Control: Internal Data Control]",
      "[1Classification: Data Control: Internal Data Control]",
      "aaaa[1Classification: Data Control: Internal Data Control]bbb",
      "Microdroid androids",
    )

    const val ERROR_TEXT =
      "Password-protected messages are disabled, please check $URL"

    val TERMS = listOf(
      "droid",
      "[Classification: Data Control: Internal Data Control]",
    )
  }
}