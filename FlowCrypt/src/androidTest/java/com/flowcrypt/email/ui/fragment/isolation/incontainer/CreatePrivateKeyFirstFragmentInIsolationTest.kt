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
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.CreatePrivateKeyFirstFragment
import com.flowcrypt.email.ui.activity.fragment.CreatePrivateKeyFirstFragmentArgs
import com.flowcrypt.email.ui.base.BaseCheckPassphraseOnFirstScreenTest
import com.flowcrypt.email.util.AccountDaoManager
import org.junit.Before
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
class CreatePrivateKeyFirstFragmentInIsolationTest : BaseCheckPassphraseOnFirstScreenTest() {
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
    launchFragmentInContainer<CreatePrivateKeyFirstFragment>(
      fragmentArgs = CreatePrivateKeyFirstFragmentArgs(
        requestKey = UUID.randomUUID().toString(),
        accountEntity = AccountDaoManager.getDefaultAccountDao()
      ).toBundle()
    )
  }

  @Test
  fun testHintVisibility() {
    onView(withText(R.string.loss_of_this_pass_phrase_cannot_be_recovered))
      .check(matches(isDisplayed()))
  }
}
