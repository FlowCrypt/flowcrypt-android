/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.livedata

import androidx.lifecycle.Observer

/**
 * @author Denys Bondarenko
 */
class SkipInitialValueObserver<T>(val action: (t: T) -> Unit) : Observer<T> {
  private var isFirstCall = true
  override fun onChanged(value: T) {
    if (isFirstCall) {
      isFirstCall = false
    } else {
      action.invoke(value)
    }
  }
}