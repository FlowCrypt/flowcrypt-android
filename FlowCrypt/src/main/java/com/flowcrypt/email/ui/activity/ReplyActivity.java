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
import com.flowcrypt.email.ui.activity.base.CreateMessageActivity;
import com.flowcrypt.email.util.GeneralUtil;

/**
 * This activity describes a logic of send encrypted or standard message as a reply.
 *
 * @author DenBond7
 *         Date: 10.05.2017
 *         Time: 09:11
 *         E-mail: DenBond7@gmail.com
 */
public class ReplyActivity extends CreateMessageActivity {

    public static final String EXTRA_KEY_INCOMING_MESSAGE_INFO = GeneralUtil.generateUniqueExtraKey
            ("EXTRA_KEY_INCOMING_MESSAGE_INFO", ReplyActivity.class);

    private View layoutContent;

    public static Intent generateIntent(Context context, IncomingMessageInfo incomingMessageInfo,
                                        MessageEncryptionType messageEncryptionType) {

        Intent intent = new Intent(context, ReplyActivity.class);
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

    /*@Override
    public void notifyUserAboutErrorWhenSendMessage() {
        ReplyFragment replyFragment = (ReplyFragment)
                getSupportFragmentManager().findFragmentById(R.id.replyFragment);

        if (replyFragment != null) {
            replyFragment.notifyUserAboutErrorWhenSendMessage();
        }
    }

    @Override
    public boolean isCanFinishActivity() {
        ReplyFragment replyFragment = (ReplyFragment)
                getSupportFragmentManager().findFragmentById(R.id.replyFragment);

        return replyFragment != null && !replyFragment.isMessageSendingNow();
    }

    @Override
    protected void notifyFragmentAboutErrorFromService(int requestCode, int errorType, Exception e) {
        ReplyFragment replyFragment = (ReplyFragment)
                getSupportFragmentManager().findFragmentById(R.id.replyFragment);

        if (replyFragment != null) {
            replyFragment.onErrorOccurred(requestCode, errorType, e);
        }
    }

    @Override
    protected void notifyFragmentAboutChangeMessageEncryptionType(MessageEncryptionType
                                                                          messageEncryptionType) {
        ReplyFragment replyFragment = (ReplyFragment)
                getSupportFragmentManager().findFragmentById(R.id.replyFragment);

        if (replyFragment != null) {
            replyFragment.onMessageEncryptionTypeChange(messageEncryptionType);
        }
    }*/
}
