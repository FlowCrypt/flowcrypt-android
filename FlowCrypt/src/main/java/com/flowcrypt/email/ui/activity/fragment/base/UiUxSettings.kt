/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.base

import androidx.core.view.MenuHost

/**
 * @author Denys Bondarenko
 */
interface UiUxSettings {
  val isToolbarVisible: Boolean
    get() = true

  val isDisplayHomeAsUpEnabled: Boolean
    get() = true

  val isSideMenuLocked: Boolean
    get() = true

  fun onSetupActionBarMenu(menuHost: MenuHost) {}

  fun getLoggingTag(): String = javaClass.simpleName + "_" + hashCode()
}
