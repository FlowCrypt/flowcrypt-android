/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.AuthCredentials;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.database.provider.FlowcryptContract;
import com.flowcrypt.email.jobscheduler.ForwardedAttachmentsDownloaderJobService;
import com.flowcrypt.email.jobscheduler.MessagesSenderJobService;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.SecurityUtils;
import com.flowcrypt.email.service.CheckClipboardToFindKeyService;
import com.flowcrypt.email.service.EmailSyncService;
import com.flowcrypt.email.service.JsBackgroundService;
import com.flowcrypt.email.ui.activity.base.BaseSignInActivity;
import com.flowcrypt.email.ui.loader.LoadPrivateKeysFromMailAsyncTaskLoader;
import com.flowcrypt.email.util.UIUtil;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.preference.PreferenceManager;

/**
 * The launcher Activity.
 *
 * @author DenBond7
 * Date: 26.14.2017
 * Time: 14:50
 * E-mail: DenBond7@gmail.com
 */
public class SplashActivity extends BaseSignInActivity implements LoaderManager.LoaderCallbacks<LoaderResult> {

  private static final int REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_GMAIL = 101;
  private static final int REQUEST_CODE_CREATE_OR_IMPORT_KEY = 102;

  private View signInView;
  private View splashView;

  private AccountDao accountDao;
  private boolean isStartCheckKeysActivityEnable;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    PreferenceManager.setDefaultValues(this, R.xml.preferences_notifications_settings, false);

    JsBackgroundService.start(this);
    ForwardedAttachmentsDownloaderJobService.schedule(getApplicationContext());
    MessagesSenderJobService.schedule(getApplicationContext());

    initViews();

    accountDao = new AccountDaoSource().getActiveAccountInformation(this);
    if (accountDao != null) {
      if (SecurityUtils.isBackupKeysExist(this)) {
        EmailSyncService.startEmailSyncService(this);
        EmailManagerActivity.runEmailManagerActivity(this);
        finish();
      }
    }
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
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_CODE_SIGN_IN:
        GoogleSignInResult googleSignInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
        handleSignInResult(googleSignInResult);
        break;

      case REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_GMAIL:
        isStartCheckKeysActivityEnable = false;

        switch (resultCode) {
          case Activity.RESULT_OK:
          case CheckKeysActivity.RESULT_NEUTRAL:
            runEmailManagerActivityWithCurrentGmailAccount();
            break;

          case Activity.RESULT_CANCELED:
          case CheckKeysActivity.RESULT_NEGATIVE:
            this.currentGoogleSignInAccount = null;
            UIUtil.exchangeViewVisibility(this, false, splashView, signInView);
            break;
        }
        break;

      case REQUEST_CODE_CREATE_OR_IMPORT_KEY:
        switch (resultCode) {
          case Activity.RESULT_OK:
            runEmailManagerActivityWithCurrentGmailAccount();
            break;

          case Activity.RESULT_CANCELED:
          case CreateOrImportKeyActivity.RESULT_CODE_USE_ANOTHER_ACCOUNT:
            this.currentGoogleSignInAccount = null;
            UIUtil.exchangeViewVisibility(this, false, splashView, signInView);
            break;
        }
        break;

      case REQUEST_CODE_ADD_OTHER_ACCOUNT:
        switch (resultCode) {
          case Activity.RESULT_OK:
            try {
              AuthCredentials authCredentials = data.getParcelableExtra(AddNewAccountManuallyActivity
                  .KEY_EXTRA_AUTH_CREDENTIALS);
              if (authCredentials != null) {
                AccountDaoSource accountDaoSource = new AccountDaoSource();
                accountDaoSource.addRow(this, authCredentials);
                EmailSyncService.startEmailSyncService(this);

                AccountDao accountDao =
                    accountDaoSource.getAccountInformation(this, authCredentials.getEmail());

                if (accountDao != null) {
                  EmailManagerActivity.runEmailManagerActivity(this);
                  finish();
                } else {
                  Toast.makeText(this, R.string.error_occurred_try_again_later,
                      Toast.LENGTH_SHORT).show();
                }
              } else {
                ExceptionUtil.handleError(new NullPointerException("AuthCredentials is null!"));
                Toast.makeText(this, R.string.error_occurred_try_again_later,
                    Toast.LENGTH_SHORT).show();
              }
            } catch (Exception e) {
              e.printStackTrace();
              ExceptionUtil.handleError(e);
              Toast.makeText(this, R.string.unknown_error, Toast.LENGTH_SHORT).show();
            }
            break;

          case CreateOrImportKeyActivity.RESULT_CODE_USE_ANOTHER_ACCOUNT:
            this.accountDao = null;
            if (data != null) {
              clearInfoAboutOldAccount((AccountDao) data.getParcelableExtra(CreateOrImportKeyActivity
                  .EXTRA_KEY_ACCOUNT_DAO));
            }
            break;

          case AddNewAccountManuallyActivity.RESULT_CODE_CONTINUE_WITH_GMAIL:
            super.onActivityResult(requestCode, resultCode, data);
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
      case R.id.buttonPrivacy:
        startActivity(HtmlViewFromAssetsRawActivity.newIntent(this,
            getString(R.string.privacy), "html/privacy.htm"));
        break;

      case R.id.buttonTerms:
        startActivity(HtmlViewFromAssetsRawActivity.newIntent(this,
            getString(R.string.terms), "html/terms.htm"));
        break;

      case R.id.buttonSecurity:
        startActivity(HtmlViewFromAssetsRawActivity.newIntent(this,
            getString(R.string.security), "html/security.htm"));
        break;
      default:
        super.onClick(v);
    }

  }

  @NonNull
  @Override
  public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
    switch (id) {
      case R.id.loader_id_load_private_key_backups_from_email:
        isStartCheckKeysActivityEnable = true;

        AccountDao accountDao = null;
        UIUtil.exchangeViewVisibility(this, true, splashView, signInView);
        if (currentGoogleSignInAccount != null) {
          accountDao = new AccountDao(currentGoogleSignInAccount.getEmail(), AccountDao.ACCOUNT_TYPE_GOOGLE);
        }
        return new LoadPrivateKeysFromMailAsyncTaskLoader(this, accountDao);

      default:
        return new Loader<>(this);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void onLoadFinished(@NonNull Loader<LoaderResult> loader, LoaderResult loaderResult) {
    switch (loader.getId()) {
      case R.id.loader_id_load_private_key_backups_from_email:
        if (loaderResult.getResult() != null) {
          ArrayList<KeyDetails> keyDetailsList = (ArrayList<KeyDetails>) loaderResult.getResult();
          if (keyDetailsList.isEmpty()) {
            if (currentGoogleSignInAccount != null) {
              startActivityForResult(CreateOrImportKeyActivity.newIntent(this,
                  new AccountDao(currentGoogleSignInAccount), true),
                  REQUEST_CODE_CREATE_OR_IMPORT_KEY);
            }
          } else if (isStartCheckKeysActivityEnable) {
            startActivityForResult(CheckKeysActivity.newIntent(this,
                keyDetailsList,
                getResources().getQuantityString(
                    R.plurals.found_backup_of_your_account_key,
                    keyDetailsList.size(),
                    keyDetailsList.size()),
                getString(R.string.continue_),
                SecurityUtils.isBackupKeysExist(this) ? getString(R.string
                    .use_existing_keys) : null,
                getString(R.string.use_another_account)),
                REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_GMAIL);
          }
        } else if (loaderResult.getException() != null) {
          UIUtil.exchangeViewVisibility(this, false, splashView, signInView);
          UIUtil.showInfoSnackbar(getRootView(), loaderResult.getException().getMessage());
        }
        break;
    }
  }

  @Override
  public void onLoaderReset(@NonNull Loader<LoaderResult> loader) {

  }

  private void runEmailManagerActivityWithCurrentGmailAccount() {
    EmailSyncService.startEmailSyncService(this);

    AccountDao accountDao = addGmailAccount(currentGoogleSignInAccount);
    if (accountDao != null) {
      EmailManagerActivity.runEmailManagerActivity(this);
      finish();
    } else {
      Toast.makeText(this, R.string.error_occurred_try_again_later,
          Toast.LENGTH_SHORT).show();
    }
  }

  /**
   * Clear information about created but a not used account.
   *
   * @param accountDao The account which will be deleted from the local database.
   */
  private void clearInfoAboutOldAccount(AccountDao accountDao) {
    if (accountDao != null) {
      getContentResolver().delete(Uri.parse(FlowcryptContract.AUTHORITY_URI + "/"
          + FlowcryptContract.CLEAN_DATABASE), null, new String[]{accountDao.getEmail()});
    }
  }

  private void handleSignInResult(GoogleSignInResult googleSignInResult) {
    if (googleSignInResult.isSuccess()) {
      currentGoogleSignInAccount = googleSignInResult.getSignInAccount();

      startService(new Intent(this, CheckClipboardToFindKeyService.class));
      getSupportLoaderManager().restartLoader(R.id.loader_id_load_private_key_backups_from_email, null, this);
    } else {
      if (!TextUtils.isEmpty(googleSignInResult.getStatus().getStatusMessage())) {
        UIUtil.showInfoSnackbar(signInView, googleSignInResult.getStatus().getStatusMessage());
      }
      UIUtil.exchangeViewVisibility(this, false, splashView, signInView);
    }
  }

  /**
   * Created a GMAIL {@link AccountDao} and add it to the database.
   *
   * @param googleSignInAccount The {@link GoogleSignInAccount} object which contains information about a Google
   *                            account.
   * @return Generated {@link AccountDao}.
   */
  private AccountDao addGmailAccount(GoogleSignInAccount googleSignInAccount) {
    if (googleSignInAccount == null) {
      ExceptionUtil.handleError(new NullPointerException("GoogleSignInAccount is null!"));
      return null;
    }

    AccountDaoSource accountDaoSource = new AccountDaoSource();

    boolean isAccountUpdated = accountDaoSource.updateAccountInformation(this, googleSignInAccount) > 0;
    if (!isAccountUpdated) {
      accountDaoSource.addRow(this, googleSignInAccount);
    }

    return new AccountDaoSource().getAccountInformation(this, googleSignInAccount.getEmail());
  }

  /**
   * In this method we init all used views.
   */
  private void initViews() {
    signInView = findViewById(R.id.signInView);
    splashView = findViewById(R.id.splashView);

    if (findViewById(R.id.buttonSignInWithGmail) != null) {
      findViewById(R.id.buttonSignInWithGmail).setOnClickListener(this);
    }

    if (findViewById(R.id.buttonOtherEmailProvider) != null) {
      findViewById(R.id.buttonOtherEmailProvider).setOnClickListener(this);
    }

    if (findViewById(R.id.buttonPrivacy) != null) {
      findViewById(R.id.buttonPrivacy).setOnClickListener(this);
    }

    if (findViewById(R.id.buttonTerms) != null) {
      findViewById(R.id.buttonTerms).setOnClickListener(this);
    }

    if (findViewById(R.id.buttonSecurity) != null) {
      findViewById(R.id.buttonSecurity).setOnClickListener(this);
    }
  }
}
