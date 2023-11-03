/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui

import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.response.api.ClientConfigurationResponse
import com.flowcrypt.email.api.retrofit.response.api.FesServerResponse
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.toPgpKeyRingDetails
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.ui.base.BaseSignTest
import com.flowcrypt.email.util.TestGeneralUtil
import com.flowcrypt.email.util.exception.ApiException
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.model.ListMessagesResponse
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.pgpainless.PGPainless
import org.pgpainless.key.util.UserId
import java.io.File
import java.net.HttpURLConnection

/**
 * https://github.com/FlowCrypt/flowcrypt-android/issues/2014
 */
@FlowCryptTestSettings(useIntents = true)
@MediumTest
@RunWith(AndroidJUnit4::class)
class SubmitPublicKeyToAttesterForImportedKeyDuringSetupFlowTest : BaseSignTest() {
  override val activityScenarioRule = activityScenarioRule<MainActivity>(
    TestGeneralUtil.genIntentForNavigationComponent(
      destinationId = R.id.mainSignInFragment
    )
  )

  private lateinit var fileWithPrivateKey: File
  private lateinit var privateKey: String

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Before
  fun prepareResources() {
    val generatedKey = PGPainless.generateKeyRing().simpleEcKeyRing(
      UserId.nameAndEmail(USER_ENFORCE_ATTESTER_SUBMIT, USER_ENFORCE_ATTESTER_SUBMIT),
      TestConstants.DEFAULT_STRONG_PASSWORD
    ).toPgpKeyRingDetails()

    privateKey = requireNotNull(generatedKey.privateKey)
    fileWithPrivateKey = TestGeneralUtil.createFileWithTextContent(
      fileName = USER_ENFORCE_ATTESTER_SUBMIT + "_prv.asc",
      fileText = privateKey
    )
  }

  @Test
  fun testEnforceAttesterSubmitRuleExistForImportedKeysDuringSetupClipboard() {
    setupAndClickSignInButton(
      genMockGoogleSignInAccountJson(
        USER_ENFORCE_ATTESTER_SUBMIT
      )
    )

    addTextToClipboard("private key", privateKey)
    doNavigationAndCheckPrivateKey(R.id.buttonLoadFromClipboard)
    checkAttesterErrorIsDisplayed()
  }

  @Test
  fun testEnforceAttesterSubmitRuleExistForImportedKeysDuringSetupFromFile() {
    setupAndClickSignInButton(
      genMockGoogleSignInAccountJson(
        USER_ENFORCE_ATTESTER_SUBMIT
      )
    )

    val resultData = TestGeneralUtil.genIntentWithPersistedReadPermissionForFile(fileWithPrivateKey)
    intendingActivityResultContractsGetContent(resultData = resultData)

    doNavigationAndCheckPrivateKey(R.id.buttonLoadFromFile)
    checkAttesterErrorIsDisplayed()
  }

  private fun doNavigationAndCheckPrivateKey(buttonId: Int) {
    onView(withId(R.id.buttonImportMyKey))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withId(buttonId))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withId(R.id.editTextKeyPassword))
      .perform(
        scrollTo(),
        typeText(TestConstants.DEFAULT_STRONG_PASSWORD),
        closeSoftKeyboard()
      )
    onView(withId(R.id.buttonPositiveAction))
      .perform(scrollTo(), click())
  }

  private fun checkAttesterErrorIsDisplayed() {
    isDialogWithTextDisplayed(
      decorView = decorView,
      message = requireNotNull(ApiException(ApiError(code = 404, message = "")).message)
    )
  }

  companion object {
    private const val USER_ENFORCE_ATTESTER_SUBMIT =
      "user_enforce_attester_submit@flowcrypt.test"

    private val FES_SUCCESS_RESPONSE = FesServerResponse(
      vendor = "FlowCrypt",
      service = "enterprise-server",
      orgId = "localhost",
      version = "2022",
      endUserApiVersion = "v1",
      adminApiVersion = "v1"
    )

    private val CLIENT_CONFIGURATION_RESPONSE = ClientConfigurationResponse(
      clientConfiguration = ClientConfiguration(
        flags = listOf(
          ClientConfiguration.ConfigurationProperty.ENFORCE_ATTESTER_SUBMIT
        ),
      )
    )

    @get:ClassRule
    @JvmStatic
    val mockWebServerRule =
      FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
          val gson = ApiHelper.getInstance(ApplicationProvider.getApplicationContext()).gson

          return when {
            request.path == "api" -> MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
              .setBody(gson.toJson(FES_SUCCESS_RESPONSE))

            request.path.equals("/shared-tenant-fes/api/v1/client-configuration?domain=flowcrypt.test") -> {
              MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(gson.toJson(CLIENT_CONFIGURATION_RESPONSE))
            }

            request.requestUrl?.encodedPath == "/gmail/v1/users/me/messages"
                && request.requestUrl?.queryParameter("q")
              ?.startsWith("from:$USER_ENFORCE_ATTESTER_SUBMIT") == true -> MockResponse()
              .setResponseCode(HttpURLConnection.HTTP_OK)
              .setBody(
                ListMessagesResponse().apply {
                  factory = GsonFactory.getDefaultInstance()
                  messages = emptyList()
                  resultSizeEstimate = 0
                }.toString()
              )

            else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
          }
        }
      })
  }
}
