/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.KeysStorageImpl;
import com.flowcrypt.email.ui.activity.base.BasePassPhraseManagerActivity;
import com.flowcrypt.email.ui.loader.ChangePassPhraseAsyncTaskLoader;
import com.flowcrypt.email.ui.loader.LoadPrivateKeysFromMailAsyncTaskLoader;
import com.flowcrypt.email.ui.loader.SaveBackupToInboxAsyncTaskLoader;
import com.flowcrypt.email.util.UIUtil;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

/**
 * This activity describes a logic of changing the pass phrase of all imported private keys of an active account.
 *
 * @author Denis Bondarenko
 * Date: 05.08.2018
 * Time: 20:15
 * E-mail: DenBond7@gmail.com
 */
public class ChangePassPhraseActivity extends BasePassPhraseManagerActivity
    implements LoaderManager.LoaderCallbacks<LoaderResult> {

  public static final int REQUEST_CODE_BACKUP_WITH_OPTION = 100;

  public static Intent newIntent(Context context, AccountDao account) {
    Intent intent = new Intent(context, ChangePassPhraseActivity.class);
    intent.putExtra(KEY_EXTRA_ACCOUNT_DAO, account);
    return intent;
  }

  @Override
  public void onConfirmPassPhraseSuccess() {
    LoaderManager.getInstance(this).restartLoader(R.id.loader_id_change_pass_phrase, null, this);
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public void onBackPressed() {
    if (isBackEnabled) {
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

    textViewFirstPasswordCheckTitle.setText(R.string.change_pass_phrase);
    textViewSecondPasswordCheckTitle.setText(R.string.change_pass_phrase);

    textViewSuccessTitle.setText(R.string.done);
    textViewSuccessSubTitle.setText(R.string.pass_phrase_changed);
    btnSuccess.setText(R.string.back);
  }

  @NonNull
  @Override
  public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
    switch (id) {
      case R.id.loader_id_change_pass_phrase:
        isBackEnabled = false;
        UIUtil.exchangeViewVisibility(this, true, layoutProgress, layoutContentView);
        return new ChangePassPhraseAsyncTaskLoader(this, account, editTextKeyPassword.getText().toString());

      case R.id.loader_id_load_private_key_backups_from_email:
        return new LoadPrivateKeysFromMailAsyncTaskLoader(this, account);

      case R.id.loader_id_save_backup_to_inbox:
        return new SaveBackupToInboxAsyncTaskLoader(this, account);

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
        isBackEnabled = true;
        break;
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void onSuccess(int loaderId, Object result) {
    switch (loaderId) {
      case R.id.loader_id_change_pass_phrase:
        KeysStorageImpl.getInstance(this).refresh(this);
        LoaderManager.getInstance(this).initLoader(R.id.loader_id_load_private_key_backups_from_email, null, this);
        break;

      case R.id.loader_id_load_private_key_backups_from_email:
        ArrayList<KeyDetails> keyDetailsList = (ArrayList<KeyDetails>) result;
        if (keyDetailsList.isEmpty()) {
          runBackupKeysActivity();
        } else {
          LoaderManager.getInstance(this).initLoader(R.id.loader_id_save_backup_to_inbox, null, this);
        }
        break;

      case R.id.loader_id_save_backup_to_inbox:
        isBackEnabled = true;
        Toast.makeText(this, R.string.pass_phrase_changed, Toast.LENGTH_SHORT).show();
        setResult(Activity.RESULT_OK);
        finish();
        break;

      default:
        super.onSuccess(loaderId, result);
        break;
    }
  }

  @Override
  public void onError(int loaderId, Exception e) {
    switch (loaderId) {
      case R.id.loader_id_change_pass_phrase:
        isBackEnabled = true;
        editTextKeyPasswordSecond.setText(null);
        UIUtil.exchangeViewVisibility(this, false, layoutProgress, layoutContentView);
        showInfoSnackbar(getRootView(), e.getMessage());
        break;

      case R.id.loader_id_load_private_key_backups_from_email:
      case R.id.loader_id_save_backup_to_inbox:
        runBackupKeysActivity();
        break;

      default:
        super.onError(loaderId, e);
    }
  }

  protected void runBackupKeysActivity() {
    isBackEnabled = true;
    Toast.makeText(this, R.string.back_up_updated_key, Toast.LENGTH_LONG).show();
    startActivityForResult(new Intent(this, BackupKeysActivity.class), REQUEST_CODE_BACKUP_WITH_OPTION);
  }
}
