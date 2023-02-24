/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.CheckPassphraseStrengthFragment
import com.flowcrypt.email.ui.activity.fragment.CheckPassphraseStrengthFragmentArgs
import org.hamcrest.Matchers.startsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class CheckPassphraseStrengthFragmentInIsolationTest : BaseTest() {
  private val addAccountToDatabaseRule = AddAccountToDatabaseRule()

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
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
    closeSoftKeyboard()
    onView(withId(R.id.btSetPassphrase))
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
      onView(withId(R.id.eTPassphrase))
        .check(matches(isDisplayed()))
        .perform(replaceText(passPhrases[i]))
      onView(withId(R.id.tVPassphraseQuality))
        .check(
          matches(
            withText(
              startsWith(
                degreeOfReliabilityOfPassPhrase[i].uppercase()
              )
            )
          )
        )
      onView(withId(R.id.eTPassphrase))
        .check(matches(isDisplayed()))
        .perform(ViewActions.clearText())
    }
  }

  private fun checkIsNonEmptyHintShown() {
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
