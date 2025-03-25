/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui

import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.junit.suites.CustomSuite
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.ui.base.AddOtherAccountBaseTest
import com.flowcrypt.email.util.AuthCredentialsManager
import com.flowcrypt.email.util.TestGeneralUtil
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(CustomSuite::class)
class AddOtherAccountFlowTest : AddOtherAccountBaseTest() {

  override val activeActivityRule = activityScenarioRule<MainActivity>(
    TestGeneralUtil.genIntentForNavigationComponent(
      destinationId = R.id.addOtherAccountFragment
    )
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(activeActivityRule)
    .around(ScreenshotTestRule())

  @Test
  fun one() {
    enableAdvancedMode()
    val credentials = AuthCredentialsManager.getAuthCredentials("user_with_not_existed_server.json")
    fillAllFields(credentials)
  }

  @Test
  fun two() {
    enableAdvancedMode()
    val credentials = AuthCredentialsManager.getAuthCredentials("user_with_not_existed_server.json")
    fillAllFields(credentials)
  }

  companion object {
    @JvmStatic
    // name argument is optional, it will show up on the test results
    @CustomSuite.Parameters(name = "Input: {0}")
    // parameters are provided as arrays, allowing more than one parameter
    fun params() = listOf(
      arrayOf(1),
      arrayOf(2),
    )
  }
}
