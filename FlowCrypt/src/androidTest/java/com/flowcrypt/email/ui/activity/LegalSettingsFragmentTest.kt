/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isSelected
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.util.TestGeneralUtil
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 * Date: 23.02.2018
 * Time: 10:25
 * E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class LegalSettingsFragmentTest : BaseTest() {
  override val activityScenarioRule = activityScenarioRule<MainActivity>(
    TestGeneralUtil.genIntentForNavigationComponent(
      uri = "flowcrypt://email.flowcrypt.com/settings/legal"
    )
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(AddAccountToDatabaseRule())
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  private val titleNames: Array<String> = arrayOf(
    getResString(R.string.privacy),
    getResString(R.string.terms),
    getResString(R.string.licence),
    getResString(R.string.sources)
  )

  @Test
  fun testClickToTitleViewPager() {
    for (titleName in titleNames) {
      onView(allOf(withParent(withParent(withParent(withId(R.id.tabLayout)))), withText(titleName)))
        .check(matches(isDisplayed()))
        .perform(click())
      onView(allOf(withParent(withParent(withParent(withId(R.id.tabLayout)))), withText(titleName)))
        .check(matches(isDisplayed())).check(matches(isSelected()))
    }
  }

  @Test
  fun testSwipeInViewPager() {
    onView(
      allOf(
        withParent(withParent(withParent(withId(R.id.tabLayout)))),
        withText(titleNames[0])
      )
    )
      .check(matches(isDisplayed())).check(matches(isSelected()))
    for (i in 1 until titleNames.size) {
      onView(withId(R.id.viewPager)).perform(swipeLeft())
      onView(
        allOf(
          withParent(withParent(withParent(withId(R.id.tabLayout)))),
          withText(titleNames[i])
        )
      )
        .check(matches(isDisplayed())).check(matches(isSelected()))
    }
  }
}
