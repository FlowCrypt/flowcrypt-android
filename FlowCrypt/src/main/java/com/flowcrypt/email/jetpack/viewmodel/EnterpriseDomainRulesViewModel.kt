/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.retrofit.ApiName
import com.flowcrypt.email.api.retrofit.ApiRepository
import com.flowcrypt.email.api.retrofit.FlowcryptApiRepository
import com.flowcrypt.email.api.retrofit.request.api.LoginRequest
import com.flowcrypt.email.api.retrofit.request.model.LoginModel
import com.flowcrypt.email.api.retrofit.response.api.LoginResponse
import com.flowcrypt.email.api.retrofit.response.base.ApiResult
import com.flowcrypt.email.database.dao.source.AccountDao
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
  val loginLiveData: MutableLiveData<ApiResult<LoginResponse>?> = MutableLiveData()

  fun login(accountDao: AccountDao, tokenId: String) {
    loginLiveData.value = ApiResult.loading(null)
    viewModelScope.launch {
      val result = repository.login(getApplication(),
          LoginRequest(ApiName.POST_LOGIN, LoginModel(accountDao.email, accountDao.uuid!!), tokenId))
      loginLiveData.value = result
    }
  }
}