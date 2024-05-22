/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasTextColor
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.junit.annotations.NotReadyForCI
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.service.PassPhrasesInRAMService
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.ui.activity.fragment.PrivateKeyDetailsFragmentArgs
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denys Bondarenko
 */
@FlowCryptTestSettings(useIntents = true)
@MediumTest
@RunWith(AndroidJUnit4::class)
class EndPassPhraseSessionFlowTest : BaseTest() {
  private val addAccountToDatabaseRule = AddAccountToDatabaseRule()
  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule(
    accountEntity = addAccountToDatabaseRule.account,
    keyPath = "pgp/default@flowcrypt.test_fisrtKey_prv_strong.asc",
    passphrase = TestConstants.DEFAULT_STRONG_PASSWORD,
    sourceType = KeyImportDetails.SourceType.EMAIL,
    passphraseType = KeyEntity.PassphraseType.RAM
  )

  override val activityScenarioRule = activityScenarioRule<MainActivity>(
    TestGeneralUtil.genIntentForNavigationComponent(
      destinationId = R.id.privateKeyDetailsFragment,
      extras = PrivateKeyDetailsFragmentArgs(
        fingerprint = PrivateKeysManager
          .getPgpKeyDetailsFromAssets(addPrivateKeyToDatabaseRule.keyPath).fingerprint
      ).toBundle()
    )
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  @FlakyTest
  @NotReadyForCI
  fun testEndPassPhraseSessionButton() {
    //as tests run a bit differently need to run PassPhrasesInRAMService manually at this stage
    PassPhrasesInRAMService.start(getTargetContext())

    val timeout = 5000L
    val activePassPhraseSessionLabel = getResString(R.string.active_passphrase_session)
    val endPassPhraseSessionLabel = getResString(R.string.end_pass_phrase_session)
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    //close the notification bar if needed
    if (device.hasObject(By.res(notificationResourcesName))) {
      device.pressBack()
    }
    val keysStorage = KeysStorageImpl.getInstance(getTargetContext())

    //at startup we don't have any passphrases in RAM
    onView(withText(R.string.pass_phrase_not_provided))
      .check(matches(isDisplayed()))
    assertFalse(keysStorage.hasNonEmptyPassphrase(KeyEntity.PassphraseType.RAM))

    //open notification and check we don't have "End Pass Phrase Session" button
    device.openNotification()
    device.wait(Until.hasObject(By.text(activePassPhraseSessionLabel)), timeout)

    assertNull(
      getActionButtonUiObject2(
        device,
        activePassPhraseSessionLabel,
        endPassPhraseSessionLabel
      )
    )
    assertFalse(keysStorage.hasNonEmptyPassphrase(KeyEntity.PassphraseType.RAM))

    //return back to the app and provide a pass phrase
    device.pressBack()
    onView(withId(R.id.btnProvidePassphrase))
      .perform(click())
    onView(withId(R.id.eTKeyPassword))
      .perform(
        clearText(),
        typeText(TestConstants.DEFAULT_STRONG_PASSWORD),
        closeSoftKeyboard()
      )
    onView(withId(R.id.btnUpdatePassphrase))
      .perform(click())
    onView(withId(R.id.tVPassPhraseVerification))
      .check(matches(withText(getResString(R.string.stored_pass_phrase_matched))))
      .check(matches(hasTextColor(R.color.colorPrimaryLight)))
    assertTrue(keysStorage.hasNonEmptyPassphrase(KeyEntity.PassphraseType.RAM))

    //open notification and check we have "End Pass Phrase Session" button
    device.openNotification()
    device.wait(Until.hasObject(By.text(activePassPhraseSessionLabel)), timeout)

    val actionButton = getActionButtonUiObject2(
      device,
      activePassPhraseSessionLabel,
      endPassPhraseSessionLabel
    )
    assertNotNull(actionButton)
    assertTrue(keysStorage.hasNonEmptyPassphrase(KeyEntity.PassphraseType.RAM))
    actionButton?.click()

    //return back to the app and check that passphrase was erased
    device.pressBack()
    onView(withText(R.string.pass_phrase_not_provided))
      .check(matches(isDisplayed()))
    assertFalse(keysStorage.hasNonEmptyPassphrase(KeyEntity.PassphraseType.RAM))
  }

  private fun getActionButtonUiObject2(
    device: UiDevice,
    activePassPhraseSessionLabel: String,
    endPassPhraseSessionLabel: String
  ): UiObject2? {
    val notifications = device.findObjects(By.res(notificationResourcesName))

    return notifications.firstNotNullOfOrNull { notification ->
      if (notification.hasObject(By.text(activePassPhraseSessionLabel))) {
        val expandButton = notification.findObject(
          By.pkg("com.android.systemui")
            .clazz("android.widget.Button")
            .res("android:id/expand_button")
        )

        expandButton?.click()
        notification.findObject(By.text(endPassPhraseSessionLabel))
      } else null
    }
  }

  companion object {
    private const val notificationResourcesName =
      "com.android.systemui:id/expandableNotificationRow"
  }
}
