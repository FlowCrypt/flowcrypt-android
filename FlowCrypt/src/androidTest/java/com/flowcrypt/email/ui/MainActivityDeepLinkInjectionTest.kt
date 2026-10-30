/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui

import android.content.Intent
import android.os.ParcelFileDescriptor
import androidx.navigation.Navigation
import androidx.test.espresso.Espresso.onIdle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.util.TestGeneralUtil
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * Third-party apps must not be able to drive navigation in [MainActivity] via explicit navigation
 * deep links (android-support-nav:controller:deepLink* extras). Navigation handles such extras
 * only when [MainActivity] was started by the app itself.
 *
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class MainActivityDeepLinkInjectionTest : BaseTest() {

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(AddAccountToDatabaseRule())
    .around(AddPrivateKeyToDatabaseRule())
    .around(ScreenshotTestRule())

  @After
  fun finishMainActivity() {
    val activity = getResumedMainActivity() ?: return
    InstrumentationRegistry.getInstrumentation().runOnMainSync { activity.finish() }
  }

  @Test
  fun testExplicitDeepLinkFromAnotherAppIsIgnored() {
    startMainActivityFromShell("--eia $KEY_DEEP_LINK_IDS $DEEP_LINK_IDS")
    assertDeepLinkIsIgnored()
  }

  @Test
  fun testExplicitDeepLinkFromAnotherAppIsIgnoredAfterSpoofedReferrer() {
    //Activity.getReferrer() prefers referrer extras of the current activity intent. The first
    //intent leaves a spoofed referrer there, the second one is delivered via onNewIntent()
    startMainActivityFromShell("--es ${Intent.EXTRA_REFERRER_NAME} android-app://$packageName")
    assertDeepLinkIsIgnored()

    startMainActivityFromShell("--eia $KEY_DEEP_LINK_IDS $DEEP_LINK_IDS")
    assertDeepLinkIsIgnored()
  }

  @Test
  fun testExplicitDeepLinkFromAppItselfIsHandled() {
    val intent = TestGeneralUtil.genIntentForNavigationComponent(
      destinationId = R.id.mainSettingsFragment
    )
    InstrumentationRegistry.getInstrumentation().targetContext.startActivity(intent)

    assertEquals(R.id.mainSettingsFragment, waitForCurrentDestinationId())
  }

  /**
   * The shell (com.android.shell) acts as a third-party app here. The test APK can't be used
   * for that as it runs in the app process and is seen by the system as the app itself.
   */
  private fun startMainActivityFromShell(extras: String) {
    val component = "$packageName/${MainActivity::class.java.name}"
    InstrumentationRegistry.getInstrumentation().uiAutomation
      .executeShellCommand("am start -W -n $component $extras")
      .use { ParcelFileDescriptor.AutoCloseInputStream(it).readBytes() }
  }

  private fun assertDeepLinkIsIgnored() {
    //'am start' adds FLAG_ACTIVITY_NEW_TASK, so a handled deep link restarts the task
    //asynchronously. Give it time to show up before checking the current destination
    waitUntil(TimeUnit.SECONDS.toMillis(5)) {
      getCurrentDestinationId() == R.id.mainSettingsFragment
    }
    assertEquals(R.id.messagesListFragment, waitForCurrentDestinationId())
  }

  private fun waitForCurrentDestinationId(): Int? {
    val expectedDestinationIds = setOf(R.id.messagesListFragment, R.id.mainSettingsFragment)
    waitUntil { getCurrentDestinationId() in expectedDestinationIds }
    onIdle()
    return getCurrentDestinationId()
  }

  private fun getCurrentDestinationId(): Int? {
    val activity = getResumedMainActivity() ?: return null
    var destinationId: Int? = null
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
      destinationId = runCatching {
        Navigation.findNavController(activity, R.id.fragmentContainerView).currentDestination?.id
      }.getOrNull()
    }
    return destinationId
  }

  private fun getResumedMainActivity(): MainActivity? {
    var activity: MainActivity? = null
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
      activity = ActivityLifecycleMonitorRegistry.getInstance()
        .getActivitiesInStage(Stage.RESUMED)
        .filterIsInstance<MainActivity>()
        .firstOrNull()
    }
    return activity
  }

  private val packageName: String
    get() = InstrumentationRegistry.getInstrumentation().targetContext.packageName

  private companion object {
    const val KEY_DEEP_LINK_IDS = "android-support-nav:controller:deepLinkIds"
    val DEEP_LINK_IDS = "${R.id.nav_graph},${R.id.mainSettingsFragment}"
  }
}
