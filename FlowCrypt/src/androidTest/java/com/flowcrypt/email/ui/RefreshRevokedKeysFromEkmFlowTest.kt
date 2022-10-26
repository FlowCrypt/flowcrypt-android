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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.pgpainless.util.Passphrase
import java.net.HttpURLConnection

/**
 * @author Denis Bondarenko
 *         Date: 6/22/22
 *         Time: 5:45 PM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class RefreshRevokedKeysFromEkmFlowTest : BaseRefreshKeysFromEkmFlowTest() {
  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule(
    accountEntity = addAccountToDatabaseRule.account,
    keyPath = "pgp/default@flowcrypt.test_fisrtKey_prv_default_revoked.asc",
    passphrase = TestConstants.DEFAULT_PASSWORD,
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
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  override fun handleEkmAPI(gson: Gson): MockResponse {
    return when (testNameRule.methodName) {
      "testDisallowUpdateRevokedKeys" ->
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setBody(gson.toJson(EKM_RESPONSE_SUCCESS_WITH_KEY_WHERE_MODIFICATION_DATE_AFTER_REVOKED))

      "testDisallowDeleteRevokedKeys" ->
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setBody(gson.toJson(EKM_RESPONSE_SUCCESS_DIFFERENT_FINGERPRINT_KEY))

      else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
    }
  }

  @Test
  fun testDisallowUpdateRevokedKeys() {
    val keysStorage = KeysStorageImpl.getInstance(getTargetContext())
    addPassphraseToRamCache(
      keysStorage = keysStorage,
      fingerprint = addPrivateKeyToDatabaseRule.pgpKeyDetails.fingerprint
    )

    //check existing key before updating
    val existingPgpKeyDetailsBeforeUpdating = keysStorage.getPgpKeyDetailsList().first()
    assertTrue(existingPgpKeyDetailsBeforeUpdating.isRevoked)

    //we need to make a delay to wait while [KeysStorageImpl] will update internal data
    Thread.sleep(2000)

    //check existing key after updating. We should have the same key as before.
    val existingPgpKeyDetailsAfterUpdating = keysStorage.getPgpKeyDetailsList().first()
    assertTrue(existingPgpKeyDetailsAfterUpdating.isRevoked)
    assertEquals(
      existingPgpKeyDetailsBeforeUpdating.fingerprint,
      existingPgpKeyDetailsAfterUpdating.fingerprint
    )
    assertEquals(
      existingPgpKeyDetailsBeforeUpdating.lastModified,
      existingPgpKeyDetailsAfterUpdating.lastModified
    )

    onView(withId(R.id.toolbar))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testDisallowDeleteRevokedKeys() {
    val keysStorage = KeysStorageImpl.getInstance(getTargetContext())
    addPassphraseToRamCache(
      keysStorage = keysStorage,
      fingerprint = addPrivateKeyToDatabaseRule.pgpKeyDetails.fingerprint
    )

    //check existing key before updating
    val firstPgpKeyDetailsBeforeUpdating = keysStorage.getPgpKeyDetailsList().first()
    assertTrue(firstPgpKeyDetailsBeforeUpdating.isRevoked)

    //we need to make a delay to wait while [KeysStorageImpl] will update internal data
    Thread.sleep(2000)

    //check existing keys after updating. We should have the same key as before + one new.
    assertEquals(keysStorage.getPgpKeyDetailsList().size, 2)

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

    val secondPgpKeyDetailsAfterUpdating = keysStorage.getPgpKeyDetailsList()
      .first { it.fingerprint == EKM_KEY_DIFFERENT_FINGERPRINT.fingerprint }
    assertFalse(secondPgpKeyDetailsAfterUpdating.isRevoked)
    assertEquals(
      secondPgpKeyDetailsAfterUpdating.fingerprint,
      EKM_KEY_DIFFERENT_FINGERPRINT.fingerprint
    )

    onView(withId(R.id.toolbar))
      .check(matches(isDisplayed()))
  }

  companion object {
    private val EKM_KEY_WITH_MODIFICATION_DATE_AFTER_REVOKED =
      PrivateKeysManager.getPgpKeyDetailsFromAssets(
        "pgp/default@flowcrypt.test_fisrtKey_prv_default_mod_06_17_2022.asc"
      )
    private val EKM_KEY_DIFFERENT_FINGERPRINT =
      PrivateKeysManager.getPgpKeyDetailsFromAssets(
        "pgp/default@flowcrypt.test_secondKey_prv_default.asc"
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

    private val EKM_RESPONSE_SUCCESS_DIFFERENT_FINGERPRINT_KEY =
      EkmPrivateKeysResponse(
        privateKeys = listOf(
          Key(
            PgpKey.decryptKey(
              requireNotNull(EKM_KEY_DIFFERENT_FINGERPRINT.privateKey),
              Passphrase.fromPassword(TestConstants.DEFAULT_PASSWORD)
            )
          )
        )
      )
  }
}
