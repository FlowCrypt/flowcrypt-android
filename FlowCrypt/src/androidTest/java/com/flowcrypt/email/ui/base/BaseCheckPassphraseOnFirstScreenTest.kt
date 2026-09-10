/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.base

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withViewBackgroundTintResId
import org.hamcrest.Matchers.startsWith
import org.junit.Test

/**
 * @author Denys Bondarenko
 */
abstract class BaseCheckPassphraseOnFirstScreenTest : BaseTest() {

  abstract val firstScreenContinueButtonResId: Int
  abstract val firstScreenEditTextResId: Int
  abstract val firstScreenPasswordQualityInfoResId: Int

  @Test
  fun testEmptyFirstPassPhrase() {
    Espresso.closeSoftKeyboard()
    onView(withId(firstScreenContinueButtonResId))
      .check(matches(isDisplayed()))
      .perform(click())

    checkIsNonEmptyHintShown()
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
      val option = degreeOfReliabilityOfPassPhrase[i]
      onView(withId(firstScreenEditTextResId))
        .check(matches(isDisplayed()))
        .perform(replaceText(passPhrases[i]))
      onView(withId(firstScreenPasswordQualityInfoResId))
        .check(matches(withText(startsWith(option.uppercase()))))
      onView(withId(firstScreenEditTextResId))
        .check(matches(isDisplayed()))
        .perform(clearText())

      val color = when (option) {
        getResString(R.string.password_quality_weak),
        getResString(R.string.password_quality_poor) -> R.color.silver

        else -> R.color.colorPrimary
      }

      onView(withId(firstScreenContinueButtonResId))
        .check(matches(withViewBackgroundTintResId(getTargetContext(), color)))
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
