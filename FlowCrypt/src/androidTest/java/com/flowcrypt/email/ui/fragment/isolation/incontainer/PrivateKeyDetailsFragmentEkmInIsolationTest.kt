/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.junit.annotations.MoveToFlowBehaviour
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.PrivateKeyDetailsFragment
import com.flowcrypt.email.ui.activity.fragment.PrivateKeyDetailsFragmentArgs
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.util.PrivateKeysManager
import org.hamcrest.Matchers.not
import org.junit.Ignore
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
class PrivateKeyDetailsFragmentEkmInIsolationTest : BaseTest() {
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
  private val addAccountToDatabaseRule = AddAccountToDatabaseRule(userWithClientConfiguration)
  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule(
    accountEntity = addAccountToDatabaseRule.account,
    keyPath = KEY_PATH,
    passphrase = TestConstants.DEFAULT_SECOND_STRONG_PASSWORD,
    sourceType = KeyImportDetails.SourceType.EMAIL,
    passphraseType = KeyEntity.PassphraseType.RAM
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(ScreenshotTestRule())

  @Test
  fun testBtnShowPrKeyGone() {
    launchFragmentInContainer<PrivateKeyDetailsFragment>(
      fragmentArgs = PrivateKeyDetailsFragmentArgs(
        fingerprint = PrivateKeysManager.getPgpKeyDetailsFromAssets(
          KEY_PATH
        ).fingerprint
      ).toBundle()
    )

    Thread.sleep(5000)

    onView(withId(R.id.btnShowPrKey))
      .check(matches(not(isDisplayed())))
  }

  @Test
  @MoveToFlowBehaviour
  @Ignore("MoveToFlowBehaviour")
  fun testActionDeleteDisabled() {
    onView(withId(R.id.menuActionDeleteKey))
      .check(matches(not(isEnabled())))
  }

  companion object {
    private const val KEY_PATH = "pgp/default@flowcrypt.test_secondKey_prv_strong_second.asc"
  }
}
