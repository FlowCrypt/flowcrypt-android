/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.fragment

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.model.OrgRules
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
 *         Date: 8/6/21
 *         Time: 2:05 PM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class ImportRecipientsFromSourceFragmentDisallowAttesterSearchForDomainFlowTest : BaseTest() {
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

  private val addAccountToDatabaseRule = AddAccountToDatabaseRule(userWithOrgRules)

  override val useIntents: Boolean = true
  override val activityScenarioRule = activityScenarioRule<MainActivity>(
    TestGeneralUtil.genIntentForNavigationComponent(
      uri = "flowcrypt://email.flowcrypt.com/settings/contacts/import"
    )
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(addAccountToDatabaseRule)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testCanLookupThisRecipientOnAttester() {
    onView(withId(R.id.eTKeyIdOrEmail))
      .perform(
        clearText(),
        typeText("user@$DISALLOWED_DOMAIN"),
        closeSoftKeyboard()
      )
    onView(withId(R.id.iBSearchKey))
      .check(matches(isDisplayed()))
      .perform(click())

    onView(withText(R.string.supported_public_key_not_found))
      .check(matches((isDisplayed())))
  }

  companion object {
    private const val DISALLOWED_DOMAIN = "disallowed.test"
  }
}
