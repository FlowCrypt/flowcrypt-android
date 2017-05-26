package com.flowcrypt.email.ui.activity.base;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.flowcrypt.email.R;

/**
 * The base back stack activity. In this activity we add the back stack functionality. The
 * extended class must implement {@link BaseBackStackActivity#getContentViewResourceId()} method
 * to define the content view resources id. And the in {@link Activity#onCreate(Bundle)} method
 * we setup the toolbar if it exist in the contents and call
 * {@link android.support.v7.app.ActionBar#setDisplayHomeAsUpEnabled(boolean)} to implement the
 * back stack functionality.
 *
 * @author DenBond7
 *         Date: 26.05.2017
 *         Time: 10:03
 *         E-mail: DenBond7@gmail.com
 */

public abstract class BaseBackStackActivity extends BaseActivity {

    public abstract int getContentViewResourceId();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentViewResourceId());
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }
}
