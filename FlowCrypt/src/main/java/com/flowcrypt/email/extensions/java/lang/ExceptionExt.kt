/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.java.lang

import com.flowcrypt.email.util.GeneralUtil

/**
 * @author Denys Bondarenko
 */
fun Exception.printStackTraceIfDebugOnly() {
  if (GeneralUtil.isDebugBuild()) {
    printStackTrace()
  }
}
