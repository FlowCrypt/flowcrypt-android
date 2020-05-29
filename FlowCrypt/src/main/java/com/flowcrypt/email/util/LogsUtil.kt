/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import android.util.Log

/**
 * @author Denis Bondarenko
 * Date: 4/26/19
 * Time: 9:33 PM
 * E-mail: DenBond7@gmail.com
 */
class LogsUtil {
  companion object {
    fun d(tag: String, msg: String) {
      if (GeneralUtil.isDebugBuild()) {
        Log.d(tag, msg)
      }
    }
  }
}
