package com.flowcrypt.email.ui.activity.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.flowcrypt.email.R;

/**
 * This is a base activity. This class describes a base logic for all activities.
 *
 * @author DenBond7
 *         Date: 30.04.2017.
 *         Time: 22:21.
 *         E-mail: DenBond7@gmail.com
 */
public abstract class BaseActivity extends AppCompatActivity {

    /**
     * This method can used to change "HomeAsUpEnabled" behavior.
     *
     * @return true if we want to show "HomeAsUpEnabled", false otherwise.
     */
    public abstract boolean isDisplayHomeAsUpEnabled();

    /**
     * Get the content view resources id. This method must return an resources id of a layout
     * if we want to show some UI.
     *
     * @return The content view resources id.
     */
    public abstract int getContentViewResourceId();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentViewResourceId());
        setupToolbarIfItExists();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (isDisplayHomeAsUpEnabled()) {
                    finish();
                    return true;
                } else return super.onOptionsItemSelected(item);

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setupToolbarIfItExists() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(isDisplayHomeAsUpEnabled());
        }
    }
}
