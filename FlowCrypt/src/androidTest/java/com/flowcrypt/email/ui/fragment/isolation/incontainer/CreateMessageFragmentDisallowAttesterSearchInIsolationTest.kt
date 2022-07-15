/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.retrofit.response.model.OrgRules
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withChipsBackgroundColor
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.CreateMessageFragment
import com.flowcrypt.email.ui.activity.fragment.CreateMessageFragmentArgs
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.util.UIUtil
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 *         Date: 8/6/21
 *         Time: 1:52 PM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class CreateMessageFragmentDisallowAttesterSearchInIsolationTest : BaseTest() {
  private val userWithOrgRules = AccountDaoManager.getUserWithOrgRules(
    OrgRules(
      flags = listOf(
        OrgRules.DomainRule.NO_PRV_CREATE,
        OrgRules.DomainRule.NO_PRV_BACKUP
      ),
      customKeyserverUrl = null,
      keyManagerUrl = "https://keymanagerurl.test",
      disallowAttesterSearchForDomains = listOf("*"),
      enforceKeygenAlgo = null,
      enforceKeygenExpireMonths = null
    )
  )

  private val addAccountToDatabaseRule = AddAccountToDatabaseRule(userWithOrgRules)
  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule(
    accountEntity = addAccountToDatabaseRule.account,
    keyPath = "pgp/default@flowcrypt.test_fisrtKey_prv_strong.asc",
    passphrase = TestConstants.DEFAULT_SECOND_STRONG_PASSWORD,
    sourceType = KeyImportDetails.SourceType.EMAIL,
    passphraseType = KeyEntity.PassphraseType.RAM
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(ScreenshotTestRule())

  @Test
  @Ignore("fix me. Fails sometimes")
  fun testDisallowLookupOnAttester() {
    launchFragmentInContainer<CreateMessageFragment>(
      fragmentArgs = CreateMessageFragmentArgs().toBundle()
    )

    val recipient = "recipient@example.test"

    onView(withId(R.id.editTextRecipientTo))
      .perform(typeText(recipient), closeSoftKeyboard())
    //need to leave focus from 'To' field. move the focus to the next view
    onView(withId(R.id.editTextEmailSubject))
      .perform(
        click(),
        typeText("subject"),
        closeSoftKeyboard()
      )
    onView(withId(R.id.editTextEmailMessage))
      .perform(
        typeText("message"),
        closeSoftKeyboard()
      )

    onView(withId(R.id.editTextRecipientTo))
      .check(
        matches(
          withChipsBackgroundColor(
            recipient,
            UIUtil.getColor(
              getTargetContext(),
              CustomChipSpanChipCreator.CHIP_COLOR_RES_ID_NO_PUB_KEY
            )
          )
        )
      )
  }
}
