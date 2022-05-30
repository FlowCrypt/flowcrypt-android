/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.databinding.FragmentHtmlViewFromAssetsRawBinding
import com.flowcrypt.email.extensions.supportActionBar
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment

/**
 * @author Denis Bondarenko
 *         Date: 3/8/22
 *         Time: 6:05 PM
 *         E-mail: DenBond7@gmail.com
 */
class HtmlViewFromAssetsRawFragment : BaseFragment<FragmentHtmlViewFromAssetsRawBinding>() {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentHtmlViewFromAssetsRawBinding.inflate(inflater, container, false)

  private val args by navArgs<HtmlViewFromAssetsRawFragmentArgs>()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    supportActionBar?.title = args.title
    binding?.webView?.loadUrl("file:///android_asset/" + args.resourceIdAsString)
  }
}
