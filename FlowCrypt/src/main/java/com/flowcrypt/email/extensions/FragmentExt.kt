/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import androidx.test.espresso.idling.CountingIdlingResource
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
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

fun androidx.fragment.app.Fragment.doBaseUISetup(uiUxSettings: UiUxSettings) {
  (activity as? BaseActivity<*>)?.setDrawerLockMode(uiUxSettings.isSideMenuLocked)
  appBarLayout?.visibleOrGone(uiUxSettings.isToolbarVisible)
  supportActionBar?.setDisplayHomeAsUpEnabled(uiUxSettings.isDisplayHomeAsUpEnabled)
  supportActionBar?.subtitle = null
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

fun androidx.fragment.app.Fragment.toast(text: String?, duration: Int = Toast.LENGTH_SHORT) {
  text?.let { context?.toast(text, duration) }
}

fun androidx.fragment.app.Fragment.toast(resId: Int, duration: Int = Toast.LENGTH_SHORT) {
  if (resId != -1) {
    context?.toast(resId, duration)
  }
}

fun androidx.fragment.app.Fragment.showInfoDialog(
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
  useWebViewToRender: Boolean = false
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
    useWebViewToRender = useWebViewToRender
  )
}

fun androidx.fragment.app.Fragment.showTwoWayDialog(
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
  useLinkify: Boolean = false
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
    useLinkify = useLinkify
  )
}

fun androidx.fragment.app.Fragment.setFragmentResultListenerForTwoWayDialog(
  listener: ((requestKey: String, bundle: Bundle) -> Unit)
) {
  val requestKey = GeneralUtil.generateUniqueExtraKey(
    Constants.REQUEST_KEY_BUTTON_CLICK,
    this::class.java
  )
  parentFragmentManager.setFragmentResultListener(requestKey, this, listener)
}

fun androidx.fragment.app.Fragment.setFragmentResultListenerForInfoDialog(
  listener: ((requestKey: String, bundle: Bundle) -> Unit)
) {
  val requestKey = GeneralUtil.generateUniqueExtraKey(
    Constants.REQUEST_KEY_INFO_BUTTON_CLICK,
    this::class.java
  )
  parentFragmentManager.setFragmentResultListener(requestKey, this, listener)
}

fun androidx.fragment.app.Fragment.showNeedPassphraseDialog(
  requestKey: String,
  fingerprints: List<String>,
  requestCode: Int = 0,
  logicType: Long = FixNeedPassphraseIssueDialogFragment.LogicType.AT_LEAST_ONE
) {
  showNeedPassphraseDialog(
    requestKey = requestKey,
    requestCode = requestCode,
    navController = navController,
    fingerprints = fingerprints,
    logicType = logicType
  )
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

fun androidx.fragment.app.Fragment.showParsePgpKeysFromSourceDialogFragment(
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

fun androidx.fragment.app.Fragment.showChoosePublicKeyDialogFragment(
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

inline fun androidx.fragment.app.Fragment.launchAndRepeatWithViewLifecycle(
  minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
  crossinline block: suspend CoroutineScope.() -> Unit
) {
  viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(minActiveState) {
      block()
    }
  }
}
