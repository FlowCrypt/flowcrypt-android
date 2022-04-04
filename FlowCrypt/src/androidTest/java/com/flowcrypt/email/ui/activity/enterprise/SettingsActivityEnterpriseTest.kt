/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.enterprise

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
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
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.util.TestGeneralUtil
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith


/**
 * @author Denis Bondarenko
 *         Date: 11/2/19
 *         Time: 11:40 AM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class SettingsActivityEnterpriseTest : BaseTest() {
  override val activityScenarioRule = activityScenarioRule<MainActivity>(
    TestGeneralUtil.genIntentForNavigationComponent(
      uri = "flowcrypt://email.flowcrypt.com/settings"
    )
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(
      AddAccountToDatabaseRule(
        AccountDaoManager.getAccountDao("enterprise_account_no_prv_backup.json")
      )
    )
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testBackupsDisabled() {
    //need to wait database updates
    Thread.sleep(1000)
    onView(withText(getResString(R.string.backups)))
      .check(doesNotExist())
  }
}
