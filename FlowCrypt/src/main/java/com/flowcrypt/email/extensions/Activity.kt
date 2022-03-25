/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions

import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.flowcrypt.email.NavGraphDirections
import com.flowcrypt.email.R
import com.flowcrypt.email.ui.activity.fragment.FeedbackFragment
import com.flowcrypt.email.util.UIUtil

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
  val screenShotByteArray = UIUtil.getScreenShotByteArray(this)
  screenShotByteArray?.let {
    navController.navigate(
      NavGraphDirections.actionGlobalFeedbackFragment(
        FeedbackFragment.Screenshot(it)
      )
    )
  }
}
