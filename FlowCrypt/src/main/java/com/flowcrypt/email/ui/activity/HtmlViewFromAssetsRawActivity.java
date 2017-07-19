/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.webkit.WebView;

import com.flowcrypt.email.R;
import com.flowcrypt.email.ui.activity.base.BaseBackStackActivity;
import com.flowcrypt.email.util.GeneralUtil;

/**
 * This activity displays a html text from some source (from the assets folder).
 *
 * @author Denis Bondarenko
 *         Date: 19.07.2017
 *         Time: 18:13
 *         E-mail: DenBond7@gmail.com
 */

public class HtmlViewFromAssetsRawActivity extends BaseBackStackActivity {

    public static final String EXTRA_KEY_ACTIVITY_TITLE = GeneralUtil.generateUniqueExtraKey
            ("EXTRA_KEY_ACTIVITY_TITLE", HtmlViewFromAssetsRawActivity.class);
    public static final String EXTRA_KEY_HTML_RESOURCES_ID = GeneralUtil.generateUniqueExtraKey
            ("EXTRA_KEY_HTML_RESOURCES_ID", HtmlViewFromAssetsRawActivity.class);

    public static Intent newIntent(Context context, String title, String pathToHtmlInAssets) {
        Intent intent = new Intent(context, HtmlViewFromAssetsRawActivity.class);
        intent.putExtra(EXTRA_KEY_ACTIVITY_TITLE, title);
        intent.putExtra(EXTRA_KEY_HTML_RESOURCES_ID, pathToHtmlInAssets);
        return intent;
    }

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_htmlview_from_assets_raw;
    }

    @Override
    public View getRootView() {
        return null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null && getIntent().hasExtra(EXTRA_KEY_HTML_RESOURCES_ID)) {
            WebView webView = (WebView) findViewById(R.id.webView);
            webView.loadUrl("file:///android_asset/"
                    + getIntent().getStringExtra(EXTRA_KEY_HTML_RESOURCES_ID));

            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(getIntent().getStringExtra
                        (EXTRA_KEY_ACTIVITY_TITLE));
            }
        }
    }
}
