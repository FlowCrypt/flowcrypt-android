/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
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
import com.flowcrypt.email.api.retrofit.response.attester.InitialLegacySubmitResponse
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.ActionQueueEntity
import com.flowcrypt.email.service.actionqueue.actions.RegisterUserPublicKeyAction
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

  fun submitPubKey(account: AccountEntity, keys: List<NodeKeyDetails>) {
    submitPubKeyLiveData.value = Result.loading()
    val context: Context = getApplication()

    val keyDetails = keys.firstOrNull()

    keyDetails?.publicKey?.let {
      viewModelScope.launch {
        val result = repository.submitPubKey(context,
            InitialLegacySubmitModel(account.email, it))

        when (result.status) {
          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            if (account.isRuleExist(AccountEntity.DomainRule.ENFORCE_ATTESTER_SUBMIT)) {
              submitPubKeyLiveData.value = result
            } else {
              val registerAction = ActionQueueEntity.fromAction(RegisterUserPublicKeyAction(0, account.email, 0, it))
              registerAction?.let { action ->
                FlowCryptRoomDatabase.getDatabase(getApplication()).actionQueueDao().insertSuspend(action)
              }
              submitPubKeyLiveData.value = Result.success(InitialLegacySubmitResponse(null, false))
            }
          }
          else -> {
            submitPubKeyLiveData.value = result
          }
        }
      }
    }
  }
}