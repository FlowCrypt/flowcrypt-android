/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.databinding.FragmentLegalBinding
import com.flowcrypt.email.databinding.SwipeToRefrechWithWebviewBinding
import com.flowcrypt.email.extensions.android.webkit.setupDayNight
import com.flowcrypt.email.extensions.android.webkit.showUrlUsingChromeCustomTabs
import com.flowcrypt.email.extensions.androidx.viewpager2.widget.reduceDragSensitivity
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.google.android.material.tabs.TabLayoutMediator

/**
 * This [Fragment] consists information about a legal.
 *
 * @author Denys Bondarenko
 */
class LegalSettingsFragment : BaseFragment<FragmentLegalBinding>() {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentLegalBinding.inflate(inflater, container, false)

  private lateinit var adapter: TabPagerAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    adapter = TabPagerAdapter(this)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()
  }

  private fun initViews() {
    binding?.viewPager2?.adapter = adapter
    binding?.viewPager2?.reduceDragSensitivity()
    binding?.tabLayout?.let { tabLayout ->
      binding?.viewPager2?.let { viewPager2 ->
        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
          tab.text = when (position) {
            TAB_POSITION_PRIVACY -> getString(R.string.privacy)

            TAB_POSITION_TERMS -> getString(R.string.terms)

            TAB_POSITION_LICENCE -> getString(R.string.licence)

            TAB_POSITION_SOURCES -> getString(R.string.sources)
            else -> ""
          }
        }.attach()
      }
    }
  }

  /**
   * The fragment with [WebView] as the root view. The [WebView] initialized by a
   * html file from the assets directory.
   */
  class WebViewFragment : BaseFragment<SwipeToRefrechWithWebviewBinding>() {
    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
      SwipeToRefrechWithWebviewBinding.inflate(inflater, container, false)

    private var assetsPath: String? = null
    private var isRefreshEnabled: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      assetsPath = arguments?.getString(KEY_URL)
      isRefreshEnabled = arguments?.getBoolean(KEY_IS_REFRESH_ENABLED, false) == true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
      super.onViewCreated(view, savedInstanceState)
      binding?.swipeRefreshLayout?.apply {
        if (isRefreshEnabled) {
          setColorSchemeResources(
            R.color.colorPrimary,
            R.color.colorPrimary,
            R.color.colorPrimary
          )
          setOnRefreshListener {
            assetsPath?.let { binding?.webView?.loadUrl(it) }
          }
        } else {
          isEnabled = false
        }
      }

      binding?.webView?.apply {
        layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT
        )

        webViewClient = object : WebViewClient() {
          override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
          ) {
            if (error?.description == "net::ERR_INTERNET_DISCONNECTED") {
              binding?.webView?.loadUrl("file:///android_asset/html/no_connection.htm")
            } else {
              super.onReceivedError(view, request, error)
            }
          }

          override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            binding?.swipeRefreshLayout?.isRefreshing = false
          }

          @Deprecated("Deprecated in Java", ReplaceWith("true"))
          override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            showUrlUsingChromeCustomTabs(context = context, uri = url.toUri())
            return true
          }

          override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest
          ): Boolean {
            showUrlUsingChromeCustomTabs(context = context, uri = request.url)
            return true
          }
        }

        assetsPath?.let {
          this.setupDayNight()
          this.loadUrl(it)
        }
      }
    }

    companion object {
      internal const val KEY_URL = BuildConfig.APPLICATION_ID + ".KEY_URL"
      internal const val KEY_IS_REFRESH_ENABLED =
        BuildConfig.APPLICATION_ID + ".KEY_IS_REFRESH_ENABLED"

      /**
       * Generate an instance of the [WebViewFragment].
       *
       * @param assetsPath The path to a html in the assets directory.
       * @param isRefreshEnabled If true a user will be able to update a tab.
       * @return <tt>[WebViewFragment]</tt>
       */
      fun newInstance(assetsPath: String, isRefreshEnabled: Boolean = false): WebViewFragment {
        val args = Bundle()
        args.putString(KEY_URL, "file:///android_asset/$assetsPath")
        args.putBoolean(KEY_IS_REFRESH_ENABLED, isRefreshEnabled)

        val webViewFragment = WebViewFragment()
        webViewFragment.arguments = args
        return webViewFragment
      }

      /**
       * Generate an instance of the [WebViewFragment].
       *
       * @param uri The [Uri] which contains info about a URL.
       * @param isRefreshEnabled If true a user will be able to update a tab.
       * @return <tt>[WebViewFragment]</tt>
       */
      fun newInstance(uri: Uri, isRefreshEnabled: Boolean = false): WebViewFragment {
        val args = Bundle()
        args.putString(KEY_URL, uri.toString())
        args.putBoolean(KEY_IS_REFRESH_ENABLED, isRefreshEnabled)

        val webViewFragment = WebViewFragment()
        webViewFragment.arguments = args
        return webViewFragment
      }
    }
  }

  /**
   * The adapter which contains information about tabs.
   */
  private inner class TabPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun createFragment(position: Int): Fragment {
      when (position) {
        TAB_POSITION_PRIVACY -> return WebViewFragment.newInstance(
          Constants.FLOWCRYPT_PRIVACY_URL.toUri(), true
        )

        TAB_POSITION_TERMS -> return WebViewFragment.newInstance(
          Constants.FLOWCRYPT_TERMS_URL.toUri(), true
        )

        TAB_POSITION_LICENCE -> return WebViewFragment.newInstance("html/license.htm")

        TAB_POSITION_SOURCES -> return WebViewFragment.newInstance("html/sources.htm")
      }

      return WebViewFragment.newInstance("")
    }

    override fun getItemCount(): Int = 4
  }

  companion object {
    private const val TAB_POSITION_PRIVACY = 0
    private const val TAB_POSITION_TERMS = 1
    private const val TAB_POSITION_LICENCE = 2
    private const val TAB_POSITION_SOURCES = 3
  }
}
