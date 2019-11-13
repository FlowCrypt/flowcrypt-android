/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.retrofit.ApiRepository
import com.flowcrypt.email.api.retrofit.FlowcryptApiRepository
import com.flowcrypt.email.api.retrofit.request.model.InitialLegacySubmitModel
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import kotlinx.coroutines.launch

/**
 * @author Denis Bondarenko
 *         Date: 11/11/19
 *         Time: 3:56 PM
 *         E-mail: DenBond7@gmail.com
 */
class SubmitPubKeyViewModel(application: Application) : BaseAndroidViewModel(application) {
  private val repository: ApiRepository = FlowcryptApiRepository()
  val submitPubKeyLiveData: MutableLiveData<Result<ApiResponse>?> = MutableLiveData()

  fun submitPubKey(keys: List<NodeKeyDetails>) {
    submitPubKeyLiveData.value = Result.loading(null)
    val context: Context = getApplication()

    val keyDetails = keys.firstOrNull()

    keyDetails?.publicKey?.let {
      viewModelScope.launch {
        submitPubKeyLiveData.value = repository.submitPubKey(context, InitialLegacySubmitModel
        (keyDetails.primaryPgpContact.email, it))
      }
    }
  }
}