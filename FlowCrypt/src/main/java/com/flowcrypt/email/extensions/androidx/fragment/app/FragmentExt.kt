/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.androidx.fragment.app

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import androidx.test.espresso.idling.CountingIdlingResource
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.extensions.showDialogFragment
import com.flowcrypt.email.extensions.showFeedbackFragment
import com.flowcrypt.email.extensions.showInfoDialog
import com.flowcrypt.email.extensions.showInfoDialogWithExceptionDetails
import com.flowcrypt.email.extensions.showNeedPassphraseDialog
import com.flowcrypt.email.extensions.showTwoWayDialog
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.extensions.visibleOrGone
import com.flowcrypt.email.ui.activity.BaseActivity
import com.flowcrypt.email.ui.activity.fragment.base.UiUxSettings
import com.flowcrypt.email.ui.activity.fragment.dialog.ChoosePublicKeyDialogFragmentArgs
import com.flowcrypt.email.ui.activity.fragment.dialog.FindKeysInClipboardDialogFragmentArgs
import com.flowcrypt.email.ui.activity.fragment.dialog.FixNeedPassphraseIssueDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.ParsePgpKeysFromSourceDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.ParsePgpKeysFromSourceDialogFragmentArgs
import com.flowcrypt.email.util.FlavorSettings
import com.flowcrypt.email.util.GeneralUtil
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * @author Denys Bondarenko
 */

val Fragment.appBarLayout: AppBarLayout?
  get() = activity?.findViewById(R.id.appBarLayout)

val Fragment.countingIdlingResource: CountingIdlingResource?
  get() = FlavorSettings.getCountingIdlingResource()

val Fragment.supportActionBar: ActionBar?
  get() = if (activity is AppCompatActivity) {
    (activity as AppCompatActivity).supportActionBar
  } else
    null

val Fragment.navController: NavController?
  get() = activity?.let {
    try {
      Navigation.findNavController(it, R.id.fragmentContainerView)
    } catch (e: Exception) {
      return@let null
    }
  }

val Fragment.currentOnResultSavedStateHandle
  get() = navController?.currentBackStackEntry?.savedStateHandle

fun Fragment.doBaseUISetup(uiUxSettings: UiUxSettings) {
  (activity as? BaseActivity<*>)?.setDrawerLockMode(uiUxSettings.isSideMenuLocked)
  appBarLayout?.visibleOrGone(uiUxSettings.isToolbarVisible)
  supportActionBar?.setDisplayHomeAsUpEnabled(uiUxSettings.isDisplayHomeAsUpEnabled)
  supportActionBar?.subtitle = null
}

fun Fragment.getOnResultSavedStateHandle(destinationId: Int? = null) =
  if (destinationId == null) {
    null
  } else {
    navController?.getBackStackEntry(destinationId)?.savedStateHandle
  }

fun <T> Fragment.getNavigationResult(
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

fun Fragment.toast(text: String?, duration: Int = Toast.LENGTH_SHORT) {
  text?.let { context?.toast(text, duration) }
}

fun Fragment.toast(resId: Int, duration: Int = Toast.LENGTH_SHORT) {
  if (resId != -1) {
    context?.toast(resId, duration)
  }
}

fun Fragment.showInfoDialog(
  requestKey: String? = GeneralUtil.generateUniqueExtraKey(
    Constants.REQUEST_KEY_INFO_BUTTON_CLICK,
    this::class.java
  ),
  requestCode: Int = 0,
  dialogTitle: String? = null,
  dialogMsg: String? = null,
  buttonTitle: String? = null,
  isCancelable: Boolean = true,
  hasHtml: Boolean = false,
  useLinkify: Boolean = false,
  useWebViewToRender: Boolean = false,
  bundle: Bundle? = null
) {
  showInfoDialog(
    context = requireContext(),
    navController = navController,
    requestKey = requestKey,
    requestCode = requestCode,
    dialogTitle = dialogTitle,
    dialogMsg = dialogMsg,
    buttonTitle = buttonTitle,
    isCancelable = isCancelable,
    hasHtml = hasHtml,
    useLinkify = useLinkify,
    useWebViewToRender = useWebViewToRender,
    bundle = bundle
  )
}

fun Fragment.showTwoWayDialog(
  requestKey: String? = GeneralUtil.generateUniqueExtraKey(
    Constants.REQUEST_KEY_BUTTON_CLICK,
    this::class.java
  ),
  requestCode: Int = 0,
  dialogTitle: String? = null,
  dialogMsg: String? = null,
  positiveButtonTitle: String? = null,
  negativeButtonTitle: String? = null,
  isCancelable: Boolean = true,
  hasHtml: Boolean = false,
  useLinkify: Boolean = false,
  bundle: Bundle? = null
) {
  showTwoWayDialog(
    context = requireContext(),
    navController = navController,
    requestKey = requestKey,
    requestCode = requestCode,
    dialogTitle = dialogTitle,
    dialogMsg = dialogMsg,
    positiveButtonTitle = positiveButtonTitle,
    negativeButtonTitle = negativeButtonTitle,
    isCancelable = isCancelable,
    hasHtml = hasHtml,
    useLinkify = useLinkify,
    bundle = bundle
  )
}

fun Fragment.setFragmentResultListenerForTwoWayDialog(
  requestKey: String = GeneralUtil.generateUniqueExtraKey(
    Constants.REQUEST_KEY_BUTTON_CLICK,
    this::class.java
  ),
  useSuperParentFragmentManagerIfPossible: Boolean = false,
  listener: ((requestKey: String, bundle: Bundle) -> Unit)
) {
  setFragmentResultListener(requestKey, useSuperParentFragmentManagerIfPossible, listener)
}

fun Fragment.setFragmentResultListenerForInfoDialog(
  useSuperParentFragmentManagerIfPossible: Boolean = false,
  listener: ((requestKey: String, bundle: Bundle) -> Unit)
) {
  val requestKey = GeneralUtil.generateUniqueExtraKey(
    Constants.REQUEST_KEY_INFO_BUTTON_CLICK,
    this::class.java
  )
  setFragmentResultListener(requestKey, useSuperParentFragmentManagerIfPossible, listener)
}

fun Fragment.showNeedPassphraseDialog(
  requestKey: String,
  fingerprints: List<String>,
  requestCode: Int = Int.MIN_VALUE,
  logicType: Long = FixNeedPassphraseIssueDialogFragment.LogicType.AT_LEAST_ONE,
  bundle: Bundle? = null
) {
  showNeedPassphraseDialog(
    requestKey = requestKey,
    requestCode = requestCode,
    navController = navController,
    fingerprints = fingerprints,
    logicType = logicType,
    bundle = bundle
  )
}

fun Fragment.showInfoDialogWithExceptionDetails(
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

fun Fragment.showFeedbackFragment() {
  showFeedbackFragment(requireActivity(), navController)
}

fun Fragment.showFindKeysInClipboardDialogFragment(
  requestKey: String,
  isPrivateKeyMode: Boolean
) {
  showDialogFragment(navController) {
    return@showDialogFragment object : NavDirections {
      override val actionId = R.id.find_keys_in_clipboard_dialog_graph
      override val arguments = FindKeysInClipboardDialogFragmentArgs(
        requestKey = requestKey,
        isPrivateKeyMode = isPrivateKeyMode
      ).toBundle()
    }
  }
}

fun Fragment.showParsePgpKeysFromSourceDialogFragment(
  requestKey: String,
  source: String? = null,
  uri: Uri? = null,
  @ParsePgpKeysFromSourceDialogFragment.FilterType filterType: Long
) {
  showDialogFragment(navController) {
    return@showDialogFragment object : NavDirections {
      override val actionId = R.id.parse_keys_from_source_dialog_graph
      override val arguments = ParsePgpKeysFromSourceDialogFragmentArgs(
        requestKey = requestKey,
        source = source,
        uri = uri,
        filterType = filterType
      ).toBundle()
    }
  }
}

fun Fragment.showChoosePublicKeyDialogFragment(
  requestKey: String,
  email: String,
  choiceMode: Int,
  titleResourceId: Int,
  returnResultImmediatelyIfSingle: Boolean = false
) {
  showDialogFragment(navController) {
    return@showDialogFragment object : NavDirections {
      override val actionId = R.id.choose_public_key_dialog_graph
      override val arguments = ChoosePublicKeyDialogFragmentArgs(
        requestKey = requestKey,
        email = email,
        choiceMode = choiceMode,
        titleResourceId = titleResourceId,
        returnResultImmediatelyIfSingle = returnResultImmediatelyIfSingle
      ).toBundle()
    }
  }
}

fun Fragment.setFragmentResultListener(
  requestKey: String,
  useSuperParentFragmentManagerIfPossible: Boolean = false,
  listener: ((requestKey: String, bundle: Bundle) -> Unit)
) {
  if (useSuperParentFragmentManagerIfPossible && parentFragment?.parentFragmentManager != null) {
    parentFragment?.parentFragmentManager?.setFragmentResultListener(requestKey, this, listener)
  } else {
    setFragmentResultListener(requestKey, listener)
  }
}

inline fun Fragment.launchAndRepeatWithViewLifecycle(
  minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
  crossinline block: suspend CoroutineScope.() -> Unit
) {
  viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(minActiveState) {
      block()
    }
  }
}
