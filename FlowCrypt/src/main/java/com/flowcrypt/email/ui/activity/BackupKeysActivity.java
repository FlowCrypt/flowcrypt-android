/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.test.espresso.idling.CountingIdlingResource;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.SecurityUtils;
import com.flowcrypt.email.ui.activity.base.BaseSettingsBackStackSyncActivity;
import com.flowcrypt.email.ui.loader.SavePrivateKeyAsFileAsyncTaskLoader;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;
import com.flowcrypt.email.util.exception.ExceptionUtil;

/**
 * This activity helps to backup private keys
 *
 * @author Denis Bondarenko
 * Date: 07.08.2018
 * Time: 15:06
 * E-mail: DenBond7@gmail.com
 */
public class BackupKeysActivity extends BaseSettingsBackStackSyncActivity implements View
        .OnClickListener, RadioGroup.OnCheckedChangeListener, LoaderManager.LoaderCallbacks<LoaderResult> {

    private static final int REQUEST_CODE_GET_URI_FOR_SAVING_PRIVATE_KEY = 10;
    private CountingIdlingResource countingIdlingResource;
    private View progressBar;
    private View layoutContent;
    private View layoutSyncStatus;
    private TextView textViewOptionsHint;
    private RadioGroup radioGroupBackupsVariants;
    private Button buttonBackupAction;

    private Uri destinationUri;
    private AccountDao accountDao;

    private boolean isPrivateKeySendingNow;
    private boolean isPrivateKeySavingNow;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initViews();
        accountDao = new AccountDaoSource().getActiveAccountInformation(this);
        countingIdlingResource = new CountingIdlingResource(GeneralUtil.generateNameForIdlingResources
                (BackupKeysActivity.class), BuildConfig.DEBUG);
        countingIdlingResource.increment();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onReplyFromServiceReceived(int requestCode, int resultCode, Object obj) {
        switch (requestCode) {
            case R.id.syns_send_backup_with_private_key_to_key_owner:
                isPrivateKeySendingNow = false;
                if (!countingIdlingResource.isIdleNow()) {
                    countingIdlingResource.decrement();
                }
                setResult(Activity.RESULT_OK);
                finish();
                break;
        }
    }

    @Override
    public void onErrorFromServiceReceived(int requestCode, int errorType, Exception e) {
        switch (requestCode) {
            case R.id.syns_send_backup_with_private_key_to_key_owner:
                isPrivateKeySendingNow = false;
                UIUtil.exchangeViewVisibility(
                        BackupKeysActivity.this, false, progressBar, layoutSyncStatus);
                if (!countingIdlingResource.isIdleNow()) {
                    countingIdlingResource.decrement();
                }
                UIUtil.showSnackbar(getRootView(),
                        getString(R.string.backup_was_not_sent),
                        getString(R.string.retry),
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                layoutSyncStatus.setVisibility(View.GONE);
                                UIUtil.exchangeViewVisibility(
                                        BackupKeysActivity.this, true,
                                        progressBar, layoutContent);
                                sendMessageWithPrivateKeyBackup(R.id.syns_send_backup_with_private_key_to_key_owner);
                            }
                        });
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (isPrivateKeySavingNow) {
            getSupportLoaderManager().destroyLoader(R.id.loader_id_validate_key_from_file);
            isPrivateKeySavingNow = false;
            UIUtil.exchangeViewVisibility(this, false, progressBar, layoutContent);
        } else if (isPrivateKeySendingNow) {
            Toast.makeText(this, R.string.please_wait_while_message_will_be_sent, Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_backup_keys;
    }

    @Override
    public View getRootView() {
        return layoutContent;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonBackupAction:
                switch (radioGroupBackupsVariants.getCheckedRadioButtonId()) {
                    case R.id.radioButtonEmail:
                        if (GeneralUtil.isInternetConnectionAvailable(this)) {
                            countingIdlingResource.increment();
                            isPrivateKeySendingNow = true;
                            UIUtil.exchangeViewVisibility(this, true, progressBar, layoutContent);
                            sendMessageWithPrivateKeyBackup(R.id.syns_send_backup_with_private_key_to_key_owner);
                        } else {
                            UIUtil.showInfoSnackbar(getRootView(), getString(R.string
                                    .internet_connection_is_not_available));
                        }
                        break;

                    case R.id.radioButtonDownload:
                        destinationUri = null;
                        runActivityToChooseDestinationForExportedKey();
                        break;
                }
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        switch (group.getId()) {
            case R.id.radioGroupBackupsVariants:
                switch (checkedId) {
                    case R.id.radioButtonEmail:
                        if (textViewOptionsHint != null) {
                            textViewOptionsHint.setText(R.string.backup_as_email_hint);
                            buttonBackupAction.setText(R.string.backup_as_email);
                        }
                        break;

                    case R.id.radioButtonDownload:
                        if (textViewOptionsHint != null) {
                            textViewOptionsHint.setText(R.string.backup_as_download_hint);
                            buttonBackupAction.setText(R.string.backup_as_a_file);
                        }
                        break;
                }
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_GET_URI_FOR_SAVING_PRIVATE_KEY:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        if (data != null && data.getData() != null) {
                            try {
                                destinationUri = data.getData();
                                getSupportLoaderManager().restartLoader(R.id.loader_id_save_private_key_as_file,
                                        null, this);
                            } catch (Exception e) {
                                e.printStackTrace();
                                ExceptionUtil.handleError(e);
                                UIUtil.showInfoSnackbar(getRootView(), e.getMessage());
                            }
                        }
                        break;
                }
                break;
        }
    }

    @NonNull
    @Override
    public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_save_private_key_as_file:
                isPrivateKeySavingNow = true;
                UIUtil.exchangeViewVisibility(this, true, progressBar, layoutContent);
                return new SavePrivateKeyAsFileAsyncTaskLoader(getApplicationContext(), accountDao, destinationUri);
            default:
                return new Loader<>(this);
        }
    }

    @Override
    public void onLoadFinished(@NonNull Loader<LoaderResult> loader, LoaderResult loaderResult) {
        handleLoaderResult(loader, loaderResult);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<LoaderResult> loader) {
        switch (loader.getId()) {
            case R.id.loader_id_save_private_key_as_file:
                isPrivateKeySavingNow = false;
                UIUtil.exchangeViewVisibility(this, false, progressBar, layoutContent);
        }
    }

    @Override
    public void handleSuccessLoaderResult(int loaderId, Object result) {
        switch (loaderId) {
            case R.id.loader_id_save_private_key_as_file:
                isPrivateKeySavingNow = false;
                if ((boolean) result) {
                    setResult(Activity.RESULT_OK);
                    finish();
                } else {
                    UIUtil.exchangeViewVisibility(this, false, progressBar, layoutContent);
                    showInfoSnackbar(getRootView(), getString(R.string.error_occurred_please_try_again));
                }
                break;

            default:
                super.handleSuccessLoaderResult(loaderId, result);
        }
    }

    @Override
    public void handleFailureLoaderResult(int loaderId, Exception e) {
        switch (loaderId) {
            case R.id.loader_id_save_private_key_as_file:
                isPrivateKeySavingNow = false;
                UIUtil.exchangeViewVisibility(this, false, progressBar, layoutContent);
                showInfoSnackbar(getRootView(), e.getMessage());
                break;

            default:
                super.handleFailureLoaderResult(loaderId, e);
        }
    }

    @VisibleForTesting
    public CountingIdlingResource getCountingIdlingResource() {
        return countingIdlingResource;
    }

    private void initViews() {
        this.progressBar = findViewById(R.id.progressBar);
        this.layoutContent = findViewById(R.id.layoutContent);
        this.layoutSyncStatus = findViewById(R.id.layoutSyncStatus);
        this.textViewOptionsHint = findViewById(R.id.textViewOptionsHint);
        this.radioGroupBackupsVariants = findViewById(R.id.radioGroupBackupsVariants);

        if (radioGroupBackupsVariants != null) {
            radioGroupBackupsVariants.setOnCheckedChangeListener(this);
        }

        if (findViewById(R.id.buttonSeeMoreBackupOptions) != null) {
            findViewById(R.id.buttonSeeMoreBackupOptions).setOnClickListener(this);
        }

        buttonBackupAction = findViewById(R.id.buttonBackupAction);
        if (buttonBackupAction != null) {
            buttonBackupAction.setOnClickListener(this);
        }
    }

    /**
     * Start a new Activity with return results to choose a destination for an exported key.
     */
    private void runActivityToChooseDestinationForExportedKey() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(Constants.MIME_TYPE_PGP_KEY);
        intent.putExtra(Intent.EXTRA_TITLE, SecurityUtils.generateNameForPrivateKey(accountDao.getEmail()));
        startActivityForResult(intent, REQUEST_CODE_GET_URI_FOR_SAVING_PRIVATE_KEY);
    }
}
