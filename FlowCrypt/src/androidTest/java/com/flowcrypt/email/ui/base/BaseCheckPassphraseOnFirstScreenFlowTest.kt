/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.base

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.flowcrypt.email.R
import org.junit.Test

/**
 * @author Denis Bondarenko
 * Date: 3/13/19
 * Time: 3:44 PM
 * E-mail: DenBond7@gmail.com
 */
abstract class BaseCheckPassphraseOnFirstScreenFlowTest : BaseCheckPassphraseOnFirstScreenTest() {

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
}
