/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.model.MessageEncryptionType;
import com.flowcrypt.email.service.EmailSyncService;
import com.flowcrypt.email.ui.activity.fragment.base.BaseSendSecurityMessageFragment;
import com.flowcrypt.email.ui.activity.listeners.OnChangeMessageEncryptedTypeListener;
import com.flowcrypt.email.ui.activity.settings.FeedbackActivity;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

/**
 * The base activity which describe a sending message logic.
 *
 * @author DenBond7
 *         Date: 10.05.2017
 *         Time: 11:43
 *         E-mail: DenBond7@gmail.com
 */

public abstract class BaseSendingMessageActivity extends BaseBackStackSyncActivity implements
        BaseSendSecurityMessageFragment.OnMessageSendListener,
        OnChangeMessageEncryptedTypeListener {

    public static final String EXTRA_KEY_ACCOUNT_EMAIL =
            GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_ACCOUNT_EMAIL",
                    BaseSendingMessageActivity.class);

    public static final String EXTRA_KEY_MESSAGE_ENCRYPTION_TYPE =
            GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_MESSAGE_ENCRYPTION_TYPE",
                    BaseSendingMessageActivity.class);

    private View nonEncryptedHintView;

    private boolean isMessageSendingNow;
    private String accountEmail;
    private MessageEncryptionType messageEncryptionType;

    protected abstract void notifyUserAboutErrorWhenSendMessage();

    protected abstract boolean isCanFinishActivity();

    protected abstract String getSecurityTitle();

    protected abstract String getStandardTitle();

    protected abstract void notifyFragmentAboutErrorFromService(int requestCode, int errorType);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initNonEncryptedHintView();

        if (getIntent() != null) {
            if (getIntent().hasExtra(EXTRA_KEY_ACCOUNT_EMAIL)) {
                this.accountEmail = getIntent().getStringExtra(EXTRA_KEY_ACCOUNT_EMAIL);
            } else throw new IllegalArgumentException("The account email not specified!");

            messageEncryptionType = (MessageEncryptionType) getIntent()
                    .getSerializableExtra(EXTRA_KEY_MESSAGE_ENCRYPTION_TYPE);

            if (messageEncryptionType == null) {
                messageEncryptionType = MessageEncryptionType.ENCRYPTED;
            } else {
                onChangeMessageEncryptedType(messageEncryptionType);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_send_message, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem menuActionSwitchType = menu.findItem(R.id.menuActionSwitchType);
        menuActionSwitchType.setTitle(messageEncryptionType == MessageEncryptionType.STANDARD ?
                R.string.switch_to_secure_email : R.string.switch_to_standard_email);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuActionHelp:
                startActivity(new Intent(this, FeedbackActivity.class));
                return true;

            case R.id.menuActionSwitchType:
                switch (messageEncryptionType) {
                    case ENCRYPTED:
                        onChangeMessageEncryptedType(MessageEncryptionType.STANDARD);
                        break;

                    case STANDARD:
                        onChangeMessageEncryptedType(MessageEncryptionType.ENCRYPTED);
                        break;
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onReplyFromSyncServiceReceived(int requestCode, int resultCode, Object obj) {
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
                isMessageSendingNow = false;
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

    @Override
    public void onChangeMessageEncryptedType(MessageEncryptionType messageEncryptionType) {
        this.messageEncryptionType = messageEncryptionType;
        switch (messageEncryptionType) {
            case ENCRYPTED:
                getAppBarLayout().setBackgroundColor(UIUtil.getColor(this, R.color.colorPrimary));
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(getSecurityTitle());
                }
                getAppBarLayout().removeView(nonEncryptedHintView);
                break;

            case STANDARD:
                getAppBarLayout().setBackgroundColor(UIUtil.getColor(this, R.color.valencia));
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(getStandardTitle());
                }
                getAppBarLayout().addView(nonEncryptedHintView);
                break;
        }

        invalidateOptionsMenu();
    }

    @Override
    public MessageEncryptionType getMessageEncryptionType() {
        return messageEncryptionType;
    }

    private void initNonEncryptedHintView() {
        nonEncryptedHintView = getLayoutInflater().inflate(
                R.layout.under_toolbar_line_with_text, getAppBarLayout(), false);

        TextView textView = (TextView) nonEncryptedHintView.findViewById(R.id
                .underToolbarTextTextView);
        textView.setText(R.string.this_message_will_not_be_encrypted);
    }
}
