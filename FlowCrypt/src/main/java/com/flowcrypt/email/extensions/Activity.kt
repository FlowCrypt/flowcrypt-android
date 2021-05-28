/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import com.flowcrypt.email.ui.activity.fragment.dialog.InfoDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.TwoWayDialogFragment

/**
 * This class describes extension function for [FragmentActivity]
 *
 * @author Denis Bondarenko
 *         Date: 11/22/19
 *         Time: 3:37 PM
 *         E-mail: DenBond7@gmail.com
 */

fun FragmentActivity.showDialogFragment(dialog: DialogFragment) {
  val fragmentTransaction: FragmentTransaction = supportFragmentManager.beginTransaction()
  supportFragmentManager.findFragmentByTag("dialog")?.let { fragmentTransaction.remove(it) }
  fragmentTransaction.addToBackStack(null)
  dialog.show(fragmentTransaction, "dialog")
}

fun FragmentActivity.showInfoDialogFragment(
  dialogTitle: String? = "", dialogMsg: String? = null,
  buttonTitle: String? = null, isPopBackStack: Boolean = false,
  isCancelable: Boolean = true, hasHtml: Boolean = false
) {
  val infoDialogFragment = InfoDialogFragment.newInstance(
    dialogTitle = dialogTitle,
    dialogMsg = dialogMsg,
    buttonTitle = buttonTitle,
    isPopBackStack = isPopBackStack,
    isCancelable = isCancelable,
    hasHtml = hasHtml
  )

  showDialogFragment(infoDialogFragment)
}

fun FragmentActivity.showTwoWayDialogFragment(
  requestCode: Int = 0, dialogTitle: String? = "",
  dialogMsg: String? = null,
  positiveButtonTitle: String? = null,
  negativeButtonTitle: String? = null,
  isCancelable: Boolean = true
) {
  val infoDialogFragment = TwoWayDialogFragment.newInstance(
    requestCode = requestCode,
    dialogTitle = dialogTitle,
    dialogMsg = dialogMsg,
    positiveButtonTitle = positiveButtonTitle,
    negativeButtonTitle = negativeButtonTitle,
    isCancelable = isCancelable
  )

  showDialogFragment(infoDialogFragment)
}

