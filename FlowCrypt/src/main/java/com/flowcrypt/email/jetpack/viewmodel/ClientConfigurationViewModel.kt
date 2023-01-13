/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.FlowcryptApiRepository
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.flowcrypt.email.api.retrofit.response.base.Result
import kotlinx.coroutines.launch

/**
 * This [ViewModel] can be used to resolve domain rules for enterprise users.
 *
 * @author Denis Bondarenko
 *         Date: 6/30/21
 *         Time: 6:56 PM
 *         E-mail: DenBond7@gmail.com
 */
class ClientConfigurationViewModel(application: Application) : BaseAndroidViewModel(application) {
  private val repository = FlowcryptApiRepository()
  val clientConfigurationLiveData: MutableLiveData<Result<ApiResponse>> =
    MutableLiveData(Result.none())

  fun fetchClientConfiguration(idToken: String, fesUrl: String? = null) {
    viewModelScope.launch {
      val context: Context = getApplication()
      clientConfigurationLiveData.value =
        Result.loading(progressMsg = context.getString(R.string.loading_domain_rules))

      try {
        clientConfigurationLiveData.value = repository.getClientConfiguration(
          context = context,
          idToken = idToken,
          fesUrl = fesUrl,
        )
      } catch (e: Exception) {
        clientConfigurationLiveData.value = Result.exception(e)
      }
    }
  }
}
