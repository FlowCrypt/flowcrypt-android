/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.flowcrypt.email.R;
import com.flowcrypt.email.ui.activity.AddNewAccountManuallyActivity;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.google.GoogleApiClientHelper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

/**
 * This activity will be a common point of a sign-in logic.
 *
 * @author Denis Bondarenko
 *         Date: 06.10.2017
 *         Time: 10:38
 *         E-mail: DenBond7@gmail.com
 */

public abstract class BaseSignInActivity extends BaseActivity implements View.OnClickListener,
        GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {
    protected static final int REQUEST_CODE_SIGN_IN = 10;
    protected static final int REQUEST_CODE_ADD_OTHER_ACCOUNT = 11;

    /**
     * The main entry point for Google Play services integration.
     */
    protected GoogleApiClient googleApiClient;
    protected boolean isRunSignInWithGmailNeeded;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initGoogleApiClient();
        initViews();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_ADD_OTHER_ACCOUNT:
                switch (resultCode) {
                    case AddNewAccountManuallyActivity.RESULT_CODE_CONTINUE_WITH_GMAIL:
                        this.isRunSignInWithGmailNeeded = true;
                        break;
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonSignInWithGmail:
                GoogleApiClientHelper.signInWithGmailUsingOAuth2(this, googleApiClient, getRootView(),
                        REQUEST_CODE_SIGN_IN);
                break;

            case R.id.buttonOtherEmailProvider:
                if (GeneralUtil.isInternetConnectionAvailable(this)) {
                    startActivityForResult(new Intent(this, AddNewAccountManuallyActivity.class),
                            REQUEST_CODE_ADD_OTHER_ACCOUNT);
                } else {
                    showInfoSnackbar(getRootView(), getString(R.string.internet_connection_is_not_available));
                }
                break;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (this.isRunSignInWithGmailNeeded) {
            this.isRunSignInWithGmailNeeded = false;
            GoogleApiClientHelper.signInWithGmailUsingOAuth2(this, googleApiClient, getRootView(),
                    REQUEST_CODE_SIGN_IN);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        showInfoSnackbar(getRootView(), connectionResult.getErrorMessage());
    }

    @Override
    public void onJsServiceConnected() {

    }

    protected void initGoogleApiClient() {
        googleApiClient = GoogleApiClientHelper.generateGoogleApiClient(this, this, this, this, GoogleApiClientHelper
                .generateGoogleSignInOptions());
    }

    private void initViews() {
        if (findViewById(R.id.buttonSignInWithGmail) != null) {
            findViewById(R.id.buttonSignInWithGmail).setOnClickListener(this);
        }

        if (findViewById(R.id.buttonOtherEmailProvider) != null) {
            findViewById(R.id.buttonOtherEmailProvider).setOnClickListener(this);
        }
    }
}
