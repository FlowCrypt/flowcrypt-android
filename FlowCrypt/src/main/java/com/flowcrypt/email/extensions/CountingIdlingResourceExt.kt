/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions

import androidx.test.espresso.idling.CountingIdlingResource
import com.flowcrypt.email.util.GeneralUtil

/**
 * @author Denis Bondarenko
 *         Date: 5/18/20
 *         Time: 7:26 PM
 *         E-mail: DenBond7@gmail.com
 */
fun CountingIdlingResource.incrementSafely() {
  if (GeneralUtil.isDebugBuild()) {
    increment()
  }
}

fun CountingIdlingResource.decrementSafely() {
  if (GeneralUtil.isDebugBuild() && !isIdleNow) {
    decrement()
  }
}