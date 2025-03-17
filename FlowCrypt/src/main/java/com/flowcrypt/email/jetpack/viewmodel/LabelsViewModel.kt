/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.IMAPStoreManager
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.LabelEntity
import com.flowcrypt.email.jetpack.workmanager.sync.UpdateLabelsWorker
import kotlinx.coroutines.launch

/**
 * @author Denys Bondarenko
 */
class LabelsViewModel(application: Application) : AccountViewModel(application) {
  val labelsLiveData: LiveData<List<LabelEntity>> = activeAccountLiveData.switchMap {
    roomDatabase.labelDao().getLabelsLD(it?.email ?: "", it?.accountType)
  }

  val foldersManagerLiveData: LiveData<FoldersManager> = labelsLiveData.switchMap {
    liveData {
      activeAccountLiveData.value?.let { account ->
        emit(FoldersManager.build(account, it))
      }
    }
  }

  val activeFolderLiveData: MediatorLiveData<LocalFolder> = MediatorLiveData()

  private val manuallyChangedFolderLiveData: MutableLiveData<LocalFolder?> = MutableLiveData()
  private val initLocalFolderLiveData: LiveData<LocalFolder?> = foldersManagerLiveData.switchMap {
    liveData {
      emit(it.folderInbox ?: it.findInboxFolder())
    }
  }

  init {
    activeFolderLiveData.addSource(manuallyChangedFolderLiveData) {
      it?.let { localFolder -> activeFolderLiveData.value = localFolder }
    }

    activeFolderLiveData.addSource(initLocalFolderLiveData) {
      if (activeFolderLiveData.value == null || activeFolderLiveData.value?.account != it?.account) {
        it?.let { localFolder -> activeFolderLiveData.value = localFolder }
      }
    }
  }

  val loadLabelsFromRemoteServerLiveData = MutableLiveData<Result<Boolean?>>()

  fun updateOutboxMsgsCount() {
    viewModelScope.launch {
      updateOutboxMsgsCount(getActiveAccountSuspend())
    }
  }

  fun changeActiveFolder(localFolder: LocalFolder) {
    manuallyChangedFolderLiveData.value = localFolder
  }

  fun loadLabels() {
    viewModelScope.launch {
      val accountEntity = getActiveAccountSuspend()
      accountEntity?.let {
        loadLabelsFromRemoteServerLiveData.value = Result.loading()
        if (accountEntity.useAPI) {
          loadLabelsFromRemoteServerLiveData.value = GmailApiHelper.executeWithResult {
            UpdateLabelsWorker.fetchAndSaveLabels(getApplication(), accountEntity)
            Result.success(true)
          }
        } else {
          val connection = IMAPStoreManager.getConnection(accountEntity.id)
          if (connection == null) {
            loadLabelsFromRemoteServerLiveData.value =
              Result.exception(NullPointerException("There is no active connection for ${accountEntity.email}"))
          } else {
            loadLabelsFromRemoteServerLiveData.value = connection.executeWithResult {
              UpdateLabelsWorker.fetchAndSaveLabels(
                getApplication(),
                accountEntity,
                connection.store
              )
              Result.success(true)
            }
          }
        }
      }
    }
  }
}
