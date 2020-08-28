/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit

import android.content.Context
import com.flowcrypt.email.api.retrofit.base.BaseApiRepository
import com.flowcrypt.email.api.retrofit.request.api.DomainRulesRequest
import com.flowcrypt.email.api.retrofit.request.api.LoginRequest
import com.flowcrypt.email.api.retrofit.request.model.InitialLegacySubmitModel
import com.flowcrypt.email.api.retrofit.request.model.PostLookUpEmailModel
import com.flowcrypt.email.api.retrofit.request.model.PostLookUpEmailsModel
import com.flowcrypt.email.api.retrofit.request.model.TestWelcomeModel
import com.flowcrypt.email.api.retrofit.response.api.DomainRulesResponse
import com.flowcrypt.email.api.retrofit.response.api.LoginResponse
import com.flowcrypt.email.api.retrofit.response.attester.InitialLegacySubmitResponse
import com.flowcrypt.email.api.retrofit.response.attester.LookUpEmailResponse
import com.flowcrypt.email.api.retrofit.response.attester.LookUpEmailsResponse
import com.flowcrypt.email.api.retrofit.response.attester.PubResponse
import com.flowcrypt.email.api.retrofit.response.attester.TestWelcomeResponse
import com.flowcrypt.email.api.retrofit.response.base.Result
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
   * @param request An instance of [LoginRequest].
   */
  suspend fun login(context: Context, request: LoginRequest): Result<LoginResponse>

  /**
   * @param context Interface to global information about an application environment.
   * @param request An instance of [DomainRulesRequest].
   */
  suspend fun getDomainRules(context: Context, request: DomainRulesRequest): Result<DomainRulesResponse>

  /**
   * @param context Interface to global information about an application environment.
   * @param model An instance of [InitialLegacySubmitModel].
   */
  suspend fun submitPubKey(context: Context, model: InitialLegacySubmitModel): Result<InitialLegacySubmitResponse>

  /**
   * @param context Interface to global information about an application environment.
   * @param model An instance of [PostLookUpEmailsModel].
   */
  suspend fun postLookUpEmails(context: Context, model: PostLookUpEmailsModel): Result<LookUpEmailsResponse>

  /**
   * @param context Interface to global information about an application environment.
   * @param model An instance of [PostLookUpEmailsModel].
   */
  suspend fun postLookUpEmail(context: Context, model: PostLookUpEmailModel): Result<LookUpEmailResponse>

  /**
   * @param context Interface to global information about an application environment.
   * @param model An instance of [PostLookUpEmailsModel].
   */
  suspend fun postInitialLegacySubmit(context: Context, model: InitialLegacySubmitModel): Result<InitialLegacySubmitResponse>

  /**
   * @param context Interface to global information about an application environment.
   * @param model An instance of [TestWelcomeModel].
   */
  suspend fun postTestWelcome(context: Context, model: TestWelcomeModel): Result<TestWelcomeResponse>

  /**
   * @param context Interface to global information about an application environment.
   * @param keyIdOrEmail A key id or the user email.
   */
  suspend fun getPub(requestCode: Long = 0L, context: Context, keyIdOrEmail: String): Result<PubResponse>

  /**
   * @param requestCode A unique request code for this call
   * @param context Interface to global information about an application environment.
   * @param authorizeCode A code which will be used to retrieve an access token.
   */
  suspend fun getMicrosoftOAuth2Token(requestCode: Long = 0L, context: Context, authorizeCode: String, scopes: String, codeVerifier: String): Result<MicrosoftOAuth2TokenResponse>

  /**
   * @param context Interface to global information about an application environment.
   * @param url The configuration url.
   */
  suspend fun getOpenIdConfiguration(requestCode: Long = 0L, context: Context, url: String): Result<JsonObject>
}