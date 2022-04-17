/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.web.assertion.WebViewAssertions.webMatches
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.getText
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.HtmlViewFromAssetsRawFragment
import com.flowcrypt.email.ui.activity.fragment.HtmlViewFromAssetsRawFragmentArgs
import org.hamcrest.Matchers.containsString
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
class HtmlViewFromAssetsRawFragmentInIsolationTest : BaseTest() {

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(ScreenshotTestRule())

  @Test
  fun testDisplayPrivacyFromAssets() {
    launchFragmentInContainer<HtmlViewFromAssetsRawFragment>(
      fragmentArgs = HtmlViewFromAssetsRawFragmentArgs(
        title = getResString(R.string.privacy),
        resourceIdAsString = "html/privacy.htm"
      ).toBundle()
    )
    onView(withId(R.id.webView))
      .check(matches(isDisplayed()))

    onWebView(withId(R.id.webView)).forceJavascriptEnabled()
    onWebView(withId(R.id.webView))
      .withElement(findElement(Locator.TAG_NAME, "h1"))
      .check(webMatches(getText(), containsString("FlowCrypt Privacy Policy")))
  }
}
