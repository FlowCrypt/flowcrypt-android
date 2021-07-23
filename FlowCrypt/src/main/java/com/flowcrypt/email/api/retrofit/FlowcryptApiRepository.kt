/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit

import android.content.Context
import com.flowcrypt.email.R
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
import com.flowcrypt.email.api.retrofit.response.oauth2.MicrosoftOAuth2TokenResponse
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Implementation of Flowcrypt API
 *
 * @author Denis Bondarenko
 *         Date: 10/24/19
 *         Time: 6:10 PM
 *         E-mail: DenBond7@gmail.com
 */
class FlowcryptApiRepository : ApiRepository {
  override suspend fun login(
    context: Context,
    loginModel: LoginModel,
    tokenId: String
  ): Result<LoginResponse> =
    withContext(Dispatchers.IO) {
      val apiService = ApiHelper.getInstance(context).retrofit.create(ApiService::class.java)
      getResult(
        context = context,
        expectedResultClass = LoginResponse::class.java
      ) { apiService.postLogin(loginModel, "Bearer $tokenId") }
    }

  override suspend fun getDomainOrgRules(
    context: Context,
    fesUrl: String,
    loginModel: LoginModel
  ): Result<DomainOrgRulesResponse> =
    withContext(Dispatchers.IO) {
      val apiService = ApiHelper.getInstance(context).retrofit.create(ApiService::class.java)
      getResult(
        context = context,
        expectedResultClass = DomainOrgRulesResponse::class.java
      ) { apiService.getDomainOrgRules(fesUrl = fesUrl, body = loginModel) }
    }

  override suspend fun submitPubKey(
    context: Context,
    model: InitialLegacySubmitModel
  ): Result<InitialLegacySubmitResponse> =
    withContext(Dispatchers.IO) {
      val apiService = ApiHelper.getInstance(context).retrofit.create(ApiService::class.java)
      getResult { apiService.submitPubKey(model) }
    }

  override suspend fun postInitialLegacySubmit(
    context: Context,
    model: InitialLegacySubmitModel
  ): Result<InitialLegacySubmitResponse> =
    withContext(Dispatchers.IO) {
      val apiService = ApiHelper.getInstance(context).retrofit.create(ApiService::class.java)
      getResult { apiService.postInitialLegacySubmitSuspend(model) }
    }

  override suspend fun postTestWelcome(
    context: Context,
    model: TestWelcomeModel
  ): Result<TestWelcomeResponse> =
    withContext(Dispatchers.IO) {
      val apiService = ApiHelper.getInstance(context).retrofit.create(ApiService::class.java)
      getResult { apiService.postTestWelcomeSuspend(model) }
    }

  //todo-denbond7 need to ask Tom to improve https://flowcrypt.com/attester/pub to use the common
  // API  response ([ApiResponse])
  override suspend fun getPub(
    requestCode: Long,
    context: Context,
    identData: String
  ): Result<PubResponse> =
    withContext(Dispatchers.IO) {
      val apiService = ApiHelper.getInstance(context).retrofit.create(ApiService::class.java)
      val result = getResult(requestCode = requestCode) { apiService.getPub(identData) }
      when (result.status) {
        Result.Status.SUCCESS -> Result.success(
          requestCode = requestCode,
          data = PubResponse(null, result.data)
        )

        Result.Status.ERROR -> Result.error(
          requestCode = requestCode,
          data = PubResponse(null, null)
        )

        Result.Status.EXCEPTION -> Result.exception(
          requestCode = requestCode, throwable = result.exception
            ?: Exception()
        )

        Result.Status.LOADING -> Result.loading(requestCode = requestCode)

        Result.Status.NONE -> Result.none()
      }
    }

  override suspend fun getMicrosoftOAuth2Token(
    requestCode: Long, context: Context,
    authorizeCode: String, scopes: String, codeVerifier: String
  ):
      Result<MicrosoftOAuth2TokenResponse> =
    withContext(Dispatchers.IO) {
      val apiService = ApiHelper.getInstance(context).retrofit.create(ApiService::class.java)
      getResult(
        context = context,
        expectedResultClass = MicrosoftOAuth2TokenResponse::class.java
      ) {
        apiService.getMicrosoftOAuth2Token(
          code = authorizeCode,
          scope = scopes,
          codeVerifier = codeVerifier,
          redirect_uri = context.getString(R.string.microsoft_redirect_uri)
        )
      }
    }

  override suspend fun getOpenIdConfiguration(
    requestCode: Long,
    context: Context,
    url: String
  ): Result<JsonObject> =
    withContext(Dispatchers.IO) {
      val apiService = ApiHelper.getInstance(context).retrofit.create(ApiService::class.java)
      getResult {
        apiService.getOpenIdConfiguration(url)
      }
    }

  override suspend fun getPrivateKeysViaEkm(
    context: Context,
    ekmUrl: String,
    tokenId: String
  ): Result<EkmPrivateKeysResponse> =
    withContext(Dispatchers.IO) {
      val apiService = ApiHelper.getInstance(context).retrofit.create(ApiService::class.java)
      val url = if (ekmUrl.endsWith("/")) ekmUrl else "$ekmUrl/"
      getResult(
        context = context,
        expectedResultClass = EkmPrivateKeysResponse::class.java
      ) { apiService.getPrivateKeysViaEkm("${url}v1/keys/private", "Bearer $tokenId") }
    }

  override suspend fun checkFes(context: Context, domain: String): Result<FesServerResponse> =
    withContext(Dispatchers.IO) {
      val connectionTimeoutInSeconds = 3L
      val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(connectionTimeoutInSeconds, TimeUnit.SECONDS)
        .writeTimeout(connectionTimeoutInSeconds, TimeUnit.SECONDS)
        .readTimeout(connectionTimeoutInSeconds, TimeUnit.SECONDS)
        .apply {
          ApiHelper.configureOkHttpClientForDebuggingIfAllowed(context, this)
        }.build()

      val retrofit = Retrofit.Builder()
        .baseUrl("https://fes.$domain/api/")
        .addConverterFactory(GsonConverterFactory.create(ApiHelper.getInstance(context).gson))
        .client(okHttpClient)
        .build()
      val apiService = retrofit.create(ApiService::class.java)
      return@withContext getResult(
        context = context,
        expectedResultClass = FesServerResponse::class.java
      ) { apiService.checkFes(domain) }
    }
}
