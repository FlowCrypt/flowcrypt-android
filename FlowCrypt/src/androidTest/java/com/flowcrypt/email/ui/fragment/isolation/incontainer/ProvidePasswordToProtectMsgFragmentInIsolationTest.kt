/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasTextColor
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.ProvidePasswordToProtectMsgFragment
import com.flowcrypt.email.ui.activity.fragment.ProvidePasswordToProtectMsgFragmentArgs
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
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
class ProvidePasswordToProtectMsgFragmentInIsolationTest : BaseTest() {
  private val accountRule = AddAccountToDatabaseRule()

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(accountRule)
    .around(ScreenshotTestRule())

  @Test
  fun testPasswordStrength() {
    launchFragmentInContainer<ProvidePasswordToProtectMsgFragment>(
      fragmentArgs = ProvidePasswordToProtectMsgFragmentArgs().toBundle()
    )

    onView(withId(R.id.btSetPassword))
      .check(matches(not(isEnabled())))

    //type one uppercase
    checkConditionItemState(R.id.checkedTVOneUppercase, false)
    onView(withId(R.id.eTPassphrase))
      .perform(
        typeText("A"),
        closeSoftKeyboard()
      )
    checkConditionItemState(R.id.checkedTVOneUppercase, true)

    //type one lowercase
    checkConditionItemState(R.id.checkedTVOneLowercase, false)
    onView(withId(R.id.eTPassphrase))
      .perform(
        typeText("a"),
        closeSoftKeyboard()
      )
    checkConditionItemState(R.id.checkedTVOneLowercase, true)

    //type one number
    checkConditionItemState(R.id.checkedTVOneNumber, false)
    onView(withId(R.id.eTPassphrase))
      .perform(
        typeText("1"),
        closeSoftKeyboard()
      )
    checkConditionItemState(R.id.checkedTVOneNumber, true)

    //type one special character
    checkConditionItemState(R.id.checkedTVOneSpecialCharacter, false)
    onView(withId(R.id.eTPassphrase))
      .perform(
        typeText("@"),
        closeSoftKeyboard()
      )
    checkConditionItemState(R.id.checkedTVOneSpecialCharacter, true)

    //type one special character
    checkConditionItemState(R.id.checkedTVMinLength, false)
    onView(withId(R.id.eTPassphrase))
      .perform(
        typeText("more than 8 symbols"),
        closeSoftKeyboard()
      )
    checkConditionItemState(R.id.checkedTVMinLength, true)

    //check that button is enabled
    onView(withId(R.id.btSetPassword))
      .check(matches(isEnabled()))
  }

  private fun checkConditionItemState(id: Int, isChecked: Boolean) {
    onView(withId(id))
      .check(
        matches(
          allOf(
            if (isChecked) isChecked() else isNotChecked(),
            hasTextColor(if (isChecked) R.color.colorPrimary else R.color.orange),
          )
        )
      )
  }
}
