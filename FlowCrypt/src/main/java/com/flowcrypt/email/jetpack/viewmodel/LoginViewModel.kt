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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.FlowcryptApiRepository
import com.flowcrypt.email.api.retrofit.request.model.LoginModel
import com.flowcrypt.email.api.retrofit.response.api.LoginResponse
import com.flowcrypt.email.api.retrofit.response.base.Result
import kotlinx.coroutines.launch

/**
 * This [ViewModel] can be used to login enterprise users.
 *
 * @author Denis Bondarenko
 *         Date: 10/23/19
 *         Time: 12:36 PM
 *         E-mail: DenBond7@gmail.com
 */
class LoginViewModel(application: Application) : BaseAndroidViewModel(application) {
  private val repository = FlowcryptApiRepository()
  val loginLiveData: MutableLiveData<Result<LoginResponse>> = MutableLiveData(Result.none())

  fun login(account: String, idToken: String) {
    viewModelScope.launch {
      val context: Context = getApplication()
      loginLiveData.value = Result.loading(progressMsg = context.getString(R.string.loading))
      try {
        loginLiveData.value = repository.login(
          context = context,
          loginModel = LoginModel(account),
          idToken = idToken
        )
      } catch (e: Exception) {
        loginLiveData.value = Result.exception(e)
      }
    }
  }
}
