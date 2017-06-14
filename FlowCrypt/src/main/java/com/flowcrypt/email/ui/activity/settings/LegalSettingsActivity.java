/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org). Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/tree/master/src/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.R;
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment;

/**
 * This Activity consists information about a legal.
 *
 * @author DenBond7
 *         Date: 26.05.2017
 *         Time: 13:27
 *         E-mail: DenBond7@gmail.com
 */

public class LegalSettingsActivity extends BaseSettingsActivity {
    private static final int TAB_POSITION_PRIVACY = 0;
    private static final int TAB_POSITION_TERMS = 1;
    private static final int TAB_POSITION_LICENCE = 2;
    private static final int TAB_POSITION_SOURCES = 3;

    private TabPagerAdapter tabPagerAdapter;
    private ViewPager viewPager;
    private TabLayout tabLayout;

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_legal;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initViews();
        updateViews();
    }

    private void initViews() {
        viewPager = (ViewPager) findViewById(R.id.viewPager);
        tabLayout = (TabLayout) findViewById(R.id.tabLayout);
    }

    private void updateViews() {
        if (tabPagerAdapter == null) {
            tabPagerAdapter = new TabPagerAdapter(getSupportFragmentManager());
        }
        viewPager.setAdapter(tabPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
    }

    /**
     * The fragment with {@link WebView} as the root view. The {@link WebView} initialized by a
     * html file from the assets directory.
     */
    public static class WebViewFragment extends BaseFragment {
        public static final String KEY_ASSETS_PATH =
                BuildConfig.APPLICATION_ID + "" + ".KEY_ASSETS_PATH";
        private String assetsPath;
        private WebView webView;

        /**
         * Generate an instance of the {@link WebViewFragment}.
         *
         * @param assetsPath The path to a html in the assets directory.
         * @return <tt>{@link WebViewFragment}</tt>
         */
        public static WebViewFragment newInstance(String assetsPath) {
            Bundle args = new Bundle();
            args.putString(KEY_ASSETS_PATH, assetsPath);

            WebViewFragment webViewFragment = new WebViewFragment();
            webViewFragment.setArguments(args);
            return webViewFragment;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Bundle args = getArguments();
            if (args != null) {
                this.assetsPath = args.getString(KEY_ASSETS_PATH);
            }
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {

            webView = new WebView(getContext());
            webView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams
                    .MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            return webView;
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            if (webView != null) {
                webView.loadUrl("file:///android_asset/" + assetsPath);
            }
        }
    }

    /**
     * The adapter which contains information about tabs.
     */
    private class TabPagerAdapter extends FragmentStatePagerAdapter {
        private static final int TAB_COUNT = 4;

        TabPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case TAB_POSITION_PRIVACY:
                    return WebViewFragment.newInstance("html/privacy.htm");

                case TAB_POSITION_TERMS:
                    return WebViewFragment.newInstance("html/terms.htm");

                case TAB_POSITION_LICENCE:
                    return WebViewFragment.newInstance("html/license.htm");

                case TAB_POSITION_SOURCES:
                    return WebViewFragment.newInstance("html/sources.htm");
            }

            return null;
        }

        @Override
        public int getCount() {
            return TAB_COUNT;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            String title = null;
            switch (position) {
                case TAB_POSITION_PRIVACY:
                    title = getString(R.string.privacy);
                    break;

                case TAB_POSITION_TERMS:
                    title = getString(R.string.terms);
                    break;

                case TAB_POSITION_LICENCE:
                    title = getString(R.string.licence);
                    break;

                case TAB_POSITION_SOURCES:
                    title = getString(R.string.sources);
                    break;
            }
            return title;
        }
    }
}
