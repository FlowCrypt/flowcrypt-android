/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import org.hamcrest.Matchers.isEmptyString
import org.hamcrest.Matchers.startsWith
import org.junit.Test

/**
 * @author Denis Bondarenko
 * Date: 3/13/19
 * Time: 3:44 PM
 * E-mail: DenBond7@gmail.com
 */
abstract class BasePassphraseActivityTest : BaseTest() {
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
  fun testEmptyFirstPassPhrase() {
    closeSoftKeyboard()
    onView(withId(R.id.buttonSetPassPhrase))
      .check(matches(isDisplayed()))
      .perform(click())

    checkIsNonEmptyHintShown()
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

  @Test
  fun testChangingQualityOfPassPhrase() {
    val passPhrases = arrayOf(
      WEAK_PASSWORD, POOR_PASSWORD, REASONABLE_PASSWORD, GOOD_PASSWORD,
      GREAT_PASSWORD, PERFECT_PASSWORD
    )

    val degreeOfReliabilityOfPassPhrase = arrayOf(
      getResString(R.string.password_quality_weak),
      getResString(R.string.password_quality_poor),
      getResString(R.string.password_quality_reasonable),
      getResString(R.string.password_quality_good),
      getResString(R.string.password_quality_great),
      getResString(R.string.password_quality_perfect)
    )

    for (i in passPhrases.indices) {
      onView(withId(R.id.editTextKeyPassword))
        .check(matches(isDisplayed()))
        .perform(replaceText(passPhrases[i]))
      onView(withId(R.id.textViewPasswordQualityInfo))
        .check(matches(withText(startsWith(degreeOfReliabilityOfPassPhrase[i].uppercase()))))
      onView(withId(R.id.editTextKeyPassword))
        .check(matches(isDisplayed()))
        .perform(clearText())
    }
  }

  @Test
  fun testShowDialogAboutBadPassPhrase() {
    val badPassPhrases = arrayOf(WEAK_PASSWORD, POOR_PASSWORD)

    for (passPhrase in badPassPhrases) {
      onView(withId(R.id.editTextKeyPassword))
        .check(matches(isDisplayed()))
        .perform(replaceText(passPhrase), closeSoftKeyboard())
      onView(withId(R.id.buttonSetPassPhrase))
        .check(matches(isDisplayed()))
        .perform(click())
      onView(withText(getResString(R.string.select_stronger_pass_phrase)))
        .check(matches(isDisplayed()))
      onView(withId(android.R.id.button1))
        .check(matches(isDisplayed()))
        .perform(click())
      onView(withId(R.id.editTextKeyPassword))
        .check(matches(isDisplayed()))
        .perform(clearText())
    }
  }

  protected fun checkIsNonEmptyHintShown() {
    onView(withText(getResString(R.string.passphrase_must_be_non_empty)))
      .check(matches(isDisplayed()))
    onView(withId(com.google.android.material.R.id.snackbar_action))
      .check(matches(isDisplayed()))
      .perform(click())
  }

  companion object {
    internal const val WEAK_PASSWORD = "weak"
    internal const val POOR_PASSWORD = "weak, perfect, great"
    internal const val REASONABLE_PASSWORD = "weak, poor, reasonable"
    internal const val GOOD_PASSWORD = "weak, poor, good,"
    internal const val GREAT_PASSWORD = "weak, poor, great, good"
    internal const val PERFECT_PASSWORD = "unconventional blueberry unlike any other"
  }
}
