/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions

import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.flowcrypt.email.R
import com.flowcrypt.email.util.GeneralUtil

/**
 * This class describes extension function for [FragmentActivity]
 *
 * @author Denys Bondarenko
 */

val FragmentActivity.navController: NavController
  get() = (supportFragmentManager.findFragmentById(R.id.fragmentContainerView)
      as NavHostFragment).navController

fun FragmentActivity.showFeedbackFragment() {
  showFeedbackFragment(this, navController)
}

fun FragmentActivity.showInfoDialog(
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
    context = this,
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

fun FragmentActivity.showTwoWayDialog(
  requestCode: Int = 0,
  requestKey: String? = GeneralUtil.generateUniqueExtraKey(
    "REQUEST_KEY_BUTTON_CLICK",
    this::class.java
  ),
  dialogTitle: String? = null,
  dialogMsg: String? = null,
  positiveButtonTitle: String? = null,
  negativeButtonTitle: String? = null,
  isCancelable: Boolean = true,
  hasHtml: Boolean = false,
  useLinkify: Boolean = false
) {
  showTwoWayDialog(
    context = this,
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
