/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import android.view.Gravity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.DrawerMatchers
import androidx.test.espresso.contrib.NavigationViewActions
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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 *         Date: 8/6/21
 *         Time: 2:14 PM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class ImportRecipientDisallowAttesterSearchFlowTest : BaseTest() {
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

  override val useIntents: Boolean = true
  override val activityScenarioRule = activityScenarioRule<MainActivity>()

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(addAccountToDatabaseRule)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  override fun setupFlowTest() {
    super.setupFlowTest()
    onView(withId(R.id.drawer_layout))
      .check(matches(DrawerMatchers.isClosed(Gravity.LEFT)))
      .perform(DrawerActions.open())

    onView(withId(R.id.navigationView))
      .perform(NavigationViewActions.navigateTo(R.id.navMenuActionSettings))

    Thread.sleep(500)
    onView(withText(getResString(R.string.contacts)))
      .perform(ViewActions.click())

    Thread.sleep(500)
    onView(withId(R.id.fABtImportPublicKey))
      .perform(ViewActions.click())
  }

  @Test
  fun testDisallowLookupOnAttester() {
    onView(withId(R.id.eTKeyIdOrEmail))
      .perform(
        clearText(),
        typeText("user@$DISALLOWED_DOMAIN"),
        pressImeActionButton()
      )

    onView(withText(R.string.supported_public_key_not_found))
      .check(matches((isDisplayed())))
  }

  companion object {
    private const val DISALLOWED_DOMAIN = "disallowed.test"
  }
}
