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
import com.flowcrypt.email.api.retrofit.ApiClientRepository
import com.flowcrypt.email.api.retrofit.response.api.ClientConfigurationResponse
import com.flowcrypt.email.api.retrofit.response.base.Result
import kotlinx.coroutines.launch

/**
 * This [ViewModel] can be used to resolve domain rules for enterprise users.
 *
 * @author Denys Bondarenko
 */
class ClientConfigurationViewModel(application: Application) : BaseAndroidViewModel(application) {
  val clientConfigurationLiveData: MutableLiveData<Result<ClientConfigurationResponse>> =
    MutableLiveData(Result.none())

  fun fetchClientConfiguration(idToken: String, baseFesUrlPath: String, domain: String) {
    viewModelScope.launch {
      val context: Context = getApplication()
      clientConfigurationLiveData.value =
        Result.loading(progressMsg = context.getString(R.string.loading_domain_rules))

      try {
        clientConfigurationLiveData.value = ApiClientRepository.FES.getClientConfiguration(
          context = context,
          idToken = idToken,
          baseFesUrlPath = baseFesUrlPath,
          domain = domain
        )
      } catch (e: Exception) {
        clientConfigurationLiveData.value = Result.exception(e)
      }
    }
  }
}
