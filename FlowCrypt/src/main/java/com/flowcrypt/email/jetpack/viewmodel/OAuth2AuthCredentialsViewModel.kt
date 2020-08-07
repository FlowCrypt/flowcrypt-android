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
import kotlinx.coroutines.launch


/**
 * @author Denis Bondarenko
 *         Date: 7/15/20
 *         Time: 2:46 PM
 *         E-mail: DenBond7@gmail.com
 */
class OAuth2AuthCredentialsViewModel(application: Application) : BaseAndroidViewModel(application) {
  private val apiRepository: ApiRepository = FlowcryptApiRepository()
  val microsoftOAuth2TokenLiveData = MutableLiveData<Result<AuthCredentials>>()

  fun getMicrosoftOAuth2Token(requestCode: Long = 0L, authorizeCode: String) {
    viewModelScope.launch {
      microsoftOAuth2TokenLiveData.postValue(Result.loading())
      val microsoftOAuth2TokenResponseResultForProfile = apiRepository.getMicrosoftOAuth2Token(
          requestCode = requestCode,
          context = getApplication(),
          authorizeCode = authorizeCode,
          scopes = OAuth2Helper.SCOPE_MICROSOFT_OAUTH2_FOR_PROFILE
      )

      if (microsoftOAuth2TokenResponseResultForProfile.status != Result.Status.SUCCESS) {
        when (microsoftOAuth2TokenResponseResultForProfile.status) {
          Result.Status.ERROR -> {
            microsoftOAuth2TokenLiveData.postValue(Result.exception(IllegalStateException()))
          }

          Result.Status.EXCEPTION -> {
            microsoftOAuth2TokenLiveData.postValue(Result.exception(microsoftOAuth2TokenResponseResultForProfile.exception
                ?: RuntimeException()))
          }

          else -> {
          }
        }
        return@launch
      }

      val tokenForProfile = microsoftOAuth2TokenResponseResultForProfile.data?.accessToken
      if (tokenForProfile == null) {
        microsoftOAuth2TokenLiveData.postValue(Result.exception(NullPointerException("token is null")))
        return@launch
      }

      val microsoftAccount = apiRepository.getMicrosoftAccountInfo(
          requestCode = requestCode,
          context = getApplication(),
          bearerToken = tokenForProfile
      )

      val userEmailAddress = microsoftAccount.data?.userPrincipalName
      if (userEmailAddress == null) {
        microsoftOAuth2TokenLiveData.postValue(Result.exception(NullPointerException("User email is null")))
        return@launch
      }

      val microsoftOAuth2TokenResponseResultForEmail = apiRepository.getMicrosoftOAuth2Token(
          requestCode = requestCode,
          context = getApplication(),
          authorizeCode = authorizeCode,
          scopes = OAuth2Helper.SCOPE_MICROSOFT_OAUTH2_FOR_MAIL
      )

      if (microsoftOAuth2TokenResponseResultForEmail.status != Result.Status.SUCCESS) {
        when (microsoftOAuth2TokenResponseResultForEmail.status) {
          Result.Status.ERROR -> {
            microsoftOAuth2TokenLiveData.postValue(Result.exception(IllegalStateException()))
          }

          Result.Status.EXCEPTION -> {
            microsoftOAuth2TokenLiveData.postValue(Result.exception(microsoftOAuth2TokenResponseResultForEmail.exception
                ?: RuntimeException()))
          }

          else -> {
          }
        }
        return@launch
      }

      val tokenForEmail = microsoftOAuth2TokenResponseResultForEmail.data?.accessToken

      if (tokenForEmail == null) {
        microsoftOAuth2TokenLiveData.postValue(Result.exception(NullPointerException("token is null")))
        return@launch
      }

      val recommendAuthCredentials = EmailProviderSettingsHelper.getBaseSettings(
          microsoftAccount.data.userPrincipalName, tokenForEmail)?.copy(useOAuth2 = true)

      microsoftOAuth2TokenLiveData.postValue(Result.success(recommendAuthCredentials!!))
    }
  }
}
