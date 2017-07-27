/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.model.PrivateKeyDetails;
import com.flowcrypt.email.model.SignInType;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.SecurityUtils;
import com.flowcrypt.email.service.CheckClipboardToFindPrivateKeyService;
import com.flowcrypt.email.service.EmailSyncService;
import com.flowcrypt.email.ui.activity.base.BaseActivity;
import com.flowcrypt.email.ui.activity.fragment.SplashActivityFragment;
import com.flowcrypt.email.ui.loader.LoadPrivateKeysFromMailAsyncTaskLoader;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;

import java.io.IOException;
import java.util.ArrayList;

/**
 * The launcher Activity.
 *
 * @author DenBond7
 *         Date: 26.14.2017
 *         Time: 14:50
 *         E-mail: DenBond7@gmail.com
 */
public class SplashActivity extends BaseActivity implements SplashActivityFragment
        .OnSignInButtonClickListener, GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks, LoaderManager.LoaderCallbacks<LoaderResult> {
    private static final String ACTION_SIGN_OUT = GeneralUtil.generateUniqueExtraKey
            ("ACTION_SIGN_OUT", SplashActivity.class);
    private static final String ACTION_REVOKE_ACCESS = GeneralUtil.generateUniqueExtraKey
            ("ACTION_REVOKE_ACCESS", SplashActivity.class);

    private static final int REQUEST_CODE_SIGN_IN = 100;
    private static final int REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_GMAIL = 101;
    /**
     * The main entry point for Google Play services integration.
     */
    private GoogleApiClient googleApiClient;

    private View signInView;
    private View splashView;

    private boolean isSignOutAction;
    private boolean isRevokeAccessAction;
    private Account account;

    /**
     * Generate the sign out intent.
     *
     * @param context Interface to global information about an application environment.
     * @return The sign out intent.
     */
    public static Intent getSignOutIntent(Context context) {
        Intent intent = new Intent(context, SplashActivity.class);
        intent.setAction(ACTION_SIGN_OUT);
        return intent;
    }

    /**
     * Generate the revoke access intent.
     *
     * @param context Interface to global information about an application environment.
     * @return The revoke access intent.
     */
    public static Intent getRevokeAccessIntent(Context context) {
        Intent intent = new Intent(context, SplashActivity.class);
        intent.setAction(ACTION_REVOKE_ACCESS);
        return intent;
    }

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
                .enableAutoManage(this, this).addConnectionCallbacks(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, googleSignInOptions)
                .build();

        initViews();

        if (getIntent() != null && getIntent().getAction() != null) {
            if (ACTION_SIGN_OUT.equals(getIntent().getAction())) {
                isSignOutAction = true;
                UIUtil.exchangeViewVisibility(this, true, splashView, signInView);
            } else if (ACTION_REVOKE_ACCESS.equals(getIntent().getAction())) {
                isRevokeAccessAction = true;
                UIUtil.exchangeViewVisibility(this, true, splashView, signInView);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!isSignOutAction && !isRevokeAccessAction) {
            checkGoogleSignResult();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                GoogleSignInResult googleSignInResult =
                        Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                handleSignInResult(googleSignInResult);
                break;

            case REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_GMAIL:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        EmailSyncService.startEmailSyncService(this, account);
                        EmailManagerActivity.runEmailManagerActivity(this, account);
                        finish();
                        break;

                    case Activity.RESULT_CANCELED:
                        finish();
                        break;

                    case CheckKeysActivity.RESULT_NEGATIVE:
                        isSignOutAction = true;
                        break;
                }
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

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (isSignOutAction) {
            signOutFromGoogleAccount();
        } else if (isRevokeAccessAction) {
            revokeAccessFomGoogleAccount();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (isSignOutAction || isRevokeAccessAction) {
            finish();
            Toast.makeText(this, R.string.error_occurred_while_this_action_running, Toast
                    .LENGTH_SHORT).show();
        }
    }

    @Override
    public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_load_gmail_backups:
                UIUtil.exchangeViewVisibility(this, true, splashView, signInView);
                return new LoadPrivateKeysFromMailAsyncTaskLoader(this, account);

            default:
                return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onLoadFinished(Loader<LoaderResult> loader, LoaderResult loaderResult) {
        switch (loader.getId()) {
            case R.id.loader_id_load_gmail_backups:
                if (loaderResult.getResult() != null) {
                    ArrayList<PrivateKeyDetails> privateKeyDetailsList =
                            (ArrayList<PrivateKeyDetails>) loaderResult.getResult();
                    if (privateKeyDetailsList.isEmpty()) {
                        finish();
                        startActivity(CreateOrImportKeyActivity.newIntent(this, account, true));
                    } else {
                        startActivityForResult(CheckKeysActivity.newIntent(this,
                                privateKeyDetailsList,
                                getString(R.string.found_backup_of_your_account_key),
                                getString(R.string.continue_),
                                getString(R.string.use_another_account), false),
                                REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_GMAIL);
                    }
                } else if (loaderResult.getException() != null) {
                    UIUtil.showInfoSnackbar(getRootView(),
                            loaderResult.getException().getMessage());
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<LoaderResult> loader) {

    }

    private void handleSignInResult(GoogleSignInResult googleSignInResult) {
        if (googleSignInResult.isSuccess()) {
            GoogleSignInAccount googleSignInAccount = googleSignInResult.getSignInAccount();
            if (googleSignInAccount != null) {
                updateInformationAboutAccountInLocalDatabase(googleSignInAccount);
                account = googleSignInAccount.getAccount();
            } else {
                //todo-denbond7 handle this situation
            }

            if (SecurityUtils.isBackupKeysExist(this)) {
                EmailSyncService.startEmailSyncService(this, account);
                EmailManagerActivity.runEmailManagerActivity(this, account);
                finish();
            } else {
                startService(new Intent(this, CheckClipboardToFindPrivateKeyService.class));
                if (account != null) {
                    getSupportLoaderManager().initLoader(
                            R.id.loader_id_load_gmail_backups, null, this);
                } else {
                    //todo-denbond7 handle this situation
                }
            }
        } else {
            if (!TextUtils.isEmpty(googleSignInResult.getStatus().getStatusMessage())) {
                UIUtil.showInfoSnackbar(signInView, googleSignInResult.getStatus()
                        .getStatusMessage());
            }
            UIUtil.exchangeViewVisibility(this, false, splashView, signInView);
        }
    }

    private void updateInformationAboutAccountInLocalDatabase(GoogleSignInAccount
                                                                      googleSignInAccount) {
        AccountDaoSource accountDaoSource = new AccountDaoSource();

        boolean isAccountUpdated = accountDaoSource.updateAccountInformation(this,
                googleSignInAccount) > 0;

        if (!isAccountUpdated) {
            accountDaoSource.addRow(this, googleSignInAccount);
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
                            if (status.isSuccess()) {
                                resetAppComponents();
                                UIUtil.exchangeViewVisibility(SplashActivity.this, false,
                                        splashView,
                                        signInView);
                            } else {
                                finish();
                                Toast.makeText(SplashActivity.this, R.string
                                                .error_occurred_while_this_action_running,
                                        Toast.LENGTH_SHORT).show();
                            }
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
                            if (status.isSuccess()) {
                                resetAppComponents();
                                UIUtil.exchangeViewVisibility(SplashActivity.this, false,
                                        splashView,
                                        signInView);
                            } else {
                                finish();
                                Toast.makeText(SplashActivity.this, R.string
                                                .error_occurred_while_this_action_running,
                                        Toast.LENGTH_SHORT).show();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            UIUtil.showInfoSnackbar(getRootView(), e.getMessage());
                        }
                    }
                });
    }

    private void resetAppComponents() throws IOException {
        stopService(new Intent(this, EmailSyncService.class));
        SecurityUtils.cleanSecurityInfo(this);
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
            UIUtil.exchangeViewVisibility(this, true, splashView, signInView);
            optionalPendingResult.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(@NonNull GoogleSignInResult googleSignInResult) {
                    UIUtil.exchangeViewVisibility(SplashActivity.this, false, splashView,
                            signInView);
                    handleSignInResult(googleSignInResult);
                }
            });
        }
    }
}
