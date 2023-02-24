/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.retrofit.response.api.EkmPrivateKeysResponse
import com.flowcrypt.email.api.retrofit.response.model.Key
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.ui.base.BaseRefreshKeysFromEkmFlowTest
import com.flowcrypt.email.util.PrivateKeysManager
import com.google.gson.Gson
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.pgpainless.util.Passphrase
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class RefreshRevokedKeysFromEkmDeleteNotMatchingFlowTest : BaseRefreshKeysFromEkmFlowTest() {
  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule(
    accountEntity = addAccountToDatabaseRule.account,
    keyPath = "pgp/default@flowcrypt.test_fisrtKey_prv_default_revoked.asc",
    passphrase = TestConstants.DEFAULT_PASSWORD,
    sourceType = KeyImportDetails.SourceType.EMAIL,
    passphraseType = KeyEntity.PassphraseType.RAM
  )
  private val addSecondPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule(
    accountEntity = addAccountToDatabaseRule.account,
    keyPath = TestConstants.DEFAULT_SECOND_KEY_PRV_STRONG,
    passphrase = TestConstants.DEFAULT_STRONG_PASSWORD,
    sourceType = KeyImportDetails.SourceType.EMAIL,
    passphraseType = KeyEntity.PassphraseType.RAM
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(mockWebServerRule)
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(addSecondPrivateKeyToDatabaseRule)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  override fun handleEkmAPI(gson: Gson): MockResponse {
    return when (testNameRule.methodName) {
      "testDeleteNotMatchingKeys" ->
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          //we need to add delay for this response to prevent showing 'need passphrase' dialog
          .setBodyDelay(DELAY_FOR_EKM_REQUEST, TimeUnit.MILLISECONDS)
          .setBody(gson.toJson(EKM_RESPONSE_SUCCESS_WITH_KEY_WHERE_MODIFICATION_DATE_AFTER_REVOKED))

      else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
    }
  }

  @Test
  fun testDeleteNotMatchingKeys() {
    val keysStorage = KeysStorageImpl.getInstance(getTargetContext())
    addPassphraseToRamCache(
      keysStorage = keysStorage,
      fingerprint = addPrivateKeyToDatabaseRule.pgpKeyDetails.fingerprint
    )

    //check existing keys before updating
    val firstPgpKeyDetailsBeforeUpdating = keysStorage.getPgpKeyDetailsList()
      .first { it.fingerprint == addPrivateKeyToDatabaseRule.pgpKeyDetails.fingerprint }
    assertTrue(firstPgpKeyDetailsBeforeUpdating.isRevoked)

    assertNotNull(keysStorage.getPgpKeyDetailsList()
      .firstOrNull { it.fingerprint == addSecondPrivateKeyToDatabaseRule.pgpKeyDetails.fingerprint })

    //we need to make a delay to wait while [KeysStorageImpl] will update internal data
    Thread.sleep(2000)

    //check existing keys after updating. We should have only the first key.
    assertEquals(keysStorage.getPgpKeyDetailsList().size, 1)
    assertNull(keysStorage.getPgpKeyDetailsList()
      .firstOrNull { it.fingerprint == addSecondPrivateKeyToDatabaseRule.pgpKeyDetails.fingerprint })

    val firstPgpKeyDetailsAfterUpdating = keysStorage.getPgpKeyDetailsList()
      .first { it.fingerprint == firstPgpKeyDetailsBeforeUpdating.fingerprint }
    assertTrue(firstPgpKeyDetailsAfterUpdating.isRevoked)
    assertEquals(
      firstPgpKeyDetailsBeforeUpdating.fingerprint,
      firstPgpKeyDetailsAfterUpdating.fingerprint
    )
    assertEquals(
      firstPgpKeyDetailsBeforeUpdating.lastModified,
      firstPgpKeyDetailsAfterUpdating.lastModified
    )

    onView(withId(R.id.toolbar))
      .check(matches(isDisplayed()))
  }

  companion object {
    private val EKM_KEY_WITH_MODIFICATION_DATE_AFTER_REVOKED =
      PrivateKeysManager.getPgpKeyDetailsFromAssets(
        "pgp/default@flowcrypt.test_fisrtKey_prv_default_mod_06_17_2022.asc"
      )
    private val EKM_RESPONSE_SUCCESS_WITH_KEY_WHERE_MODIFICATION_DATE_AFTER_REVOKED =
      EkmPrivateKeysResponse(
        privateKeys = listOf(
          Key(
            PgpKey.decryptKey(
              requireNotNull(EKM_KEY_WITH_MODIFICATION_DATE_AFTER_REVOKED.privateKey),
              Passphrase.fromPassword(TestConstants.DEFAULT_PASSWORD)
            )
          )
        )
      )
  }
}
