/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org). Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/tree/master/src/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.content.Intent;
import android.view.View;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.R;
import com.flowcrypt.email.ui.activity.base.BaseAuthenticationActivity;
import com.flowcrypt.email.ui.activity.fragment.CreateOrImportKeyFragment;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;

import java.util.ArrayList;

/**
 * This activity describes a logic for create ot import private keys.
 *
 * @author DenBond7
 *         Date: 23.05.2017.
 *         Time: 16:15.
 *         E-mail: DenBond7@gmail.com
 */
public class CreateOrImportKeyActivity extends BaseAuthenticationActivity implements
        CreateOrImportKeyFragment.OnPrivateKeysSelectedListener {
    public static final String KEY_IS_SHOW_USE_ANOTHER_ACCOUNT_BUTTON = BuildConfig
            .APPLICATION_ID + ".KEY_IS_SHOW_USE_ANOTHER_ACCOUNT_BUTTON";

    @Override
    public View getRootView() {
        return findViewById(R.id.layoutContent);
    }

    @Override
    public void handleSignInResult(GoogleSignInResult googleSignInResult, boolean isOnStartCall) {

    }

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_create_or_import_key;
    }

    @Override
    public void onPrivateKeySelected(ArrayList<String> privateKeys) {
        if (privateKeys != null && !privateKeys.isEmpty()) {
            finish();
            Intent intent = new Intent(this, RestoreAccountActivity.class);
            intent.putStringArrayListExtra(RestoreAccountActivity.KEY_EXTRA_PRIVATE_KEYS,
                    privateKeys);
            startActivity(intent);
        }
    }
}
