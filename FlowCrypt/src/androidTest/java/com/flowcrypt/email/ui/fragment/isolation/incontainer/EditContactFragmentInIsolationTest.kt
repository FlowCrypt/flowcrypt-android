/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isNotEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.entity.PublicKeyEntity
import com.flowcrypt.email.junit.annotations.DependsOnMailServer
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.EditContactFragment
import com.flowcrypt.email.ui.activity.fragment.EditContactFragmentArgs
import com.flowcrypt.email.ui.base.AddAccountToDatabaseRuleInterface
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.startsWith
import org.hamcrest.Matchers
import org.hamcrest.Matchers.emptyString
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
class EditContactFragmentInIsolationTest : BaseTest(), AddAccountToDatabaseRuleInterface {
  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule()

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
    launchFragmentInContainer<EditContactFragment>(
      fragmentArgs = EditContactFragmentArgs(
        accountEntity = addAccountToDatabaseRule.account,
        publicKeyEntity = PublicKeyEntity(
          recipient = requireNotNull(addPrivateKeyToDatabaseRule.pgpKeyRingDetails.primaryUserId),
          fingerprint = addPrivateKeyToDatabaseRule.pgpKeyRingDetails.fingerprint,
          publicKey = addPrivateKeyToDatabaseRule.pgpKeyRingDetails.publicKey.toByteArray()
        )
      ).toBundle()
    )
  }

  @Test
  fun testBaseInfo() {
    Thread.sleep(1000)

    onView(withId(R.id.editTextNewPubKey))
      .check(matches(withText(`is`(emptyString()))))
    onView(withId(R.id.buttonCheck))
      .check(matches(isNotEnabled()))

    onView(withId(R.id.editTextNewPubKey))
      .perform(ViewActions.replaceText("Some text"))

    onView(withId(R.id.buttonCheck))
      .check(matches(isEnabled()))
  }
}