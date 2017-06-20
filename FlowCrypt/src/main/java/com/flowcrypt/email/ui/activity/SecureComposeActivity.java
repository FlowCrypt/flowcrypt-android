/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.os.Bundle;
import android.view.View;

import com.flowcrypt.email.R;
import com.flowcrypt.email.ui.activity.base.BaseSendingMessageActivity;
import com.flowcrypt.email.ui.activity.fragment.SecureComposeFragment;


/**
 * This activity describes a logic of send encrypted message.
 *
 * @author DenBond7
 *         Date: 08.05.2017
 *         Time: 14:44
 *         E-mail: DenBond7@gmail.com
 */
public class SecureComposeActivity extends BaseSendingMessageActivity {
    private View layoutContent;

    @Override
    public View getRootView() {
        return layoutContent;
    }

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_secure_compose;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        layoutContent = findViewById(R.id.layoutContent);
    }

    @Override
    public boolean isMessageSendingNow() {
        SecureComposeFragment secureComposeFragment = (SecureComposeFragment)
                getSupportFragmentManager()
                        .findFragmentById(R.id.secureComposeFragment);

        return secureComposeFragment != null && secureComposeFragment.isMessageSendingNow();
    }
}
