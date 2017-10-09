/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.View;

import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.model.SignInType;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.SecurityUtils;
import com.flowcrypt.email.service.CheckClipboardToFindPrivateKeyService;
import com.flowcrypt.email.service.EmailSyncService;
import com.flowcrypt.email.ui.activity.base.BaseSignInActivity;
import com.flowcrypt.email.ui.activity.fragment.SplashActivityFragment;
import com.flowcrypt.email.ui.loader.LoadPrivateKeysFromMailAsyncTaskLoader;
import com.flowcrypt.email.util.UIUtil;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;

import java.util.ArrayList;

/**
 * The launcher Activity.
 *
 * @author DenBond7
 *         Date: 26.14.2017
 *         Time: 14:50
 *         E-mail: DenBond7@gmail.com
 */
public class SplashActivity extends BaseSignInActivity implements SplashActivityFragment.OnSignInButtonClickListener,
        GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks,
        LoaderManager.LoaderCallbacks<LoaderResult> {

    private static final int REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_GMAIL = 101;
    private static final int REQUEST_CODE_CREATE_OR_IMPORT_KEY = 102;

    private View signInView;
    private View splashView;

    private AccountDao accountDao;

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
        initViews();

        accountDao = new AccountDaoSource().getActiveAccountInformation(this);
        if (accountDao != null) {
            if (SecurityUtils.isBackupKeysExist(this)) {
                EmailSyncService.startEmailSyncService(this);
                EmailManagerActivity.runEmailManagerActivity(this, accountDao);
                finish();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (accountDao != null) {
            if (AccountDao.ACCOUNT_TYPE_GOOGLE.equalsIgnoreCase(accountDao.getAccountType())) {
                checkGoogleSignResult();
            }
        } else {
            UIUtil.exchangeViewVisibility(this, false, splashView, signInView);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                GoogleSignInResult googleSignInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                handleSignInResult(googleSignInResult);
                break;

            case REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_GMAIL:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        EmailSyncService.startEmailSyncService(this);
                        EmailManagerActivity.runEmailManagerActivity(this, accountDao);
                        finish();
                        break;

                    case Activity.RESULT_CANCELED:
                        finish();
                        break;

                    case CheckKeysActivity.RESULT_NEGATIVE:
                        break;
                }
                break;


            case REQUEST_CODE_CREATE_OR_IMPORT_KEY:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        EmailSyncService.startEmailSyncService(this);
                        EmailManagerActivity.runEmailManagerActivity(this, accountDao);
                        finish();
                        break;

                    case Activity.RESULT_CANCELED:
                        finish();
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
    public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_load_gmail_backups:
                UIUtil.exchangeViewVisibility(this, true, splashView, signInView);
                return new LoadPrivateKeysFromMailAsyncTaskLoader(this, accountDao.getAccount());

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
                    ArrayList<KeyDetails> keyDetailsList = (ArrayList<KeyDetails>) loaderResult.getResult();
                    if (keyDetailsList.isEmpty()) {
                        finish();
                        startActivityForResult(CreateOrImportKeyActivity.newIntent(this, new AccountDaoSource()
                                .getActiveAccountInformation(this), true), REQUEST_CODE_CREATE_OR_IMPORT_KEY);
                    } else {
                        startActivityForResult(CheckKeysActivity.newIntent(this,
                                keyDetailsList,
                                getString(R.string.found_backup_of_your_account_key),
                                getString(R.string.continue_),
                                getString(R.string.use_another_account), false),
                                REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_GMAIL);
                    }
                } else if (loaderResult.getException() != null) {
                    UIUtil.showInfoSnackbar(getRootView(), loaderResult.getException().getMessage());
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
                accountDao = updateInformationAboutAccountInLocalDatabase(googleSignInAccount);
            } else {
                //todo-denbond7 handle this situation
            }

            if (SecurityUtils.isBackupKeysExist(this)) {
                EmailSyncService.startEmailSyncService(this);
                EmailManagerActivity.runEmailManagerActivity(this, accountDao);
                finish();
            } else {
                startService(new Intent(this, CheckClipboardToFindPrivateKeyService.class));
                if (accountDao != null) {
                    getSupportLoaderManager().initLoader(R.id.loader_id_load_gmail_backups, null, this);
                } else {
                    //todo-denbond7 handle this situation
                }
            }
        } else {
            if (!TextUtils.isEmpty(googleSignInResult.getStatus().getStatusMessage())) {
                UIUtil.showInfoSnackbar(signInView, googleSignInResult.getStatus().getStatusMessage());
            }
            UIUtil.exchangeViewVisibility(this, false, splashView, signInView);
        }
    }

    private AccountDao updateInformationAboutAccountInLocalDatabase(GoogleSignInAccount googleSignInAccount) {
        AccountDaoSource accountDaoSource = new AccountDaoSource();

        boolean isAccountUpdated = accountDaoSource.updateAccountInformation(this, googleSignInAccount) > 0;
        if (!isAccountUpdated) {
            accountDaoSource.addRow(this, googleSignInAccount);
        }

        return new AccountDaoSource().getAccountInformation(this, googleSignInAccount.getEmail());
    }

    private void initViews() {
        signInView = findViewById(R.id.signInView);
        splashView = findViewById(R.id.splashView);
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
                    UIUtil.exchangeViewVisibility(SplashActivity.this, false, splashView, signInView);
                    handleSignInResult(googleSignInResult);
                }
            });
        }
    }
}
