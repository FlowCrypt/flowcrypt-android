package com.flowcrypt.email.ui.activity.base;

import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.auth.api.signin.GoogleSignInResult;

/**
 * The base back stack authentication activity.
 *
 * @author DenBond7
 *         Date: 09.05.2017
 *         Time: 16:27
 *         E-mail: DenBond7@gmail.com
 */

public abstract class BaseBackStackAuthenticationActivity extends BaseAuthenticationActivity {

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View getRootView() {
        return null;
    }

    @Override
    public void handleSignInResult(GoogleSignInResult googleSignInResult, boolean isOnStartCall) {

    }
}
