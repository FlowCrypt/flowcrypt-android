/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.email.EmailProviderSettingsHelper
import com.flowcrypt.email.api.email.model.AuthCredentials
import com.flowcrypt.email.api.oauth.OAuth2Helper
import com.flowcrypt.email.api.retrofit.ApiRepository
import com.flowcrypt.email.api.retrofit.FlowcryptApiRepository
import com.flowcrypt.email.api.retrofit.response.base.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthorizationRequest
import org.jose4j.jwk.HttpsJwks
import org.jose4j.jwt.JwtClaims
import org.jose4j.jwt.consumer.JwtConsumerBuilder
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver
import java.util.*


/**
 * @author Denis Bondarenko
 *         Date: 7/15/20
 *         Time: 2:46 PM
 *         E-mail: DenBond7@gmail.com
 */
class OAuth2AuthCredentialsViewModel(application: Application) : BaseAndroidViewModel(application) {
  private val apiRepository: ApiRepository = FlowcryptApiRepository()
  val microsoftOAuth2TokenLiveData = MutableLiveData<Result<AuthCredentials>>()

  fun getMicrosoftOAuth2Token(requestCode: Long = 0L, authorizeCode: String, authRequest: AuthorizationRequest) {
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
              microsoftOAuth2TokenLiveData.postValue(Result.exception(IllegalStateException()))
            }

            Result.Status.EXCEPTION -> {
              microsoftOAuth2TokenLiveData.postValue(Result.exception(response.exception
                  ?: RuntimeException()))
            }

            else -> {
            }
          }
          return@launch
        }

        val claims = validateTokenAndGetClaims(response.data?.idToken ?: "", authRequest
            .clientId, jwks = JWKS_MICROSOFT)
        val email: String? = claims.getClaimValueAsString(CLAIM_EMAIL)?.toLowerCase(Locale.US)
        val displayName: String? = claims.getClaimValueAsString(CLAIM_NAME)

        if (email == null) {
          microsoftOAuth2TokenLiveData.postValue(Result.exception(NullPointerException("User email is null!")))
          return@launch
        }

        val token = response.data?.accessToken ?: throw NullPointerException("token is null")
        val recommendAuthCredentials = EmailProviderSettingsHelper.getBaseSettings(
            email, token)?.copy(useOAuth2 = true, displayName = displayName)
            ?: throw NullPointerException("Couldn't find default settings!")

        microsoftOAuth2TokenLiveData.postValue(Result.success(recommendAuthCredentials))
      } catch (e: Exception) {
        microsoftOAuth2TokenLiveData.postValue(Result.exception(e))
      }
    }
  }

  private suspend fun validateTokenAndGetClaims(idToken: String, clientId: String, jwks: String):
      JwtClaims =
      withContext(Dispatchers.IO) {
        val httpsJkws = HttpsJwks(jwks)
        val httpsJwksKeyResolver = HttpsJwksVerificationKeyResolver(httpsJkws)
        val jwtConsumer = JwtConsumerBuilder()
            .setVerificationKeyResolver(httpsJwksKeyResolver)
            .setExpectedAudience(clientId)
            .build()
        return@withContext jwtConsumer.processToClaims(idToken)
      }

  companion object {
    private const val CLAIM_EMAIL = "email"
    private const val CLAIM_NAME = "name"

    //https://login.microsoftonline.com/common/v2.0/.well-known/openid-configuration
    private const val JWKS_MICROSOFT = "https://login.microsoftonline.com/common/discovery/v2.0/keys"
  }
}
