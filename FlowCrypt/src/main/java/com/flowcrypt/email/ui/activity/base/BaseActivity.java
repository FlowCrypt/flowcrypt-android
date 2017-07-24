/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

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

    private Snackbar snackbar;

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

    /**
     * Get root view which will be used for show Snackbar.
     */
    public abstract View getRootView();

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


    /**
     * Show an information as Snackbar.
     *
     * @param view        The view to find a parent from.
     * @param messageText The text to show.  Can be formatted text.
     */
    public void showInfoSnackbar(View view, String messageText) {
        showInfoSnackbar(view, messageText, Snackbar.LENGTH_INDEFINITE);
    }

    /**
     * Show an information as Snackbar.
     *
     * @param view        The view to find a parent from.
     * @param messageText The text to show.  Can be formatted text.
     * @param duration    How long to display the message.
     */
    public void showInfoSnackbar(View view, String messageText, int duration) {
        snackbar = Snackbar.make(view, messageText, duration)
                .setAction(android.R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                });
        snackbar.show();
    }

    /**
     * Show some information as Snackbar with custom message, action button mame and listener.
     *
     * @param view            he view to find a parent from
     * @param messageText     The text to show.  Can be formatted text
     * @param buttonName      The text of the Snackbar button
     * @param onClickListener The Snackbar button click listener.
     */
    public void showSnackbar(View view, String messageText, String buttonName,
                             @NonNull View.OnClickListener onClickListener) {
        showSnackbar(view, messageText, buttonName, Snackbar.LENGTH_INDEFINITE, onClickListener);
    }

    /**
     * Show some information as Snackbar with custom message, action button mame and listener.
     *
     * @param view            he view to find a parent from
     * @param messageText     The text to show.  Can be formatted text
     * @param buttonName      The text of the Snackbar button
     * @param duration        How long to display the message.
     * @param onClickListener The Snackbar button click listener.
     */
    public void showSnackbar(View view, String messageText, String buttonName, int duration,
                             @NonNull View.OnClickListener onClickListener) {
        snackbar = Snackbar.make(view, messageText, duration)
                .setAction(buttonName, onClickListener);
        snackbar.show();
    }

    public Snackbar getSnackBar() {
        return snackbar;
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
