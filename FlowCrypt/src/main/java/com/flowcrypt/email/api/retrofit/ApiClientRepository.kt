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
import com.flowcrypt.email.util.GeneralUtil
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit

/**
 * This class describes all network requests over the app.
 *
 * @author Denis Bondarenko
 *         Date: 10/24/19
 *         Time: 6:10 PM
 *         E-mail: DenBond7@gmail.com
 */
object ApiClientRepository : BaseApiRepository {
  object FES : BaseApiRepository {
    val ALLOWED_SERVICES = arrayOf("enterprise-server", "external-service")

    /**
     * @param context Interface to global information about an application environment.
     * @param baseFesUrlPath A base FES URL path.
     * @param domain A company domain.
     * @param idToken OIDC token.
     */
    suspend fun getClientConfiguration(
      context: Context,
      idToken: String,
      baseFesUrlPath: String,
      domain: String,
    ): Result<ClientConfigurationResponse> =
      withContext(Dispatchers.IO) {
        val retrofitApiService = ApiHelper.createRetrofitApiService(context)
        getResult(context = context) {
          retrofitApiService.fesGetClientConfiguration(
            authorization = "Bearer $idToken",
            baseFesUrlPath = baseFesUrlPath,
            domain = domain
          )
        }
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
        val retrofitApiService = ApiHelper.createRetrofitApiService(context)
        getResult(context = context) {
          retrofitApiService.fesGetReplyTokenForPasswordProtectedMsg(
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
        val retrofitApiService = ApiHelper.createRetrofitApiService(context)
        getResult(context = context) {
          val details = GsonBuilder().create().toJson(messageUploadRequest)
            .toRequestBody(Constants.MIME_TYPE_JSON.toMediaTypeOrNull())

          val content = MultipartBody.Part.createFormData(
            "content",
            "content",
            msg.toByteArray().toRequestBody(Constants.MIME_TYPE_BINARY_DATA.toMediaTypeOrNull())
          )

          retrofitApiService.fesUploadPasswordProtectedMsgToWebPortal(
            authorization = "Bearer $idToken",
            baseFesUrlPath = baseFesUrlPath,
            details = details,
            content = content
          )
        }
      }

    /**
     * Check if "https://fes.$domain/api/" is available for interactions
     *
     * @param context Interface to global information about an application environment.
     * @param domain A company domain.
     */
    suspend fun checkIfFesIsAvailableAtCustomerFesUrl(
      context: Context,
      domain: String
    ): Result<FesServerResponse> =
      withContext(Dispatchers.IO) {
        val connectionTimeoutInSeconds = 3L
        val okHttpClient = OkHttpClient.Builder()
          .connectTimeout(connectionTimeoutInSeconds, TimeUnit.SECONDS)
          .writeTimeout(connectionTimeoutInSeconds, TimeUnit.SECONDS)
          .readTimeout(connectionTimeoutInSeconds, TimeUnit.SECONDS)
          .apply {
            ApiHelper.configureOkHttpClientForDebuggingIfAllowed(context, this)
          }.build()

        val genBaseFesUrlPath = GeneralUtil.genBaseFesUrlPath(
          useCustomerFesUrl = true,
          domain = domain
        )
        val retrofit = Retrofit.Builder()
          .baseUrl("https://$genBaseFesUrlPath/api/")
          .addConverterFactory(GsonConverterFactory.create(ApiHelper.getInstance(context).gson))
          .client(okHttpClient)
          .build()
        val retrofitApiService = retrofit.create(RetrofitApiServiceInterface::class.java)
        return@withContext getResult(context = context) {
          retrofitApiService.fesCheckIfServerIsAvailable(
            domain = domain
          )
        }
      }
  }

  object Attester : BaseApiRepository {
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
        val retrofitApiService = ApiHelper.createRetrofitApiService(context)
        getResult(context = context) {
          retrofitApiService.attesterPostWelcomeMessage(
            authorization = "Bearer $idToken",
            body = model
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
        val retrofitApiService = ApiHelper.createRetrofitApiService(context)
        getResult(context = context) {
          val response = retrofitApiService.attesterSubmitPrimaryEmailPubKey(
            authorization = "Bearer $idToken",
            email = email,
            pubKey = pubKey
          )
          //we have to handle a response manually due to different behavior. More details here
          //https://github.com/FlowCrypt/flowcrypt-android/issues/2241#issuecomment-1497787161
          if (response.isSuccessful) {
            Response.success(SubmitPubKeyResponse(isSent = true))
          } else {
            Response.error(response.errorBody() ?: byteArrayOf().toResponseBody(), response.raw())
          }
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
        val retrofitApiService = ApiHelper.createRetrofitApiService(context)
        getResult(context = context) {
          val response = retrofitApiService.attesterSubmitPubKeyWithConditionalEmailVerification(
            email = email,
            pubKey = pubKey
          )
          //we have to handle a response manually due to different behavior. More details here
          //https://github.com/FlowCrypt/flowcrypt-android/issues/2241#issuecomment-1497787161
          if (response.isSuccessful) {
            Response.success(SubmitPubKeyResponse(isSent = true))
          } else {
            Response.error(response.errorBody() ?: byteArrayOf().toResponseBody(), response.raw())
          }
        }
      }
  }

  object PubLookup : BaseApiRepository {
    /**
     * Fetch a public key using all appropriate public key sources(WKD + Attester)
     *
     * @param context     Interface to global information about an application environment.
     * @param email       A user email.
     * @param clientConfiguration    Contains client configurations.
     */
    suspend fun fetchPubKey(
      context: Context,
      email: String,
      clientConfiguration: ClientConfiguration? = null
    ): Result<PubResponse> =
      withContext(Dispatchers.IO) {
        if (email.isValidEmail()) {
          val wkdResult = getResult {
            val pgpPublicKeyRingCollection = WkdClient.lookupEmail(context, email)
            pgpPublicKeyRingCollection?.armor()?.let { armoredPubKey ->
              Response.success(armoredPubKey)
            } ?: Response.error(HttpURLConnection.HTTP_NOT_FOUND, "Not found".toResponseBody())
          }

          if (wkdResult.status == Result.Status.SUCCESS && wkdResult.data?.isNotEmpty() == true) {
            return@withContext Result.success(data = PubResponse(pubkey = wkdResult.data))
          }

          if (clientConfiguration?.canLookupThisRecipientOnAttester(email) == false) {
            return@withContext Result.success(data = PubResponse())
          }
        } else return@withContext Result.exception(
          throwable = IllegalStateException(context.getString(R.string.error_email_is_not_valid))
        )

        val retrofitApiService = ApiHelper.createRetrofitApiService(context)
        return@withContext getResult(context = context) {
          val response = retrofitApiService.attesterGetPubKey(keyIdOrEmailOrFingerprint = email)
          //we have to handle a response manually due to different behavior. More details here
          //https://github.com/FlowCrypt/flowcrypt-android/issues/2241#issuecomment-1497787161
          if (response.isSuccessful) {
            Response.success(PubResponse(pubkey = response.body()))
          } else {
            Response.error(response.errorBody() ?: byteArrayOf().toResponseBody(), response.raw())
          }
        }
      }
  }

  object OAuth : BaseApiRepository {
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
      val retrofitApiService = ApiHelper.createRetrofitApiService(context)
      getResult(context = context) {
        retrofitApiService.oAuthGetMicrosoftOAuth2Token(
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
        val retrofitApiService = ApiHelper.createRetrofitApiService(context)
        getResult {
          retrofitApiService.oAuthGetOpenIdConfiguration(url = url)
        }
      }
  }

  object EKM : BaseApiRepository {
    /**
     * Get private keys via "<ekm>/v1/keys/private"
     *
     * @param context Interface to global information about an application environment.
     * @param ekmUrl key_manager_url from [ClientConfiguration].
     * @param idToken OIDC token.
     */
    suspend fun getPrivateKeys(
      context: Context,
      ekmUrl: String,
      idToken: String
    ): Result<EkmPrivateKeysResponse> =
      withContext(Dispatchers.IO) {
        val retrofitApiService = ApiHelper.createRetrofitApiService(context)
        val url = if (ekmUrl.endsWith("/")) ekmUrl else "$ekmUrl/"
        getResult(context = context) {
          retrofitApiService.ekmGetPrivateKeys(
            ekmUrl = "${url}v1/keys/private",
            authorization = "Bearer $idToken"
          )
        }
      }
  }

  object Backend : BaseApiRepository {
    /**
     * Post a user feedback to FlowCrypt server
     *
     * @param context Interface to global information about an application environment.
     * @param postHelpFeedbackModel an instance of [PostHelpFeedbackModel]
     */
    suspend fun postHelpFeedback(
      context: Context,
      postHelpFeedbackModel: PostHelpFeedbackModel
    ): Result<PostHelpFeedbackResponse> = withContext(Dispatchers.IO) {
      val retrofitApiService = ApiHelper.createRetrofitApiService(context)
      getResult(context = context) {
        retrofitApiService.backendPostHelpFeedback(
          body = postHelpFeedbackModel
        )
      }
    }
  }
}
