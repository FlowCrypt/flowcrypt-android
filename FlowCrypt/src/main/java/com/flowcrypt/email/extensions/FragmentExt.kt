/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions

import android.widget.Toast
import androidx.annotation.IdRes
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.flowcrypt.email.NavGraphDirections
import com.flowcrypt.email.R
import com.flowcrypt.email.ui.activity.fragment.FeedbackFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.FixNeedPassphraseIssueDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.InfoDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.TwoWayDialogFragment
import com.flowcrypt.email.util.UIUtil

/**
 * @author Denis Bondarenko
 *         Date: 7/6/20
 *         Time: 4:16 PM
 *         E-mail: DenBond7@gmail.com
 */

val androidx.fragment.app.Fragment.navController: NavController?
  get() = activity?.let {
    try {
      Navigation.findNavController(it, R.id.fragmentContainerView)
    } catch (e: Exception) {
      //to prevent error in fragments which don't use navController
      //todo-denbond7 remove this after the full migration
      null
    }
  }

val androidx.fragment.app.Fragment.currentOnResultSavedStateHandle
  get() = navController?.currentBackStackEntry?.savedStateHandle

val androidx.fragment.app.Fragment.previousOnResultSavedStateHandle
  get() = navController?.previousBackStackEntry?.savedStateHandle

fun androidx.fragment.app.Fragment.toast(text: String?, duration: Int = Toast.LENGTH_SHORT) {
  text?.let { context?.toast(text, duration) }
}

fun <T> androidx.fragment.app.Fragment.setNavigationResult(key: String, value: T) {
  previousOnResultSavedStateHandle?.set(key, value)
}

fun androidx.fragment.app.Fragment.getOnResultSavedStateHandle(destinationId: Int? = null) =
  if (destinationId == null) {
    null
  } else {
    navController?.getBackStackEntry(destinationId)?.savedStateHandle
  }

fun <T> androidx.fragment.app.Fragment.getNavigationResult(
  key: String,
  onResult: (result: T) -> Unit
) {
  currentOnResultSavedStateHandle
    ?.getLiveData<T>(key)
    ?.observe(viewLifecycleOwner) {
      currentOnResultSavedStateHandle?.remove<T>(key)
      onResult.invoke(it)
    }
}

fun <T> androidx.fragment.app.Fragment.getNavigationResultForDialog(
  @IdRes destinationId: Int,
  key: String,
  onResult: (result: T) -> Unit
) {
  val navBackStackEntry = navController?.getBackStackEntry(destinationId) ?: return

  val observer = LifecycleEventObserver { _, event ->
    if (event == Lifecycle.Event.ON_RESUME && navBackStackEntry.savedStateHandle.contains(key)) {
      navBackStackEntry.savedStateHandle.get<T>(key)?.let(onResult)
      navBackStackEntry.savedStateHandle.remove<T>(key)
    }
  }
  navBackStackEntry.lifecycle.addObserver(observer)

  viewLifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
    if (event == Lifecycle.Event.ON_DESTROY) {
      navBackStackEntry.lifecycle.removeObserver(observer)
    }
  })
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

fun androidx.fragment.app.Fragment.showNeedPassphraseDialog(
  fingerprints: List<String>,
  requestCode: Int,
  logicType: FixNeedPassphraseIssueDialogFragment.LogicType
  = FixNeedPassphraseIssueDialogFragment.LogicType.AT_LEAST_ONE
) {
  val tag = FixNeedPassphraseIssueDialogFragment::class.java.simpleName
  if (parentFragmentManager.findFragmentByTag(tag) == null) {
    val fragment = FixNeedPassphraseIssueDialogFragment.newInstance(fingerprints, logicType)
    fragment.setTargetFragment(this, requestCode)
    fragment.show(parentFragmentManager, tag)
  }
}

fun androidx.fragment.app.Fragment.showInfoDialogWithExceptionDetails(
  e: Throwable?,
  msgDetails: String? = null
) {
  val msg =
    e?.message ?: e?.javaClass?.simpleName ?: msgDetails ?: getString(R.string.unknown_error)

  navController?.navigate(
    NavGraphDirections.actionGlobalInfoDialogFragment(
      dialogTitle = "",
      dialogMsg = msg
    )
  )
}

fun androidx.fragment.app.Fragment.showFeedbackFragment() {
  val screenShotByteArray = UIUtil.getScreenShotByteArray(requireActivity())
  screenShotByteArray?.let {
    navController?.navigate(
      NavGraphDirections.actionGlobalFeedbackFragment(
        FeedbackFragment.Screenshot(it)
      )
    )
  }
}
