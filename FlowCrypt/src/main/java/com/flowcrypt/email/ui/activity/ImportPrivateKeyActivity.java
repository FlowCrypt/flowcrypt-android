/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.flowcrypt.email.R;
import com.flowcrypt.email.ui.activity.base.BaseBackStackActivity;
import com.flowcrypt.email.ui.activity.listeners.OnPrivateKeysSelectedListener;
import com.flowcrypt.email.util.GeneralUtil;

import java.util.ArrayList;

/**
 * This activity describes a logic of import private keys.
 *
 * @author Denis Bondarenko
 *         Date: 20.07.2017
 *         Time: 16:59
 *         E-mail: DenBond7@gmail.com
 */

public class ImportPrivateKeyActivity extends BaseBackStackActivity implements
        OnPrivateKeysSelectedListener {

    public static final String KEY_ACCOUNT = GeneralUtil.generateUniqueExtraKey("KEY_ACCOUNT",
            ImportPrivateKeyActivity.class);

    public static Intent newIntent(Context context, Account account) {
        Intent intent = new Intent(context, ImportPrivateKeyActivity.class);
        intent.putExtra(KEY_ACCOUNT, account);
        return intent;
    }

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_import_private_key;
    }

    @Override
    public View getRootView() {
        return null;
    }

    @Override
    public void onPrivateKeysSelected(ArrayList<String> privateKeys) {
    }
}
