/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebView

import com.flowcrypt.email.R
import com.flowcrypt.email.ui.activity.base.BaseBackStackActivity
import com.flowcrypt.email.util.GeneralUtil

/**
 * This activity displays a html text from some source (from the assets folder).
 *
 * @author Denis Bondarenko
 * Date: 19.07.2017
 * Time: 18:13
 * E-mail: DenBond7@gmail.com
 */

class HtmlViewFromAssetsRawActivity : BaseBackStackActivity() {

  override val contentViewResourceId: Int
    get() = R.layout.activity_htmlview_from_assets_raw

  override val rootView: View
    get() = View(this)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (intent?.hasExtra(EXTRA_KEY_HTML_RESOURCES_ID) == true) {
      supportActionBar?.title = intent.getStringExtra(EXTRA_KEY_ACTIVITY_TITLE)

      val webView = findViewById<WebView>(R.id.webView)
      webView.loadUrl("file:///android_asset/" + intent.getStringExtra(EXTRA_KEY_HTML_RESOURCES_ID))
    }
  }

  companion object {

    val EXTRA_KEY_ACTIVITY_TITLE =
        GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_ACTIVITY_TITLE", HtmlViewFromAssetsRawActivity::class.java)
    val EXTRA_KEY_HTML_RESOURCES_ID =
        GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_HTML_RESOURCES_ID", HtmlViewFromAssetsRawActivity::class.java)

    fun newIntent(context: Context, title: String, pathToHtmlInAssets: String): Intent {
      val intent = Intent(context, HtmlViewFromAssetsRawActivity::class.java)
      intent.putExtra(EXTRA_KEY_ACTIVITY_TITLE, title)
      intent.putExtra(EXTRA_KEY_HTML_RESOURCES_ID, pathToHtmlInAssets)
      return intent
    }
  }
}
