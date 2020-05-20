/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions

import androidx.test.espresso.idling.CountingIdlingResource
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.LogsUtil

/**
 * @author Denis Bondarenko
 *         Date: 5/18/20
 *         Time: 7:26 PM
 *         E-mail: DenBond7@gmail.com
 */
fun CountingIdlingResource.incrementSafely(msg: String? = null) {
  if (GeneralUtil.isDebugBuild()) {
    increment()
    msg?.let { LogsUtil.d("CountingIdlingResource", "$this:incrementSafely: $it") }
  }
}

fun CountingIdlingResource.decrementSafely(msg: String? = null) {
  if (GeneralUtil.isDebugBuild() && !isIdleNow) {
    decrement()
    msg?.let { LogsUtil.d("CountingIdlingResource", "$this:decrementSafely: $it") }
  }
}

fun CountingIdlingResource.shutdown() {
  if (GeneralUtil.isDebugBuild()) {
    while (!isIdleNow) {
      decrement()
    }
  }
}