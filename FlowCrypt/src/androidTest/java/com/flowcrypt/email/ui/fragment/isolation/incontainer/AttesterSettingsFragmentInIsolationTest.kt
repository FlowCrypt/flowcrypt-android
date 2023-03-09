/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasTextColor
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withEmptyRecyclerView
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withRecyclerViewItemCount
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.AttesterSettingsFragment
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.util.PrivateKeysManager
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.Matchers.not
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.net.HttpURLConnection

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class AttesterSettingsFragmentInIsolationTest : BaseTest() {
  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(ScreenshotTestRule())

  @Test
  fun testKeysExistOnAttester() {
    FlowCryptRoomDatabase.getDatabase(getTargetContext()).accountDao().addAccount(defaultAccount)
    PrivateKeysManager.saveKeyToDatabase(
      accountEntity = defaultAccount,
      pgpKeyDetails = pgpKeyDetailsPrimaryDefaultAccount,
      passphrase = TestConstants.DEFAULT_STRONG_PASSWORD,
      sourceType = KeyImportDetails.SourceType.EMAIL,
      passphraseType = KeyEntity.PassphraseType.DATABASE
    )

    launchFragmentInContainer<AttesterSettingsFragment>()

    onView(withId(R.id.empty))
      .check(matches(not(isDisplayed())))

    onView(withId(R.id.rVAttester))
      .check(matches(isDisplayed()))
      .check(matches(not(withEmptyRecyclerView()))).check(matches(isDisplayed()))
      .check(matches(withRecyclerViewItemCount(1)))

    onView(withId(R.id.rVAttester))
      .perform(scrollToPosition<RecyclerView.ViewHolder>(1))
      .check(matches(hasDescendant(withText(getResString(R.string.submitted_can_receive_encrypted_email)))))
      .check(matches(hasDescendant(withText(pgpKeyDetailsPrimaryDefaultAccount.getUserIdsAsSingleString()))))

    onView(withText(getResString(R.string.submitted_can_receive_encrypted_email)))
      .check(matches(hasTextColor(R.color.colorPrimary)))
  }

  @Test
  fun testDifferentKeysOnAttester() {
    FlowCryptRoomDatabase.getDatabase(getTargetContext()).accountDao()
      .addAccount(defaultAccount)

    val pgpKeyDetails = PrivateKeysManager.getPgpKeyDetailsFromAssets(
      "pgp/default@flowcrypt.test_secondKey_prv_strong.asc"
    )

    PrivateKeysManager.saveKeyToDatabase(
      accountEntity = defaultAccount,
      pgpKeyDetails = pgpKeyDetails,
      passphrase = TestConstants.DEFAULT_STRONG_PASSWORD,
      sourceType = KeyImportDetails.SourceType.EMAIL,
      passphraseType = KeyEntity.PassphraseType.DATABASE
    )

    launchFragmentInContainer<AttesterSettingsFragment>()

    onView(withId(R.id.empty))
      .check(matches(not(isDisplayed())))

    onView(withId(R.id.rVAttester))
      .check(matches(isDisplayed()))
      .check(matches(not(withEmptyRecyclerView()))).check(matches(isDisplayed()))
      .check(matches(withRecyclerViewItemCount(1)))

    onView(withId(R.id.rVAttester))
      .perform(scrollToPosition<RecyclerView.ViewHolder>(1))
      .check(matches(hasDescendant(withText(getResString(R.string.wrong_public_key_recorded)))))
      .check(matches(hasDescendant(withText(pgpKeyDetails.getUserIdsAsSingleString()))))

    onView(withText(getResString(R.string.wrong_public_key_recorded)))
      .check(matches(hasTextColor(R.color.red)))
  }

  @Test
  @FlakyTest
  fun testAccountWithNoKeysOnAttester() {
    FlowCryptRoomDatabase.getDatabase(getTargetContext()).accountDao()
      .addAccount(userWithoutPubKeyOnAttester)

    val pgpKeyDetails = PrivateKeysManager.getPgpKeyDetailsFromAssets(
      "pgp/not_attested_user@flowcrypt.test_prv_default.asc"
    )

    PrivateKeysManager.saveKeyToDatabase(
      accountEntity = userWithoutPubKeyOnAttester,
      pgpKeyDetails = pgpKeyDetails,
      passphrase = TestConstants.DEFAULT_PASSWORD,
      sourceType = KeyImportDetails.SourceType.EMAIL,
      passphraseType = KeyEntity.PassphraseType.DATABASE
    )

    launchFragmentInContainer<AttesterSettingsFragment>()

    onView(withId(R.id.empty))
      .check(matches(isDisplayed()))

    onView(withId(R.id.rVAttester))
      .check(matches(not(isDisplayed())))
  }

  companion object {
    private val defaultAccount = AccountDaoManager.getDefaultAccountDao()
    private val userWithoutPubKeyOnAttester = AccountDaoManager.getUserWithoutBackup()

    private val pgpKeyDetailsPrimaryDefaultAccount = PrivateKeysManager.getPgpKeyDetailsFromAssets(
      "pgp/default@flowcrypt.test_fisrtKey_prv_strong.asc"
    )

    @get:ClassRule
    @JvmStatic
    val mockWebServerRule =
      FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
          if (request.path?.startsWith("/attester/pub", ignoreCase = true) == true) {
            val lastSegment = request.requestUrl?.pathSegments?.lastOrNull()
            when {
              defaultAccount.email.equals(lastSegment, true) -> {
                return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
                  .setBody(pgpKeyDetailsPrimaryDefaultAccount.publicKey)
              }
            }
          }

          return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
        }
      })
  }
}
