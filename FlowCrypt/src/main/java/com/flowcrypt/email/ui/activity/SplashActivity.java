/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.flowcrypt.email.model.SignInType;
import com.flowcrypt.email.security.SecurityUtils;
import com.flowcrypt.email.service.EmailSyncService;
import com.flowcrypt.email.ui.activity.base.BaseActivity;
import com.flowcrypt.email.ui.activity.fragment.SplashActivityFragment;
import com.flowcrypt.email.util.UIUtil;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;

import java.io.IOException;

/**
 * The launcher Activity.
 *
 * @author DenBond7
 *         Date: 26.14.2017
 *         Time: 14:50
 *         E-mail: DenBond7@gmail.com
 */
public class SplashActivity extends BaseActivity implements SplashActivityFragment
        .OnSignInButtonClickListener, GoogleApiClient.OnConnectionFailedListener {
    private static final int REQUEST_CODE_SIGN_IN = 100;
    /**
     * The main entry point for Google Play services integration.
     */
    protected GoogleApiClient googleApiClient;
    private View signInView;
    private View splashView;

    @Override
    public boolean isDisplayHomeAsUpEnabled() {
        return false;
    }

    @Override
    public View getRootView() {
        return signInView;
    }

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_splash;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder
                (GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(Constants.SCOPE_MAIL_GOOGLE_COM))
                .requestEmail()
                .build();

        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, googleSignInOptions)
                .build();

        initViews();
    }

    @Override
    public void onStart() {
        super.onStart();
        checkGoogleSignResult();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                GoogleSignInResult googleSignInResult =
                        Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                handleSignInResult(googleSignInResult);
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
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

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        UIUtil.showInfoSnackbar(getRootView(), connectionResult.getErrorMessage());
    }

    /**
     * Sign out from the account.
     */
    public void signOut(SignInType signInType) {
        switch (signInType) {
            case GMAIL:
                signOutFromGoogleAccount();
                break;
        }
    }

    /**
     * Revoke access to the mail.
     */
    public void revokeAccess(SignInType signInType) {
        switch (signInType) {
            case GMAIL:
                revokeAccessFomGoogleAccount();
                break;
        }
    }

    private void handleSignInResult(GoogleSignInResult googleSignInResult) {
        if (googleSignInResult.isSuccess()) {

            Account account = null;
            if (googleSignInResult.getSignInAccount() != null) {
                account = googleSignInResult.getSignInAccount().getAccount();
            } else {
                //todo-denbond7 handle this situation
            }

            finish();
            if (SecurityUtils.isBackupKeysExist(this)) {
                if (account != null) {
                    Intent startEmailServiceIntent = new Intent(this, EmailSyncService.class);
                    startEmailServiceIntent.putExtra(EmailSyncService.EXTRA_KEY_GMAIL_ACCOUNT,
                            account);
                    startService(startEmailServiceIntent);
                }

                Intent intentRunEmailManagerActivity = new Intent(this, EmailManagerActivity.class);
                intentRunEmailManagerActivity.putExtra(EmailManagerActivity.EXTRA_KEY_ACCOUNT,
                        account);
                startActivity(intentRunEmailManagerActivity);
            } else {
                if (account != null) {
                    Intent intentRunRestoreActivity = new Intent(this,
                            RestoreAccountActivity.class);
                    intentRunRestoreActivity.putExtra(
                            RestoreAccountActivity.KEY_EXTRA_ACCOUNT, account);
                    startActivity(intentRunRestoreActivity);
                } else {
                    //todo-denbond7 handle this situation
                }
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

    private void initViews() {
        signInView = findViewById(R.id.signInView);
        splashView = findViewById(R.id.splashView);
    }

    /**
     * Revoke access to the Gmail account.
     */
    private void revokeAccessFomGoogleAccount() {
        Auth.GoogleSignInApi.revokeAccess(googleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        try {
                            runSplashActivity();
                        } catch (IOException e) {
                            e.printStackTrace();
                            UIUtil.showInfoSnackbar(getRootView(), e.getMessage());
                        }
                    }
                });
    }

    /**
     * Sign out from the Google account.
     */
    private void signOutFromGoogleAccount() {
        Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        try {
                            runSplashActivity();
                        } catch (IOException e) {
                            e.printStackTrace();
                            UIUtil.showInfoSnackbar(getRootView(), e.getMessage());
                        }
                    }
                });
    }

    private void runSplashActivity() throws IOException {
        stopService(new Intent(this, EmailSyncService.class));
        SecurityUtils.cleanSecurityInfo(this);
        startActivity(new Intent(this, SplashActivity.class));
        finish();
    }

    /**
     * In this method we check GoogleSignResult. If the user's cached credentials are valid the
     * OptionalPendingResult will be "done" and the GoogleSignInResult will be available
     * instantly. If the user has not previously signed in on this device or the sign-in has
     * expired, this asynchronous branch will attempt to sign in the user silently. Cross-device
     * single sign-on will occur in this branch.
     */
    private void checkGoogleSignResult() {
        OptionalPendingResult<GoogleSignInResult> optionalPendingResult
                = Auth.GoogleSignInApi.silentSignIn(googleApiClient);
        if (optionalPendingResult.isDone()) {
            GoogleSignInResult googleSignInResult = optionalPendingResult.get();
            handleSignInResult(googleSignInResult);
        } else {
            optionalPendingResult.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(@NonNull GoogleSignInResult googleSignInResult) {
                    handleSignInResult(googleSignInResult);
                }
            });
        }
    }
}
