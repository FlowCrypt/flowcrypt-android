/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.CheckPassphraseStrengthFragment
import com.flowcrypt.email.ui.activity.fragment.CheckPassphraseStrengthFragmentArgs
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 *         Date: 4/9/22
 *         Time: 3:04 PM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class CheckPassphraseStrengthFragmentInIsolationTest : BaseTest() {
  private val addAccountToDatabaseRule = AddAccountToDatabaseRule()

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(addAccountToDatabaseRule)
    .around(AddPrivateKeyToDatabaseRule())
    .around(ScreenshotTestRule())

  @Before
  fun launchFragmentInContainerWithPredefinedArgs() {
    launchFragmentInContainer<CheckPassphraseStrengthFragment>(
      fragmentArgs = CheckPassphraseStrengthFragmentArgs(
        popBackStackIdIfSuccess = R.id.securitySettingsFragment,
        title = getResString(R.string.change_pass_phrase)
      ).toBundle()
    )
  }

  @Test
  fun testEmptyPassPhrase() {
    ViewActions.closeSoftKeyboard()
    Espresso.onView(ViewMatchers.withId(R.id.btSetPassphrase))
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
      .perform(ViewActions.click())

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
      Espresso.onView(ViewMatchers.withId(R.id.eTPassphrase))
        .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        .perform(ViewActions.replaceText(passPhrases[i]))
      Espresso.onView(ViewMatchers.withId(R.id.tVPassphraseQuality))
        .check(
          ViewAssertions.matches(
            ViewMatchers.withText(
              Matchers.startsWith(
                degreeOfReliabilityOfPassPhrase[i].uppercase()
              )
            )
          )
        )
      Espresso.onView(ViewMatchers.withId(R.id.eTPassphrase))
        .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        .perform(ViewActions.clearText())
    }
  }

  private fun checkIsNonEmptyHintShown() {
    Espresso.onView(ViewMatchers.withText(getResString(R.string.passphrase_must_be_non_empty)))
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    Espresso.onView(ViewMatchers.withId(com.google.android.material.R.id.snackbar_action))
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
      .perform(ViewActions.click())
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
