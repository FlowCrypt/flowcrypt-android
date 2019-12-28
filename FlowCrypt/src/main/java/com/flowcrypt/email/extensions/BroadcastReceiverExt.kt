/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions

import android.content.BroadcastReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * @author Denis Bondarenko
 *         Date: 12/28/19
 *         Time: 3:24 PM
 *         E-mail: DenBond7@gmail.com
 */


/**
 * See details here https://github.com/romannurik/muzei/blob/master/extensions/src/main/java/com/google/android/apps/muzei/util/BroadcastReceiverExt.kt
 */
fun BroadcastReceiver.goAsync(
    coroutineScope: CoroutineScope = GlobalScope,
    block: suspend () -> Unit
) {
  val result = goAsync()
  coroutineScope.launch {
    try {
      block()
    } finally {
      // Always call finish(), even if the coroutineScope was cancelled
      result.finish()
    }
  }
}