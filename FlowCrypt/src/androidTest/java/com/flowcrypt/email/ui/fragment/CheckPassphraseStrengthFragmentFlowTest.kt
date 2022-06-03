/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.fragment

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
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
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.ui.activity.fragment.CheckPassphraseStrengthFragmentArgs
import com.flowcrypt.email.util.TestGeneralUtil
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 * Date: 3/13/19
 * Time: 12:15 PM
 * E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class CheckPassphraseStrengthFragmentFlowTest : BaseTest() {
  private val addAccountToDatabaseRule = AddAccountToDatabaseRule()
  override val activityScenarioRule = activityScenarioRule<MainActivity>(
    TestGeneralUtil.genIntentForNavigationComponent(
      destinationId = R.id.checkPassphraseStrengthFragment,
      extras = CheckPassphraseStrengthFragmentArgs(
        popBackStackIdIfSuccess = R.id.securitySettingsFragment,
        title = getResString(R.string.change_pass_phrase)
      ).toBundle()
    )
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(addAccountToDatabaseRule)
    .around(AddPrivateKeyToDatabaseRule())
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testShowDialogWithPasswordRecommendation() {
    onView(withId(R.id.iBShowPasswordHint))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withId(R.id.webView))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testShowDialogAboutBadPassPhrase() {
    val badPassPhrases = arrayOf(WEAK_PASSWORD, POOR_PASSWORD)

    for (passPhrase in badPassPhrases) {
      onView(withId(R.id.eTPassphrase))
        .check(matches(isDisplayed()))
        .perform(replaceText(passPhrase), closeSoftKeyboard())
      onView(withId(R.id.btSetPassphrase))
        .check(matches(isDisplayed()))
        .perform(click())
      onView(withText(getResString(R.string.select_stronger_pass_phrase)))
        .check(matches(isDisplayed()))
      onView(withId(android.R.id.button1))
        .check(matches(isDisplayed()))
        .perform(click())
      onView(withId(R.id.eTPassphrase))
        .check(matches(isDisplayed()))
        .perform(clearText())
    }
  }

  companion object {
    internal const val WEAK_PASSWORD = "weak"
    internal const val POOR_PASSWORD = "weak, perfect, great"
  }
}
