/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailProviderSettingsHelper
import com.flowcrypt.email.api.email.model.AuthCredentials
import com.flowcrypt.email.api.email.model.AuthTokenInfo
import com.flowcrypt.email.api.oauth.OAuth2Helper
import com.flowcrypt.email.api.retrofit.ApiRepository
import com.flowcrypt.email.api.retrofit.FlowcryptApiRepository
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.util.exception.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.AuthorizationServiceDiscovery
import org.jose4j.jwk.HttpsJwks
import org.jose4j.jwt.JwtClaims
import org.jose4j.jwt.consumer.InvalidJwtException
import org.jose4j.jwt.consumer.JwtConsumerBuilder
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException


/**
 * @author Denys Bondarenko
 */
class OAuth2AuthCredentialsViewModel(application: Application) : BaseAndroidViewModel(application) {
  private val apiRepository: ApiRepository = FlowcryptApiRepository()
  val microsoftOAuth2TokenLiveData = MutableLiveData<Result<AuthCredentials?>>()
  val authorizationRequestLiveData = MutableLiveData<Result<AuthorizationRequest>>()

  fun getAuthorizationRequestForProvider(requestCode: Long = 0L, provider: OAuth2Helper.Provider) {
    viewModelScope.launch {
      authorizationRequestLiveData.postValue(Result.loading())
      val context: Context = getApplication()

      try {
        val authRequest = when (provider) {
          OAuth2Helper.Provider.MICROSOFT -> {
            val jsonObject = getJsonObjectForOpenidConfiguration(requestCode, provider)
            val authorizationServiceDiscovery = AuthorizationServiceDiscovery(jsonObject)
            OAuth2Helper.getMicrosoftAuthorizationRequest(
              configuration = AuthorizationServiceConfiguration(authorizationServiceDiscovery),
              redirectUri = context.getString(R.string.microsoft_redirect_uri)
            )
          }
        }
        authorizationRequestLiveData.postValue(Result.success(authRequest))
      } catch (e: IOException) {
        e.printStackTrace()
        authorizationRequestLiveData.postValue(
          Result.exception(
            AuthorizationException.fromTemplate(
              AuthorizationException.GeneralErrors.NETWORK_ERROR,
              e
            )
          )
        )
      } catch (e: JSONException) {
        e.printStackTrace()
        authorizationRequestLiveData.postValue(
          Result.exception(
            AuthorizationException.fromTemplate(
              AuthorizationException.GeneralErrors.JSON_DESERIALIZATION_ERROR, e
            )
          )
        )
      } catch (e: AuthorizationServiceDiscovery.MissingArgumentException) {
        e.printStackTrace()
        authorizationRequestLiveData.postValue(
          Result.exception(
            AuthorizationException.fromTemplate(
              AuthorizationException.GeneralErrors.INVALID_DISCOVERY_DOCUMENT, e
            )
          )
        )
      } catch (e: Exception) {
        e.printStackTrace()
        authorizationRequestLiveData.postValue(Result.exception(e))
      }
    }
  }

  fun getMicrosoftOAuth2Token(
    requestCode: Long = 0L,
    authorizeCode: String,
    authRequest: AuthorizationRequest
  ) {
    viewModelScope.launch {
      microsoftOAuth2TokenLiveData.postValue(Result.loading())
      try {
        val response = apiRepository.getMicrosoftOAuth2Token(
          requestCode = requestCode,
          context = getApplication(),
          authorizeCode = authorizeCode,
          scopes = OAuth2Helper.SCOPE_MICROSOFT_OAUTH2_FOR_MAIL,
          codeVerifier = authRequest.codeVerifier ?: ""
        )

        if (response.status != Result.Status.SUCCESS) {
          when (response.status) {
            Result.Status.ERROR -> {
              microsoftOAuth2TokenLiveData.postValue(Result.exception(ApiException(response.data?.apiError)))
            }

            Result.Status.EXCEPTION -> {
              microsoftOAuth2TokenLiveData.postValue(
                Result.exception(
                  response.exception
                    ?: RuntimeException()
                )
              )
            }

            else -> {
            }
          }
          return@launch
        }

        val claims = validateTokenAndGetClaims(
          idToken = response.data?.idToken ?: "",
          clientId = authRequest.clientId,
          jwks = authRequest.configuration.discoveryDoc?.jwksUri.toString()
        )

        val email: String? = claims.getClaimValueAsString(CLAIM_EMAIL)?.lowercase()
        val displayName: String? = claims.getClaimValueAsString(CLAIM_NAME)

        if (email == null) {
          microsoftOAuth2TokenLiveData.postValue(Result.exception(NullPointerException("IdToken:User email is null!")))
          return@launch
        }

        val accessToken = response.data?.accessToken
          ?: throw NullPointerException("API error: accessToken is null!")
        val recommendAuthCredentials = EmailProviderSettingsHelper.getBaseSettingsForProvider(
          email,
          OAuth2Helper.Provider.MICROSOFT
        ).copy(
          password = "",
          smtpSignInPassword = null,
          useOAuth2 = true,
          displayName = displayName,
          authTokenInfo = AuthTokenInfo(
            email = email,
            accessToken = accessToken,
            expiresAt = OAuth2Helper.getExpiresAtTime(response.data.expiresIn),
            refreshToken = KeyStoreCryptoManager.encryptSuspend(response.data.refreshToken)
          )
        )

        microsoftOAuth2TokenLiveData.postValue(Result.success(recommendAuthCredentials))
      } catch (e: Exception) {
        //we don't store stacktrace to prevent tokens leaks
        if (e is InvalidJwtException) {
          microsoftOAuth2TokenLiveData.postValue(
            Result.exception(
              InvalidJwtException(
                "JWT validation was failed!\n\n",
                e.errorDetails,
                e.jwtContext
              )
            )
          )
        } else {
          microsoftOAuth2TokenLiveData.postValue(Result.exception(e))
        }
      }
    }
  }

  private suspend fun validateTokenAndGetClaims(idToken: String, clientId: String, jwks: String):
      JwtClaims =
    withContext(Dispatchers.IO) {
      val httpsJkws = HttpsJwks(jwks)
      val verificationKeyResolver = HttpsJwksVerificationKeyResolver(httpsJkws)
      val jwtConsumer = JwtConsumerBuilder()
        .setVerificationKeyResolver(verificationKeyResolver)
        .setExpectedAudience(clientId)
        .setRequireIssuedAt()
        .setRequireNotBefore()
        .setRequireExpirationTime()
        .build()
      return@withContext jwtConsumer.processToClaims(idToken)
    }

  private suspend fun getJsonObjectForOpenidConfiguration(
    requestCode: Long,
    provider: OAuth2Helper.Provider
  ): JSONObject =
    withContext(Dispatchers.IO) {
      val jsonObjectResult = apiRepository.getOpenIdConfiguration(
        requestCode = requestCode,
        context = getApplication(),
        url = provider.openidConfigurationUrl
      )

      when (jsonObjectResult.status) {
        Result.Status.SUCCESS -> {
          return@withContext JSONObject(jsonObjectResult.data.toString())
        }

        Result.Status.EXCEPTION -> {
          throw jsonObjectResult.exception ?: IOException("Couldn't fetch configurations")
        }

        else -> throw IOException("Couldn't fetch configurations")
      }
    }


  companion object {
    private const val CLAIM_EMAIL = "email"
    private const val CLAIM_NAME = "name"
  }
}
