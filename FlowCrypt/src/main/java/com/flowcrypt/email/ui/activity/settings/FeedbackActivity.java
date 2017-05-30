package com.flowcrypt.email.ui.activity.settings;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.View;

import com.flowcrypt.email.R;
import com.flowcrypt.email.ui.activity.base.BaseBackStackAuthenticationActivity;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;

/**
 * The feedback activity. Anywhere there is a question mark, it should take the user to this
 * screen.
 *
 * @author DenBond7
 *         Date: 30.05.2017
 *         Time: 9:56
 *         E-mail: DenBond7@gmail.com
 */

public class FeedbackActivity extends BaseBackStackAuthenticationActivity {

    @Override
    public View getRootView() {
        return null;
    }

    @Override
    public void handleSignInResult(GoogleSignInResult googleSignInResult, boolean isOnStartCall) {

    }

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_feedback;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_feedback, menu);
        return true;
    }
}
