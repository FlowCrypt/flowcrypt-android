/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions

import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.flowcrypt.email.R

/**
 * This class describes extension function for [FragmentActivity]
 *
 * @author Denis Bondarenko
 *         Date: 11/22/19
 *         Time: 3:37 PM
 *         E-mail: DenBond7@gmail.com
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
