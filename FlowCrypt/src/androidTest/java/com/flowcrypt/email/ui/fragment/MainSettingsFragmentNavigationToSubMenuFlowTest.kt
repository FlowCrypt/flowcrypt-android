/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.fragment

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.junit.annotations.DependsOnMailServer
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.util.TestGeneralUtil
import org.hamcrest.Matchers.allOf
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
@FlowCryptTestSettings(useCommonIdling = false)
class MainSettingsFragmentNavigationToSubMenuFlowTest : BaseTest() {
  override val activityScenarioRule = activityScenarioRule<MainActivity>(
    TestGeneralUtil.genIntentForNavigationComponent(
      destinationId = R.id.mainSettingsFragment
    )
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(AddAccountToDatabaseRule())
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  @DependsOnMailServer
  fun testShowBackupsScreen() {
    checkIsScreenDisplaying(getResString(R.string.backups))
  }

  @Test
  fun testShowSecurityScreen() {
    checkIsScreenDisplaying(getResString(R.string.security_and_privacy))
  }

  @Test
  fun testShowContactsScreen() {
    checkIsScreenDisplaying(getResString(R.string.contacts))
  }

  @Test
  fun testShowKeysScreen() {
    checkIsScreenDisplaying(getResString(R.string.keys))
  }

  @Test
  fun testShowAttesterScreen() {
    checkIsScreenDisplaying(getResString(R.string.attester))
  }

  @Test
  fun testShowLegalScreen() {
    checkIsScreenDisplaying(
      getResString(R.string.experimental),
      getResString(R.string.experimental_settings)
    )
  }

  @Test
  fun testHiddenOrVisibleItems() {
    onView(withText(getResString(R.string.backups)))
      .check(matches(isDisplayed()))
    onView(withText(getResString(R.string.security_and_privacy)))
      .check(matches(isDisplayed()))
    onView(withText(getResString(R.string.contacts)))
      .check(matches(isDisplayed()))
    onView(withText(getResString(R.string.keys)))
      .check(matches(isDisplayed()))
    onView(withText(getResString(R.string.attester)))
      .check(matches(isDisplayed()))
    onView(withText(getResString(R.string.general)))
      .check(matches(isDisplayed()))
    onView(withText(getResString(R.string.experimental)))
      .check(matches(isDisplayed()))
  }

  private fun checkIsScreenDisplaying(screenName: String) {
    checkIsScreenDisplaying(screenName, screenName)
  }

  private fun checkIsScreenDisplaying(commandName: String, screenName: String) {
    onView(withText(commandName))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(allOf(withText(screenName), withParent(withId(R.id.toolbar))))
      .check(matches(isDisplayed()))
  }
}
