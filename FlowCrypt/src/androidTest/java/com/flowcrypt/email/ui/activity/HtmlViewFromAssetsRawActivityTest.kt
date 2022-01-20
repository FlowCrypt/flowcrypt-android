/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.rules.lazyActivityScenarioRule
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
  override val activeActivityRule =
    lazyActivityScenarioRule<HtmlViewFromAssetsRawActivity>(launchActivity = false)
  override val activityScenario: ActivityScenario<*>?
    get() = activeActivityRule.scenario

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(activeActivityRule)
    .around(ScreenshotTestRule())

  @Test
  fun testShowPrivacyTitle() {
    startActivity(InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.privacy))
    onView(allOf(withText(R.string.privacy), withParent(withId(R.id.toolbar))))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testShowTermsTitle() {
    startActivity(InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.terms))
    onView(allOf(withText(R.string.terms), withParent(withId(R.id.toolbar))))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testShowSecurityTitle() {
    startActivity(InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.security))
    onView(allOf(withText(R.string.security), withParent(withId(R.id.toolbar))))
      .check(matches(isDisplayed()))
  }

  private fun startActivity(title: String) {
    val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    val intent = Intent(targetContext, HtmlViewFromAssetsRawActivity::class.java)
    intent.putExtra(HtmlViewFromAssetsRawActivity.EXTRA_KEY_ACTIVITY_TITLE, title)
    intent.putExtra(HtmlViewFromAssetsRawActivity.EXTRA_KEY_HTML_RESOURCES_ID, "html/privacy.htm")
    activeActivityRule.launch(intent)
    registerAllIdlingResources()
  }
}
