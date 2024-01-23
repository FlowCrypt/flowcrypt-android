/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions

import androidx.test.espresso.idling.CountingIdlingResource
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.IdlingCountListener
import com.flowcrypt.email.util.LogsUtil

/**
 * @author Denys Bondarenko
 */
fun CountingIdlingResource?.incrementSafely(any: Any, msg: String = "") {
  if (GeneralUtil.isDebugBuild()) {
    LogsUtil.d("CountingIdlingResource", "$this:incrementSafely from ${any.javaClass.name}: $msg")
    this?.increment()
    (any as? IdlingCountListener)?.incrementIdlingCount()
  }
}

fun CountingIdlingResource?.decrementSafely(any: Any, msg: String = "") {
  if (GeneralUtil.isDebugBuild()) {
    LogsUtil.d("CountingIdlingResource", "$this:decrementSafely from ${any.javaClass.name}: $msg")
    if (this?.isIdleNow == false) {
      decrement()
    }
    (any as? IdlingCountListener)?.decrementIdlingCount()
  }
}

fun CountingIdlingResource?.shutdown() {
  if (GeneralUtil.isDebugBuild()) {
    while (this?.isIdleNow == false) {
      decrement()
    }
  }
}
