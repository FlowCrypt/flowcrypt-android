/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions.androidx.navigation

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.Navigator

/**
 * @author Denys Bondarenko
 */
fun androidx.navigation.NavController.navigateSafe(
  @IdRes currentDestinationId: Int,
  directions: NavDirections
) {
  navigateSafe(currentDestinationId, directions.actionId, directions.arguments, null)
}

fun androidx.navigation.NavController.navigateSafe(
  @IdRes currentDestinationId: Int,
  @IdRes resId: Int,
  args: Bundle? = null,
  navOptions: NavOptions? = null,
  navigatorExtras: Navigator.Extras? = null
) {
  if (currentDestination?.id == currentDestinationId) {
    navigate(resId, args, navOptions, navigatorExtras)
  }
}
