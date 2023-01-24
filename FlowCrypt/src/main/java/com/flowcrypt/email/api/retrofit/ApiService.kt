/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit

import com.flowcrypt.email.api.oauth.OAuth2Helper
import com.flowcrypt.email.api.retrofit.request.model.PostHelpFeedbackModel
import com.flowcrypt.email.api.retrofit.request.model.WelcomeMessageModel
import com.flowcrypt.email.api.retrofit.response.api.ClientConfigurationResponse
import com.flowcrypt.email.api.retrofit.response.api.EkmPrivateKeysResponse
import com.flowcrypt.email.api.retrofit.response.api.FesServerResponse
import com.flowcrypt.email.api.retrofit.response.api.MessageReplyTokenResponse
import com.flowcrypt.email.api.retrofit.response.api.MessageUploadResponse
import com.flowcrypt.email.api.retrofit.response.api.PostHelpFeedbackResponse
import com.flowcrypt.email.api.retrofit.response.attester.SubmitPubKeyResponse
import com.flowcrypt.email.api.retrofit.response.attester.WelcomeMessageResponse
import com.flowcrypt.email.api.retrofit.response.oauth2.MicrosoftOAuth2TokenResponse
import com.google.gson.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * A base API interface for RETROFIT.
 *
 * @author Denis Bondarenko
 * Date: 10.03.2015
 * Time: 13:39
 * E-mail: DenBond7@gmail.com
 */
interface ApiService {
  /**
   * This method create a [Response] object for the API "https://flowcrypt.com/attester/welcome-message"
   */
  @POST("welcome-message")
  suspend fun postWelcomeMessage(
    @Body body: WelcomeMessageModel,
    @Header("Authorization") authorization: String
  ): Response<WelcomeMessageResponse>

  /**
   * This method create a [Call] object for the API "https://flowcrypt.com/api/help/feedback"
   *
   * @param body POJO model for requests
   * @return [<]
   */
  //@POST(BuildConfig.API_URL + "help/feedback")
  //ref https://github.com/FlowCrypt/flowcrypt-android/pull/2171#discussion_r1084124018
  @POST("https://flowcrypt.com/api/help/feedback")
  suspend fun postHelpFeedback(@Body body: PostHelpFeedbackModel): Response<PostHelpFeedbackResponse>

  /**
   * This method create a [Call] object for the API "https://flowcrypt.com/attester/pub"
   *
   * @return [<]
   */
  @GET("pub/{keyIdOrEmailOrFingerprint}")
  suspend fun getPubFromAttester(@Path("keyIdOrEmailOrFingerprint") keyIdOrEmailOrFingerprint: String): Response<String>

  /**
   * Get RAW pub key(s) using an advanced WKD url
   */
  @Streaming
  @GET("https://{advancedHost}/.well-known/openpgpkey/{directDomain}/hu/{hu}")
  suspend fun getPubFromWkdAdvanced(
    @Path("advancedHost") advancedHost: String,
    @Path("directDomain") directDomain: String,
    @Path("hu") hu: String,
    @Query("l") user: String
  ): Response<ResponseBody>

  /**
   * Check that 'policy' file is available for the advanced method
   */
  @Streaming
  @GET("https://{advancedHost}/.well-known/openpgpkey/{directDomain}/policy")
  suspend fun checkPolicyForWkdAdvanced(
    @Path("advancedHost") advancedHost: String,
    @Path("directDomain") directDomain: String
  ): Response<ResponseBody>

  /**
   * Get RAW pub key(s) using a direct WKD url
   */
  @Streaming
  @GET("https://{directHost}/.well-known/openpgpkey/hu/{hu}")
  suspend fun getPubFromWkdDirect(
    @Path("directHost") directHost: String,
    @Path("hu") hu: String,
    @Query("l") user: String
  ): Response<ResponseBody>

  /**
   * Check that 'policy' file is available for the direct method
   */
  @Streaming
  @GET("https://{directHost}/.well-known/openpgpkey/policy")
  suspend fun checkPolicyForWkdDirect(@Path("directHost") directHost: String): Response<ResponseBody>

  /**
   * This method should call 'customFesUrl'. The last one should equal the following template
   * "https://$domain/api/v1/client-configuration?domain=$domain"
   *
   * @param customFesUrl A custom FES URL
   */
  @GET
  suspend fun getClientConfigurationFromFes(
    @Header("Authorization") authorization: String,
    @Url customFesUrl: String
  ): Response<ClientConfigurationResponse>

  /**
   * Set or replace public key with idToken as an auth mechanism
   * Used during setup
   * Can only be used for primary email because idToken does not contain info about aliases
   */
  @POST("pub/{email}")
  suspend fun submitPrimaryEmailPubKey(
    @Path("email") email: String,
    @Body pubKey: String,
    @Header("Authorization") authorization: String
  ): Response<SubmitPubKeyResponse>

  /**
   * Request to replace public key that will be verified by clicking email
   * Used when user manually chooses to replace key
   * Can also be used for aliases
   */
  @POST("pub/{email}")
  suspend fun submitPubKeyWithConditionalEmailVerification(
    @Path("email") email: String,
    @Body pubKey: String
  ): Response<SubmitPubKeyResponse>

  @FormUrlEncoded
  @POST(OAuth2Helper.MICROSOFT_OAUTH2_TOKEN_URL)
  suspend fun getMicrosoftOAuth2Token(
    @Field("code") code: String,
    @Field("scope") scope: String,
    @Field("code_verifier") codeVerifier: String,
    @Field("redirect_uri") redirect_uri: String,
    @Field("client_id") clientId: String = OAuth2Helper.MICROSOFT_AZURE_APP_ID,
    @Field("grant_type") grant_type: String = OAuth2Helper.OAUTH2_GRANT_TYPE
  ): Response<MicrosoftOAuth2TokenResponse>

  @FormUrlEncoded
  @POST(OAuth2Helper.MICROSOFT_OAUTH2_TOKEN_URL)
  fun refreshMicrosoftOAuth2Token(
    @Field("refresh_token") code: String,
    @Field("scope") scope: String = OAuth2Helper.SCOPE_MICROSOFT_OAUTH2_FOR_MAIL,
    @Field("client_id") clientId: String = OAuth2Helper.MICROSOFT_AZURE_APP_ID,
    @Field("grant_type") grant_type: String = OAuth2Helper.OAUTH2_GRANT_TYPE_REFRESH_TOKEN
  ): Call<MicrosoftOAuth2TokenResponse>

  @GET
  suspend fun getOpenIdConfiguration(@Url url: String): Response<JsonObject>

  /**
   * Get private keys via "<ekm>/v1/keys/private"
   */
  @GET
  suspend fun getPrivateKeysViaEkm(
    @Url ekmUrl: String,
    @Header("Authorization") authorization: String
  ): Response<EkmPrivateKeysResponse>

  /**
   * This method check if "https://fes.$domain/api/" is available for interactions
   */
  @GET("https://fes.{domain}/api/")
  suspend fun checkFes(@Path("domain") domain: String): Response<FesServerResponse>

  /**
   * This method check if "url" is available for interactions
   */
  @GET()
  suspend fun isAvailable(@Url url: String): Response<ResponseBody>

  /**
   * This method grabs a reply token before uploading a password protected message
   */
  @POST("https://{baseFesUrlPath}/api/v1/message/new-reply-token")
  suspend fun getReplyTokenForPasswordProtectedMsg(
    @Path("baseFesUrlPath") baseFesUrlPath: String,
    @Header("Authorization") authorization: String
  ): Response<MessageReplyTokenResponse>

  /**
   * This method uploads a password protected message to a web portal
   */
  @Multipart
  @POST("https://{baseFesUrlPath}/api/v1/message")
  suspend fun uploadPasswordProtectedMsgToWebPortal(
    @Path("baseFesUrlPath") baseFesUrlPath: String,
    @Header("Authorization") authorization: String,
    @Part("details") details: RequestBody,
    @Part content: MultipartBody.Part
  ): Response<MessageUploadResponse>
}
