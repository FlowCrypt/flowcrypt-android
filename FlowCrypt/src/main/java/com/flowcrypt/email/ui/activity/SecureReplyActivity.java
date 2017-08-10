/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.IncomingMessageInfo;
import com.flowcrypt.email.model.MessageEncryptionType;
import com.flowcrypt.email.ui.activity.base.BaseSendingMessageActivity;
import com.flowcrypt.email.ui.activity.fragment.SecureReplyFragment;
import com.flowcrypt.email.util.GeneralUtil;

/**
 * This activity describes a logic of send encrypted message as a reply.
 *
 * @author DenBond7
 *         Date: 10.05.2017
 *         Time: 09:11
 *         E-mail: DenBond7@gmail.com
 */
public class SecureReplyActivity extends BaseSendingMessageActivity {

    public static final String EXTRA_KEY_INCOMING_MESSAGE_INFO = GeneralUtil.generateUniqueExtraKey
            ("EXTRA_KEY_INCOMING_MESSAGE_INFO", SecureReplyActivity.class);

    private View layoutContent;

    public static Intent generateIntent(Context context, IncomingMessageInfo incomingMessageInfo,
                                        MessageEncryptionType messageEncryptionType) {

        Intent intent = new Intent(context, SecureReplyActivity.class);
        intent.putExtra(EXTRA_KEY_INCOMING_MESSAGE_INFO, incomingMessageInfo);
        intent.putExtra(EXTRA_KEY_MESSAGE_ENCRYPTION_TYPE, messageEncryptionType);
        return intent;
    }

    @Override
    public View getRootView() {
        return layoutContent;
    }

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_security_reply;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        layoutContent = findViewById(R.id.layoutContent);

        if (getIntent() == null || !getIntent().hasExtra(EXTRA_KEY_INCOMING_MESSAGE_INFO)) {
            finish();
        }
    }

    @Override
    public void notifyUserAboutErrorWhenSendMessage() {
        SecureReplyFragment secureReplyFragment = (SecureReplyFragment)
                getSupportFragmentManager().findFragmentById(R.id.secureReplyFragment);

        if (secureReplyFragment != null) {
            secureReplyFragment.notifyUserAboutErrorWhenSendMessage();
        }
    }

    @Override
    public boolean isCanFinishActivity() {
        SecureReplyFragment secureReplyFragment = (SecureReplyFragment)
                getSupportFragmentManager().findFragmentById(R.id.secureReplyFragment);

        return secureReplyFragment != null && !secureReplyFragment.isMessageSendingNow();
    }

    @Override
    protected String getSecurityTitle() {
        return getString(R.string.security_reply);
    }

    @Override
    protected String getStandardTitle() {
        return getString(R.string.reply);
    }

    @Override
    protected void notifyFragmentAboutErrorFromService(int requestCode, int errorType) {
        SecureReplyFragment secureReplyFragment = (SecureReplyFragment)
                getSupportFragmentManager().findFragmentById(R.id.secureReplyFragment);

        if (secureReplyFragment != null) {
            secureReplyFragment.onErrorOccurred(requestCode, errorType);
        }
    }

    @Override
    protected void notifyFragmentAboutChangeMessageEncryptionType(MessageEncryptionType
                                                                          messageEncryptionType) {
        SecureReplyFragment secureReplyFragment = (SecureReplyFragment)
                getSupportFragmentManager().findFragmentById(R.id.secureReplyFragment);

        if (secureReplyFragment != null) {
            secureReplyFragment.onMessageEncryptionTypeChange(messageEncryptionType);
        }
    }
}
