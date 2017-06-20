/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;

import com.flowcrypt.email.R;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.ui.activity.base.BaseActivity;
import com.flowcrypt.email.ui.activity.fragment.RestoreAccountFragment;
import com.flowcrypt.email.ui.loader.LoadPrivateKeysFromMailAsyncTaskLoader;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

import java.util.List;

/**
 * This class described restore an account functionality.
 *
 * @author DenBond7
 *         Date: 05.01.2017
 *         Time: 01:37
 *         E-mail: DenBond7@gmail.com
 */
public class RestoreAccountActivity extends BaseActivity
        implements LoaderManager.LoaderCallbacks<LoaderResult> {

    public static final String KEY_EXTRA_PRIVATE_KEYS = GeneralUtil.generateUniqueExtraKey(
            "KEY_EXTRA_PRIVATE_KEYS", RestoreAccountActivity.class);

    public static final String KEY_EXTRA_ACCOUNT = GeneralUtil.generateUniqueExtraKey(
            "KEY_EXTRA_ACCOUNT", RestoreAccountActivity.class);

    private View restoreAccountView;
    private View layoutProgress;
    private Account account;
    private List<String> privateKeys;
    private boolean isThrowErrorIfDuplicateFound;

    @Override
    public View getRootView() {
        return null;
    }

    @Override
    public boolean isDisplayHomeAsUpEnabled() {
        return false;
    }

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_restore_account;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() != null) {
            this.privateKeys = getIntent().getStringArrayListExtra(KEY_EXTRA_PRIVATE_KEYS);
            this.account = getIntent().getParcelableExtra(KEY_EXTRA_ACCOUNT);
            if (getIntent().hasExtra(KEY_EXTRA_PRIVATE_KEYS)) {
                this.isThrowErrorIfDuplicateFound = true;
            }
        }

        initViews();
        if (privateKeys == null) {
            if (account != null) {
                getSupportLoaderManager().initLoader(R.id.loader_id_load_gmail_backups, null,
                        this);
            } else {
                finish();
            }
        } else {
            showContent();
            updateKeysOnRestoreAccountFragment();
        }
    }

    @Override
    public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_load_gmail_backups:
                showProgress();
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
                if (loaderResult != null) {
                    if (loaderResult.getResult() != null) {
                        List<String> stringList = (List<String>) loaderResult.getResult();
                        if (stringList != null) {
                            if (!stringList.isEmpty()) {
                                this.privateKeys = stringList;
                                showContent();
                                updateKeysOnRestoreAccountFragment();
                            } else {
                                finish();
                                startActivity(new Intent(this, CreateOrImportKeyActivity.class));
                            }
                        } else {
                            showNoBackupsSnackbar();
                        }
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
    public void onLoaderReset(Loader<LoaderResult> loader) {

    }

    /**
     * Update privateKeys list in RestoreAccountFragment.
     */
    private void updateKeysOnRestoreAccountFragment() {
        RestoreAccountFragment restoreAccountFragment = (RestoreAccountFragment)
                getSupportFragmentManager()
                        .findFragmentById(R.id.restoreAccountFragment);

        if (restoreAccountFragment != null) {
            restoreAccountFragment.setPrivateKeys(privateKeys, isThrowErrorIfDuplicateFound);
        }
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
        restoreAccountView = findViewById(R.id.restoreAccountView);
        layoutProgress = findViewById(R.id.layoutProgress);
    }

}
