/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit

import android.content.Context
import com.flowcrypt.email.api.retrofit.base.BaseApiRepository
import com.flowcrypt.email.api.retrofit.request.model.InitialLegacySubmitModel
import com.flowcrypt.email.api.retrofit.request.model.LoginModel
import com.flowcrypt.email.api.retrofit.request.model.TestWelcomeModel
import com.flowcrypt.email.api.retrofit.response.api.DomainOrgRulesResponse
import com.flowcrypt.email.api.retrofit.response.api.EkmPrivateKeysResponse
import com.flowcrypt.email.api.retrofit.response.api.FesServerResponse
import com.flowcrypt.email.api.retrofit.response.api.LoginResponse
import com.flowcrypt.email.api.retrofit.response.attester.InitialLegacySubmitResponse
import com.flowcrypt.email.api.retrofit.response.attester.PubResponse
import com.flowcrypt.email.api.retrofit.response.attester.TestWelcomeResponse
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.OrgRules
import com.flowcrypt.email.api.retrofit.response.oauth2.MicrosoftOAuth2TokenResponse
import com.google.gson.JsonObject

/**
 * It's a repository interface for the whole API calls over the app
 *
 * @author Denis Bondarenko
 *         Date: 10/24/19
 *         Time: 6:08 PM
 *         E-mail: DenBond7@gmail.com
 */
interface ApiRepository : BaseApiRepository {
  /**
   * @param context Interface to global information about an application environment.
   * @param loginModel An instance of [LoginModel].
   * @param tokenId OIDC token.
   */
  suspend fun login(
    context: Context,
    loginModel: LoginModel,
    tokenId: String
  ): Result<LoginResponse>

  /**
   * @param context Interface to global information about an application environment.
   * @param fesUrl Url that will be used to fetch [OrgRules].
   * @param loginModel An instance of [LoginModel].
   */
  suspend fun getDomainOrgRules(
    context: Context,
    loginModel: LoginModel,
    fesUrl: String? = null
  ): Result<DomainOrgRulesResponse>

  /**
   * @param context Interface to global information about an application environment.
   * @param model An instance of [InitialLegacySubmitModel].
   */
  suspend fun submitPubKey(
    context: Context,
    model: InitialLegacySubmitModel
  ): Result<InitialLegacySubmitResponse>

  /**
   * @param context Interface to global information about an application environment.
   * @param model An instance of [InitialLegacySubmitModel].
   */
  suspend fun postInitialLegacySubmit(
    context: Context,
    model: InitialLegacySubmitModel
  ): Result<InitialLegacySubmitResponse>

  /**
   * @param context Interface to global information about an application environment.
   * @param model An instance of [TestWelcomeModel].
   */
  suspend fun postTestWelcome(
    context: Context,
    model: TestWelcomeModel
  ): Result<TestWelcomeResponse>

  /**
   * @param context Interface to global information about an application environment.
   * @param identData A key id or the user email or a fingerprint.
   */
  suspend fun getPub(
    requestCode: Long = 0L,
    context: Context,
    identData: String
  ): Result<PubResponse>

  /**
   * @param requestCode A unique request code for this call
   * @param context Interface to global information about an application environment.
   * @param authorizeCode A code which will be used to retrieve an access token.
   */
  suspend fun getMicrosoftOAuth2Token(
    requestCode: Long = 0L,
    context: Context,
    authorizeCode: String,
    scopes: String,
    codeVerifier: String
  ): Result<MicrosoftOAuth2TokenResponse>

  /**
   * @param context Interface to global information about an application environment.
   * @param url The configuration url.
   */
  suspend fun getOpenIdConfiguration(
    requestCode: Long = 0L,
    context: Context,
    url: String
  ): Result<JsonObject>

  /**
   * Get private keys via "<ekm>/v1/keys/private"
   *
   * @param context Interface to global information about an application environment.
   * @param ekmUrl key_manager_url from [OrgRules].
   * @param tokenId OIDC token.
   */
  suspend fun getPrivateKeysViaEkm(
    context: Context, ekmUrl: String, tokenId: String
  ): Result<EkmPrivateKeysResponse>

  /**
   * Check if "https://fes.$domain/api/" is available for interactions
   *
   * @param context Interface to global information about an application environment.
   * @param domain A company domain.
   */
  suspend fun checkFes(context: Context, domain: String): Result<FesServerResponse>
}
