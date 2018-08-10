/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.js.JsForUiManager;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.ui.activity.base.BasePassPhraseManagerActivity;
import com.flowcrypt.email.ui.loader.ChangePassPhraseAsyncTaskLoader;
import com.flowcrypt.email.ui.loader.LoadPrivateKeysFromMailAsyncTaskLoader;
import com.flowcrypt.email.ui.loader.SaveBackupToInboxAsyncTaskLoader;
import com.flowcrypt.email.util.UIUtil;

import java.util.ArrayList;

/**
 * This activity describes a logic of changing the pass phrase of all imported private keys of an active account.
 *
 * @author Denis Bondarenko
 *         Date: 05.08.2018
 *         Time: 20:15
 *         E-mail: DenBond7@gmail.com
 */
public class ChangePassPhraseActivity extends BasePassPhraseManagerActivity
        implements LoaderManager.LoaderCallbacks<LoaderResult> {

    public static final int REQUEST_CODE_BACKUP_WITH_OPTION = 100;

    public static Intent newIntent(Context context, AccountDao accountDao) {
        Intent intent = new Intent(context, ChangePassPhraseActivity.class);
        intent.putExtra(KEY_EXTRA_ACCOUNT_DAO, accountDao);
        return intent;
    }

    @Override
    public void onConfirmPassPhraseSuccess() {
        getSupportLoaderManager().initLoader(R.id.loader_id_change_pass_phrase, null, this);
        editTextKeyPassword.setText(null);
        editTextKeyPasswordSecond.setText(null);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        if (isBackEnable) {
            super.onBackPressed();
        } else {
            Toast.makeText(this, R.string.please_wait_while_pass_phrase_will_be_changed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonSuccess:
                setResult(Activity.RESULT_OK);
                finish();
                break;

            default:
                super.onClick(v);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_BACKUP_WITH_OPTION:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Toast.makeText(this, R.string.backed_up_successfully, Toast.LENGTH_SHORT).show();
                        break;
                }
                setResult(Activity.RESULT_OK);
                finish();
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void initViews() {
        super.initViews();

        textViewFirstPasswordCheckTitle.setText(R.string.changing_pass_phrase);
        textViewSecondPasswordCheckTitle.setText(R.string.changing_pass_phrase);

        textViewSuccessTitle.setText(R.string.done);
        textViewSuccessSubTitle.setText(R.string.pass_phrase_changed);
        buttonSuccess.setText(R.string.back);
    }

    @NonNull
    @Override
    public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_change_pass_phrase:
                isBackEnable = false;
                UIUtil.exchangeViewVisibility(this, true, layoutProgress, layoutContentView);
                return new ChangePassPhraseAsyncTaskLoader(this, accountDao,
                        editTextKeyPassword.getText().toString());

            case R.id.loader_id_load_private_key_backups_from_email:
                return new LoadPrivateKeysFromMailAsyncTaskLoader(this, accountDao);

            case R.id.loader_id_save_backup_to_inbox:
                return new SaveBackupToInboxAsyncTaskLoader(this, accountDao);

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
            case R.id.loader_id_change_pass_phrase:
            case R.id.loader_id_load_private_key_backups_from_email:
            case R.id.loader_id_save_backup_to_inbox:
                isBackEnable = true;
                break;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handleSuccessLoaderResult(int loaderId, Object result) {
        switch (loaderId) {
            case R.id.loader_id_change_pass_phrase:
                JsForUiManager.getInstance(this).getJs().getStorageConnector().refresh(this);
                restartJsService();
                getSupportLoaderManager().initLoader(R.id.loader_id_load_private_key_backups_from_email, null, this);
                break;

            case R.id.loader_id_load_private_key_backups_from_email:
                ArrayList<KeyDetails> keyDetailsList = (ArrayList<KeyDetails>) result;
                if (keyDetailsList.isEmpty()) {//need to remove "!"
                    runBackupKeysActivity();
                } else {
                    getSupportLoaderManager().initLoader(R.id.loader_id_save_backup_to_inbox, null, this);
                }
                break;

            case R.id.loader_id_save_backup_to_inbox:
                isBackEnable = true;
                Toast.makeText(this, R.string.pass_phrase_changed, Toast.LENGTH_SHORT).show();
                setResult(Activity.RESULT_OK);
                finish();
                break;

            default:
                super.handleSuccessLoaderResult(loaderId, result);
                break;
        }
    }

    @Override
    public void handleFailureLoaderResult(int loaderId, Exception e) {
        switch (loaderId) {
            case R.id.loader_id_change_pass_phrase:
                isBackEnable = true;
                editTextKeyPasswordSecond.setText(null);
                UIUtil.exchangeViewVisibility(this, false, layoutProgress, layoutContentView);
                showInfoSnackbar(getRootView(), e.getMessage());
                break;

            case R.id.loader_id_load_private_key_backups_from_email:
            case R.id.loader_id_save_backup_to_inbox:
                runBackupKeysActivity();
                break;

            default:
                super.handleFailureLoaderResult(loaderId, e);
        }
    }

    protected void runBackupKeysActivity() {
        isBackEnable = true;
        Toast.makeText(this, R.string.pass_phrase_changed, Toast.LENGTH_SHORT).show();
        startActivityForResult(new Intent(this, BackupKeysActivity.class), REQUEST_CODE_BACKUP_WITH_OPTION);
    }
}
