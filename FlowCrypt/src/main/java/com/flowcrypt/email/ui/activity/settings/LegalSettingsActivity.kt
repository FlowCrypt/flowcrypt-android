/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.google.android.material.tabs.TabLayout

/**
 * This Activity consists information about a legal.
 *
 * @author DenBond7
 * Date: 26.05.2017
 * Time: 13:27
 * E-mail: DenBond7@gmail.com
 */

class LegalSettingsActivity : BaseSettingsActivity() {

  private var tabPagerAdapter: TabPagerAdapter? = null
  private var viewPager: ViewPager? = null
  private var tabLayout: TabLayout? = null

  override val contentViewResourceId: Int
    get() = R.layout.activity_legal

  override val rootView: View
    get() = View(this)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initViews()
    updateViews()
  }

  private fun initViews() {
    viewPager = findViewById(R.id.viewPager)
    tabLayout = findViewById(R.id.tabLayout)
  }

  private fun updateViews() {
    if (tabPagerAdapter == null) {
      tabPagerAdapter = TabPagerAdapter(supportFragmentManager)
    }
    viewPager!!.adapter = tabPagerAdapter
    tabLayout!!.setupWithViewPager(viewPager)
  }

  /**
   * The fragment with [WebView] as the root view. The [WebView] initialized by a
   * html file from the assets directory.
   */
  class WebViewFragment : BaseFragment() {
    private var assetsPath: String? = null
    private lateinit var webView: WebView

    override val contentResourceId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      val args = arguments
      if (args != null) {
        this.assetsPath = args.getString(KEY_URL)
      }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

      webView = WebView(context)
      webView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT)

      return webView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
      super.onViewCreated(view, savedInstanceState)

      if (::webView.isInitialized) {
        webView.loadUrl(assetsPath!!)
      }
    }

    companion object {
      internal const val KEY_URL = BuildConfig.APPLICATION_ID + ".KEY_URL"

      /**
       * Generate an instance of the [WebViewFragment].
       *
       * @param assetsPath The path to a html in the assets directory.
       * @return <tt>[WebViewFragment]</tt>
       */
      @JvmStatic
      fun newInstance(assetsPath: String): WebViewFragment {
        val args = Bundle()
        args.putString(KEY_URL, "file:///android_asset/$assetsPath")

        val webViewFragment = WebViewFragment()
        webViewFragment.arguments = args
        return webViewFragment
      }

      /**
       * Generate an instance of the [WebViewFragment].
       *
       * @param uri The [Uri] which contains info about a URL.
       * @return <tt>[WebViewFragment]</tt>
       */
      @JvmStatic
      fun newInstance(uri: Uri): WebViewFragment {
        val args = Bundle()
        args.putString(KEY_URL, uri.toString())

        val webViewFragment = WebViewFragment()
        webViewFragment.arguments = args
        return webViewFragment
      }
    }
  }

  /**
   * The adapter which contains information about tabs.
   */
  private inner class TabPagerAdapter internal constructor(fragmentManager: FragmentManager)
    : FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(i: Int): Fragment {
      when (i) {
        TAB_POSITION_PRIVACY -> return WebViewFragment.newInstance(Uri.parse(Constants.FLOWCRYPT_PRIVACY_URL))

        TAB_POSITION_TERMS -> return WebViewFragment.newInstance(Uri.parse(Constants.FLOWCRYPT_TERMS_URL))

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
