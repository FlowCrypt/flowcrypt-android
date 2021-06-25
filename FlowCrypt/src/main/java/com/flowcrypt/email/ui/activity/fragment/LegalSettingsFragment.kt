/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager.widget.ViewPager
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.google.android.material.tabs.TabLayout

/**
 * This [Fragment] consists information about a legal.
 *
 * @author DenBond7
 * Date: 26.05.2017
 * Time: 13:27
 * E-mail: DenBond7@gmail.com
 */
class LegalSettingsFragment : BaseFragment() {
  private var viewPager: ViewPager? = null
  private var tabLayout: TabLayout? = null

  override val contentResourceId: Int = R.layout.fragment_legal

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    supportActionBar?.title = getString(R.string.legal)
    initViews(view)
    updateViews()
  }

  private fun initViews(view: View) {
    viewPager = view.findViewById(R.id.viewPager)
    tabLayout = view.findViewById(R.id.tabLayout)
  }

  private fun updateViews() {
    viewPager?.adapter = TabPagerAdapter(childFragmentManager)
    tabLayout?.setupWithViewPager(viewPager)
  }

  /**
   * The fragment with [WebView] as the root view. The [WebView] initialized by a
   * html file from the assets directory.
   */
  class WebViewFragment : BaseFragment() {
    private var assetsPath: String? = null
    private var isRefreshEnabled: Boolean = false

    override val contentResourceId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      assetsPath = arguments?.getString(KEY_URL)
      isRefreshEnabled = arguments?.getBoolean(KEY_IS_REFRESH_ENABLED, false) ?: false
    }

    override fun onCreateView(
      inflater: LayoutInflater, container: ViewGroup?,
      savedInstanceState: Bundle?
    ): View? {

      return inflater.inflate(R.layout.swipe_to_refrech_with_webview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
      super.onViewCreated(view, savedInstanceState)
      val webView = view.findViewById<WebView>(R.id.webView)
      val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
      if (isRefreshEnabled) {
        swipeRefreshLayout?.setColorSchemeResources(
          R.color.colorPrimary,
          R.color.colorPrimary,
          R.color.colorPrimary
        )
        swipeRefreshLayout.setOnRefreshListener {
          assetsPath?.let { webView?.loadUrl(it) }
        }
      } else {
        swipeRefreshLayout.isEnabled = false
      }

      webView?.layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
      webView?.webViewClient = object : WebViewClient() {
        override fun onReceivedError(
          view: WebView?,
          request: WebResourceRequest?,
          error: WebResourceError?
        ) {
          if (error?.description == "net::ERR_INTERNET_DISCONNECTED") {
            webView?.loadUrl("file:///android_asset/html/no_connection.htm")
          } else {
            super.onReceivedError(view, request, error)
          }
        }

        override fun onPageFinished(view: WebView?, url: String?) {
          super.onPageFinished(view, url)
          swipeRefreshLayout.isRefreshing = false
        }
      }
      assetsPath?.let { webView?.loadUrl(it) }
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
  private inner class TabPagerAdapter(fragmentManager: FragmentManager) :
    FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(i: Int): Fragment {
      when (i) {
        TAB_POSITION_PRIVACY -> return WebViewFragment.newInstance(
          Uri.parse(
            Constants
              .FLOWCRYPT_PRIVACY_URL
          ), true
        )

        TAB_POSITION_TERMS -> return WebViewFragment.newInstance(
          Uri.parse(
            Constants
              .FLOWCRYPT_TERMS_URL
          ), true
        )

        TAB_POSITION_LICENCE -> return WebViewFragment.newInstance("html/license.htm")

        TAB_POSITION_SOURCES -> return WebViewFragment.newInstance("html/sources.htm")
      }

      return WebViewFragment.newInstance("")
    }

    override fun getCount(): Int {
      return 4
    }

    override fun getPageTitle(position: Int): CharSequence? {
      var title: String? = null
      when (position) {
        TAB_POSITION_PRIVACY -> title = getString(R.string.privacy)

        TAB_POSITION_TERMS -> title = getString(R.string.terms)

        TAB_POSITION_LICENCE -> title = getString(R.string.licence)

        TAB_POSITION_SOURCES -> title = getString(R.string.sources)
      }
      return title
    }
  }

  companion object {
    private const val TAB_POSITION_PRIVACY = 0
    private const val TAB_POSITION_TERMS = 1
    private const val TAB_POSITION_LICENCE = 2
    private const val TAB_POSITION_SOURCES = 3
  }
}
