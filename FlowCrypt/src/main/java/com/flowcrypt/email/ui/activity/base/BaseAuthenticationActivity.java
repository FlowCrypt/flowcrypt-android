package com.flowcrypt.email.ui.activity.base;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.model.SignInType;
import com.flowcrypt.email.security.SecurityUtils;
import com.flowcrypt.email.ui.activity.SplashActivity;
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
 * This activity is a base activity for a work with GoogleApiClient.
 *
 * @author DenBond7
 *         Date: 27.04.2017
 *         Time: 16:12
 *         E-mail: DenBond7@gmail.com
 */

public abstract class BaseAuthenticationActivity extends BaseActivity implements
        GoogleApiClient.OnConnectionFailedListener {

    /**
     * The main entry point for Google Play services integration.
     */
    protected GoogleApiClient googleApiClient;

    /**
     * Get root view which will be used for show Snackbar.
     */
    public abstract View getRootView();

    /**
     * Handle a Google sign result. In this method, we check the result received from Google.
     *
     * @param googleSignInResult Object which contain information about sign in.
     * @param isOnStartCall      true if is onStart() method call, false otherwise.
     */
    public abstract void handleSignInResult(GoogleSignInResult googleSignInResult, boolean
            isOnStartCall);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
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
    }

    @Override
    public void onStart() {
        super.onStart();
        checkGoogleSignResult();
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
        SecurityUtils.cleanSecurityInfo(this);
        startActivity(new Intent(BaseAuthenticationActivity.this, SplashActivity.class));
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
            handleSignInResult(googleSignInResult, true);
        } else {
            optionalPendingResult.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(@NonNull GoogleSignInResult googleSignInResult) {
                    handleSignInResult(googleSignInResult, true);
                }
            });
        }
    }
}
