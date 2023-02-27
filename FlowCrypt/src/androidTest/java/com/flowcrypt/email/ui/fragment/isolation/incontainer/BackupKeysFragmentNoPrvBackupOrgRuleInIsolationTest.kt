/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.BackupKeysFragment
import com.flowcrypt.email.ui.base.BaseBackupKeysFragmentTest
import com.flowcrypt.email.util.AccountDaoManager
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class BackupKeysFragmentNoPrvBackupOrgRuleInIsolationTest : BaseBackupKeysFragmentTest() {
  private val userWithClientConfiguration = AccountDaoManager.getUserWithClientConfiguration(
    ClientConfiguration(
      flags = listOf(
        ClientConfiguration.ConfigurationProperty.NO_PRV_CREATE,
        ClientConfiguration.ConfigurationProperty.NO_PRV_BACKUP
      ),
      customKeyserverUrl = null,
      keyManagerUrl = "https://keymanagerurl.test",
      disallowAttesterSearchForDomains = null,
      enforceKeygenAlgo = null,
      enforceKeygenExpireMonths = null
    )
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(ScreenshotTestRule())

  override val addAccountToDatabaseRule: AddAccountToDatabaseRule
    get() = AddAccountToDatabaseRule(userWithClientConfiguration)

  @Before
  fun launchFragmentInContainerWithPredefinedArgs() {
    launchFragmentInContainer<BackupKeysFragment>()
  }

  @Test
  fun testBtBackupGone() {
    onView(withId(R.id.rBDownloadOption))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withId(R.id.btBackup))
      .check(matches(not(isDisplayed())))
    onView(withId(R.id.rBEmailOption))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withId(R.id.btBackup))
      .check(matches(not(isDisplayed())))
  }
}
