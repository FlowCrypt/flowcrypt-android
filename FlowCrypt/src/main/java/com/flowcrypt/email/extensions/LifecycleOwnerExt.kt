/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import com.flowcrypt.email.R
import com.flowcrypt.email.ui.activity.fragment.FeedbackFragment
import com.flowcrypt.email.ui.activity.fragment.FeedbackFragmentArgs
import com.flowcrypt.email.ui.activity.fragment.dialog.FixNeedPassphraseIssueDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.FixNeedPassphraseIssueDialogFragmentArgs
import com.flowcrypt.email.ui.activity.fragment.dialog.InfoDialogFragmentArgs
import com.flowcrypt.email.ui.activity.fragment.dialog.TwoWayDialogFragmentArgs
import com.flowcrypt.email.util.UIUtil

/**
 * @author Denis Bondarenko
 *         Date: 4/26/22
 *         Time: 4:11 PM
 *         E-mail: DenBond7@gmail.com
 */
fun LifecycleOwner.showInfoDialog(
  context: Context,
  navController: NavController?,
  requestCode: Int = 0,
  dialogTitle: String? = null,
  dialogMsg: String? = null,
  buttonTitle: String? = null,
  isCancelable: Boolean = true,
  hasHtml: Boolean = false,
  useLinkify: Boolean = false,
  useWebViewToRender: Boolean = false
) {
  //to show the current dialog we should be sure there is no active dialogs
  if (navController?.currentDestination?.navigatorName == "dialog") {
    navController.navigateUp()
  }

  val navDirections = object : NavDirections {
    override val actionId: Int = R.id.info_dialog_graph
    override val arguments: Bundle = InfoDialogFragmentArgs(
      requestCode = requestCode,
      dialogTitle = dialogTitle,
      dialogMsg = dialogMsg,
      buttonTitle = buttonTitle ?: context.getString(android.R.string.ok),
      isCancelable = isCancelable,
      hasHtml = hasHtml,
      useLinkify = useLinkify,
      useWebViewToRender = useWebViewToRender
    ).toBundle()
  }

  navController?.navigate(navDirections)
}

fun LifecycleOwner.showTwoWayDialog(
  context: Context,
  navController: NavController?,
  requestCode: Int = 0,
  dialogTitle: String? = null,
  dialogMsg: String? = null,
  positiveButtonTitle: String? = null,
  negativeButtonTitle: String? = null,
  isCancelable: Boolean = true,
  hasHtml: Boolean = false,
  useLinkify: Boolean = false
) {
  //to show the current dialog we should be sure there is no active dialogs
  if (navController?.currentDestination?.navigatorName == "dialog") {
    navController.navigateUp()
  }

  val navDirections = object : NavDirections {
    override val actionId: Int = R.id.two_way_dialog_graph
    override val arguments: Bundle = TwoWayDialogFragmentArgs(
      requestCode = requestCode,
      dialogTitle = dialogTitle,
      dialogMsg = dialogMsg,
      positiveButtonTitle = positiveButtonTitle ?: context.getString(android.R.string.ok),
      negativeButtonTitle = negativeButtonTitle ?: context.getString(android.R.string.cancel),
      isCancelable = isCancelable,
      hasHtml = hasHtml,
      useLinkify = useLinkify
    ).toBundle()
  }

  navController?.navigate(navDirections)
}

fun LifecycleOwner.showNeedPassphraseDialog(
  navController: NavController?,
  fingerprints: List<String>,
  logicType: Long = FixNeedPassphraseIssueDialogFragment.LogicType.AT_LEAST_ONE,
  requestCode: Int = 0,
  customTitle: String? = null,
  showKeys: Boolean = true
) {
  if (navController?.currentDestination?.navigatorName == "dialog") {
    navController.navigateUp()
  }

  val navDirections = object : NavDirections {
    override val actionId: Int = R.id.fix_need_pass_phrase_dialog_graph
    override val arguments: Bundle = FixNeedPassphraseIssueDialogFragmentArgs(
      fingerprints = fingerprints.toTypedArray(),
      logicType = logicType,
      requestCode = requestCode,
      customTitle = customTitle,
      showKeys = showKeys
    ).toBundle()
  }

  navController?.navigate(navDirections)
}

fun LifecycleOwner.showInfoDialogWithExceptionDetails(
  context: Context,
  navController: NavController?,
  throwable: Throwable?,
  msgDetails: String? = null
) {
  val msg =
    throwable?.message ?: throwable?.javaClass?.simpleName ?: msgDetails
    ?: context.getString(R.string.unknown_error)

  showInfoDialog(
    navController = navController,
    context = context,
    dialogTitle = "",
    dialogMsg = msg
  )
}

fun LifecycleOwner.showFeedbackFragment(
  activity: Activity,
  navController: NavController?
) {
  val screenShotByteArray = UIUtil.getScreenShotByteArray(activity)
  screenShotByteArray?.let {
    val navDirections = object : NavDirections {
      override val actionId: Int = R.id.feedback_graph
      override val arguments: Bundle = FeedbackFragmentArgs(
        screenshot = FeedbackFragment.Screenshot(it)
      ).toBundle()
    }
    navController?.navigate(navDirections)
  }
}
