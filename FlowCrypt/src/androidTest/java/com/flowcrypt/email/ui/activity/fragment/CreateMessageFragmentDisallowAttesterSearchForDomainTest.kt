/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.retrofit.response.model.OrgRules
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withChipsBackgroundColor
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.base.BaseCreateMessageActivityTest
import com.flowcrypt.email.ui.widget.CustomChipSpanChipCreator
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.util.UIUtil
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 *         Date: 8/6/21
 *         Time: 1:24 PM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class CreateMessageFragmentDisallowAttesterSearchForDomainTest : BaseCreateMessageActivityTest() {

  private val userWithOrgRules = AccountDaoManager.getUserWithOrgRules(
    OrgRules(
      flags = listOf(
        OrgRules.DomainRule.NO_PRV_CREATE,
        OrgRules.DomainRule.NO_PRV_BACKUP
      ),
      customKeyserverUrl = null,
      keyManagerUrl = "https://keymanagerurl.test",
      disallowAttesterSearchForDomains = listOf(DISALLOWED_DOMAIN),
      enforceKeygenAlgo = null,
      enforceKeygenExpireMonths = null
    )
  )

  override val addAccountToDatabaseRule = AddAccountToDatabaseRule(userWithOrgRules)
  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule(
    accountEntity = addAccountToDatabaseRule.account,
    keyPath = "pgp/default@flowcrypt.test_fisrtKey_prv_strong.asc",
    passphrase = TestConstants.DEFAULT_SECOND_STRONG_PASSWORD,
    sourceType = KeyImportDetails.SourceType.EMAIL,
    passphraseType = KeyEntity.PassphraseType.RAM
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(ClearAppSettingsRule())
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(RetryRule.DEFAULT)
    .around(activeActivityRule)
    .around(ScreenshotTestRule())

  @Test
  fun testCanLookupThisRecipientOnAttester() {
    activeActivityRule.launch(intent)
    registerAllIdlingResources()

    val recipient = "user@$DISALLOWED_DOMAIN"

    fillInAllFields(recipient)

    onView(withId(R.id.editTextRecipientTo))
      .check(
        matches(
          withChipsBackgroundColor(
            recipient,
            UIUtil.getColor(
              getTargetContext(),
              CustomChipSpanChipCreator.CHIP_COLOR_RES_ID_PGP_NOT_EXISTS
            )
          )
        )
      )
  }

  companion object {
    private const val DISALLOWED_DOMAIN = "disallowed.test"
  }
}
