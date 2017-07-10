/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.R;
import com.flowcrypt.email.ui.activity.base.BaseActivity;
import com.flowcrypt.email.ui.activity.fragment.CreateOrImportKeyFragment;

import java.util.ArrayList;

/**
 * This activity describes a logic for create ot import private keys.
 *
 * @author DenBond7
 *         Date: 23.05.2017.
 *         Time: 16:15.
 *         E-mail: DenBond7@gmail.com
 */
public class CreateOrImportKeyActivity extends BaseActivity implements
        CreateOrImportKeyFragment.OnPrivateKeysSelectedListener {
    public static final String KEY_IS_SHOW_USE_ANOTHER_ACCOUNT_BUTTON = BuildConfig
            .APPLICATION_ID + ".KEY_IS_SHOW_USE_ANOTHER_ACCOUNT_BUTTON";

    @Override
    public View getRootView() {
        return findViewById(R.id.layoutContent);
    }

    @Override
    public boolean isDisplayHomeAsUpEnabled() {
        return false;
    }

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_create_or_import_key;
    }

    @Override
    public void onPrivateKeysSelected(ArrayList<String> privateKeys) {
        if (privateKeys != null && !privateKeys.isEmpty()) {
            Intent intent = new Intent();
            intent.putStringArrayListExtra(RestoreAccountActivity.KEY_EXTRA_PRIVATE_KEYS,
                    privateKeys);
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(Activity.RESULT_CANCELED);
    }
}
