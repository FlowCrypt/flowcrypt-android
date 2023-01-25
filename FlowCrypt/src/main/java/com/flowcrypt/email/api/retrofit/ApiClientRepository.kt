/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit

import android.content.Context
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
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
class ApiClientRepository : BaseApiRepository {
  /**
   * @param context Interface to global information about an application environment.
   * @param baseFesUrlPath A base FES URL path.
   * @param domain A company domain.
   * @param idToken OIDC token.
   */
  suspend fun getClientConfigurationFromFes(
    context: Context,
    idToken: String,
    baseFesUrlPath: String,
    domain: String,
  ): Result<ClientConfigurationResponse> =
    withContext(Dispatchers.IO) {
      val retrofitApiService =
        ApiHelper.getInstance(context).retrofit.create(RetrofitApiServiceInterface::class.java)
      getResult(context = context) {
        retrofitApiService.getClientConfigurationFromFes(
          authorization = "Bearer $idToken",
          baseFesUrlPath = baseFesUrlPath,
          domain = domain
        )
      }
    }

  /**
   * @param context Interface to global information about an application environment.
   * @param idToken JSON Web Token signed by Google that can be used to identify a user to a backend.
   * @param email For this email address will be applied changes.
   * @param pubKey A new public key.
   * @param clientConfiguration An instance of [ClientConfiguration]. We have to check if submitting pub keys is allowed.
   */
  suspend fun submitPrimaryEmailPubKey(
    context: Context,
    idToken: String,
    email: String,
    pubKey: String,
    clientConfiguration: ClientConfiguration? = null
  ): Result<SubmitPubKeyResponse> =
    withContext(Dispatchers.IO) {
      if (clientConfiguration?.canSubmitPubToAttester() == false) {
        return@withContext Result.exception(
          IllegalStateException(context.getString(R.string.can_not_replace_public_key_at_attester))
        )
      }
      val retrofitApiService =
        ApiHelper.getInstance(context).retrofit.create(RetrofitApiServiceInterface::class.java)
      getResult {
        retrofitApiService.submitPrimaryEmailPubKey(
          authorization = "Bearer $idToken",
          email = email,
          pubKey = pubKey
        )
      }
    }

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
  ): Result<SubmitPubKeyResponse> =
    withContext(Dispatchers.IO) {
      if (clientConfiguration?.canSubmitPubToAttester() == false) {
        return@withContext Result.exception(
          IllegalStateException(context.getString(R.string.can_not_replace_public_key_at_attester))
        )
      }
      val retrofitApiService =
        ApiHelper.getInstance(context).retrofit.create(RetrofitApiServiceInterface::class.java)
      getResult { retrofitApiService.submitPubKeyWithConditionalEmailVerification(email, pubKey) }
    }

  /**
   * @param context Interface to global information about an application environment.
   * @param idToken OIDC token.
   * @param model An instance of [WelcomeMessageModel].
   */
  suspend fun postWelcomeMessage(
    context: Context,
    idToken: String,
    model: WelcomeMessageModel,
  ): Result<WelcomeMessageResponse> =
    withContext(Dispatchers.IO) {
      val retrofitApiService =
        ApiHelper.getInstance(context).retrofit.create(RetrofitApiServiceInterface::class.java)
      getResult {
        retrofitApiService.postWelcomeMessage(
          authorization = "Bearer $idToken",
          body = model
        )
      }
    }

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

      if (email.isValidEmail()) {
        val wkdResult = getResult(requestCode = requestCode) {
          val pgpPublicKeyRingCollection = WkdClient.lookupEmail(context, email)

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

        if (clientConfiguration?.canLookupThisRecipientOnAttester(email) == false) {
          return@withContext Result.success(
            requestCode = requestCode,
            data = PubResponse(null, null)
          )
        }
      } else return@withContext Result.exception(
        requestCode = requestCode,
        throwable = IllegalStateException(context.getString(R.string.error_email_is_not_valid))
      )

      val retrofitApiService =
        ApiHelper.getInstance(context).retrofit.create(RetrofitApiServiceInterface::class.java)
      val result =
        getResult(requestCode = requestCode) { retrofitApiService.getPubFromAttester(email) }
      return@withContext resultWrapperFun(result)
    }

  /**
   * @param context Interface to global information about an application environment.
   * @param authorizeCode A code which will be used to retrieve an access token.
   */
  suspend fun getMicrosoftOAuth2Token(
    context: Context,
    authorizeCode: String,
    scopes: String,
    codeVerifier: String
  ): Result<MicrosoftOAuth2TokenResponse> = withContext(Dispatchers.IO) {
    val retrofitApiService =
      ApiHelper.getInstance(context).retrofit.create(RetrofitApiServiceInterface::class.java)
    getResult(
      context = context,
      expectedResultClass = MicrosoftOAuth2TokenResponse::class.java
    ) {
      retrofitApiService.getMicrosoftOAuth2Token(
        code = authorizeCode,
        scope = scopes,
        codeVerifier = codeVerifier,
        redirect_uri = context.getString(R.string.microsoft_redirect_uri)
      )
    }
  }

  /**
   * @param context Interface to global information about an application environment.
   * @param url The configuration url.
   */
  suspend fun getOpenIdConfiguration(context: Context, url: String): Result<JsonObject> =
    withContext(Dispatchers.IO) {
      val retrofitApiService =
        ApiHelper.getInstance(context).retrofit.create(RetrofitApiServiceInterface::class.java)
      getResult {
        retrofitApiService.getOpenIdConfiguration(url)
      }
    }

  /**
   * Get private keys via "<ekm>/v1/keys/private"
   *
   * @param context Interface to global information about an application environment.
   * @param ekmUrl key_manager_url from [ClientConfiguration].
   * @param idToken OIDC token.
   */
  suspend fun getPrivateKeysViaEkm(
    context: Context,
    ekmUrl: String,
    idToken: String
  ): Result<EkmPrivateKeysResponse> =
    withContext(Dispatchers.IO) {
      val retrofitApiService =
        ApiHelper.getInstance(context).retrofit.create(RetrofitApiServiceInterface::class.java)
      val url = if (ekmUrl.endsWith("/")) ekmUrl else "$ekmUrl/"
      getResult(
        context = context,
        expectedResultClass = EkmPrivateKeysResponse::class.java
      ) { retrofitApiService.getPrivateKeysViaEkm("${url}v1/keys/private", "Bearer $idToken") }
    }

  /**
   * Check if "https://fes.$domain/api/" is available for interactions
   *
   * @param context Interface to global information about an application environment.
   * @param domain A company domain.
   */
  suspend fun checkFes(context: Context, domain: String): Result<FesServerResponse> =
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
      val retrofitApiService = retrofit.create(RetrofitApiServiceInterface::class.java)
      return@withContext getResult(
        context = context,
        expectedResultClass = FesServerResponse::class.java
      ) { retrofitApiService.checkFes(domain) }
    }

  /**
   * Grab a reply token before uploading a password protected message
   *
   * @param context Interface to global information about an application environment.
   * @param idToken OIDC token.
   * @param baseFesUrlPath A base FES URL path.
   */
  suspend fun getReplyTokenForPasswordProtectedMsg(
    context: Context,
    idToken: String,
    baseFesUrlPath: String,
  ): Result<MessageReplyTokenResponse> =
    withContext(Dispatchers.IO) {
      val retrofitApiService =
        ApiHelper.getInstance(context).retrofit.create(RetrofitApiServiceInterface::class.java)
      getResult(
        context = context,
        expectedResultClass = MessageReplyTokenResponse::class.java
      ) {
        retrofitApiService.getReplyTokenForPasswordProtectedMsg(
          authorization = "Bearer $idToken",
          baseFesUrlPath = baseFesUrlPath
        )
      }
    }

  /**
   * Upload a password protected message to a web portal
   *
   * @param context Interface to global information about an application environment.
   * @param idToken OIDC token.
   * @param baseFesUrlPath A base FES URL path.
   * @param messageUploadRequest an instance of [MessageUploadRequest]
   * @param msg an encrypted message that will be sent
   */
  suspend fun uploadPasswordProtectedMsgToWebPortal(
    context: Context,
    idToken: String,
    baseFesUrlPath: String,
    messageUploadRequest: MessageUploadRequest,
    msg: String
  ): Result<MessageUploadResponse> =
    withContext(Dispatchers.IO) {
      val retrofitApiService =
        ApiHelper.getInstance(context).retrofit.create(RetrofitApiServiceInterface::class.java)
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

        retrofitApiService.uploadPasswordProtectedMsgToWebPortal(
          authorization = "Bearer $idToken",
          baseFesUrlPath = baseFesUrlPath,
          details = details,
          content = content
        )
      }
    }

  /**
   * Post a user feedback to our server
   *
   * @param context Interface to global information about an application environment.
   * @param postHelpFeedbackModel an instance of [PostHelpFeedbackModel]
   */
  suspend fun postHelpFeedback(
    context: Context,
    postHelpFeedbackModel: PostHelpFeedbackModel
  ): Result<PostHelpFeedbackResponse> = withContext(Dispatchers.IO) {
    val retrofitApiService =
      ApiHelper.getInstance(context).retrofit.create(RetrofitApiServiceInterface::class.java)
    getResult(
      context = context,
      expectedResultClass = PostHelpFeedbackResponse::class.java
    ) { retrofitApiService.postHelpFeedback(postHelpFeedbackModel) }
  }
}
