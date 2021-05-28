/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData

/**
 * This [androidx.lifecycle.ViewModel] helps indicate when the app is ready to continue the init
 * launch
 *
 * @author Denis Bondarenko
 *         Date: 5/6/20
 *         Time: 4:22 PM
 *         E-mail: DenBond7@gmail.com
 */
class LauncherViewModel(application: Application) : BaseAndroidViewModel(application) {
  val isNodeInfoReceivedLiveData: MutableLiveData<Boolean> = MutableLiveData()
  val isAccountInfoReceivedLiveData: MutableLiveData<Boolean> = MutableLiveData()

  val mediatorLiveData = MediatorLiveData<Boolean>()

  init {
    mediatorLiveData.addSource(isNodeInfoReceivedLiveData) { value ->
      mediatorLiveData.setValue(
        value
      )
    }
    mediatorLiveData.addSource(isAccountInfoReceivedLiveData) { value ->
      mediatorLiveData.setValue(
        value
      )
    }
  }
}