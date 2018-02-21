/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.widget;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.flowcrypt.email.R;

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
        setWebViewClient(new CustomWebClient(getContext()));

        WebSettings webSettings = this.getSettings();

        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setJavaScriptEnabled(false);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
    }

    /**
     * The custom realization of {@link WebViewClient}
     */
    private static class CustomWebClient extends WebViewClient {
        private Context context;

        CustomWebClient(Context context) {
            this.context = context;
        }

        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            showUrlUsingChromeCustomTabs(Uri.parse(url));
            return true;
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            showUrlUsingChromeCustomTabs(request.getUrl());
            return true;
        }

        /**
         * Use {@link CustomTabsIntent} to show some url.
         *
         * @param uri The {@link Uri} which contains a url.
         */
        private void showUrlUsingChromeCustomTabs(Uri uri) {
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            CustomTabsIntent customTabsIntent = builder.build();
            builder.setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimary));
            customTabsIntent.launchUrl(context, uri);
        }
    }
}
