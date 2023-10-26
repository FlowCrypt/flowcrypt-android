/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withChild
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.matchers.CustomMatchers.Companion.hasItem
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withRecyclerViewItemCount
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.util.TestGeneralUtil
import org.hamcrest.Matchers.allOf
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
class ImportUpdatedVersionOfPrivateKeyViaSettingsImportFlowTest : BaseTest() {
  override val activityScenarioRule = activityScenarioRule<MainActivity>(
    TestGeneralUtil.genIntentForNavigationComponent(
      destinationId = R.id.privateKeysListFragment
    )
  )

  private val addAccountToDatabaseRule = AddAccountToDatabaseRule()
  private val addPrivateKeyToDatabaseRuleDefault = AddPrivateKeyToDatabaseRule()
  private val addPrivateKeyToDatabaseRuleExpired = AddPrivateKeyToDatabaseRule(
    accountEntity = addAccountToDatabaseRule.account,
    keyPath = "pgp/expired@flowcrypt.test_prv_default.asc",
    passphrase = TestConstants.DEFAULT_PASSWORD,
    sourceType = KeyImportDetails.SourceType.EMAIL
  )

  private val expiredExtendedPrvKey = TestGeneralUtil.readFileFromAssetsAsString(
    "pgp/expired@flowcrypt.test_prv_default_extended.asc"
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRuleDefault)
    .around(addPrivateKeyToDatabaseRuleExpired)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testDisallowUpdatingPrivateKeyFromSettingsImport() {

    onView(withId(R.id.recyclerViewKeys))
      .check(matches(withRecyclerViewItemCount(2)))

    onView(withId(R.id.recyclerViewKeys))
      .check(
        matches(prepareMatcherForExpiredKey())
      )

    assertTrue(
      KeysStorageImpl.getInstance(getTargetContext()).getPgpKeyDetailsList().first {
        it.fingerprint == addPrivateKeyToDatabaseRuleExpired.pgpKeyDetails.fingerprint
      }.isExpired
    )

    onView(withId(R.id.floatActionButtonAddKey))
      .check(matches(isDisplayed()))
      .perform(click())

    onView(withText(R.string.import_private_key))
      .check(matches(isDisplayed()))

    addTextToClipboard("private key", expiredExtendedPrvKey)

    onView(withId(R.id.buttonLoadFromClipboard))
      .check(matches(isDisplayed()))
      .perform(click())

    onView(withId(R.id.editTextKeyPassword))
      .check(doesNotExist())

    isDialogWithTextDisplayed(decorView, getResString(R.string.key_already_added))

    assertTrue(
      KeysStorageImpl.getInstance(getTargetContext()).getPgpKeyDetailsList().first {
        it.fingerprint == addPrivateKeyToDatabaseRuleExpired.pgpKeyDetails.fingerprint
      }.isExpired
    )
  }

  private fun prepareMatcherForExpiredKey() = hasItem(
    withChild(
      allOf(
        hasSibling(
          withText(
            addPrivateKeyToDatabaseRuleExpired.pgpKeyDetails.getUserIdsAsSingleString()
          )
        ),
        hasSibling(
          withText(
            addPrivateKeyToDatabaseRuleExpired.pgpKeyDetails.getStatusText(getTargetContext())
          )
        )
      )
    )
  )
}
