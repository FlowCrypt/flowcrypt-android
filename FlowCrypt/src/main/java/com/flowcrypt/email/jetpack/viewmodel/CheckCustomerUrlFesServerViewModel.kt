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
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.retrofit.ApiClientRepository
import com.flowcrypt.email.api.retrofit.response.api.FesServerResponse
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.util.GeneralUtil
import kotlinx.coroutines.launch

/**
 * @author Denys Bondarenko
 */
class CheckCustomerUrlFesServerViewModel(application: Application) :
  BaseAndroidViewModel(application) {
  val checkFesServerAvailabilityLiveData: MutableLiveData<Result<FesServerResponse>> =
    MutableLiveData(Result.none())

  fun checkServerAvailability(account: String) {
    viewModelScope.launch {
      val context: Context = getApplication()
      checkFesServerAvailabilityLiveData.value =
        Result.loading(progressMsg = context.getString(R.string.loading))

      try {
        val result = ApiClientRepository.FES.checkIfFesIsAvailableAtCustomerFesUrl(
          context = getApplication(),
          domain = EmailUtil.getDomain(account)
        )

        if (result.status == Result.Status.EXCEPTION) {
          val causedException = result.exception
          if (causedException != null) {
            checkFesServerAvailabilityLiveData.value = Result.exception(
              GeneralUtil.preProcessException(
                context = context,
                causedException = causedException
              )
            )
            return@launch
          }
        }

        checkFesServerAvailabilityLiveData.value = result
      } catch (e: Exception) {
        checkFesServerAvailabilityLiveData.value = Result.exception(e)
      }
    }
  }
}
