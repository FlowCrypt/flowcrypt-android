/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.junit.annotations.DependsOnMailServer
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.base.BasePassphraseActivityTest
import com.flowcrypt.email.util.AccountDaoManager
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * A test for [CreatePrivateKeyActivity]
 *
 * @author Denis Bondarenko
 * Date: 15.01.2018
 * Time: 09:21
 * E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class CreatePrivateKeyActivityTest : BasePassphraseActivityTest() {

  override val activityScenarioRule = activityScenarioRule<CreatePrivateKeyActivity>(
    CreatePrivateKeyActivity.newIntent(
      this@CreatePrivateKeyActivityTest.getTargetContext(),
      AccountDaoManager.getUserWithMoreThan21Letters()
    )
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  @DependsOnMailServer
  fun testUseCorrectPassPhrase() {
    onView(withId(R.id.editTextKeyPassword))
      .check(matches(isDisplayed()))
      .perform(replaceText(PERFECT_PASSWORD), closeSoftKeyboard())
    onView(withId(R.id.buttonSetPassPhrase))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withId(R.id.editTextKeyPasswordSecond))
      .check(matches(isDisplayed()))
      .perform(replaceText(PERFECT_PASSWORD), closeSoftKeyboard())
    onView(withId(R.id.buttonConfirmPassPhrases))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withId(R.id.buttonSuccess))
      .check(matches(isDisplayed()))
  }
}
