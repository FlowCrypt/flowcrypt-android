/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import android.graphics.BitmapFactory
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withBitmap
import com.flowcrypt.email.model.Screenshot
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.FeedbackFragment
import com.flowcrypt.email.ui.activity.fragment.FeedbackFragmentArgs
import com.flowcrypt.email.ui.base.BaseFeedbackFragmentTest
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 *         Date: 8/9/22
 *         Time: 10:32 AM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class FeedbackFragmentNoAccountsInIsolationTest : BaseFeedbackFragmentTest() {
  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(ScreenshotTestRule())

  @Before
  fun launchFragmentInContainerWithPredefinedArgs() {
    launchFragmentInContainer<FeedbackFragment>(
      fragmentArgs = FeedbackFragmentArgs(
        screenshot = Screenshot(SCREENSHOT_BYTE_ARRAY)
      ).toBundle()
    )
  }

  @Test
  fun testUserEmailVisibility() {
    onView(withId(R.id.editTextUserEmail))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testIncludeAppScreenshot() {
    onView(withId(R.id.imageButtonScreenshot))
      .check(matches(not(isDisplayed())))
    onView(withId(R.id.checkBoxScreenshot))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withId(R.id.imageButtonScreenshot))
      .check(matches(isDisplayed()))
      .check(
        matches(
          withBitmap(
            BitmapFactory.decodeByteArray(SCREENSHOT_BYTE_ARRAY, 0, SCREENSHOT_BYTE_ARRAY.size)
          )
        )
      )
  }
}
