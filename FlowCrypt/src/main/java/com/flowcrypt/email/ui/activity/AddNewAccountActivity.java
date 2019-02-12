/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.AuthCredentials;
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.SecurityUtils;
import com.flowcrypt.email.ui.activity.base.BaseSignInActivity;
import com.flowcrypt.email.ui.loader.LoadPrivateKeysFromMailAsyncTaskLoader;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.util.CollectionUtils;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

/**
 * This activity describes a logic of add a new email account.
 *
 * @author Denis Bondarenko
 * Date: 05.10.2017
 * Time: 10:34
 * E-mail: DenBond7@gmail.com
 */

public class AddNewAccountActivity extends BaseSignInActivity implements View.OnClickListener, GoogleApiClient
    .OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, LoaderManager.LoaderCallbacks<LoaderResult> {

  public static final String KEY_EXTRA_NEW_ACCOUNT =
      GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_NEW_ACCOUNT", AddNewAccountActivity.class);

  private static final int REQUEST_CODE_CREATE_OR_IMPORT_KEY_FOR_GMAIL = 100;
  private static final int REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_GMAIL = 101;

  private View progressView;
  private View contentView;

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
        GoogleSignInResult signInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
        if (signInResult.isSuccess()) {
          this.sign = signInResult.getSignInAccount();
          if (this.sign != null) {
            if (new AccountDaoSource().getAccountInformation(this, this.sign.getEmail()) == null) {
              LoaderManager.getInstance(this).restartLoader(R.id.loader_id_load_private_key_backups_from_email, null,
                  this);
            } else {
              showInfoSnackbar(getRootView(), getString(R.string.template_email_alredy_added,
                  this.sign.getEmail()), Snackbar.LENGTH_LONG);
            }
          } else throw new NullPointerException("GoogleSignInAccount is null!");
        }
        break;

      case REQUEST_CODE_ADD_OTHER_ACCOUNT:
        switch (resultCode) {
          case RESULT_OK:
            try {
              AuthCredentials authCreds = data.getParcelableExtra(AddNewAccountManuallyActivity
                  .KEY_EXTRA_AUTH_CREDENTIALS);
              AccountDaoSource accountDaoSource = new AccountDaoSource();
              accountDaoSource.addRow(this, authCreds);
              accountDaoSource.setActiveAccount(this, authCreds.getEmail());

              Intent intent = new Intent();
              intent.putExtra(KEY_EXTRA_NEW_ACCOUNT, accountDaoSource.getActiveAccountInformation(this));

              setResult(Activity.RESULT_OK, intent);
              finish();
            } catch (Exception e) {
              e.printStackTrace();
              ExceptionUtil.handleError(e);
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
            LoaderManager.getInstance(this).destroyLoader(R.id.loader_id_load_private_key_backups_from_email);
            break;
        }
        break;

      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  @NonNull
  public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
    switch (id) {
      case R.id.loader_id_load_private_key_backups_from_email:
        UIUtil.exchangeViewVisibility(this, true, progressView, contentView);
        AccountDao account = new AccountDao(sign.getEmail(), AccountDao.ACCOUNT_TYPE_GOOGLE);
        return new LoadPrivateKeysFromMailAsyncTaskLoader(this, account);

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
      case R.id.loader_id_load_private_key_backups_from_email:
        this.sign = null;
        break;
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void onSuccess(int loaderId, Object result) {
    switch (loaderId) {
      case R.id.loader_id_load_private_key_backups_from_email:
        ArrayList<NodeKeyDetails> keyDetailsList = (ArrayList<NodeKeyDetails>) result;
        if (CollectionUtils.isEmpty(keyDetailsList)) {
          AccountDao account = new AccountDao(sign.getEmail(), AccountDao.ACCOUNT_TYPE_GOOGLE);
          startActivityForResult(CreateOrImportKeyActivity.newIntent(this, account, true),
              REQUEST_CODE_CREATE_OR_IMPORT_KEY_FOR_GMAIL);
          UIUtil.exchangeViewVisibility(this, false, progressView, contentView);
        } else {
          String bottomTitle = getResources().getQuantityString(R.plurals.found_backup_of_your_account_key,
              keyDetailsList.size(), keyDetailsList.size());
          String neutralBtnTitle = SecurityUtils.hasBackup(this) ? getString(R.string.use_existing_keys) : null;
          Intent intent = CheckKeysActivity.newIntent(this, keyDetailsList, KeyDetails.Type.EMAIL, bottomTitle,
              getString(R.string.continue_), neutralBtnTitle, getString(R.string.use_another_account));
          startActivityForResult(intent, REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_GMAIL);
        }
        break;

      default:
        super.onSuccess(loaderId, result);
        break;
    }
  }

  @Override
  public void onError(int loaderId, Exception e) {
    switch (loaderId) {
      case R.id.loader_id_load_private_key_backups_from_email:
        UIUtil.exchangeViewVisibility(this, false, progressView, contentView);
        showInfoSnackbar(getRootView(), e != null && !TextUtils.isEmpty(e.getMessage()) ? e.getMessage()
            : getString(R.string.unknown_error), Snackbar.LENGTH_LONG);
        break;

      default:
        super.onError(loaderId, e);
        break;
    }
  }

  private void returnResultOk() {
    AccountDaoSource accountDaoSource = saveGmailAccount();

    Intent intent = new Intent();
    intent.putExtra(KEY_EXTRA_NEW_ACCOUNT, accountDaoSource.getActiveAccountInformation(this));

    setResult(Activity.RESULT_OK, intent);
    finish();
  }

  @NonNull
  private AccountDaoSource saveGmailAccount() {
    AccountDaoSource accountDaoSource = new AccountDaoSource();
    accountDaoSource.addRow(this, sign);
    accountDaoSource.setActiveAccount(this, sign.getEmail());
    return accountDaoSource;
  }
}
