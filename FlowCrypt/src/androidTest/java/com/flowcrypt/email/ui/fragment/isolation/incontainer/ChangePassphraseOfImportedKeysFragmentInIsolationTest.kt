/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.junit.annotations.DependsOnMailServer
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.ChangePassphraseOfImportedKeysFragment
import com.flowcrypt.email.ui.activity.fragment.ChangePassphraseOfImportedKeysFragmentArgs
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
class ChangePassphraseOfImportedKeysFragmentInIsolationTest : BaseTest() {
  private val addAccountToDatabaseRule = AddAccountToDatabaseRule()

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(AddPrivateKeyToDatabaseRule())
    .around(ScreenshotTestRule())

  @Before
  fun launchFragmentInContainerWithPredefinedArgs() {
    launchFragmentInContainer<ChangePassphraseOfImportedKeysFragment>(
      fragmentArgs = ChangePassphraseOfImportedKeysFragmentArgs(
        popBackStackIdIfSuccess = R.id.backupKeysFragment,
        title = getResString(R.string.pass_phrase_changed),
        subTitle = getResString(R.string.passphrase_was_changed),
        passphrase = PERFECT_PASSWORD,
        accountEntity = addAccountToDatabaseRule.account
      ).toBundle()
    )
  }

  @Test
  @DependsOnMailServer
  fun testShowSuccess() {
    onView(withText(getResString(R.string.pass_phrase_changed)))
      .check(matches(isDisplayed()))
    onView(withText(getResString(R.string.passphrase_was_changed)))
      .check(matches(isDisplayed()))
  }

  companion object {
    internal const val PERFECT_PASSWORD = "unconventional blueberry unlike any other"
  }
}
