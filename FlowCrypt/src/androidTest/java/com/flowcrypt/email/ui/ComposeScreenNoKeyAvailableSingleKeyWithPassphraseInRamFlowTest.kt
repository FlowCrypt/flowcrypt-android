/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasTextColor
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.extensions.kotlin.asInternetAddress
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.base.BaseComposeScreenNoKeyAvailableTest
import org.hamcrest.CoreMatchers.not
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
class ComposeScreenNoKeyAvailableSingleKeyWithPassphraseInRamFlowTest : BaseComposeScreenNoKeyAvailableTest() {
  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule(
    keyPath = "pgp/key_testing@flowcrypt.test_keyA_strong.asc",
    passphraseType = KeyEntity.PassphraseType.RAM
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(activeActivityRule)
    .around(ScreenshotTestRule())

  @Test
  fun testAddEmailToExistingKey() {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()
    fillInAllFields(
      to = setOf(
        requireNotNull(TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER.asInternetAddress())
      )
    )

    //check that editTextFrom has gray text color. It means a sender doesn't have a private key
    onView(withId(R.id.editTextFrom))
      .check(matches(isDisplayed()))
      .check(matches(hasTextColor(R.color.gray)))

    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())

    isDialogWithTextDisplayed(
      decorView,
      getResString(R.string.no_key_available, addAccountToDatabaseRule.account.email)
    )

    onView(withText(R.string.add_email_to_existing_key))
      .check(matches(isDisplayed()))
      .perform(click())

    onView(withId(R.id.buttonOk))
      .check(doesNotExist())

    waitForObjectWithText(getResString(R.string.provide_passphrase), 2000)

    onView(withId(R.id.eTKeyPassword))
      .inRoot(RootMatchers.isDialog())
      .perform(
        ViewActions.clearText(),
        replaceText(TestConstants.DEFAULT_STRONG_PASSWORD),
        ViewActions.pressImeActionButton()
      )

    Thread.sleep(2000)

    //check that editTextFrom doesn't have gray text color. It means a sender has a private key.
    onView(withId(R.id.editTextFrom))
      .check(matches(isDisplayed()))
      .check(matches(not(hasTextColor(R.color.gray))))
  }
}
