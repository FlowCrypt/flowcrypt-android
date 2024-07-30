/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.flowcrypt.email.extensions.android.webkit.setupDayNight
import com.flowcrypt.email.extensions.android.webkit.showUrlUsingChromeCustomTabs
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.ui.activity.CreateMessageActivity

/**
 * The custom realization of [WebView]
 *
 * @author Denys Bondarenko
 */
class EmailWebView : WebView {
  private var onPageLoadingListener: OnPageLoadingListener? = null

  constructor(context: Context) : super(context)

  constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
    context,
    attrs,
    defStyleAttr
  )

  /**
   * This method does job of configure the current [WebView]
   */
  fun configure() {
    isVerticalScrollBarEnabled = false
    scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
    overScrollMode = View.OVER_SCROLL_NEVER
    webViewClient = CustomWebClient(context)
    webChromeClient = object : WebChromeClient() {
      override fun onProgressChanged(view: WebView, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onPageLoadingListener?.onPageLoading(newProgress)
      }
    }

    val webSettings = this.settings

    webSettings.useWideViewPort = true
    webSettings.loadWithOverviewMode = true
    webSettings.setSupportZoom(true)
    webSettings.builtInZoomControls = true
    webSettings.displayZoomControls = false
    webSettings.loadsImagesAutomatically = true
    webSettings.blockNetworkLoads = true
    webSettings.javaScriptEnabled = false
    webSettings.allowFileAccess = false

    setupDayNight()
  }

  fun setOnPageLoadingListener(onPageLoadingListener: OnPageLoadingListener) {
    this.onPageLoadingListener = onPageLoadingListener
  }

  interface OnPageLoadingListener {
    fun onPageLoading(newProgress: Int)
  }

  /**
   * The custom realization of [WebViewClient]
   */
  private class CustomWebClient(private val context: Context) :
    WebViewClient() {

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
      return if (url.startsWith(SCHEME_MAILTO)) {
        handleEmailLinks(Uri.parse(url))
        false
      } else {
        showUrlUsingChromeCustomTabs(context = context, uri = Uri.parse(url))
        true
      }
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
      if ("mailto".equals(request.url.scheme, ignoreCase = true)) {
        handleEmailLinks(request.url)
      } else {
        showUrlUsingChromeCustomTabs(context = context, uri = request.url)
      }

      return true
    }

    /**
     * Handle email links and open the internal compose screen.
     *
     * @param uri [Uri] with mailto: scheme.
     */
    private fun handleEmailLinks(uri: Uri) {
      val intent = CreateMessageActivity.generateIntent(context, MessageType.NEW)
      intent.action = Intent.ACTION_SENDTO
      intent.data = uri
      context.startActivity(intent)
    }
  }
}
