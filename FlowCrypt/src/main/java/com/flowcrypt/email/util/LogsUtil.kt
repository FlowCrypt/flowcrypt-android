/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import android.util.Log

/**
 * @author Denys Bondarenko
 */
class LogsUtil {
  companion object {
    fun d(tag: String, msg: String, tr: Throwable? = null) {
      if (GeneralUtil.isDebugBuild()) {
        Log.d(tag, msg, tr)
      }
    }
  }
}
