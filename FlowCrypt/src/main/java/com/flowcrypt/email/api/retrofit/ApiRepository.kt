/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit

import android.content.Context
import com.flowcrypt.email.api.retrofit.base.BaseApiRepository
import com.flowcrypt.email.api.retrofit.request.model.InitialLegacySubmitModel
import com.flowcrypt.email.api.retrofit.request.model.LoginModel
import com.flowcrypt.email.api.retrofit.request.model.MessageUploadRequest
import com.flowcrypt.email.api.retrofit.request.model.PostHelpFeedbackModel
import com.flowcrypt.email.api.retrofit.request.model.TestWelcomeModel
import com.flowcrypt.email.api.retrofit.response.api.EkmPrivateKeysResponse
import com.flowcrypt.email.api.retrofit.response.api.FesServerResponse
import com.flowcrypt.email.api.retrofit.response.api.LoginResponse
import com.flowcrypt.email.api.retrofit.response.api.MessageReplyTokenResponse
import com.flowcrypt.email.api.retrofit.response.api.MessageUploadResponse
import com.flowcrypt.email.api.retrofit.response.api.PostHelpFeedbackResponse
import com.flowcrypt.email.api.retrofit.response.attester.InitialLegacySubmitResponse
import com.flowcrypt.email.api.retrofit.response.attester.PubResponse
import com.flowcrypt.email.api.retrofit.response.attester.TestWelcomeResponse
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
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
   * @param idToken OIDC token.
   */
  suspend fun login(
    context: Context,
    loginModel: LoginModel,
    idToken: String
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
  ): Result<ApiResponse>

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
   * @param requestCode A unique request code for this call
   * @param context     Interface to global information about an application environment.
   * @param email       A user email.
   * @param orgRules    Contains client configurations.
   */
  suspend fun pubLookup(
    requestCode: Long = 0L,
    context: Context,
    email: String,
    orgRules: OrgRules? = null
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
   * @param idToken OIDC token.
   */
  suspend fun getPrivateKeysViaEkm(
    context: Context, ekmUrl: String, idToken: String
  ): Result<EkmPrivateKeysResponse>

  /**
   * Check if "https://fes.$domain/api/" is available for interactions
   *
   * @param context Interface to global information about an application environment.
   * @param domain A company domain.
   */
  suspend fun checkFes(context: Context, domain: String): Result<FesServerResponse>

  /**
   * Grab a reply token before uploading a password protected message
   *
   * @param context Interface to global information about an application environment.
   * @param domain A company domain.
   * @param domain OIDC token.
   */
  suspend fun getReplyTokenForPasswordProtectedMsg(
    context: Context,
    domain: String,
    idToken: String
  ): Result<MessageReplyTokenResponse>

  /**
   * Upload a password protected message to a web portal
   *
   * @param context Interface to global information about an application environment.
   * @param domain A company domain.
   * @param idToken OIDC token.
   * @param messageUploadRequest an instance of [MessageUploadRequest]
   * @param msg an encrypted message that will be sent
   */
  suspend fun uploadPasswordProtectedMsgToWebPortal(
    context: Context,
    domain: String,
    idToken: String,
    messageUploadRequest: MessageUploadRequest,
    msg: String
  ): Result<MessageUploadResponse>

  /**
   * Post a user feedback to our server
   *
   * @param context Interface to global information about an application environment.
   * @param postHelpFeedbackModel an instance of [PostHelpFeedbackModel]
   */
  suspend fun postHelpFeedback(
    context: Context,
    postHelpFeedbackModel: PostHelpFeedbackModel
  ): Result<PostHelpFeedbackResponse>
}
