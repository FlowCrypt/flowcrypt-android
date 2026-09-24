/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.java.lang

import androidx.fragment.app.Fragment
import com.flowcrypt.email.R
import com.flowcrypt.email.extensions.androidx.fragment.app.showInfoDialog
import com.flowcrypt.email.util.GeneralUtil

/**
 * @author Denys Bondarenko
 */
fun Exception.printStackTraceIfDebugOnly() {
  if (GeneralUtil.isDebugBuild()) {
    printStackTrace()
  }
}

fun Exception.showDialogWithErrorDetails(fragment: Fragment) {
  fragment.showInfoDialog(
    dialogTitle = "",
    dialogMsg = fragment.getString(
      R.string.error_occurred_with_details_please_try_again,
      localizedMessage
    )
  )
}
