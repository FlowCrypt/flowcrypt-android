/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.retrofit.response.api.EkmPrivateKeysResponse
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.model.Key
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.extensions.org.pgpainless.util.asString
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.ui.base.BaseRefreshKeysFromEkmFlowTest
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.exception.ApiException
import com.google.gson.Gson
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
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
class RefreshKeysFromEkmFlowTest : BaseRefreshKeysFromEkmFlowTest() {
  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule(
    accountEntity = addAccountToDatabaseRule.account,
    keyPath = "pgp/expired@flowcrypt.test_prv_default.asc",
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
      "testUpdatePrvKeyFromEkmSuccessSilent" ->
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setBodyDelay(DELAY_FOR_EKM_REQUEST, TimeUnit.MILLISECONDS)
          .setBody(gson.toJson(EKM_RESPONSE_SUCCESS))

      "testUpdatePrvKeyFromEkmShowFixMissingPassphrase" ->
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setBody(gson.toJson(EKM_RESPONSE_SUCCESS))

      "testUpdatePrvKeyFromEkmShowApiError" ->
        MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
          .setBody(gson.toJson(EKM_API_ERROR))

      else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
    }
  }

  @Test
  @FlakyTest
  @Ignore("fix me")
  fun testUpdatePrvKeyFromEkmSuccessSilent() {
    val keysStorage = KeysStorageImpl.getInstance(getTargetContext())
    addPassphraseToRamCache(
      keysStorage = keysStorage,
      fingerprint = addPrivateKeyToDatabaseRule.pgpKeyRingDetails.fingerprint
    )

    //check existing key before updating
    val existingPgpKeyDetailsBeforeUpdating = checkExistingKeyBeforeUpdating(keysStorage)

    //we need to make a delay to wait while [KeysStorageImpl] will update internal data
    Thread.sleep(2000)

    //check existing key after updating
    val existingPgpKeyDetailsAfterUpdating = keysStorage.getPgpKeyDetailsList().first()
    assertEquals(1, keysStorage.getPgpKeyDetailsList().size)
    assertTrue(!existingPgpKeyDetailsAfterUpdating.isExpired)
    assertTrue(existingPgpKeyDetailsAfterUpdating.isNewerThan(existingPgpKeyDetailsBeforeUpdating))

    onView(withId(R.id.toolbar))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testUpdatePrvKeyFromEkmShowFixMissingPassphrase() {
    val keysStorage = KeysStorageImpl.getInstance(getTargetContext())

    //check existing key before updating
    val existingPgpKeyDetailsBeforeUpdating = keysStorage.getPgpKeyDetailsList().first()
    assertTrue(existingPgpKeyDetailsBeforeUpdating.isExpired)

    //check that we show dialog where a user provides a pass phrase
    isDialogWithTextDisplayed(
      decorView,
      getResString(R.string.please_provide_passphrase_for_following_keys_to_keep_keys_up_to_date)
    )

    onView(withId(R.id.eTKeyPassword))
      .inRoot(isDialog())
      .perform(typeText(TestConstants.DEFAULT_PASSWORD))

    onView(withId(R.id.btnUpdatePassphrase))
      .inRoot(isDialog())
      .perform(click())

    //we need to make a delay to wait while [KeysStorageImpl] will update internal data
    Thread.sleep(2000)

    //check existing key after updating
    val existingPgpKeyDetailsAfterUpdating = keysStorage.getPgpKeyDetailsList().first()
    assertTrue(!existingPgpKeyDetailsAfterUpdating.isExpired)
    assertTrue(existingPgpKeyDetailsAfterUpdating.isNewerThan(existingPgpKeyDetailsBeforeUpdating))

    onView(withId(R.id.toolbar))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testUpdatePrvKeyFromEkmShowApiError() {
    val keysStorage = KeysStorageImpl.getInstance(getTargetContext())
    addPassphraseToRamCache(
      keysStorage = keysStorage,
      fingerprint = addPrivateKeyToDatabaseRule.pgpKeyRingDetails.fingerprint
    )

    //check existing key before updating
    val existingPgpKeyDetailsBeforeUpdating = checkExistingKeyBeforeUpdating(keysStorage)

    //check error dialog content
    isDialogWithTextDisplayed(decorView, getResString(R.string.refreshing_keys_from_ekm_failed))

    val stringBuilder = StringBuilder()
    val exception = ApiException(EKM_API_ERROR)
    stringBuilder.append(exception.javaClass.simpleName)
    stringBuilder.append(":")
    stringBuilder.append(exception.message)

    isDialogWithTextDisplayed(decorView, stringBuilder.toString())

    //check that after fetching prv keys from EKM we have the same keys as before
    val existingPgpKeyDetailsAfterUpdating = keysStorage.getPgpKeyDetailsList().first()
    assertEquals(existingPgpKeyDetailsBeforeUpdating, existingPgpKeyDetailsAfterUpdating)
  }

  private fun checkExistingKeyBeforeUpdating(keysStorage: KeysStorageImpl): PgpKeyRingDetails {
    val existingPgpKeyDetailsBeforeUpdating = keysStorage.getPgpKeyDetailsList().first()
    assertTrue(existingPgpKeyDetailsBeforeUpdating.isExpired)
    assertEquals(
      addPrivateKeyToDatabaseRule.passphrase,
      keysStorage.getPassphraseByFingerprint(existingPgpKeyDetailsBeforeUpdating.fingerprint)?.asString
    )
    return existingPgpKeyDetailsBeforeUpdating
  }

  companion object {
    private val EKM_KEY_WITH_EXTENDED_EXPIRATION = PrivateKeysManager.getPgpKeyDetailsFromAssets(
      "pgp/expired@flowcrypt.test_prv_default_extended.asc"
    )
    private const val EKM_ERROR = "some error"
    private val EKM_API_ERROR =
      ApiError(code = HttpURLConnection.HTTP_BAD_REQUEST, message = EKM_ERROR)
    private val EKM_RESPONSE_SUCCESS = EkmPrivateKeysResponse(
      privateKeys = listOf(
        Key(
          PgpKey.decryptKey(
            requireNotNull(EKM_KEY_WITH_EXTENDED_EXPIRATION.privateKey),
            Passphrase.fromPassword(TestConstants.DEFAULT_PASSWORD)
          )
        )
      )
    )
  }
}
