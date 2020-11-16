/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions

import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.flowcrypt.email.R
import com.flowcrypt.email.ui.activity.fragment.dialog.InfoDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.TwoWayDialogFragment

/**
 * @author Denis Bondarenko
 *         Date: 7/6/20
 *         Time: 4:16 PM
 *         E-mail: DenBond7@gmail.com
 */

val androidx.fragment.app.Fragment.navController: NavController?
  get() = activity?.let { Navigation.findNavController(it, R.id.fragmentContainerView) }

val androidx.fragment.app.Fragment.currentOnResultSavedStateHandle
  get() = navController?.currentBackStackEntry?.savedStateHandle

val androidx.fragment.app.Fragment.previousOnResultSavedStateHandle
  get() = navController?.previousBackStackEntry?.savedStateHandle

fun androidx.fragment.app.Fragment.toast(text: String?, duration: Int = Toast.LENGTH_SHORT) {
  text?.let { activity?.toast(text, duration) }
}

fun androidx.fragment.app.Fragment.toast(resId: Int, duration: Int = Toast.LENGTH_SHORT) {
  if (resId != -1) {
    activity?.toast(resId, duration)
  }
}

fun androidx.fragment.app.Fragment.showDialogFragment(dialog: DialogFragment) {
  dialog.show(parentFragmentManager, dialog.javaClass::class.java.simpleName)
}

fun androidx.fragment.app.Fragment.showInfoDialog(
    dialogTitle: String? = null,
    dialogMsg: String? = null,
    buttonTitle: String? = null,
    isPopBackStack: Boolean = false,
    isCancelable: Boolean = false,
    hasHtml: Boolean = false,
    useLinkify: Boolean = false,
    requestCode: Int = 10000
) {
  val fragment = InfoDialogFragment.newInstance(
      dialogTitle = dialogTitle,
      dialogMsg = dialogMsg,
      buttonTitle = buttonTitle,
      isPopBackStack = isPopBackStack,
      isCancelable = isCancelable,
      hasHtml = hasHtml,
      useLinkify = useLinkify
  )
  fragment.setTargetFragment(this, requestCode)
  showDialogFragment(fragment)
}

fun androidx.fragment.app.Fragment.showTwoWayDialog(
    dialogTitle: String? = null,
    dialogMsg: String? = null,
    positiveButtonTitle: String? = null,
    negativeButtonTitle: String? = null,
    isCancelable: Boolean = false,
    requestCode: Int = 10000,
    hasHtml: Boolean = false,
    useLinkify: Boolean = false
) {
  val fragment = TwoWayDialogFragment.newInstance(
      dialogTitle = dialogTitle,
      dialogMsg = dialogMsg,
      positiveButtonTitle = positiveButtonTitle,
      negativeButtonTitle = negativeButtonTitle,
      isCancelable = isCancelable,
      hasHtml = hasHtml,
      useLinkify = useLinkify
  )
  fragment.setTargetFragment(this, requestCode)
  showDialogFragment(fragment)
}