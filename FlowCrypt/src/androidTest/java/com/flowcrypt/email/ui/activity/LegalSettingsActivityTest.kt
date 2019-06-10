/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isSelected
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.ui.activity.settings.LegalSettingsActivity
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
@LargeTest
@RunWith(AndroidJUnit4::class)
class LegalSettingsActivityTest : BaseTest() {
  override val activityTestRule: ActivityTestRule<*>? = ActivityTestRule(LegalSettingsActivity::class.java)

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(AddAccountToDatabaseRule())
      .around(activityTestRule)

  private val titleNames: Array<String> = arrayOf(
      getResString(R.string.privacy),
      getResString(R.string.terms),
      getResString(R.string.licence),
      getResString(R.string.sources))

  @Test
  fun testClickToTitleViewPager() {
    for (titleName in titleNames) {
      onView(allOf<View>(withParent(withParent(withParent(withId(R.id.tabLayout)))), withText(titleName)))
          .check(matches(isDisplayed()))
          .perform(click())
      onView(allOf<View>(withParent(withParent(withParent(withId(R.id.tabLayout)))), withText(titleName)))
          .check(matches(isDisplayed())).check(matches(isSelected()))
    }
  }

  @Test
  fun testShowHelpScreen() {
    //Added a timeout because an initialization of WebViews needs more time.
    Thread.sleep(5000)
    testHelpScreen()
  }

  @Test
  fun testSwipeInViewPager() {
    onView(allOf<View>(withParent(withParent(withParent(withId(R.id.tabLayout)))), withText(titleNames[0])))
        .check(matches(isDisplayed())).check(matches(isSelected()))
    for (i in 1 until titleNames.size) {
      onView(withId(R.id.viewPager)).perform(swipeLeft())
      onView(allOf<View>(withParent(withParent(withParent(withId(R.id.tabLayout)))), withText(titleNames[i])))
          .check(matches(isDisplayed())).check(matches(isSelected()))
    }
  }
}
