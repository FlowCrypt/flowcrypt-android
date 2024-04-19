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
import com.flowcrypt.email.extensions.android.webkit.setupDayNight
import com.flowcrypt.email.extensions.androidx.fragment.app.supportActionBar
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment

/**
 * @author Denys Bondarenko
 */
class HtmlViewFromAssetsRawFragment : BaseFragment<FragmentHtmlViewFromAssetsRawBinding>() {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentHtmlViewFromAssetsRawBinding.inflate(inflater, container, false)

  private val args by navArgs<HtmlViewFromAssetsRawFragmentArgs>()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    supportActionBar?.title = args.title

    binding?.webView?.setupDayNight()
    binding?.webView?.loadUrl("file:///android_asset/" + args.resourceIdAsString)
  }
}
