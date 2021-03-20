/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *  DenBond7
 *  Ivan Pizhenko
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.ApiName
import com.flowcrypt.email.api.retrofit.ApiRepository
import com.flowcrypt.email.api.retrofit.FlowcryptApiRepository
import com.flowcrypt.email.api.retrofit.request.api.DomainRulesRequest
import com.flowcrypt.email.api.retrofit.request.api.LoginRequest
import com.flowcrypt.email.api.retrofit.request.model.LoginModel
import com.flowcrypt.email.api.retrofit.response.api.LoginResponse
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.flowcrypt.email.api.retrofit.response.base.Result
import kotlinx.coroutines.launch

/**
 * This [androidx.lifecycle.ViewModel] can be used to resolve domain rules for enterprise users.
 *
 * @author Denis Bondarenko
 *         Date: 10/23/19
 *         Time: 12:36 PM
 *         E-mail: DenBond7@gmail.com
 */
class EnterpriseDomainRulesViewModel(application: Application) : BaseAndroidViewModel(application) {
  private val repository: ApiRepository = FlowcryptApiRepository()
  val domainRulesLiveData: MutableLiveData<Result<ApiResponse>?> = MutableLiveData()

  fun getDomainRules(account: String, uuid: String, tokenId: String) {
    domainRulesLiveData.value = Result.loading()
    val context: Context = getApplication()

    viewModelScope.launch {
      val loginResult = repository.login(context,
          LoginRequest(ApiName.POST_LOGIN, LoginModel(account, uuid), tokenId))

      when (loginResult.status) {
        Result.Status.ERROR, Result.Status.EXCEPTION -> {
          domainRulesLiveData.value = loginResult
          return@launch
        }

        Result.Status.SUCCESS -> {
          if (loginResult.data?.isVerified == true) {
            val domainRulesResult = repository.getDomainRules(context,
                DomainRulesRequest(ApiName.POST_GET_DOMAIN_RULES, LoginModel(account, uuid)))
            domainRulesLiveData.value = domainRulesResult
          } else {
            domainRulesLiveData.value = Result.error(LoginResponse(ApiError(-1,
                context.getString(R.string.user_not_registered_or_verified)), false))
          }
        }

        else -> {
          domainRulesLiveData.value = Result.exception(IllegalStateException("Unhandled error"))
        }
      }
    }
  }
}
