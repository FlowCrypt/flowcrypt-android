/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import com.flowcrypt.email.api.retrofit.response.node.NodeResponseWrapper
import com.flowcrypt.email.jetpack.livedata.SingleLiveEvent

/**
 * It's a base implementation of [androidx.lifecycle.ViewModel] which is going to handle requests to Node.js
 *
 * @author Denis Bondarenko
 * Date: 2/15/19
 * Time: 3:35 PM
 * E-mail: DenBond7@gmail.com
 */
abstract class BaseNodeApiViewModel(application: Application) : BaseAndroidViewModel(application) {
  var responsesLiveData: SingleLiveEvent<NodeResponseWrapper<*>>
    protected set

  init {
    this.responsesLiveData = SingleLiveEvent()
  }
}
