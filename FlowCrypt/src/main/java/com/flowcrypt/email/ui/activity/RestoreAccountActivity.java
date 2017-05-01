package com.flowcrypt.email.ui.activity;

import android.accounts.Account;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.ui.activity.base.BaseAuthenticationActivity;
import com.flowcrypt.email.ui.loader.LoadPrivateKeyAsyncTaskLoader;
import com.flowcrypt.email.util.UIUtil;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;

import java.util.Arrays;
import java.util.List;

/**
 * This class described restore an account functionality.
 *
 * @author DenBond7
 *         Date: 05.01.2017
 *         Time: 01:37
 *         E-mail: DenBond7@gmail.com
 */
public class RestoreAccountActivity extends BaseAuthenticationActivity implements LoaderManager
        .LoaderCallbacks<List<String>> {

    private View restoreAccountView;
    private View layoutProgress;
    private Account account;
    private List<String> keys;

    @Override
    public View getRootView() {
        return null;
    }

    @Override
    public void handleSignInResult(GoogleSignInResult googleSignInResult, boolean isOnStartCall) {
        if (googleSignInResult.isSuccess()) {
            GoogleSignInAccount googleSignInAccount = googleSignInResult.getSignInAccount();
            if (googleSignInAccount != null) {
                account = googleSignInAccount.getAccount();
                getSupportLoaderManager().initLoader(R.id.loader_id_load_gmail_backups, null, this);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initViews();
    }

    @Override
    public Loader<List<String>> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_load_gmail_backups:
                showProgress();
                return new LoadPrivateKeyAsyncTaskLoader(this, account);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<List<String>> loader, List<String> data) {
        switch (loader.getId()) {
            case R.id.loader_id_load_gmail_backups:
                if (data != null) {
                    if (!data.isEmpty()) {
                        this.keys = data;
                        showContent();
                        Toast.makeText(this, "Path to keys:\n" + Arrays.toString(data.toArray()),
                                Toast.LENGTH_LONG)
                                .show();
                    } else {
                        showNoBackupsSnackbar();
                    }
                } else {
                    showNoBackupsSnackbar();
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<List<String>> loader) {

    }

    /**
     * Shows the progress UI and hides the restore account form.
     */
    private void showProgress() {
        if (restoreAccountView != null) {
            restoreAccountView.setVisibility(View.GONE);
        }

        if (layoutProgress != null) {
            layoutProgress.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Shows restore account form and hides the progress UI.
     */
    private void showContent() {
        if (layoutProgress != null) {
            layoutProgress.setVisibility(View.GONE);
        }

        if (restoreAccountView != null) {
            restoreAccountView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Shows no backups snackbar and "refresh" action button.
     */
    private void showNoBackupsSnackbar() {
        UIUtil.showSnackbar(layoutProgress, getString(R.string.no_backups_found),
                getString(R.string.refresh), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getSupportLoaderManager().restartLoader(R.id
                                .loader_id_load_gmail_backups, null, RestoreAccountActivity
                                .this);
                    }
                });
    }

    private void initViews() {
        setContentView(R.layout.activity_restore_account);
        restoreAccountView = findViewById(R.id.restoreAccountView);
        layoutProgress = findViewById(R.id.layoutProgress);
    }

}
