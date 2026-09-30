/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.UserRecoverableAuthExceptionFragment
import com.flowcrypt.email.util.AccountDaoManager
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
@FlowCryptTestSettings(useIntents = true)
class UserRecoverableAuthExceptionFragmentDeepLinkInjectionTest : BaseTest() {

  private val addAccountToDatabaseRule =
    AddAccountToDatabaseRule(
      account = AccountDaoManager.getDefaultAccountDao().copy(
        accountType = AccountEntity.ACCOUNT_TYPE_GOOGLE
      )
    )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(AddPrivateKeyToDatabaseRule(addAccountToDatabaseRule.account))
    .around(ScreenshotTestRule())

  @Test
  fun testAttackerControlledForeignComponentIsNotLaunched() {
    val attackerIntent = Intent().setComponent(
      ComponentName("com.attacker.poc", "com.attacker.poc.StealActivity")
    )

    launchFragmentWithRecoverableIntent(attackerIntent)
    clickReconnect()

    //an attacker controlled intent must never be launched
    assertIntentsNotContaining(attackerIntent)
  }

  @Test
  fun testIntentWithUriPermissionGrantFlagIsNotLaunched() {
    val attackerIntent = Intent()
      .setComponent(ComponentName("com.attacker.poc", "com.attacker.poc.StealActivity"))
      .setData(Uri.parse("content://com.flowcrypt.email.embedded.attachments/some-uuid"))
      .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    launchFragmentWithRecoverableIntent(attackerIntent)
    clickReconnect()

    assertIntentsNotContaining(attackerIntent)
  }

  @Test
  fun testLegitimateGoogleIntentIsStillLaunched() {
    val googleIntent = Intent().setComponent(
      ComponentName("com.google.android.gms", "com.google.android.gms.auth.uic.MintTokenActivity")
    )

    //no real Google Play services on the test device, so stub the launch to verify it is sent
    Intents.intending(hasComponent(googleIntent.component))
      .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, Intent()))

    launchFragmentWithRecoverableIntent(googleIntent)
    clickReconnect()

    Intents.intended(hasComponent(googleIntent.component))
  }

  private fun clickReconnect() {
    onView(withId(R.id.buttonReconnect))
      .check(matches(isDisplayed()))
      .perform(click())
  }

  private fun launchFragmentWithRecoverableIntent(intent: Intent) {
    val args = Bundle().apply {
      putParcelable(KEY_RECOVERABLE_INTENT, intent)
    }
    launchFragmentInContainer<UserRecoverableAuthExceptionFragment>(
      fragmentArgs = args
    )
  }

  private fun assertIntentsNotContaining(intent: Intent) {
    Intents.intended(not(hasComponent(intent.component)))
  }

  private companion object {
    const val KEY_RECOVERABLE_INTENT = "recoverableIntent"
  }
}
