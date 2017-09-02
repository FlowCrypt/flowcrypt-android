/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * The custom realization of {@link WebView}
 *
 * @author Denis Bondarenko
 *         Date: 02.09.2017
 *         Time: 12:19
 *         E-mail: DenBond7@gmail.com
 */

public class EmailWebView extends WebView {
    public EmailWebView(Context context) {
        super(context);
    }

    public EmailWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EmailWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * This method does job of configure the current {@link WebView}
     */
    public void configure() {
        setVerticalScrollBarEnabled(false);
        setScrollBarStyle(SCROLLBARS_INSIDE_OVERLAY);
        setOverScrollMode(OVER_SCROLL_NEVER);
        setWebViewClient(new WebViewClient());

        WebSettings webSettings = this.getSettings();
        /*webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);*/
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setJavaScriptEnabled(false);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
    }
}
