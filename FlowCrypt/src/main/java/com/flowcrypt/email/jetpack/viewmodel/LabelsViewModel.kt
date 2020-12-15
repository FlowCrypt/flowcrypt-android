/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.IMAPStoreManager
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.LabelEntity
import com.flowcrypt.email.jetpack.workmanager.sync.UpdateLabelsWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.mail.Store

/**
 * @author Denis Bondarenko
 *         Date: 1/13/20
 *         Time: 12:32 PM
 *         E-mail: DenBond7@gmail.com
 */
class LabelsViewModel(application: Application) : AccountViewModel(application) {
  val labelsLiveData: LiveData<List<LabelEntity>> = Transformations.switchMap(activeAccountLiveData) {
    roomDatabase.labelDao().getLabelsLD(it?.email ?: "")
  }

  val foldersManagerLiveData: LiveData<FoldersManager> = Transformations.switchMap(labelsLiveData) {
    liveData {
      val foldersManager = activeAccountLiveData.value?.let { account ->
        FoldersManager.build(account.email, it)
      } ?: FoldersManager("")

      emit(foldersManager)
    }
  }

  val loadLabelsFromRemoteServerLiveData = MutableLiveData<Result<Boolean?>>()

  fun updateOutboxMsgsCount() {
    viewModelScope.launch {
      updateOutboxMsgsCount(getActiveAccountSuspend())
    }
  }

  fun loadLabels() {
    viewModelScope.launch {
      val accountEntity = getActiveAccountSuspend()
      accountEntity?.let {
        loadLabelsFromRemoteServerLiveData.value = Result.loading()
        val connection = IMAPStoreManager.activeConnections[accountEntity.id]
        if (connection == null) {
          loadLabelsFromRemoteServerLiveData.value = Result.exception(NullPointerException("There is no active connection for ${accountEntity.email}"))
        } else {
          loadLabelsFromRemoteServerLiveData.value = connection.executeWithResult {
            fetchLabels(accountEntity, connection.store)
          }
        }
      }
    }
  }

  private suspend fun fetchLabels(accountEntity: AccountEntity, store: Store): Result<Boolean> = withContext(Dispatchers.IO) {
    UpdateLabelsWorker.fetchAndSaveLabels(getApplication(), accountEntity, store)
    return@withContext Result.success(true)
  }
}