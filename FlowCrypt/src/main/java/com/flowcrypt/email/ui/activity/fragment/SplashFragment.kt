/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.ViewGroup
import androidx.core.view.get
import com.flowcrypt.email.databinding.FragmentSplashBinding
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment

/**
 * @author Denis Bondarenko
 *         Date: 3/21/22
 *         Time: 8:13 PM
 *         E-mail: DenBond7@gmail.com
 */
class SplashFragment : BaseFragment<FragmentSplashBinding>() {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentSplashBinding.inflate(inflater, container, false)

  override val isDisplayHomeAsUpEnabled = false
  override val isToolbarVisible: Boolean = false

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
}
