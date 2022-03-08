/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.flowcrypt.email.R
import com.flowcrypt.email.jetpack.workmanager.ForwardedAttachmentsDownloaderWorker
import com.flowcrypt.email.jetpack.workmanager.MessagesSenderWorker
import com.flowcrypt.email.service.FeedbackJobIntentService
import com.flowcrypt.email.util.CacheManager
import com.flowcrypt.email.util.FileAndDirectoryUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * This [androidx.lifecycle.ViewModel] helps indicate when the app is ready to continue the init
 * launch
 *
 * @author Denis Bondarenko
 *         Date: 5/6/20
 *         Time: 4:22 PM
 *         E-mail: DenBond7@gmail.com
 */
class LauncherViewModel(application: Application) : AccountViewModel(application) {
  private val isLoadingMutableStateFlow = MutableStateFlow(true)
  val isLoadingStateFlow = isLoadingMutableStateFlow.asStateFlow()

  init {
    viewModelScope.launch {
      PreferenceManager.setDefaultValues(
        application,
        R.xml.preferences_notifications_settings,
        false
      )
      ForwardedAttachmentsDownloaderWorker.enqueue(application)
      MessagesSenderWorker.enqueue(application)
      FeedbackJobIntentService.enqueueWork(application)
      FileAndDirectoryUtils.cleanDir(CacheManager.getCurrentMsgTempDir())

      isLoadingMutableStateFlow.value = false
    }
  }
}
