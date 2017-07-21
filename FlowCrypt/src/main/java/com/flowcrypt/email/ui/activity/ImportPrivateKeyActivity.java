/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.view.View;

import com.flowcrypt.email.R;
import com.flowcrypt.email.ui.activity.base.BaseBackStackActivity;
import com.flowcrypt.email.ui.activity.listeners.OnPrivateKeysSelectedListener;

import java.util.ArrayList;

/**
 * @author Denis Bondarenko
 *         Date: 20.07.2017
 *         Time: 16:59
 *         E-mail: DenBond7@gmail.com
 */

public class ImportPrivateKeyActivity extends BaseBackStackActivity implements
        OnPrivateKeysSelectedListener {

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
