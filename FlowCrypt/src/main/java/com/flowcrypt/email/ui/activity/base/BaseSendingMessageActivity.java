/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.service.EmailSyncService;
import com.flowcrypt.email.ui.activity.fragment.base.BaseSendSecurityMessageFragment;
import com.flowcrypt.email.util.GeneralUtil;

/**
 * The base activity which describe a sending message logic.
 *
 * @author DenBond7
 *         Date: 10.05.2017
 *         Time: 11:43
 *         E-mail: DenBond7@gmail.com
 */

public abstract class BaseSendingMessageActivity extends BaseBackStackSyncActivity implements
        BaseSendSecurityMessageFragment.OnMessageSendListener {

    public static final String EXTRA_KEY_ACCOUNT_EMAIL =
            GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_ACCOUNT_EMAIL",
                    BaseSendingMessageActivity.class);

    private boolean isMessageSendingNow;
    private String accountEmail;

    protected abstract void notifyUserAboutErrorWhenSendMessage();

    protected abstract boolean isCanFinishActivity();

    protected abstract void notifyFragmentAboutErrorFromService(int requestCode, int errorType);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null) {
            if (getIntent().hasExtra(EXTRA_KEY_ACCOUNT_EMAIL)) {
                this.accountEmail = getIntent().getStringExtra(EXTRA_KEY_ACCOUNT_EMAIL);
            } else throw new IllegalArgumentException("The account email not specified!");
        }
    }

    @Override
    public void onReplyFromSyncServiceReceived(int requestCode, int resultCode) {
        switch (requestCode) {
            case R.id.syns_request_send_encrypted_message:
                isMessageSendingNow = false;
                switch (resultCode) {
                    case EmailSyncService.REPLY_RESULT_CODE_ACTION_OK:
                        Toast.makeText(this, R.string.message_was_sent,
                                Toast.LENGTH_SHORT).show();
                        finish();
                        break;

                    case EmailSyncService.REPLY_RESULT_CODE_ACTION_ERROR:
                        notifyUserAboutErrorWhenSendMessage();
                        break;
                }
                break;
        }
    }

    @Override
    public void onErrorFromSyncServiceReceived(int requestCode, int errorType, Exception e) {
        switch (requestCode) {
            case R.id.syns_request_send_encrypted_message:
                notifyFragmentAboutErrorFromService(requestCode, errorType);
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (!isMessageSendingNow && isCanFinishActivity()) {
            super.onBackPressed();
        } else {
            Toast.makeText(this, R.string.please_wait_while_message_will_be_sent, Toast
                    .LENGTH_SHORT).show();
        }
    }

    @Override
    public void sendMessage(String rawEncryptedMessage) {
        isMessageSendingNow = true;
        sendEncryptedMessage(R.id.syns_request_send_encrypted_message, rawEncryptedMessage);
    }

    @Override
    public String getSenderEmail() {
        return accountEmail;
    }
}
