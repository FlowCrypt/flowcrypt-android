/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.ui.activity.fragment.ImportPrivateKeysDuringSetupFragmentArgs
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@FlowCryptTestSettings(useIntents = true, useCommonIdling = false)
class CheckKeysFragmentSomePartiallyEncryptedKeyTest : BaseTest() {
  private val addAccountToDatabaseRule = AddAccountToDatabaseRule()

  override val activityScenarioRule = activityScenarioRule<MainActivity>(
    TestGeneralUtil.genIntentForNavigationComponent(
      destinationId = R.id.importPrivateKeysDuringSetupFragment,
      extras = ImportPrivateKeysDuringSetupFragmentArgs(
        requestKey = UUID.randomUUID().toString(),
        accountEntity = addAccountToDatabaseRule.account
      ).toBundle()
    )
  )

  private val pgpKeyRingDetails: PgpKeyRingDetails = PrivateKeysManager.getPgpKeyDetailsFromAssets(
    "pgp/partially_encrypted@flowcrypt.test_prv_default.asc"
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testShowingDialog() {
    addTextToClipboard(
      "private key",
      requireNotNull(pgpKeyRingDetails.privateKey) + "\n" + TestGeneralUtil.readFileFromAssetsAsString(
        "pgp/default@flowcrypt.test_secondKey_prv_strong_second.asc"
      )
    )

    waitForObjectWithText(
      getResString(R.string.load_from_clipboard).uppercase(),
      TimeUnit.SECONDS.toMillis(5)
    )

    onView(withId(R.id.buttonLoadFromClipboard))
      .check(matches(isDisplayed()))
      .perform(click())

    waitForObjectWithText(
      getResString(R.string.partially_encrypted_private_key_error_msg),
      TimeUnit.SECONDS.toMillis(5)
    )

    isDialogWithTextDisplayed(
      decorView,
      getResString(R.string.partially_encrypted_private_key_error_msg)
    )

    onView(withText(android.R.string.ok))
      .check(matches(isDisplayed()))
      .perform(click())

    //check that we didn't leave the correct screen
    onView(withId(R.id.editTextKeyPassword))
      .check(matches(isDisplayed()))
  }
}