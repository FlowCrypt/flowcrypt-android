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
      val microsoftOAuth2TokenResponseResult = apiRepository.getMicrosoftOAuth2Token(
          requestCode = requestCode,
          context = getApplication(),
          authorizeCode = authorizeCode
      )

      if (microsoftOAuth2TokenResponseResult.status != Result.Status.SUCCESS) {
        when (microsoftOAuth2TokenResponseResult.status) {
          Result.Status.ERROR -> {
            microsoftOAuth2TokenLiveData.postValue(Result.exception(IllegalStateException()))
          }

          Result.Status.EXCEPTION -> {
            microsoftOAuth2TokenLiveData.postValue(Result.exception(microsoftOAuth2TokenResponseResult.exception
                ?: RuntimeException()))
          }

          else -> {
          }
        }
        return@launch
      }

      val token = microsoftOAuth2TokenResponseResult.data?.accessToken

      if (token == null) {
        microsoftOAuth2TokenLiveData.postValue(Result.exception(NullPointerException("token is null")))
        return@launch
      }

      /*val microsoftAccount = apiRepository.getMicrosoftAccountInfo(
          requestCode = requestCode,
          context = getApplication(),
          bearerToken = token
      )*/

      val recommendAuthCredentials = EmailProviderSettingsHelper.getBaseSettings(
          "temp@outlook.com", token)?.copy(useOAuth2 = true)

      microsoftOAuth2TokenLiveData.postValue(Result.success(recommendAuthCredentials!!))
    }
  }
}
