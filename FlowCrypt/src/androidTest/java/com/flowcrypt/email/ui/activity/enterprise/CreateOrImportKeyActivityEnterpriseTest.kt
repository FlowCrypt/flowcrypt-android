/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.enterprise

import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.flowcrypt.email.DoesNotNeedMailserverEnterprise
import com.flowcrypt.email.R
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.ui.activity.CreateOrImportKeyActivity
import com.flowcrypt.email.ui.activity.base.BaseCreateOrImportKeyActivityTest
import com.flowcrypt.email.util.AccountDaoManager
import org.hamcrest.CoreMatchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 *         Date: 11/2/19
 *         Time: 10:07 AM
 *         E-mail: DenBond7@gmail.com
 */
@LargeTest
@DoesNotNeedMailserverEnterprise
@RunWith(AndroidJUnit4::class)
class CreateOrImportKeyActivityEnterpriseTest : BaseCreateOrImportKeyActivityTest() {
  override val activityTestRule =
      object : IntentsTestRule<CreateOrImportKeyActivity>(CreateOrImportKeyActivity::class.java) {
        override fun getActivityIntent(): Intent {
          return CreateOrImportKeyActivity.newIntent(getTargetContext(), AccountDaoManager
              .getAccountDao("enterprise_account_no_prv_create.json"), true)
        }
      }

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(AddPrivateKeyToDatabaseRule())
      .around(activityTestRule)

  @Test
  fun testCreateNewKeyNotExist() {
    onView(withId(R.id.buttonCreateNewKey))
        .check(matches(not(isDisplayed())))
  }

  @Test
  fun testButtonSkipSetupNotExist() {
    onView(withId(R.id.buttonSkipSetup))
        .check(matches(not(isDisplayed())))
  }
}
