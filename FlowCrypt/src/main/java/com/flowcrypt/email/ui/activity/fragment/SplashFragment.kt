/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.core.view.get
import com.flowcrypt.email.extensions.actionBar
import com.flowcrypt.email.extensions.mainActivity
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment

/**
 * @author Denis Bondarenko
 *         Date: 3/21/22
 *         Time: 8:13 PM
 *         E-mail: DenBond7@gmail.com
 */
class SplashFragment : BaseFragment() {
  override val contentResourceId: Int = -1

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)
  }

  override fun onPrepareOptionsMenu(menu: Menu) {
    super.onPrepareOptionsMenu(menu)
    //disable all menu items
    for (i in 0 until menu.size()) {
      menu[i].isVisible = false
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    actionBar?.setDisplayHomeAsUpEnabled(false)
    actionBar?.setHomeButtonEnabled(false)
    mainActivity?.setDrawerLockMode(true)
  }
}
