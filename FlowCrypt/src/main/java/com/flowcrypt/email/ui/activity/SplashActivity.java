package com.flowcrypt.email.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.flowcrypt.email.R;
import com.flowcrypt.email.model.SignInType;
import com.flowcrypt.email.security.SecurityUtils;
import com.flowcrypt.email.ui.activity.base.BaseAuthenticationActivity;
import com.flowcrypt.email.ui.activity.fragment.SplashActivityFragment;
import com.flowcrypt.email.util.UIUtil;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;

import java.io.File;

public class SplashActivity extends BaseAuthenticationActivity implements SplashActivityFragment
        .OnSignInButtonClickListener {
    private static final int REQUEST_CODE_SIGN_IN = 100;

    private View signInView;
    private View splashView;

    @Override
    public View getRootView() {
        return signInView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initViews();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                GoogleSignInResult googleSignInResult =
                        Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                handleSignInResult(googleSignInResult, false);
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void handleSignInResult(GoogleSignInResult googleSignInResult, boolean isOnStartCall) {
        if (googleSignInResult.isSuccess()) {
            finish();
            if (isBackupKeysExist()) {
                startActivity(new Intent(this, EmailManagerActivity.class));
            } else {
                startActivity(new Intent(this, RestoreAccountActivity.class));
            }
        } else {
            if (!TextUtils.isEmpty(googleSignInResult.getStatus().getStatusMessage())) {
                UIUtil.showInfoSnackbar(signInView, googleSignInResult.getStatus()
                        .getStatusMessage());
            }
            signInView.setVisibility(View.VISIBLE);
            splashView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSignInButtonClick(SignInType signInType) {
        switch (signInType) {
            case GMAIL:
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
                startActivityForResult(signInIntent, REQUEST_CODE_SIGN_IN);
                break;
        }
    }

    /**
     * Check is decrypted backups exist in the application directory.
     *
     * @return <tt>Boolean</tt> true if exists one or more decrypted keys, false otherwise;
     */
    private boolean isBackupKeysExist() {
        File keysFolder = SecurityUtils.getSecurityFolder(this);
        File[] correctKeysArray = SecurityUtils.getCorrectPrivateKeys(this);
        return keysFolder.exists() && correctKeysArray.length > 0;
    }

    private void initViews() {
        setContentView(R.layout.activity_splash);
        signInView = findViewById(R.id.signInView);
        splashView = findViewById(R.id.splashView);
    }
}
