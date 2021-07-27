/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.enterprise

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.request.model.LoginModel
import com.flowcrypt.email.api.retrofit.response.api.DomainOrgRulesResponse
import com.flowcrypt.email.api.retrofit.response.api.EkmPrivateKeysResponse
import com.flowcrypt.email.api.retrofit.response.api.LoginResponse
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.model.Key
import com.flowcrypt.email.api.retrofit.response.model.OrgRules
import com.flowcrypt.email.junit.annotations.NotReadyForCI
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.CreateOrImportKeyActivity
import com.flowcrypt.email.ui.activity.SignInActivity
import com.flowcrypt.email.ui.activity.base.BaseSignActivityTest
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import com.google.gson.Gson
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.InputStreamReader

/**
 * @author Denis Bondarenko
 *         Date: 10/31/19
 *         Time: 3:08 PM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class SignInActivityEnterpriseTest : BaseSignActivityTest() {
  override val useIntents: Boolean = true
  override val activityScenarioRule = activityScenarioRule<SignInActivity>(
    TestGeneralUtil.genIntentForNavigationComponent(
      uri = "flowcrypt://email.flowcrypt.com/sign-in/gmail"
    )
  )

  @Before
  fun waitWhileToastWillBeDismissed() {
    Thread.sleep(1000)
  }

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(ClearAppSettingsRule())
    .around(RetryRule.DEFAULT)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testErrorLogin() {
    setupAndClickSignInButton(genMockGoogleSignInAccountJson(EMAIL_LOGIN_ERROR))
    isDialogWithTextDisplayed(decorView, LOGIN_API_ERROR_RESPONSE.apiError?.msg!!)
    onView(withText(R.string.retry))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testSuccessLoginNotVerified() {
    setupAndClickSignInButton(genMockGoogleSignInAccountJson(EMAIL_LOGIN_NOT_VERIFIED))
    isDialogWithTextDisplayed(decorView, getResString(R.string.user_not_verified))
  }

  @Test
  fun testErrorGetDomainRules() {
    setupAndClickSignInButton(genMockGoogleSignInAccountJson(EMAIL_DOMAIN_ORG_RULES_ERROR))
    isDialogWithTextDisplayed(decorView, DOMAIN_ORG_RULES_ERROR_RESPONSE.apiError?.msg!!)
    onView(withText(R.string.retry))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testOrgRulesCombinationNotSupportedForMustAutogenPassPhraseQuietlyExisted() {
    setupAndClickSignInButton(
      genMockGoogleSignInAccountJson(
        EMAIL_MUST_AUTOGEN_PASS_PHRASE_QUIETLY_EXISTED
      )
    )
    isDialogWithTextDisplayed(
      decorView = decorView,
      message = getResString(
        R.string.combination_of_org_rules_is_not_supported,
        OrgRules.DomainRule.PRV_AUTOIMPORT_OR_AUTOGEN.name +
            " + " + OrgRules.DomainRule.PASS_PHRASE_QUIET_AUTOGEN
      )
    )
  }

  @Test
  fun testOrgRulesCombinationNotSupportedForForbidStoringPassPhraseMissing() {
    setupAndClickSignInButton(
      genMockGoogleSignInAccountJson(
        EMAIL_FORBID_STORING_PASS_PHRASE_MISSING
      )
    )
    isDialogWithTextDisplayed(
      decorView = decorView,
      message = getResString(
        R.string.combination_of_org_rules_is_not_supported,
        OrgRules.DomainRule.PRV_AUTOIMPORT_OR_AUTOGEN.name + " + missing " +
            OrgRules.DomainRule.FORBID_STORING_PASS_PHRASE
      )
    )
  }

  @Test
  fun testOrgRulesCombinationNotSupportedForMustSubmitToAttesterExisted() {
    setupAndClickSignInButton(
      genMockGoogleSignInAccountJson(
        EMAIL_MUST_SUBMIT_TO_ATTESTER_EXISTED
      )
    )
    isDialogWithTextDisplayed(
      decorView = decorView,
      message = getResString(
        R.string.combination_of_org_rules_is_not_supported,
        OrgRules.DomainRule.PRV_AUTOIMPORT_OR_AUTOGEN.name +
            " + " + OrgRules.DomainRule.ENFORCE_ATTESTER_SUBMIT
      )
    )
  }

  @Test
  fun testOrgRulesCombinationNotSupportedForForbidCreatingPrivateKeyMissing() {
    setupAndClickSignInButton(
      genMockGoogleSignInAccountJson(
        EMAIL_FORBID_CREATING_PRIVATE_KEY_MISSING
      )
    )
    isDialogWithTextDisplayed(
      decorView = decorView,
      message = getResString(
        R.string.combination_of_org_rules_is_not_supported,
        OrgRules.DomainRule.PRV_AUTOIMPORT_OR_AUTOGEN.name + " + missing "
            + OrgRules.DomainRule.NO_PRV_CREATE
      )
    )
  }

  @Test
  fun testErrorGetPrvKeysViaEkm() {
    setupAndClickSignInButton(genMockGoogleSignInAccountJson(EMAIL_GET_KEYS_VIA_EKM_ERROR))
    isDialogWithTextDisplayed(decorView, EKM_ERROR)
    onView(withText(R.string.retry))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testErrorGetPrvKeysViaEkmEmptyList() {
    setupAndClickSignInButton(genMockGoogleSignInAccountJson(EMAIL_GET_KEYS_VIA_EKM_EMPTY_LIST))
    isDialogWithTextDisplayed(decorView, getResString(R.string.no_prv_keys_ask_admin))
    onView(withText(R.string.retry))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testErrorGetPrvKeysViaEkmNotFullyDecryptedKey() {
    setupAndClickSignInButton(
      genMockGoogleSignInAccountJson(
        EMAIL_GET_KEYS_VIA_EKM_NOT_FULLY_DECRYPTED
      )
    )
    val pgpKeyDetails = PrivateKeysManager.getPgpKeyDetailsFromAssets(
      "pgp/keys/user_with_not_fully_decrypted_prv_key@flowcrypt.test_prv_default.asc"
    )
    isDialogWithTextDisplayed(
      decorView, getResString(
        R.string.found_not_fully_decrypted_key_ask_admin,
        pgpKeyDetails.fingerprint
      )
    )
    onView(withText(R.string.retry))
      .check(matches(isDisplayed()))
  }

  @Test
  @NotReadyForCI
  fun testNoPrvCreateRule() {
    setupAndClickSignInButton(genMockGoogleSignInAccountJson(EMAIL_WITH_NO_PRV_CREATE_RULE))
    intended(hasComponent(CreateOrImportKeyActivity::class.java.name))

    onView(withId(R.id.buttonCreateNewKey))
      .check(matches(not(isDisplayed())))
  }

  @Test
  fun testFesAvailabilityRequestTimeOut() {
    try {
      changeConnectionState(false)
      setupAndClickSignInButton(genMockGoogleSignInAccountJson(EMAIL_FES_REQUEST_TIME_OUT))
      isDialogWithTextDisplayed(
        decorView = decorView,
        message = getResString(R.string.no_connection_or_server_is_not_reachable)
      )
    } finally {
      changeConnectionState(true)
    }
  }

  companion object {
    private const val EMAIL_EKM_URL_SUCCESS = "https://localhost:1212/ekm/"
    private const val EMAIL_EKM_URL_SUCCESS_EMPTY_LIST = "https://localhost:1212/ekm/empty/"
    private const val EMAIL_EKM_URL_SUCCESS_NOT_FULLY_DECRYPTED_KEY =
      "https://localhost:1212/ekm/not_fully_decrypted_key/"
    private const val EMAIL_EKM_URL_ERROR = "https://localhost:1212/ekm/error/"
    private const val EMAIL_WITH_NO_PRV_CREATE_RULE = "no_prv_create@flowcrypt.test"
    private const val EMAIL_LOGIN_ERROR = "login_error@flowcrypt.test"
    private const val EMAIL_LOGIN_NOT_VERIFIED = "login_not_verified@flowcrypt.test"
    private const val EMAIL_DOMAIN_ORG_RULES_ERROR = "domain_org_rules_error@flowcrypt.test"
    private const val EMAIL_MUST_AUTOGEN_PASS_PHRASE_QUIETLY_EXISTED =
      "must_autogen_pass_phrase_quietly_existed@flowcrypt.test"
    private const val EMAIL_FORBID_STORING_PASS_PHRASE_MISSING =
      "forbid_storing_pass_phrase_missing@flowcrypt.test"
    private const val EMAIL_MUST_SUBMIT_TO_ATTESTER_EXISTED =
      "must_submit_to_attester_existed@flowcrypt.test"
    private const val EMAIL_FORBID_CREATING_PRIVATE_KEY_MISSING =
      "forbid_creating_private_key_missing@flowcrypt.test"
    private const val EMAIL_GET_KEYS_VIA_EKM_ERROR = "keys_via_ekm_error@flowcrypt.test"
    private const val EMAIL_GET_KEYS_VIA_EKM_EMPTY_LIST = "keys_via_ekm_empty_list@flowcrypt.test"
    private const val EMAIL_GET_KEYS_VIA_EKM_NOT_FULLY_DECRYPTED =
      "user_with_not_fully_decrypted_prv_key@flowcrypt.test"
    private const val EMAIL_FES_REQUEST_TIME_OUT = "fes_request_timeout@flowcrypt.test"

    private val ACCEPTED_ORG_RULES = listOf(
      OrgRules.DomainRule.PRV_AUTOIMPORT_OR_AUTOGEN,
      OrgRules.DomainRule.FORBID_STORING_PASS_PHRASE,
      OrgRules.DomainRule.NO_PRV_CREATE
    )

    private val LOGIN_API_ERROR_RESPONSE = LoginResponse(
      ApiError(
        400, "Something wrong happened.",
        "api input: missing key: token"
      ), null
    )

    private val DOMAIN_ORG_RULES_ERROR_RESPONSE = DomainOrgRulesResponse(
      ApiError(
        401,
        "Not logged in or unknown account", "auth"
      ), null
    )

    private const val EKM_ERROR = "some error"
    private val EKM_ERROR_RESPONSE = EkmPrivateKeysResponse(
      code = 400,
      message = EKM_ERROR
    )

    @get:ClassRule
    @JvmStatic
    val mockWebServerRule =
      FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
          val gson =
            ApiHelper.getInstance(InstrumentationRegistry.getInstrumentation().targetContext).gson

          if (request.path.equals("/ekm/error/v1/keys/private")) {
            return MockResponse().setResponseCode(200)
              .setBody(gson.toJson(EKM_ERROR_RESPONSE))
          }

          if (request.path.equals("/ekm/empty/v1/keys/private")) {
            return MockResponse().setResponseCode(200)
              .setBody(
                gson.toJson(
                  EkmPrivateKeysResponse(
                    privateKeys = emptyList()
                  )
                )
              )
          }

          if (request.path.equals("/ekm/not_fully_decrypted_key/v1/keys/private")) {
            return MockResponse().setResponseCode(200)
              .setBody(
                gson.toJson(
                  EkmPrivateKeysResponse(
                    privateKeys = listOf(
                      Key(
                        TestGeneralUtil.readFileFromAssetsAsString(
                          "pgp/keys/user_with_not_fully_decrypted_prv_key@flowcrypt.test_prv_default.asc"
                        )
                      )
                    )
                  )
                )
              )
          }

          val model =
            gson.fromJson(InputStreamReader(request.body.inputStream()), LoginModel::class.java)

          if (request.path.equals("/account/login")) {
            return handleLoginAPI(model, gson)
          }

          if (request.path.equals("/account/get")) {
            return handleGetDomainRulesAPI(model, gson)
          }

          return MockResponse().setResponseCode(404)
        }
      })

    private fun handleGetDomainRulesAPI(model: LoginModel, gson: Gson): MockResponse {
      when (model.account) {
        EMAIL_DOMAIN_ORG_RULES_ERROR -> return MockResponse().setResponseCode(200)
          .setBody(gson.toJson(DOMAIN_ORG_RULES_ERROR_RESPONSE))

        EMAIL_WITH_NO_PRV_CREATE_RULE -> return successMockResponseForOrgRules(
          gson = gson,
          orgRules = OrgRules(
            flags = listOf(
              OrgRules.DomainRule.NO_PRV_CREATE,
              OrgRules.DomainRule.NO_PRV_BACKUP
            )
          )
        )

        EMAIL_MUST_AUTOGEN_PASS_PHRASE_QUIETLY_EXISTED
        -> return successMockResponseForOrgRules(
          gson = gson,
          orgRules = OrgRules(
            flags = listOf(
              OrgRules.DomainRule.PRV_AUTOIMPORT_OR_AUTOGEN,
              OrgRules.DomainRule.PASS_PHRASE_QUIET_AUTOGEN
            ),
            keyManagerUrl = EMAIL_EKM_URL_SUCCESS,
          )
        )

        EMAIL_FORBID_STORING_PASS_PHRASE_MISSING -> return successMockResponseForOrgRules(
          gson = gson,
          orgRules = OrgRules(
            flags = listOf(
              OrgRules.DomainRule.PRV_AUTOIMPORT_OR_AUTOGEN
            ),
            keyManagerUrl = EMAIL_EKM_URL_SUCCESS,
          )
        )

        EMAIL_MUST_SUBMIT_TO_ATTESTER_EXISTED -> return successMockResponseForOrgRules(
          gson = gson,
          orgRules = OrgRules(
            flags = listOf(
              OrgRules.DomainRule.PRV_AUTOIMPORT_OR_AUTOGEN,
              OrgRules.DomainRule.FORBID_STORING_PASS_PHRASE,
              OrgRules.DomainRule.ENFORCE_ATTESTER_SUBMIT
            ),
            keyManagerUrl = EMAIL_EKM_URL_SUCCESS,
          )
        )

        EMAIL_FORBID_CREATING_PRIVATE_KEY_MISSING -> return successMockResponseForOrgRules(
          gson = gson,
          orgRules = OrgRules(
            flags = listOf(
              OrgRules.DomainRule.PRV_AUTOIMPORT_OR_AUTOGEN,
              OrgRules.DomainRule.FORBID_STORING_PASS_PHRASE
            ),
            keyManagerUrl = EMAIL_EKM_URL_SUCCESS,
          )
        )

        EMAIL_GET_KEYS_VIA_EKM_ERROR -> return successMockResponseForOrgRules(
          gson = gson,
          orgRules = OrgRules(
            flags = ACCEPTED_ORG_RULES,
            keyManagerUrl = EMAIL_EKM_URL_ERROR,
          )
        )

        EMAIL_GET_KEYS_VIA_EKM_EMPTY_LIST -> return successMockResponseForOrgRules(
          gson = gson,
          orgRules = OrgRules(
            flags = ACCEPTED_ORG_RULES,
            keyManagerUrl = EMAIL_EKM_URL_SUCCESS_EMPTY_LIST,
          )
        )

        EMAIL_GET_KEYS_VIA_EKM_NOT_FULLY_DECRYPTED -> return successMockResponseForOrgRules(
          gson = gson,
          orgRules = OrgRules(
            flags = ACCEPTED_ORG_RULES,
            keyManagerUrl = EMAIL_EKM_URL_SUCCESS_NOT_FULLY_DECRYPTED_KEY,
          )
        )

        else -> return MockResponse().setResponseCode(404)
      }
    }

    private fun handleLoginAPI(model: LoginModel, gson: Gson): MockResponse {
      when (model.account) {
        EMAIL_LOGIN_ERROR -> return MockResponse().setResponseCode(200)
          .setBody(gson.toJson(LOGIN_API_ERROR_RESPONSE))

        EMAIL_LOGIN_NOT_VERIFIED -> return MockResponse().setResponseCode(200)
          .setBody(gson.toJson(LoginResponse(null, isVerified = false)))

        EMAIL_DOMAIN_ORG_RULES_ERROR -> return MockResponse().setResponseCode(200)
          .setBody(gson.toJson(LoginResponse(null, isVerified = true)))

        EMAIL_WITH_NO_PRV_CREATE_RULE -> return MockResponse().setResponseCode(200)
          .setBody(gson.toJson(LoginResponse(null, isVerified = true)))

        else -> return MockResponse().setResponseCode(200)
          .setBody(gson.toJson(LoginResponse(null, isVerified = true)))
      }
    }

    private fun successMockResponseForOrgRules(gson: Gson, orgRules: OrgRules) =
      MockResponse().setResponseCode(200)
        .setBody(
          gson.toJson(
            DomainOrgRulesResponse(
              orgRules = orgRules
            )
          )
        )
  }
}
