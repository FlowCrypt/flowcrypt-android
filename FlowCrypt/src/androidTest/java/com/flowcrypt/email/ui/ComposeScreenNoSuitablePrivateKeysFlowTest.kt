/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.extensions.kotlin.asInternetAddress
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.ui.base.BaseComposeScreenTest
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.flowcrypt.email.util.exception.NoKeyAvailableException
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
class ComposeScreenNoSuitablePrivateKeysFlowTest : BaseComposeScreenTest() {
  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule(
    keyPath = "pgp/default@flowcrypt.test_secondKey_prv_strong_revoked.asc"
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
  fun testShowNoPrivateKeysSuitableForEncryptionWarning() {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()
    fillInAllFields(
      to = setOf(
        requireNotNull(TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER.asInternetAddress())
      )
    )

    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())

    val exception = Assert.assertThrows(NoKeyAvailableException::class.java) {
      SecurityUtils.getSenderPublicKeys(getTargetContext(), addAccountToDatabaseRule.account.email)
    }

    assertEquals(
      getResString(
        R.string.no_key_available_for_your_email_account,
        getResString(R.string.support_email)
      ),
      exception.message
    )

    isDialogWithTextDisplayed(
      decorView,
      UIUtil.getHtmlSpannedFromText(
        GeneralUtil.prepareWarningTextAboutUnusableForEncryptionKeys(
          context = getTargetContext(),
          keysStorage = KeysStorageImpl.getInstance(getTargetContext())
        )
      ).toString()
    )
  }

  @Test
  fun testDoNotShowNoPrivateKeysSuitableForEncryptionWarningIfAtLeastOneKeyAvailable() {
    val details = PrivateKeysManager.getPgpKeyDetailsFromAssets(
      "pgp/default@flowcrypt.test_fisrtKey_prv_strong.asc"
    )
    PrivateKeysManager.saveKeyToDatabase(
      accountEntity = addAccountToDatabaseRule.account,
      pgpKeyRingDetails = details,
      passphrase = null,
      sourceType = KeyImportDetails.SourceType.EMAIL,
      passphraseType = KeyEntity.PassphraseType.RAM
    )

    activeActivityRule?.launch(intent)
    registerAllIdlingResources()
    fillInAllFields(
      to = setOf(
        requireNotNull(TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER.asInternetAddress())
      )
    )

    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())

    //the sender has 2 private keys. But one of them is revoked.
    //Anyway at this point we should have a private key that is usable for signing
    assertNotNull(
      SecurityUtils.getSenderPgpKeyDetails(
        getTargetContext(),
        addAccountToDatabaseRule.account,
        addAccountToDatabaseRule.account.email
      )
    )

    //SecurityUtils.getSenderPublicKeys should = 1
    assertEquals(
      1,
      SecurityUtils.getSenderPublicKeys(
        getTargetContext(),
        addAccountToDatabaseRule.account.email
      ).size
    )

    isDialogWithTextDisplayed(
      decorView,
      getQuantityString(R.plurals.please_provide_passphrase_for_following_keys, 1)
    )
  }

  companion object {
    @get:ClassRule
    @JvmStatic
    val mockWebServerRule = FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT,
      object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
          if (request.path?.startsWith("/attester/pub", ignoreCase = true) == true) {
            val lastSegment = request.requestUrl?.pathSegments?.lastOrNull()

            when {
              TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER.equals(
                lastSegment, true
              ) -> {
                return MockResponse()
                  .setResponseCode(HttpURLConnection.HTTP_OK)
                  .setBody(TestGeneralUtil.readResourceAsString("3.txt"))
              }
            }
          }

          return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
        }
      })
  }
}
