/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit

import android.content.Context
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.request.model.InitialLegacySubmitModel
import com.flowcrypt.email.api.retrofit.request.model.LoginModel
import com.flowcrypt.email.api.retrofit.request.model.MessageUploadRequest
import com.flowcrypt.email.api.retrofit.request.model.TestWelcomeModel
import com.flowcrypt.email.api.retrofit.response.api.EkmPrivateKeysResponse
import com.flowcrypt.email.api.retrofit.response.api.FesServerResponse
import com.flowcrypt.email.api.retrofit.response.api.LoginResponse
import com.flowcrypt.email.api.retrofit.response.api.MessageReplyTokenResponse
import com.flowcrypt.email.api.retrofit.response.api.MessageUploadResponse
import com.flowcrypt.email.api.retrofit.response.attester.InitialLegacySubmitResponse
import com.flowcrypt.email.api.retrofit.response.attester.PubResponse
import com.flowcrypt.email.api.retrofit.response.attester.TestWelcomeResponse
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.OrgRules
import com.flowcrypt.email.api.retrofit.response.oauth2.MicrosoftOAuth2TokenResponse
import com.flowcrypt.email.api.wkd.WkdClient
import com.flowcrypt.email.extensions.kotlin.isValidEmail
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.armor
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.pgpainless.algorithm.EncryptionPurpose
import org.pgpainless.key.info.KeyRingInfo
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.HttpURLConnection
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
    idToken: String
  ): Result<LoginResponse> =
    withContext(Dispatchers.IO) {
      val apiService = ApiHelper.getInstance(context).retrofit.create(ApiService::class.java)
      getResult(
        context = context,
        expectedResultClass = LoginResponse::class.java
      ) { apiService.postLogin(loginModel, "Bearer $idToken") }
    }

  override suspend fun getDomainOrgRules(
    context: Context,
    loginModel: LoginModel,
    fesUrl: String?
  ): Result<ApiResponse> =
    withContext(Dispatchers.IO) {
      val apiService = ApiHelper.getInstance(context).retrofit.create(ApiService::class.java)
      getResult(context = context) {
        if (fesUrl != null) {
          apiService.getOrgRulesFromFes(fesUrl = fesUrl)
        } else {
          apiService.getOrgRulesFromFlowCryptComBackend(body = loginModel)
        }
      }
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

  override suspend fun pubLookup(
    requestCode: Long,
    context: Context,
    identData: String,
    orgRules: OrgRules?
  ): Result<PubResponse> =
    withContext(Dispatchers.IO) {
      val resultWrapperFun = fun(result: Result<String>): Result<PubResponse> {
        return when (result.status) {
          Result.Status.SUCCESS -> Result.success(
            requestCode = requestCode,
            data = PubResponse(null, result.data)
          )

          Result.Status.ERROR -> Result.error(
            requestCode = requestCode,
            data = PubResponse(null, null)
          )

          Result.Status.EXCEPTION -> Result.exception(
            requestCode = requestCode,
            throwable = result.exception ?: Exception(context.getString(R.string.unknown_error))
          )

          Result.Status.LOADING -> Result.loading(requestCode = requestCode)

          Result.Status.NONE -> Result.none()
        }
      }

      if (identData.isValidEmail()) {
        val wkdResult = getResult(requestCode = requestCode) {
          val pgpPublicKeyRingCollection = WkdClient.lookupEmail(context, identData)

          //For now, we just peak at the first matching key. It should be improved inthe future.
          // See more details here https://github.com/FlowCrypt/flowcrypt-android/issues/480
          val firstMatchingKey = pgpPublicKeyRingCollection?.firstOrNull {
            KeyRingInfo(it)
              .getEncryptionSubkeys(EncryptionPurpose.ANY)
              .isNotEmpty()
          }
          firstMatchingKey?.armor()?.let { armoredPubKey ->
            Response.success(armoredPubKey)
          } ?: Response.error(HttpURLConnection.HTTP_NOT_FOUND, "Not found".toResponseBody())
        }

        if (wkdResult.status == Result.Status.SUCCESS && wkdResult.data?.isNotEmpty() == true) {
          return@withContext resultWrapperFun(wkdResult)
        }

        if (orgRules?.canLookupThisRecipientOnAttester(identData) == false) {
          return@withContext Result.success(
            requestCode = requestCode,
            data = PubResponse(null, null)
          )
        }
      } else if (orgRules?.disallowLookupOnAttester() == true) {
        return@withContext Result.success(
          requestCode = requestCode,
          data = PubResponse(null, null)
        )
      }

      val apiService = ApiHelper.getInstance(context).retrofit.create(ApiService::class.java)
      val result = getResult(requestCode = requestCode) { apiService.getPubFromAttester(identData) }
      return@withContext resultWrapperFun(result)
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
    idToken: String
  ): Result<EkmPrivateKeysResponse> =
    withContext(Dispatchers.IO) {
      val apiService = ApiHelper.getInstance(context).retrofit.create(ApiService::class.java)
      val url = if (ekmUrl.endsWith("/")) ekmUrl else "$ekmUrl/"
      getResult(
        context = context,
        expectedResultClass = EkmPrivateKeysResponse::class.java
      ) { apiService.getPrivateKeysViaEkm("${url}v1/keys/private", "Bearer $idToken") }
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

  override suspend fun getReplyTokenForPasswordProtectedMsg(
    context: Context,
    domain: String,
    idToken: String
  ): Result<MessageReplyTokenResponse> =
    withContext(Dispatchers.IO) {
      val apiService = ApiHelper.getInstance(context).retrofit.create(ApiService::class.java)
      getResult(
        context = context,
        expectedResultClass = MessageReplyTokenResponse::class.java
      ) { apiService.getReplyTokenForPasswordProtectedMsg(domain, "Bearer $idToken") }
    }

  override suspend fun uploadPasswordProtectedMsgToWebPortal(
    context: Context,
    domain: String,
    idToken: String,
    messageUploadRequest: MessageUploadRequest,
    msg: String
  ): Result<MessageUploadResponse> =
    withContext(Dispatchers.IO) {
      val apiService = ApiHelper.getInstance(context).retrofit.create(ApiService::class.java)
      getResult(
        context = context,
        expectedResultClass = MessageUploadResponse::class.java
      ) {
        val details = GsonBuilder().create().toJson(messageUploadRequest)
          .toRequestBody(Constants.MIME_TYPE_JSON.toMediaTypeOrNull())

        val content = MultipartBody.Part.createFormData(
          "content",
          "content",
          msg.toByteArray().toRequestBody(Constants.MIME_TYPE_BINARY_DATA.toMediaTypeOrNull())
        )

        apiService.uploadPasswordProtectedMsgToWebPortal(
          domain = domain,
          authorization = "Bearer $idToken",
          details = details,
          content = content
        )
      }
    }
}
