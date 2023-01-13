/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit

import android.content.Context
import com.flowcrypt.email.api.retrofit.base.BaseApiRepository
import com.flowcrypt.email.api.retrofit.request.model.MessageUploadRequest
import com.flowcrypt.email.api.retrofit.request.model.PostHelpFeedbackModel
import com.flowcrypt.email.api.retrofit.request.model.WelcomeMessageModel
import com.flowcrypt.email.api.retrofit.response.api.ClientConfigurationResponse
import com.flowcrypt.email.api.retrofit.response.api.EkmPrivateKeysResponse
import com.flowcrypt.email.api.retrofit.response.api.FesServerResponse
import com.flowcrypt.email.api.retrofit.response.api.MessageReplyTokenResponse
import com.flowcrypt.email.api.retrofit.response.api.MessageUploadResponse
import com.flowcrypt.email.api.retrofit.response.api.PostHelpFeedbackResponse
import com.flowcrypt.email.api.retrofit.response.attester.PubResponse
import com.flowcrypt.email.api.retrofit.response.attester.SubmitPubKeyResponse
import com.flowcrypt.email.api.retrofit.response.attester.WelcomeMessageResponse
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration
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
   * @param fesUrl Url that will be used to fetch [ClientConfiguration].
   * @param idToken OIDC token.
   */
  suspend fun getClientConfiguration(
    context: Context,
    fesUrl: String? = null,
    idToken: String
  ): Result<ClientConfigurationResponse>

  /**
   * @param context Interface to global information about an application environment.
   * @param email For this email address will be applied changes.
   * @param pubKey A new public key.
   * @param idToken JSON Web Token signed by Google that can be used to identify a user to a backend.
   * @param clientConfiguration An instance of [ClientConfiguration]. We have to check if submitting pub keys is allowed.
   */
  suspend fun submitPrimaryEmailPubKey(
    context: Context,
    email: String,
    pubKey: String,
    idToken: String,
    clientConfiguration: ClientConfiguration? = null
  ): Result<SubmitPubKeyResponse>

  /**
   * @param context Interface to global information about an application environment.
   * @param email For this email address will be applied changes.
   * @param pubKey A new public key.
   * @param clientConfiguration An instance of [ClientConfiguration]. We have to check if submitting pub keys is allowed.
   */
  suspend fun submitPubKeyWithConditionalEmailVerification(
    context: Context,
    email: String,
    pubKey: String,
    clientConfiguration: ClientConfiguration? = null
  ): Result<SubmitPubKeyResponse>

  /**
   * @param context Interface to global information about an application environment.
   * @param model An instance of [WelcomeMessageModel].
   */
  suspend fun postWelcomeMessage(
    context: Context,
    model: WelcomeMessageModel,
    idToken: String
  ): Result<WelcomeMessageResponse>

  /**
   * @param requestCode A unique request code for this call
   * @param context     Interface to global information about an application environment.
   * @param email       A user email.
   * @param clientConfiguration    Contains client configurations.
   */
  suspend fun pubLookup(
    requestCode: Long = 0L,
    context: Context,
    email: String,
    clientConfiguration: ClientConfiguration? = null
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
   * @param ekmUrl key_manager_url from [ClientConfiguration].
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
