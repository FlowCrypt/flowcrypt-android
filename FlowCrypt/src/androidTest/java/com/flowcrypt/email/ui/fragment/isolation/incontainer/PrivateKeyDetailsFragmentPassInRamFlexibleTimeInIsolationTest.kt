/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasTextColor
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.GrantPermissionRule
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.retrofit.response.model.OrgRules
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.service.PassPhrasesInRAMService
import com.flowcrypt.email.ui.activity.fragment.PrivateKeyDetailsFragment
import com.flowcrypt.email.ui.activity.fragment.PrivateKeyDetailsFragmentArgs
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.util.PrivateKeysManager
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.emptyString
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 *         Date: 8/30/22
 *         Time: 11:05 AM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class PrivateKeyDetailsFragmentPassInRamFlexibleTimeInIsolationTest : BaseTest() {
  private val userWithOrgRules = AccountDaoManager.getUserWithOrgRules(
    OrgRules(
      flags = listOf(
        OrgRules.DomainRule.NO_PRV_CREATE,
        OrgRules.DomainRule.NO_PRV_BACKUP
      ),
      customKeyserverUrl = null,
      keyManagerUrl = "https://keymanagerurl.test",
      disallowAttesterSearchForDomains = null,
      enforceKeygenAlgo = null,
      enforceKeygenExpireMonths = null,
      inMemoryPassPhraseSessionLength = 1
    )
  )
  private val addAccountToDatabaseRule = AddAccountToDatabaseRule(userWithOrgRules)
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
    .around(GrantPermissionRule.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(ScreenshotTestRule())

  @Test
  fun testInMemoryPassPhraseSessionLengthParameter() {
    launchFragmentInContainer<PrivateKeyDetailsFragment>(
      fragmentArgs = PrivateKeyDetailsFragmentArgs(
        fingerprint = PrivateKeysManager.getPgpKeyDetailsFromAssets(
          KEY_PATH
        ).fingerprint
      ).toBundle()
    )

    onView(withId(R.id.tVPassPhraseVerification))
      .check(matches(withText(getResString(R.string.pass_phrase_not_provided))))
      .check(matches(hasTextColor(R.color.red)))
    onView(withId(R.id.eTKeyPassword))
      .perform(
        clearText(),
        typeText(TestConstants.DEFAULT_SECOND_STRONG_PASSWORD),
        closeSoftKeyboard()
      )
    onView(withId(R.id.btnUpdatePassphrase))
      .perform(click())
    onView(withId(R.id.tVPassPhraseVerification))
      .check(matches(withText(getResString(R.string.stored_pass_phrase_matched))))
      .check(matches(hasTextColor(R.color.colorPrimaryLight)))
    onView(withId(R.id.btnUpdatePassphrase))
      .check(matches(not(isDisplayed())))

    //we need to wait around 1 minute to check that [PassPhrasesInRAMService] works well
    Thread.sleep(PassPhrasesInRAMService.DELAY_TIMEOUT)
    onView(withId(R.id.tVPassPhraseVerification))
      .check(matches(withText(getResString(R.string.pass_phrase_not_provided))))
      .check(matches(hasTextColor(R.color.red)))
    onView(withId(R.id.eTKeyPassword))
      .check(matches(withText(`is`(emptyString()))))
  }

  companion object {
    private const val KEY_PATH = "pgp/default@flowcrypt.test_secondKey_prv_strong_second.asc"
  }
}
