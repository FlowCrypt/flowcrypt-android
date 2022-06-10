/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions

import android.net.Uri
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import androidx.test.espresso.idling.CountingIdlingResource
import com.flowcrypt.email.R
import com.flowcrypt.email.ui.activity.BaseActivity
import com.flowcrypt.email.ui.activity.fragment.base.UiUxSettings
import com.flowcrypt.email.ui.activity.fragment.dialog.ChoosePublicKeyDialogFragmentArgs
import com.flowcrypt.email.ui.activity.fragment.dialog.FindKeysInClipboardDialogFragmentArgs
import com.flowcrypt.email.ui.activity.fragment.dialog.FixNeedPassphraseIssueDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.ParsePgpKeysFromSourceDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.ParsePgpKeysFromSourceDialogFragmentArgs
import com.flowcrypt.email.util.FlavorSettings
import com.google.android.material.appbar.AppBarLayout

/**
 * @author Denis Bondarenko
 *         Date: 7/6/20
 *         Time: 4:16 PM
 *         E-mail: DenBond7@gmail.com
 */

val androidx.fragment.app.Fragment.appBarLayout: AppBarLayout?
  get() = activity?.findViewById(R.id.appBarLayout)

val androidx.fragment.app.Fragment.countingIdlingResource: CountingIdlingResource?
  get() = FlavorSettings.getCountingIdlingResource()

val androidx.fragment.app.Fragment.supportActionBar: ActionBar?
  get() = if (activity is AppCompatActivity) {
    (activity as AppCompatActivity).supportActionBar
  } else
    null

val androidx.fragment.app.Fragment.navController: NavController?
  get() = activity?.let {
    try {
      Navigation.findNavController(it, R.id.fragmentContainerView)
    } catch (e: Exception) {
      return@let null
    }
  }

val androidx.fragment.app.Fragment.currentOnResultSavedStateHandle
  get() = navController?.currentBackStackEntry?.savedStateHandle

val androidx.fragment.app.Fragment.previousOnResultSavedStateHandle
  get() = navController?.previousBackStackEntry?.savedStateHandle

fun androidx.fragment.app.Fragment.doBaseUISetup(uiUxSettings: UiUxSettings) {
  (activity as? BaseActivity<*>)?.setDrawerLockMode(uiUxSettings.isSideMenuLocked)
  appBarLayout?.visibleOrGone(uiUxSettings.isToolbarVisible)
  supportActionBar?.setDisplayHomeAsUpEnabled(uiUxSettings.isDisplayHomeAsUpEnabled)
  supportActionBar?.subtitle = null
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

fun androidx.fragment.app.Fragment.toast(text: String?, duration: Int = Toast.LENGTH_SHORT) {
  text?.let { context?.toast(text, duration) }
}

fun androidx.fragment.app.Fragment.toast(resId: Int, duration: Int = Toast.LENGTH_SHORT) {
  if (resId != -1) {
    context?.toast(resId, duration)
  }
}

fun androidx.fragment.app.Fragment.showInfoDialog(
  requestCode: Int = 0,
  dialogTitle: String? = null,
  dialogMsg: String? = null,
  buttonTitle: String? = null,
  isCancelable: Boolean = true,
  hasHtml: Boolean = false,
  useLinkify: Boolean = false,
  useWebViewToRender: Boolean = false
) {
  showInfoDialog(
    context = requireContext(),
    navController = navController,
    requestCode = requestCode,
    dialogTitle = dialogTitle,
    dialogMsg = dialogMsg,
    buttonTitle = buttonTitle,
    isCancelable = isCancelable,
    hasHtml = hasHtml,
    useLinkify = useLinkify,
    useWebViewToRender = useWebViewToRender
  )
}

fun androidx.fragment.app.Fragment.showTwoWayDialog(
  requestCode: Int = 0,
  dialogTitle: String? = null,
  dialogMsg: String? = null,
  positiveButtonTitle: String? = null,
  negativeButtonTitle: String? = null,
  isCancelable: Boolean = true,
  hasHtml: Boolean = false,
  useLinkify: Boolean = false
) {
  showTwoWayDialog(
    context = requireContext(),
    navController = navController,
    requestCode = requestCode,
    dialogTitle = dialogTitle,
    dialogMsg = dialogMsg,
    positiveButtonTitle = positiveButtonTitle,
    negativeButtonTitle = negativeButtonTitle,
    isCancelable = isCancelable,
    hasHtml = hasHtml,
    useLinkify = useLinkify
  )
}

fun androidx.fragment.app.Fragment.showNeedPassphraseDialog(
  fingerprints: List<String>,
  logicType: Long = FixNeedPassphraseIssueDialogFragment.LogicType.AT_LEAST_ONE
) {
  showNeedPassphraseDialog(navController, fingerprints, logicType)
}

fun androidx.fragment.app.Fragment.showInfoDialogWithExceptionDetails(
  e: Throwable?,
  msgDetails: String? = null
) {
  showInfoDialogWithExceptionDetails(
    context = requireContext(),
    navController = navController,
    throwable = e,
    msgDetails = msgDetails
  )
}

fun androidx.fragment.app.Fragment.showFeedbackFragment() {
  showFeedbackFragment(requireActivity(), navController)
}

fun androidx.fragment.app.Fragment.showFindKeysInClipboardDialogFragment(
  isPrivateKeyMode: Boolean
) {
  showDialogFragment(navController) {
    return@showDialogFragment object : NavDirections {
      override val actionId = R.id.find_keys_in_clipboard_dialog_graph
      override val arguments = FindKeysInClipboardDialogFragmentArgs(
        isPrivateKeyMode = isPrivateKeyMode
      ).toBundle()
    }
  }
}

fun androidx.fragment.app.Fragment.showParsePgpKeysFromSourceDialogFragment(
  source: String? = null,
  uri: Uri? = null,
  @ParsePgpKeysFromSourceDialogFragment.FilterType filterType: Long
) {
  showDialogFragment(navController) {
    return@showDialogFragment object : NavDirections {
      override val actionId = R.id.parse_keys_from_source_dialog_graph
      override val arguments = ParsePgpKeysFromSourceDialogFragmentArgs(
        source = source,
        uri = uri,
        filterType = filterType
      ).toBundle()
    }
  }
}

fun androidx.fragment.app.Fragment.showChoosePublicKeyDialogFragment(
  email: String,
  choiceMode: Int,
  titleResourceId: Int,
  returnResultImmediatelyIfSingle: Boolean = false
) {
  showDialogFragment(navController) {
    return@showDialogFragment object : NavDirections {
      override val actionId = R.id.choose_public_key_dialog_graph
      override val arguments = ChoosePublicKeyDialogFragmentArgs(
        email = email,
        choiceMode = choiceMode,
        titleResourceId = titleResourceId,
        returnResultImmediatelyIfSingle = returnResultImmediatelyIfSingle
      ).toBundle()
    }
  }
}
