/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.ui.activity.fragment.UpdatePrivateKeyFragment
import com.flowcrypt.email.ui.activity.fragment.UpdatePrivateKeyFragmentArgs
import com.flowcrypt.email.ui.base.AddAccountToDatabaseRuleInterface
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.*
import org.hamcrest.Matchers
import org.hamcrest.Matchers.emptyString
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.pgpainless.key.info.KeyRingInfo

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@FlowCryptTestSettings(useCommonIdling = false)
class UpdatePrivateKeyFragmentInIsolationTest : BaseTest(), AddAccountToDatabaseRuleInterface {

  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule()

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(ScreenshotTestRule())

  @Before
  fun launchFragmentInContainerWithPredefinedArgs() {
    launchFragmentInContainer<UpdatePrivateKeyFragment>(
      UpdatePrivateKeyFragmentArgs(
        accountEntity = addAccountToDatabaseRule.account,
        existingPgpKeyRingDetails = addPrivateKeyToDatabaseRule.pgpKeyRingDetails,
      ).toBundle()
    )
  }

  @Test
  fun testBaseInfo() {
    Thread.sleep(1000)

    onView(withId(R.id.editTextNewPrivateKey))
      .check(matches(withText(`is`(emptyString()))))
    onView(withId(R.id.buttonCheck))
      .check(matches(isNotEnabled()))

    onView(withId(R.id.editTextNewPrivateKey))
      .perform(ViewActions.replaceText("Some text"))

    onView(withId(R.id.buttonCheck))
      .check(matches(isEnabled()))
  }
}