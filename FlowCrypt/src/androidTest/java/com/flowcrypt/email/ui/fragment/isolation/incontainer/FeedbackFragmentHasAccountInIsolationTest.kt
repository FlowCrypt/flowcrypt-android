/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.model.Screenshot
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
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
import java.util.concurrent.TimeUnit

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@FlowCryptTestSettings(useCommonIdling = false)
class FeedbackFragmentHasAccountInIsolationTest : BaseFeedbackFragmentTest() {
  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(AddAccountToDatabaseRule())
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
    waitForObjectWithText(
      getResString(
        R.string.feedback_thank_you_for_trying_message,
        TimeUnit.SECONDS.toMillis(5)
      )
    )
    onView(withId(R.id.textInputLayoutUserEmail))
      .check(matches(not(isDisplayed())))
  }
}
