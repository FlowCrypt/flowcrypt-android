/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit

import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.api.oauth.OAuth2Helper
import com.flowcrypt.email.api.retrofit.request.model.InitialLegacySubmitModel
import com.flowcrypt.email.api.retrofit.request.model.LoginModel
import com.flowcrypt.email.api.retrofit.request.model.PostHelpFeedbackModel
import com.flowcrypt.email.api.retrofit.request.model.TestWelcomeModel
import com.flowcrypt.email.api.retrofit.response.api.ClientConfigurationResponse
import com.flowcrypt.email.api.retrofit.response.api.DomainOrgRulesResponse
import com.flowcrypt.email.api.retrofit.response.api.EkmPrivateKeysResponse
import com.flowcrypt.email.api.retrofit.response.api.FesServerResponse
import com.flowcrypt.email.api.retrofit.response.api.LoginResponse
import com.flowcrypt.email.api.retrofit.response.api.MessageReplyTokenResponse
import com.flowcrypt.email.api.retrofit.response.api.MessageUploadResponse
import com.flowcrypt.email.api.retrofit.response.api.PostHelpFeedbackResponse
import com.flowcrypt.email.api.retrofit.response.attester.InitialLegacySubmitResponse
import com.flowcrypt.email.api.retrofit.response.attester.TestWelcomeResponse
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
   * This method create a [Call] object for the API "https://flowcrypt.com/attester/initial/legacy_submit"
   *
   * @param body POJO model for requests
   * @return [<]
   */
  @POST("initial/legacy_submit")
  fun postInitialLegacySubmit(@Body body: InitialLegacySubmitModel): Call<InitialLegacySubmitResponse>

  /**
   * This method create a [Response] object for the API "https://flowcrypt.com/attester/initial/legacy_submit"
   *
   * @param body POJO model for requests
   * @return [<]
   */
  @POST("initial/legacy_submit")
  suspend fun postInitialLegacySubmitSuspend(@Body body: InitialLegacySubmitModel): Response<InitialLegacySubmitResponse>

  /**
   * This method create a [Call] object for the API "https://flowcrypt.com/attester/test/welcome"
   *
   * @param body POJO model for requests
   * @return [<]
   */
  @POST("test/welcome")
  fun postTestWelcome(@Body body: TestWelcomeModel): Call<TestWelcomeResponse>

  /**
   * This method create a [Response] object for the API "https://flowcrypt.com/attester/test/welcome"
   *
   * @param body POJO model for requests
   * @return [<]
   */
  @POST("test/welcome")
  suspend fun postTestWelcomeSuspend(@Body body: TestWelcomeModel): Response<TestWelcomeResponse>

  /**
   * This method create a [Call] object for the API "https://flowcrypt.com/api/help/feedback"
   *
   * @param body POJO model for requests
   * @return [<]
   */
  @POST(BuildConfig.API_URL + "help/feedback")
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
   * This method calls the API "https://flowcrypt.com/api/account/login"
   *
   * @param body POJO model for requests
   */
  @POST(BuildConfig.API_URL + "account/login")
  suspend fun postLogin(@Body body: LoginModel, @Header("Authorization") authorization: String):
      Response<LoginResponse>

  /**
   * This method calls the API "https://flowcrypt.com/api/account/get"
   *
   * @param body POJO model for requests
   */
  @POST(BuildConfig.API_URL + "account/get")
  suspend fun getOrgRulesFromFlowCryptComBackend(@Body body: LoginModel): Response<DomainOrgRulesResponse>

  /**
   * This method calls "https://fes.$domain/api/v1/client-configuration?domain=$domain"
   *
   * @param fesUrl URL of FES
   */
  @GET
  suspend fun getOrgRulesFromFes(@Url fesUrl: String): Response<ClientConfigurationResponse>

  /**
   * This method calls API "https://flowcrypt.com/attester/initial/legacy_submit" via coroutines
   *
   * @param body POJO model for requests
   * @return [<]
   */
  @POST("initial/legacy_submit")
  suspend fun submitPubKey(@Body body: InitialLegacySubmitModel): Response<InitialLegacySubmitResponse>

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
  @POST("https://fes.{domain}/api/v1/message/new-reply-token")
  suspend fun getReplyTokenForPasswordProtectedMsg(
    @Path("domain") domain: String,
    @Header("Authorization") authorization: String
  ): Response<MessageReplyTokenResponse>

  /**
   * This method uploads a password protected message to a web portal
   */
  @Multipart
  @POST("https://fes.{domain}/api/v1/message")
  suspend fun uploadPasswordProtectedMsgToWebPortal(
    @Path("domain") domain: String,
    @Header("Authorization") authorization: String,
    @Part("details") details: RequestBody,
    @Part content: MultipartBody.Part
  ): Response<MessageUploadResponse>
}
