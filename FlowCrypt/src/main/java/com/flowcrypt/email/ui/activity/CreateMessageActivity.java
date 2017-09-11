/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

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
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo;
import com.flowcrypt.email.model.MessageEncryptionType;
import com.flowcrypt.email.service.EmailSyncService;
import com.flowcrypt.email.ui.activity.base.BaseBackStackSyncActivity;
import com.flowcrypt.email.ui.activity.fragment.base.CreateMessageFragment;
import com.flowcrypt.email.ui.activity.listeners.OnChangeMessageEncryptedTypeListener;
import com.flowcrypt.email.ui.activity.settings.FeedbackActivity;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

/**
 * This activity describes a logic of send encrypted or standard message.
 *
 * @author DenBond7
 *         Date: 10.05.2017
 *         Time: 11:43
 *         E-mail: DenBond7@gmail.com
 */

public class CreateMessageActivity extends BaseBackStackSyncActivity implements
        CreateMessageFragment.OnMessageSendListener, OnChangeMessageEncryptedTypeListener {

    public static final String EXTRA_KEY_ACCOUNT_EMAIL =
            GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_ACCOUNT_EMAIL", CreateMessageActivity.class);

    public static final String EXTRA_KEY_MESSAGE_ENCRYPTION_TYPE =
            GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_MESSAGE_ENCRYPTION_TYPE", CreateMessageActivity.class);

    private View nonEncryptedHintView;
    private View layoutContent;

    private String accountEmail;
    private MessageEncryptionType messageEncryptionType;

    private boolean isMessageSendingNow;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        layoutContent = findViewById(R.id.layoutContent);
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
                onMessageEncryptionTypeChange(messageEncryptionType);
            }
        }
    }

    @Override
    public View getRootView() {
        return layoutContent;
    }

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_secure_compose;
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
                        onMessageEncryptionTypeChange(MessageEncryptionType.STANDARD);
                        break;

                    case STANDARD:
                        onMessageEncryptionTypeChange(MessageEncryptionType.ENCRYPTED);
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
                        Toast.makeText(this, R.string.message_was_sent, Toast.LENGTH_SHORT).show();
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
                notifyFragmentAboutErrorFromService(requestCode, errorType, e);
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (!isMessageSendingNow && isCanFinishActivity()) {
            super.onBackPressed();
        } else {
            Toast.makeText(this, R.string.please_wait_while_message_will_be_sent, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void sendMessage(OutgoingMessageInfo outgoingMessageInfo) {
        isMessageSendingNow = true;
        sendEncryptedMessage(R.id.syns_request_send_encrypted_message, outgoingMessageInfo);
    }

    @Override
    public String getSenderEmail() {
        return accountEmail;
    }

    @Override
    public void onMessageEncryptionTypeChange(MessageEncryptionType messageEncryptionType) {
        this.messageEncryptionType = messageEncryptionType;
        switch (messageEncryptionType) {
            case ENCRYPTED:
                getAppBarLayout().setBackgroundColor(UIUtil.getColor(this, R.color.colorPrimary));
                getAppBarLayout().removeView(nonEncryptedHintView);
                break;

            case STANDARD:
                getAppBarLayout().setBackgroundColor(UIUtil.getColor(this, R.color.red));
                getAppBarLayout().addView(nonEncryptedHintView);
                break;
        }

        invalidateOptionsMenu();
        notifyFragmentAboutChangeMessageEncryptionType(messageEncryptionType);
    }

    @Override
    public MessageEncryptionType getMessageEncryptionType() {
        return messageEncryptionType;
    }

    private void notifyUserAboutErrorWhenSendMessage() {
        CreateMessageFragment composeFragment = (CreateMessageFragment) getSupportFragmentManager
                ().findFragmentById(R.id.composeFragment);
        if (composeFragment != null) {
            composeFragment.notifyUserAboutErrorWhenSendMessage();
        }
    }

    private boolean isCanFinishActivity() {
        CreateMessageFragment composeFragment = (CreateMessageFragment) getSupportFragmentManager()
                .findFragmentById(R.id.composeFragment);

        return composeFragment != null && !composeFragment.isMessageSendingNow();
    }

    private void notifyFragmentAboutErrorFromService(int requestCode, int errorType, Exception e) {
        CreateMessageFragment composeFragment = (CreateMessageFragment) getSupportFragmentManager()
                .findFragmentById(R.id.composeFragment);

        if (composeFragment != null) {
            composeFragment.onErrorOccurred(requestCode, errorType, e);
        }
    }

    private void notifyFragmentAboutChangeMessageEncryptionType(MessageEncryptionType
                                                                        messageEncryptionType) {
        CreateMessageFragment composeFragment = (CreateMessageFragment) getSupportFragmentManager()
                .findFragmentById(R.id.composeFragment);

        if (composeFragment != null) {
            composeFragment.onMessageEncryptionTypeChange(messageEncryptionType);
        }
    }

    private void initNonEncryptedHintView() {
        nonEncryptedHintView = getLayoutInflater().inflate(R.layout.under_toolbar_line_with_text,
                getAppBarLayout(), false);

        TextView textView = (TextView) nonEncryptedHintView.findViewById(R.id.underToolbarTextTextView);
        textView.setText(R.string.this_message_will_not_be_encrypted);
    }
}
