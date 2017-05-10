package com.flowcrypt.email.ui.activity;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.IncomingMessageInfo;
import com.flowcrypt.email.ui.activity.base.BaseSendingMessageActivity;
import com.flowcrypt.email.ui.activity.fragment.SecureReplyFragment;
import com.flowcrypt.email.util.UIUtil;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;

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
    public void handleSignInResult(GoogleSignInResult googleSignInResult, boolean isOnStartCall) {
        if (googleSignInResult.isSuccess()) {
            GoogleSignInAccount googleSignInAccount = googleSignInResult.getSignInAccount();
            if (googleSignInAccount != null) {
                SecureReplyFragment secureReplyFragment = (SecureReplyFragment)
                        getSupportFragmentManager()
                                .findFragmentById(R.id.secureReplyFragment);

                if (secureReplyFragment != null) {
                    secureReplyFragment.updateAccount(googleSignInAccount.getAccount());
                }
            }
        } else if (!TextUtils.isEmpty(googleSignInResult.getStatus().getStatusMessage())) {
            UIUtil.showInfoSnackbar(getRootView(), googleSignInResult.getStatus()
                    .getStatusMessage());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_reply);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

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
    public boolean isMessageSendingNow() {
        SecureReplyFragment secureReplyFragment = (SecureReplyFragment)
                getSupportFragmentManager()
                        .findFragmentById(R.id.secureReplyFragment);

        return secureReplyFragment != null && secureReplyFragment.isMessageSendingNow();
    }
}
