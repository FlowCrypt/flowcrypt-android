/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import androidx.test.espresso.Espresso.onView
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
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.HtmlViewFromAssetsRawFragmentArgs
import com.flowcrypt.email.util.TestGeneralUtil
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 * Date: 24.02.2018
 * Time: 14:56
 * E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class HtmlViewFromAssetsRawActivityTest : BaseTest() {
  override val activeActivityRule = activityScenarioRule<MainActivity>(
    TestGeneralUtil.genIntentForNavigationComponent(
      uri = "flowcrypt://email.flowcrypt.com/html_from_assets",
      extras = HtmlViewFromAssetsRawFragmentArgs(
        title = getResString(R.string.privacy),
        resourceIdAsString = "html/privacy.htm"
      ).toBundle()
    )
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(activeActivityRule)
    .around(ScreenshotTestRule())

  @Test
  fun testDisplayPrivacyFromAssets() {
    onView(allOf(withText(R.string.privacy), withParent(withId(R.id.toolbar))))
      .check(matches(isDisplayed()))
  }
}
