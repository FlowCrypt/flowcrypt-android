/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.jetpack.workmanager.ForwardedAttachmentsDownloaderWorker
import com.flowcrypt.email.jetpack.workmanager.MessagesSenderWorker
import com.flowcrypt.email.util.CacheManager
import com.flowcrypt.email.util.FileAndDirectoryUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * This [androidx.lifecycle.ViewModel] helps indicate when the app is ready to continue the init
 * launch
 *
 * @author Denys Bondarenko
 */
class LauncherViewModel(application: Application) : AccountViewModel(application) {
  private val isInitLoadingCompletedMutableStateFlow = MutableStateFlow<InitData?>(null)
  val isInitLoadingCompletedStateFlow = isInitLoadingCompletedMutableStateFlow.asStateFlow()

  init {
    viewModelScope.launch {
      PreferenceManager.setDefaultValues(
        application,
        R.xml.preferences_notifications_settings,
        false
      )
      ForwardedAttachmentsDownloaderWorker.enqueue(application)
      MessagesSenderWorker.enqueue(application)

      isInitLoadingCompletedMutableStateFlow.value =
        InitData(roomDatabase.accountDao().getActiveAccountSuspend())
    }
  }

  data class InitData(val accountEntity: AccountEntity?)
}
