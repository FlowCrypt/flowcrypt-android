/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.CreatePrivateKeySecondFragment
import com.flowcrypt.email.ui.activity.fragment.CreatePrivateKeySecondFragmentArgs
import com.flowcrypt.email.ui.base.BaseCheckPassphraseOnFirstScreenTest
import com.flowcrypt.email.util.AccountDaoManager
import org.hamcrest.Matchers
import org.hamcrest.Matchers.emptyString
import org.hamcrest.Matchers.`is`
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
@FlowCryptTestSettings(useCommonIdling = false)
class CreatePrivateKeySecondFragmentInIsolationTest : BaseCheckPassphraseOnFirstScreenTest() {
  override val firstScreenContinueButtonResId: Int = R.id.buttonSetPassPhrase
  override val firstScreenEditTextResId: Int = R.id.editTextKeyPassword
  override val firstScreenPasswordQualityInfoResId: Int = R.id.textViewPasswordQualityInfo

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(ScreenshotTestRule())

  @Before
  fun launchFragmentInContainerWithPredefinedArgs() {
    launchFragmentInContainer<CreatePrivateKeySecondFragment>(
      fragmentArgs = CreatePrivateKeySecondFragmentArgs(
        accountEntity = AccountDaoManager.getDefaultAccountDao(),
        passphrase = PASSPHRASE
      ).toBundle()
    )
  }

  @Test
  fun testPassphrase() {
    onView(withId(R.id.textViewSecondPasswordCheckTitle))
      .check(matches(withText(getResString(R.string.set_up_flow_crypt, getResString(R.string.app_name)))))
    onView(withId(R.id.editTextKeyPasswordSecond))
      .check(matches(withText(`is`(emptyString()))))

    onView(withId(R.id.buttonConfirmPassPhrases))
      .perform(click())
    checkIsSnackbarDisplayedAndClick(getResString(R.string.passphrase_must_be_non_empty))

    onView(withId(R.id.editTextKeyPasswordSecond))
      .perform(replaceText("Some text"))
    onView(withId(R.id.buttonConfirmPassPhrases))
      .perform(click())
    checkIsSnackbarDisplayedAndClick(getResString(R.string.pass_phrases_do_not_match))
  }

  companion object {
    val PASSPHRASE = "qwerty1234".chars().toArray()
  }
}
