/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.gmail.GmailConstants;
import com.flowcrypt.email.api.email.model.AuthCredentials;
import com.flowcrypt.email.api.email.model.SecurityType;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.SecurityUtils;
import com.flowcrypt.email.ui.activity.base.BaseActivity;
import com.flowcrypt.email.ui.loader.CheckEmailSettingsAsyncTaskLoader;
import com.flowcrypt.email.ui.loader.LoadPrivateKeysFromMailAsyncTaskLoader;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.SharedPreferencesHelper;
import com.flowcrypt.email.util.UIUtil;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.mail.util.MailConnectException;

import java.net.SocketTimeoutException;
import java.util.ArrayList;

import javax.mail.AuthenticationFailedException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.preference.PreferenceManager;

/**
 * This activity describes a logic of adding a new account of other email providers.
 *
 * @author Denis Bondarenko
 * Date: 12.09.2017
 * Time: 17:21
 * E-mail: DenBond7@gmail.com
 */

public class AddNewAccountManuallyActivity extends BaseActivity implements CompoundButton.OnCheckedChangeListener,
    AdapterView.OnItemSelectedListener, View.OnClickListener, TextWatcher,
    LoaderManager.LoaderCallbacks<LoaderResult> {
  public static final int RESULT_CODE_CONTINUE_WITH_GMAIL = 101;

  public static final String KEY_EXTRA_AUTH_CREDENTIALS =
      GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_AUTH_CREDENTIALS", AddNewAccountManuallyActivity.class);

  private static final int REQUEST_CODE_ADD_NEW_ACCOUNT = 10;
  private static final int REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_EMAIL = 11;

  private EditText editTextEmail;
  private EditText editTextUserName;
  private EditText editTextPassword;
  private EditText editTextImapServer;
  private EditText editTextImapPort;
  private EditText editTextSmtpServer;
  private EditText editTextSmtpPort;
  private EditText editTextSmtpUsername;
  private EditText editTextSmtpPassword;
  private Spinner spinnerImapSecyrityType;
  private Spinner spinnerSmtpSecyrityType;
  private View layoutSmtpSignIn;
  private View progressView;
  private View contentView;
  private CheckBox checkBoxRequireSignInForSmtp;
  private AuthCredentials authCreds;

  private boolean isImapSpinnerRestored;
  private boolean isSmtpSpinnerRestored;

  @Override
  public boolean isDisplayHomeAsUpEnabled() {
    return true;
  }

  @Override
  public int getContentViewResourceId() {
    return R.layout.activity_add_new_account_manually;
  }

  @Override
  public View getRootView() {
    return contentView;
  }

  @Override
  public void onJsServiceConnected() {

  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.authCreds = getTempAuthCredentials();

    if (authCreds == null) {
      isImapSpinnerRestored = true;
      isSmtpSpinnerRestored = true;
    }

    initViews(savedInstanceState);
  }

  @Override
  public void onPause() {
    super.onPause();
    saveTempCredentials();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_CODE_ADD_NEW_ACCOUNT:
        switch (resultCode) {
          case Activity.RESULT_OK:
            returnOkResult();
            break;

          case CreateOrImportKeyActivity.RESULT_CODE_USE_ANOTHER_ACCOUNT:
            setResult(CreateOrImportKeyActivity.RESULT_CODE_USE_ANOTHER_ACCOUNT, data);
            finish();
            break;
        }
        break;

      case REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_EMAIL:
        switch (resultCode) {
          case Activity.RESULT_OK:
          case CheckKeysActivity.RESULT_NEUTRAL:
            returnOkResult();
            break;

          case Activity.RESULT_CANCELED:
            UIUtil.exchangeViewVisibility(this, false, progressView, contentView);
            break;

          case CheckKeysActivity.RESULT_NEGATIVE:
            setResult(CreateOrImportKeyActivity.RESULT_CODE_USE_ANOTHER_ACCOUNT, data);
            finish();
            break;
        }
        break;

      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    switch (buttonView.getId()) {
      case R.id.checkBoxRequireSignInForSmtp:
        layoutSmtpSignIn.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        break;
    }
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    switch (parent.getId()) {
      case R.id.spinnerImapSecurityType:
        SecurityType imapSecurityType = (SecurityType) parent.getAdapter().getItem(position);
        if (isImapSpinnerRestored) {
          editTextImapPort.setText(String.valueOf(imapSecurityType.getDefImapPort()));
        } else {
          isImapSpinnerRestored = true;
        }
        break;

      case R.id.spinnerSmtpSecyrityType:
        SecurityType smtpSecurityType = (SecurityType) parent.getAdapter().getItem(position);
        if (isSmtpSpinnerRestored) {
          editTextSmtpPort.setText(String.valueOf(smtpSecurityType.getDefaultSmtpPort()));
        } else {
          isSmtpSpinnerRestored = true;
        }
        break;
    }
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {

  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.buttonTryToConnect:
        if (isDataCorrect()) {
          authCreds = generateAuthCredentials();
          UIUtil.hideSoftInput(this, getRootView());
          if (checkDuplicate()) {
            LoaderManager.getInstance(this).restartLoader(R.id.loader_id_check_email_settings, null, this);
          } else {
            showInfoSnackbar(getRootView(), getString(R.string.template_email_alredy_added,
                authCreds.getEmail()), Snackbar.LENGTH_LONG);
          }
        }
        break;
    }
  }

  @Override
  public void beforeTextChanged(CharSequence s, int start, int count, int after) {

  }

  @Override
  public void onTextChanged(CharSequence s, int start, int before, int count) {

  }

  @Override
  public void afterTextChanged(Editable editable) {
    if (GeneralUtil.isEmailValid(editable)) {
      String email = editable.toString();
      String mainDomain = email.substring(email.indexOf('@') + 1, email.length());
      editTextImapServer.setText(getString(R.string.template_imap_server, mainDomain));
      editTextSmtpServer.setText(getString(R.string.template_smtp_server, mainDomain));
      editTextUserName.setText(email.substring(0, email.indexOf('@')));
      editTextSmtpUsername.setText(email.substring(0, email.indexOf('@')));
    }
  }

  @NonNull
  @Override
  public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
    switch (id) {
      case R.id.loader_id_check_email_settings:
        UIUtil.exchangeViewVisibility(this, true, progressView, contentView);

        authCreds = generateAuthCredentials();
        return new CheckEmailSettingsAsyncTaskLoader(this, authCreds);

      case R.id.loader_id_load_private_key_backups_from_email:
        UIUtil.exchangeViewVisibility(this, true, progressView, contentView);
        AccountDao account = new AccountDao(authCreds.getEmail(), null, authCreds.getUsername(), null, null, null,
            authCreds, false);
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

  }

  @SuppressWarnings("unchecked")
  @Override
  public void onSuccess(int loaderId, Object result) {
    switch (loaderId) {
      case R.id.loader_id_check_email_settings:
        boolean isCorrect = (boolean) result;
        if (isCorrect) {
          LoaderManager.getInstance(this).restartLoader(R.id.loader_id_load_private_key_backups_from_email, null, this);
        } else {
          UIUtil.exchangeViewVisibility(this, false, progressView, contentView);
          showInfoSnackbar(getRootView(), getString(R.string.settings_not_valid), Snackbar.LENGTH_LONG);
        }
        break;

      case R.id.loader_id_load_private_key_backups_from_email:
        ArrayList<KeyDetails> keyDetailsList = (ArrayList<KeyDetails>) result;
        if (keyDetailsList.isEmpty()) {
          AccountDao account = new AccountDao(authCreds.getEmail(), null, authCreds.getUsername(), null, null, null,
              authCreds, false);
          startActivityForResult(CreateOrImportKeyActivity.newIntent(this, account, true),
              REQUEST_CODE_ADD_NEW_ACCOUNT);
          UIUtil.exchangeViewVisibility(this, false, progressView, contentView);
        } else {
          String bottomTitle = getResources().getQuantityString(R.plurals.found_backup_of_your_account_key,
              keyDetailsList.size(), keyDetailsList.size());
          String neutralBtnTitle = SecurityUtils.hasBackup(this) ? getString(R.string.use_existing_keys) : null;
          Intent intent = CheckKeysActivity.newIntent(this, keyDetailsList, bottomTitle,
              getString(R.string.continue_), neutralBtnTitle, getString(R.string.use_another_account));
          startActivityForResult(intent, REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_EMAIL);
        }

        LoaderManager.getInstance(this).destroyLoader(R.id.loader_id_load_private_key_backups_from_email);
        break;

      default:
        super.onSuccess(loaderId, result);
        break;
    }
  }

  @Override
  public void onError(int loaderId, Exception e) {
    switch (loaderId) {
      case R.id.loader_id_check_email_settings:
        UIUtil.exchangeViewVisibility(this, false, progressView, contentView);
        Throwable original = e != null ? e.getCause() : null;
        if (original != null) {
          if (original instanceof AuthenticationFailedException) {
            boolean isGmailImapServer = editTextImapServer.getText().toString().equalsIgnoreCase(GmailConstants
                .GMAIL_IMAP_SERVER);
            boolean isMsgEmpty = TextUtils.isEmpty(original.getMessage());
            boolean hasAlert = original.getMessage().startsWith(GmailConstants
                .GMAIL_ALERT_MESSAGE_WHEN_LESS_SECURE_NOT_ALLOWED);
            if (isGmailImapServer && !isMsgEmpty && hasAlert) {
              showSnackbar(getRootView(), getString(R.string.less_secure_login_is_not_allowed),
                  getString(android.R.string.ok), Snackbar.LENGTH_LONG, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                      setResult(RESULT_CODE_CONTINUE_WITH_GMAIL);
                      finish();
                    }
                  });
            } else {
              showInfoSnackbar(getRootView(), !TextUtils.isEmpty(e.getMessage()) ? e.getMessage()
                  : getString(R.string.unknown_error), Snackbar.LENGTH_LONG);
            }
          } else if (original instanceof MailConnectException || original instanceof SocketTimeoutException) {
            showSnackbar(getRootView(), getString(R.string.network_error_please_retry), getString(R.string.retry),
                Snackbar.LENGTH_LONG, new View.OnClickListener() {
                  @Override
                  public void onClick(View v) {
                    LoaderManager.getInstance(AddNewAccountManuallyActivity.this)
                        .restartLoader(R.id.loader_id_check_email_settings, null, AddNewAccountManuallyActivity.this);
                  }
                });
          }
        } else {
          showInfoSnackbar(getRootView(), e != null && !TextUtils.isEmpty(e.getMessage()) ? e.getMessage()
              : getString(R.string.unknown_error), Snackbar.LENGTH_LONG);
        }
        break;

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

  /**
   * Return the {@link Activity#RESULT_OK} to the initiator-activity.
   */
  private void returnOkResult() {
    authCreds = generateAuthCredentials();
    Intent intent = new Intent();
    intent.putExtra(KEY_EXTRA_AUTH_CREDENTIALS, authCreds);
    setResult(Activity.RESULT_OK, intent);
    finish();
  }

  /**
   * Check that current email is not duplicate and not added yet.
   *
   * @return true if email not added yet, otherwise false.
   */
  private boolean checkDuplicate() {
    return new AccountDaoSource().getAccountInformation(this, authCreds.getEmail()) == null;
  }

  private void initViews(Bundle savedInstanceState) {
    editTextEmail = findViewById(R.id.editTextEmail);
    editTextUserName = findViewById(R.id.editTextUserName);
    editTextPassword = findViewById(R.id.editTextPassword);
    editTextImapServer = findViewById(R.id.editTextImapServer);
    editTextImapPort = findViewById(R.id.editTextImapPort);
    editTextSmtpServer = findViewById(R.id.editTextSmtpServer);
    editTextSmtpPort = findViewById(R.id.editTextSmtpPort);
    editTextSmtpUsername = findViewById(R.id.editTextSmtpUsername);
    editTextSmtpPassword = findViewById(R.id.editTextSmtpPassword);

    editTextEmail.addTextChangedListener(this);

    layoutSmtpSignIn = findViewById(R.id.layoutSmtpSignIn);
    progressView = findViewById(R.id.progressView);
    contentView = findViewById(R.id.layoutContent);
    checkBoxRequireSignInForSmtp = findViewById(R.id.checkBoxRequireSignInForSmtp);
    checkBoxRequireSignInForSmtp.setOnCheckedChangeListener(this);

    spinnerImapSecyrityType = findViewById(R.id.spinnerImapSecurityType);
    spinnerSmtpSecyrityType = findViewById(R.id.spinnerSmtpSecyrityType);

    ArrayAdapter<SecurityType> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
        SecurityType.generateSecurityTypes(this));

    spinnerImapSecyrityType.setAdapter(adapter);
    spinnerSmtpSecyrityType.setAdapter(adapter);

    spinnerImapSecyrityType.setOnItemSelectedListener(this);
    spinnerSmtpSecyrityType.setOnItemSelectedListener(this);

    if (findViewById(R.id.buttonTryToConnect) != null) {
      findViewById(R.id.buttonTryToConnect).setOnClickListener(this);
    }

    if (savedInstanceState == null) {
      updateView();
    }
  }

  /**
   * Update the current views if {@link AuthCredentials} not null.l
   */
  private void updateView() {
    if (authCreds != null) {

      editTextEmail.setText(authCreds.getEmail());
      editTextUserName.setText(authCreds.getUsername());
      editTextImapServer.setText(authCreds.getImapServer());
      editTextImapPort.setText(String.valueOf(authCreds.getImapPort()));
      editTextSmtpServer.setText(authCreds.getSmtpServer());
      editTextSmtpPort.setText(String.valueOf(authCreds.getSmtpPort()));
      checkBoxRequireSignInForSmtp.setChecked(authCreds.hasCustomSignInForSmtp());
      editTextSmtpUsername.setText(authCreds.getSmtpSigInUsername());

      int imapOptionsCount = spinnerImapSecyrityType.getAdapter().getCount();
      for (int i = 0; i < imapOptionsCount; i++) {
        if (authCreds.getImapOpt() == ((SecurityType) spinnerImapSecyrityType.getAdapter().getItem(i)).getOption()) {
          spinnerImapSecyrityType.setSelection(i);
        }
      }

      int smtpOptionsCount = spinnerSmtpSecyrityType.getAdapter().getCount();
      for (int i = 0; i < smtpOptionsCount; i++) {
        if (authCreds.getSmtpOpt() == ((SecurityType) spinnerSmtpSecyrityType.getAdapter().getItem(i)).getOption()) {
          spinnerSmtpSecyrityType.setSelection(i);
        }
      }
    }
  }

  /**
   * Save the current {@link AuthCredentials} to the shared preferences.
   */
  private void saveTempCredentials() {
    authCreds = generateAuthCredentials();
    Gson gson = new Gson();
    authCreds.setPassword(null);
    authCreds.setSmtpSignInPassword(null);
    SharedPreferencesHelper.setString(PreferenceManager.getDefaultSharedPreferences(this),
        Constants.PREFERENCES_KEY_TEMP_LAST_AUTH_CREDENTIALS, gson.toJson(authCreds));
  }

  /**
   * Retrieve a temp {@link AuthCredentials} from the shared preferences.
   */
  private AuthCredentials getTempAuthCredentials() {
    String authCredentialsJson = SharedPreferencesHelper.getString(PreferenceManager.getDefaultSharedPreferences
        (this), Constants.PREFERENCES_KEY_TEMP_LAST_AUTH_CREDENTIALS, "");

    if (!TextUtils.isEmpty(authCredentialsJson)) {
      try {
        return new Gson().fromJson(authCredentialsJson, AuthCredentials.class);
      } catch (JsonSyntaxException e) {
        e.printStackTrace();
        ExceptionUtil.handleError(e);
      }
    }

    return null;
  }

  /**
   * Generate the {@link AuthCredentials} using user input.
   *
   * @return {@link AuthCredentials}.
   */
  private AuthCredentials generateAuthCredentials() {
    int imapPort = TextUtils.isEmpty(editTextImapPort.getText()) ? JavaEmailConstants.DEFAULT_IMAP_PORT
        : Integer.parseInt(editTextImapPort.getText().toString());

    int smtpPort = TextUtils.isEmpty(editTextSmtpPort.getText()) ? JavaEmailConstants.DEFAULT_SMTP_PORT
        : Integer.parseInt(editTextSmtpPort.getText().toString());

    return new AuthCredentials.Builder().setEmail(editTextEmail.getText().toString())
        .setUsername(editTextUserName.getText().toString())
        .setPassword(editTextPassword.getText().toString())
        .setImapServer(editTextImapServer.getText().toString())
        .setImapPort(imapPort)
        .setImapSecurityTypeOption(((SecurityType) spinnerImapSecyrityType.getSelectedItem()).getOption())
        .setSmtpServer(editTextSmtpServer.getText().toString())
        .setSmtpPort(smtpPort)
        .setSmtpSecurityTypeOption(((SecurityType) spinnerSmtpSecyrityType.getSelectedItem()).getOption())
        .setIsUseCustomSignInForSmtp(checkBoxRequireSignInForSmtp.isChecked())
        .setSmtpSigInUsername(editTextSmtpUsername.getText().toString())
        .setSmtpSignInPassword(editTextSmtpPassword.getText().toString())
        .build();
  }

  /**
   * Do a lot of checks to validate an outgoing message info.
   *
   * @return <tt>Boolean</tt> true if all information is correct, false otherwise.
   */
  private boolean isDataCorrect() {
    if (TextUtils.isEmpty(editTextEmail.getText())) {
      showInfoSnackbar(editTextEmail, getString(R.string.text_must_not_be_empty, getString(R.string.e_mail)));
      editTextEmail.requestFocus();
    } else if (GeneralUtil.isEmailValid(editTextEmail.getText())) {
      if (TextUtils.isEmpty(editTextUserName.getText())) {
        showInfoSnackbar(editTextUserName, getString(R.string.text_must_not_be_empty,
            getString(R.string.username)));
        editTextUserName.requestFocus();
      } else if (TextUtils.isEmpty(editTextPassword.getText())) {
        showInfoSnackbar(editTextPassword, getString(R.string.text_must_not_be_empty,
            getString(R.string.password)));
        editTextPassword.requestFocus();
      } else if (TextUtils.isEmpty(editTextImapServer.getText())) {
        showInfoSnackbar(editTextImapServer, getString(R.string.text_must_not_be_empty,
            getString(R.string.imap_server)));
        editTextImapServer.requestFocus();
      } else if (TextUtils.isEmpty(editTextImapPort.getText())) {
        showInfoSnackbar(editTextImapPort, getString(R.string.text_must_not_be_empty,
            getString(R.string.imap_port)));
        editTextImapPort.requestFocus();
      } else if (TextUtils.isEmpty(editTextSmtpServer.getText())) {
        showInfoSnackbar(editTextSmtpServer, getString(R.string.text_must_not_be_empty,
            getString(R.string.smtp_server)));
        editTextSmtpServer.requestFocus();
      } else if (TextUtils.isEmpty(editTextSmtpPort.getText())) {
        showInfoSnackbar(editTextSmtpPort, getString(R.string.text_must_not_be_empty,
            getString(R.string.smtp_port)));
        editTextSmtpPort.requestFocus();
      } else if (checkBoxRequireSignInForSmtp.isChecked()) {
        if (TextUtils.isEmpty(editTextSmtpUsername.getText())) {
          showInfoSnackbar(editTextSmtpUsername, getString(R.string.text_must_not_be_empty,
              getString(R.string.smtp_username)));
          editTextSmtpUsername.requestFocus();
        } else if (TextUtils.isEmpty(editTextSmtpPassword.getText())) {
          showInfoSnackbar(editTextSmtpPassword, getString(R.string.text_must_not_be_empty,
              getString(R.string.smtp_password)));
          editTextSmtpPassword.requestFocus();
        } else return true;
      } else return true;
    } else {
      showInfoSnackbar(editTextEmail, getString(R.string.error_email_is_not_valid));
      editTextEmail.requestFocus();
    }

    return false;
  }
}
