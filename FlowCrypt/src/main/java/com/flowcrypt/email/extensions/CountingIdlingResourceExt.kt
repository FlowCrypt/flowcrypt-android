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
fun CountingIdlingResource?.incrementSafely(msg: String = "") {
  if (GeneralUtil.isDebugBuild()) {
    LogsUtil.d("CountingIdlingResource", "$this:incrementSafely: $msg")
    this?.increment()
  }
}

fun CountingIdlingResource?.decrementSafely(msg: String = "") {
  if (GeneralUtil.isDebugBuild() && this?.isIdleNow == false) {
    LogsUtil.d("CountingIdlingResource", "$this:decrementSafely: $msg")
    decrement()
  }
}

fun CountingIdlingResource?.shutdown() {
  if (GeneralUtil.isDebugBuild()) {
    while (this?.isIdleNow == false) {
      decrement()
    }
  }
}
