/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.flowcrypt.email.R
import com.flowcrypt.email.ui.base.BaseCheckPassphraseOnFirstScreenFlowTest
import org.hamcrest.Matchers.isEmptyString
import org.junit.Test

/**
 * @author Denis Bondarenko
 * Date: 3/13/19
 * Time: 3:44 PM
 * E-mail: DenBond7@gmail.com
 */
abstract class BasePassphraseFlowTest : BaseCheckPassphraseOnFirstScreenFlowTest() {

  override val firstScreenContinueButtonResId: Int = R.id.buttonSetPassPhrase
  override val firstScreenEditTextResId: Int = R.id.editTextKeyPassword
  override val firstScreenPasswordQualityInfoResId: Int = R.id.textViewPasswordQualityInfo

  @Test
  fun testShowDialogWithPasswordRecommendation() {
    onView(withId(R.id.imageButtonShowPasswordHint))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withId(R.id.webView))
      .check(matches(isDisplayed()))
    onView(withId(R.id.buttonOk))
      .perform(click())
    onView(withId(R.id.textViewFirstPasswordCheckTitle))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testEmptySecondPassPhrase() {
    testShowRepeatingPassPhraseScreen()
    onView(withId(R.id.buttonConfirmPassPhrases))
      .perform(click())

    checkIsNonEmptyHintShown()
  }

  @Test
  fun testShowRepeatingPassPhraseScreen() {
    onView(withId(R.id.editTextKeyPassword))
      .perform(scrollTo(), replaceText(PERFECT_PASSWORD), closeSoftKeyboard())
    onView(withId(R.id.buttonSetPassPhrase))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withId(R.id.buttonConfirmPassPhrases))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testShowMsgMismatchOfPassPhrase() {
    testShowRepeatingPassPhraseScreen()

    onView(withId(R.id.editTextKeyPasswordSecond))
      .check(matches(isDisplayed()))
      .perform(scrollTo(), replaceText("some text"), closeSoftKeyboard())
    onView(withId(R.id.buttonConfirmPassPhrases))
      .perform(click())
    onView(withText(getResString(R.string.pass_phrases_do_not_match)))
      .check(matches(isDisplayed()))
    onView(withId(com.google.android.material.R.id.snackbar_action))
      .check(matches(isDisplayed())).perform(click())
  }

  @Test
  fun testGoToUseAnotherPassPhrase() {
    testShowRepeatingPassPhraseScreen()

    onView(withId(R.id.buttonUseAnotherPassPhrase))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withId(R.id.imageButtonShowPasswordHint))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testCheckEraseOfRepeatingPassPhrase() {
    testShowRepeatingPassPhraseScreen()

    onView(withId(R.id.editTextKeyPasswordSecond))
      .check(matches(isDisplayed()))
      .perform(replaceText(PERFECT_PASSWORD), closeSoftKeyboard())
    onView(withId(R.id.buttonUseAnotherPassPhrase))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withId(R.id.buttonSetPassPhrase))
      .check(matches(isDisplayed()))

    testShowRepeatingPassPhraseScreen()
    onView(withId(R.id.editTextKeyPasswordSecond))
      .check(matches(isDisplayed())).check(matches(withText(isEmptyString())))
  }
}
