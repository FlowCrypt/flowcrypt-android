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
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.CreateOrImportPrivateKeyDuringSetupFragment
import com.flowcrypt.email.ui.activity.fragment.CreateOrImportPrivateKeyDuringSetupFragmentArgs
import com.flowcrypt.email.util.AccountDaoManager
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.UUID

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class CreateOrImportPrivateKeyDuringSetupFragmentInIsolationTest : BaseTest() {
  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(ScreenshotTestRule())

  @Test
  fun testButtonCreateNewKeyVisibilityForMissedRule() {
    launchFragmentInContainer<CreateOrImportPrivateKeyDuringSetupFragment>(
      fragmentArgs = CreateOrImportPrivateKeyDuringSetupFragmentArgs(
        requestKey = UUID.randomUUID().toString(),
        accountEntity = AccountDaoManager.getDefaultAccountDao()
      ).toBundle()
    )

    onView(withId(R.id.buttonCreateNewKey))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testButtonCreateNewKeyVisibilityForExistingRule() {
    launchFragmentInContainer<CreateOrImportPrivateKeyDuringSetupFragment>(
      fragmentArgs = CreateOrImportPrivateKeyDuringSetupFragmentArgs(
        requestKey = UUID.randomUUID().toString(),
        accountEntity = AccountDaoManager.getUserWithClientConfiguration(
          ClientConfiguration(
            flags = listOf(
              ClientConfiguration.ConfigurationProperty.NO_PRV_CREATE,
              ClientConfiguration.ConfigurationProperty.NO_PRV_BACKUP
            )
          )
        )
      ).toBundle()
    )

    onView(withId(R.id.buttonCreateNewKey))
      .check(matches(not(isDisplayed())))
  }
}
