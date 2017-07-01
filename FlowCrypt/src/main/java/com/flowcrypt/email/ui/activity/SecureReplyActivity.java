/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.os.Bundle;
import android.view.View;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.IncomingMessageInfo;
import com.flowcrypt.email.ui.activity.base.BaseSendingMessageActivity;
import com.flowcrypt.email.ui.activity.fragment.SecureReplyFragment;

/**
 * This activity describes a logic of send encrypted message as a reply.
 *
 * @author DenBond7
 *         Date: 10.05.2017
 *         Time: 09:11
 *         E-mail: DenBond7@gmail.com
 */
public class SecureReplyActivity extends BaseSendingMessageActivity {

    public static final String KEY_INCOMING_MESSAGE_INFO = BuildConfig.APPLICATION_ID + "" +
            ".KEY_INCOMING_MESSAGE_INFO";
    private View layoutContent;

    @Override
    public View getRootView() {
        return layoutContent;
    }

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_security_reply;
    }

    @Override
    public void onErrorFromSyncServiceReceived(int requestCode, int errorType, Exception e) {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        layoutContent = findViewById(R.id.layoutContent);

        if (getIntent() != null && getIntent().hasExtra(KEY_INCOMING_MESSAGE_INFO)) {
            SecureReplyFragment secureReplyFragment = (SecureReplyFragment)
                    getSupportFragmentManager()
                            .findFragmentById(R.id.secureReplyFragment);

            if (secureReplyFragment != null) {
                IncomingMessageInfo incomingMessageInfo = getIntent().getParcelableExtra
                        (KEY_INCOMING_MESSAGE_INFO);
                secureReplyFragment.setIncomingMessageInfo(incomingMessageInfo);
            }
        } else {
            finish();
        }
    }

    @Override
    public void notifyUserAboutErrorWhenSendMessage() {
        SecureReplyFragment secureReplyFragment = (SecureReplyFragment)
                getSupportFragmentManager()
                        .findFragmentById(R.id.secureReplyFragment);

        if (secureReplyFragment != null) {
            secureReplyFragment.notifyUserAboutErrorWhenSendMessage();
        }
    }

    @Override
    public boolean isCanFinishActivity() {
        SecureReplyFragment secureReplyFragment = (SecureReplyFragment)
                getSupportFragmentManager()
                        .findFragmentById(R.id.secureReplyFragment);

        return secureReplyFragment != null && !secureReplyFragment.isMessageSendingNow();
    }
}
