/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.AuthCredentials;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.SecurityUtils;
import com.flowcrypt.email.ui.activity.base.BaseSignInActivity;
import com.flowcrypt.email.ui.loader.LoadPrivateKeysFromMailAsyncTaskLoader;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;

import org.acra.ACRA;

import java.util.ArrayList;

/**
 * This activity describes a logic of add a new email account.
 *
 * @author Denis Bondarenko
 *         Date: 05.10.2017
 *         Time: 10:34
 *         E-mail: DenBond7@gmail.com
 */

public class AddNewAccountActivity extends BaseSignInActivity implements View.OnClickListener, GoogleApiClient
        .OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, LoaderManager.LoaderCallbacks<LoaderResult> {

    public static final String KEY_EXTRA_NEW_ACCOUNT =
            GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_NEW_ACCOUNT", AddNewAccountActivity.class);

    private static final int REQUEST_CODE_CREATE_OR_IMPORT_KEY_FOR_GMAIL = 100;
    private static final int REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_GMAIL = 101;

    private View progressView;
    private View contentView;
    private GoogleSignInAccount googleSignInAccount;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        progressView = findViewById(R.id.progressView);
        contentView = findViewById(R.id.layoutContent);
    }

    @Override
    public boolean isDisplayHomeAsUpEnabled() {
        return true;
    }

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_add_new_account;
    }

    @Override
    public View getRootView() {
        return findViewById(R.id.screenContent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                GoogleSignInResult googleSignInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                if (googleSignInResult.isSuccess()) {
                    googleSignInAccount = googleSignInResult.getSignInAccount();
                    if (googleSignInAccount != null) {
                        if (new AccountDaoSource().getAccountInformation(this,
                                googleSignInAccount.getEmail()) == null) {
                            getSupportLoaderManager().restartLoader(R.id.loader_id_load_private_key_backups_from_email,
                                    null, this);
                        } else {
                            showInfoSnackbar(getRootView(), getString(R.string.template_email_alredy_added,
                                    googleSignInAccount.getEmail()), Snackbar.LENGTH_LONG);
                        }
                    } else throw new NullPointerException("GoogleSignInAccount is null!");
                }
                break;

            case REQUEST_CODE_ADD_OTHER_ACCOUNT:
                switch (resultCode) {
                    case RESULT_OK:
                        try {
                            AuthCredentials authCredentials = data.getParcelableExtra(
                                    AddNewAccountManuallyActivity.KEY_EXTRA_AUTH_CREDENTIALS);
                            AccountDaoSource accountDaoSource = new AccountDaoSource();
                            accountDaoSource.addRow(this, authCredentials);
                            accountDaoSource.setActiveAccount(this, authCredentials.getEmail());

                            Intent intent = new Intent();
                            intent.putExtra(KEY_EXTRA_NEW_ACCOUNT, accountDaoSource.getActiveAccountInformation(this));

                            setResult(Activity.RESULT_OK, intent);
                            finish();
                        } catch (Exception e) {
                            e.printStackTrace();
                            if (ACRA.isInitialised()) {
                                ACRA.getErrorReporter().handleException(e);
                            }
                            Toast.makeText(this, R.string.unknown_error, Toast.LENGTH_SHORT).show();
                        }
                        break;

                    case AddNewAccountManuallyActivity.RESULT_CODE_CONTINUE_WITH_GMAIL:
                        super.onActivityResult(requestCode, resultCode, data);
                        break;
                }
                break;

            case REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_GMAIL:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                    case CheckKeysActivity.RESULT_NEUTRAL:
                        returnResultOk();
                        break;

                    case Activity.RESULT_CANCELED:
                    case CheckKeysActivity.RESULT_NEGATIVE:
                        UIUtil.exchangeViewVisibility(this, false, progressView, contentView);
                        break;
                }
                break;

            case REQUEST_CODE_CREATE_OR_IMPORT_KEY_FOR_GMAIL:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        returnResultOk();
                        break;

                    case Activity.RESULT_CANCELED:
                    case CreateOrImportKeyActivity.RESULT_CODE_USE_ANOTHER_ACCOUNT:
                        this.googleSignInAccount = null;
                        break;
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_load_private_key_backups_from_email:
                UIUtil.exchangeViewVisibility(this, true, progressView, contentView);
                AccountDao accountDao = new AccountDao(googleSignInAccount.getEmail(),
                        AccountDao.ACCOUNT_TYPE_GOOGLE, null, null, null, null, null);
                return new LoadPrivateKeysFromMailAsyncTaskLoader(this, accountDao);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<LoaderResult> loader, LoaderResult loaderResult) {
        handleLoaderResult(loader, loaderResult);
    }

    @Override
    public void onLoaderReset(Loader<LoaderResult> loader) {

    }

    @SuppressWarnings("unchecked")
    @Override
    public void handleSuccessLoaderResult(int loaderId, Object result) {
        switch (loaderId) {
            case R.id.loader_id_load_private_key_backups_from_email:
                ArrayList<KeyDetails> keyDetailsList = (ArrayList<KeyDetails>) result;
                if (keyDetailsList.isEmpty()) {
                    AccountDao accountDao = new AccountDao(googleSignInAccount.getEmail(),
                            AccountDao.ACCOUNT_TYPE_GOOGLE, null, null, null, null, null);
                    startActivityForResult(CreateOrImportKeyActivity.newIntent(this, accountDao, true),
                            REQUEST_CODE_CREATE_OR_IMPORT_KEY_FOR_GMAIL);
                    UIUtil.exchangeViewVisibility(this, false, progressView, contentView);
                } else {
                    startActivityForResult(CheckKeysActivity.newIntent(this,
                            keyDetailsList,
                            getString(R.string.found_backup_of_your_account_key),
                            getString(R.string.continue_),
                            SecurityUtils.isBackupKeysExist(this) ? getString(R.string
                                    .use_existing_keys) : null,
                            getString(R.string.use_another_account), true),
                            REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_GMAIL);
                }
                break;

            default:
                super.handleSuccessLoaderResult(loaderId, result);
                break;
        }
    }

    @Override
    public void handleFailureLoaderResult(int loaderId, Exception e) {
        switch (loaderId) {
            case R.id.loader_id_load_private_key_backups_from_email:
                UIUtil.exchangeViewVisibility(this, false, progressView, contentView);
                showInfoSnackbar(getRootView(), e != null && !TextUtils.isEmpty(e.getMessage()) ? e.getMessage()
                        : getString(R.string.unknown_error), Snackbar.LENGTH_LONG);
                break;

            default:
                super.handleFailureLoaderResult(loaderId, e);
                break;
        }
    }

    private void returnResultOk() {
        AccountDaoSource accountDaoSource = saveGmailAccountToDatabase();

        Intent intent = new Intent();
        intent.putExtra(KEY_EXTRA_NEW_ACCOUNT, accountDaoSource.getActiveAccountInformation(this));

        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    @NonNull
    private AccountDaoSource saveGmailAccountToDatabase() {
        AccountDaoSource accountDaoSource = new AccountDaoSource();
        accountDaoSource.addRow(this, googleSignInAccount);
        accountDaoSource.setActiveAccount(this, googleSignInAccount.getEmail());
        return accountDaoSource;
    }
}
