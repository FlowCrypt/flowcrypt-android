/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.preference.PreferenceManager;
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
import com.flowcrypt.email.api.email.model.AuthCredentials;
import com.flowcrypt.email.api.email.model.SecurityType;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.service.EmailSyncService;
import com.flowcrypt.email.ui.activity.base.BaseActivity;
import com.flowcrypt.email.ui.loader.CheckEmailSettingsAsyncTaskLoader;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.SharedPreferencesHelper;
import com.flowcrypt.email.util.UIUtil;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * This activity describes a logic of adding a new account of other email providers.
 *
 * @author Denis Bondarenko
 *         Date: 12.09.2017
 *         Time: 17:21
 *         E-mail: DenBond7@gmail.com
 */

public class AddNewAccountManuallyActivity extends BaseActivity implements CompoundButton.OnCheckedChangeListener,
        AdapterView.OnItemSelectedListener, View.OnClickListener, TextWatcher,
        LoaderManager.LoaderCallbacks<LoaderResult> {
    private static final int REQUEST_CODE_ADD_NEW_ACCOUNT = 10;

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
    private AuthCredentials authCredentials;

    private boolean isImapSpinnerInitAfterStart;
    private boolean isSmtpSpinnerInitAfterStart;

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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.authCredentials = getTempAuthCredentialsFromPreferences();

        if (authCredentials == null) {
            isImapSpinnerInitAfterStart = true;
            isSmtpSpinnerInitAfterStart = true;
        }

        initViews(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        saveTempCredentialsToPreferences();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_ADD_NEW_ACCOUNT:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        try {
                            authCredentials = generateAuthCredentials();
                            AccountDaoSource accountDaoSource = new AccountDaoSource();
                            accountDaoSource.addRow(this, authCredentials);
                            AccountDao accountDao = accountDaoSource.getAccountInformation(this,
                                    authCredentials.getEmail());
                            EmailSyncService.startEmailSyncService(this);
                            EmailManagerActivity.runEmailManagerActivity(this, accountDao);
                            setResult(Activity.RESULT_OK);
                            finish();
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new IllegalStateException("Something wrong happened", e);
                        }
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
                SecurityType securityTypeForImap = (SecurityType) parent.getAdapter().getItem(position);
                if (isImapSpinnerInitAfterStart) {
                    editTextImapPort.setText(String.valueOf(securityTypeForImap.getDefaultImapPort()));
                } else {
                    isImapSpinnerInitAfterStart = true;
                }
                break;

            case R.id.spinnerSmtpSecyrityType:
                SecurityType securityTypeForSmtp = (SecurityType) parent.getAdapter().getItem(position);
                if (isSmtpSpinnerInitAfterStart) {
                    editTextSmtpPort.setText(String.valueOf(securityTypeForSmtp.getDefaultSmtpPort()));
                } else {
                    isSmtpSpinnerInitAfterStart = true;
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
                authCredentials = generateAuthCredentials();
                if (isAllInformationCorrect()) {
                    UIUtil.hideSoftInput(this, getRootView());
                    if (isNotDuplicate()) {
                        getSupportLoaderManager().restartLoader(R.id.loader_id_check_email_settings, null, this);
                    } else {
                        showInfoSnackbar(getRootView(), getString(R.string.template_email_alredy_added,
                                authCredentials.getEmail()), Snackbar.LENGTH_LONG);
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

    @Override
    public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_check_email_settings:
                UIUtil.exchangeViewVisibility(this, true, progressView, contentView);

                authCredentials = generateAuthCredentials();
                return new CheckEmailSettingsAsyncTaskLoader(this, authCredentials);

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

    @Override
    public void handleSuccessLoaderResult(int loaderId, Object result) {
        switch (loaderId) {
            case R.id.loader_id_check_email_settings:
                boolean isSettingsValid = (boolean) result;
                if (isSettingsValid) {
                    AccountDao accountDao = new AccountDao(authCredentials.getEmail(),
                            null, null, null, null, null, authCredentials);
                    startActivityForResult(CreateOrImportKeyActivity.newIntent(this, accountDao, true),
                            REQUEST_CODE_ADD_NEW_ACCOUNT);
                    UIUtil.exchangeViewVisibility(this, false, progressView, contentView);
                } else {
                    UIUtil.exchangeViewVisibility(this, false, progressView, contentView);
                    showInfoSnackbar(getRootView(), getString(R.string.settings_not_valid), Snackbar.LENGTH_LONG);
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
            case R.id.loader_id_check_email_settings:
                UIUtil.exchangeViewVisibility(this, false, progressView, contentView);
                showInfoSnackbar(getRootView(), e != null && !TextUtils.isEmpty(e.getMessage()) ? e.getMessage()
                        : getString(R.string.unknown_error), Snackbar.LENGTH_LONG);
                break;

            default:
                super.handleFailureLoaderResult(loaderId, e);
                break;
        }
    }

    /**
     * Check that current email is not duplicate and not added yet.
     *
     * @return true if email not added yet, otherwise false.
     */
    private boolean isNotDuplicate() {
        return new AccountDaoSource().getAccountInformation(this, authCredentials.getEmail()) == null;
    }

    private void initViews(Bundle savedInstanceState) {
        editTextEmail = (EditText) findViewById(R.id.editTextEmail);
        editTextUserName = (EditText) findViewById(R.id.editTextUserName);
        editTextPassword = (EditText) findViewById(R.id.editTextPassword);
        editTextImapServer = (EditText) findViewById(R.id.editTextImapServer);
        editTextImapPort = (EditText) findViewById(R.id.editTextImapPort);
        editTextSmtpServer = (EditText) findViewById(R.id.editTextSmtpServer);
        editTextSmtpPort = (EditText) findViewById(R.id.editTextSmtpPort);
        editTextSmtpUsername = (EditText) findViewById(R.id.editTextSmtpUsername);
        editTextSmtpPassword = (EditText) findViewById(R.id.editTextSmtpPassword);

        editTextEmail.addTextChangedListener(this);

        layoutSmtpSignIn = findViewById(R.id.layoutSmtpSignIn);
        progressView = findViewById(R.id.progressView);
        contentView = findViewById(R.id.layoutContent);
        checkBoxRequireSignInForSmtp = (CheckBox) findViewById(R.id.checkBoxRequireSignInForSmtp);
        checkBoxRequireSignInForSmtp.setOnCheckedChangeListener(this);

        spinnerImapSecyrityType = (Spinner) findViewById(R.id.spinnerImapSecurityType);
        spinnerSmtpSecyrityType = (Spinner) findViewById(R.id.spinnerSmtpSecyrityType);

        ArrayAdapter<SecurityType> userAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, SecurityType.generateAvailableSecurityTypes(this));

        spinnerImapSecyrityType.setAdapter(userAdapter);
        spinnerSmtpSecyrityType.setAdapter(userAdapter);

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
        if (authCredentials != null) {

            editTextEmail.setText(authCredentials.getEmail());
            editTextUserName.setText(authCredentials.getUsername());
            editTextImapServer.setText(authCredentials.getImapServer());
            editTextImapPort.setText(String.valueOf(authCredentials.getImapPort()));
            editTextSmtpServer.setText(authCredentials.getSmtpServer());
            editTextSmtpPort.setText(String.valueOf(authCredentials.getSmtpPort()));
            checkBoxRequireSignInForSmtp.setChecked(authCredentials.isUseCustomSignInForSmtp());
            editTextSmtpUsername.setText(authCredentials.getSmtpSigInUsername());

            int imapOptionsCount = spinnerImapSecyrityType.getAdapter().getCount();
            for (int i = 0; i < imapOptionsCount; i++) {
                if (authCredentials.getImapSecurityTypeOption() ==
                        ((SecurityType) spinnerImapSecyrityType.getAdapter().getItem(i)).getOption()) {
                    spinnerImapSecyrityType.setSelection(i);
                }
            }

            int smtpOptionsCount = spinnerSmtpSecyrityType.getAdapter().getCount();
            for (int i = 0; i < smtpOptionsCount; i++) {
                if (authCredentials.getSmtpSecurityTypeOption() ==
                        ((SecurityType) spinnerSmtpSecyrityType.getAdapter().getItem(i)).getOption()) {
                    spinnerSmtpSecyrityType.setSelection(i);
                }
            }
        }
    }

    /**
     * Save the current {@link AuthCredentials} to the shared preferences.
     */
    private void saveTempCredentialsToPreferences() {
        authCredentials = generateAuthCredentials();
        Gson gson = new Gson();
        authCredentials.setPassword(null);
        authCredentials.setSmtpSignInPassword(null);
        SharedPreferencesHelper.setString(PreferenceManager.getDefaultSharedPreferences(this),
                Constants.PREFERENCES_KEY_TEMP_LAST_AUTH_CREDENTIALS, gson.toJson(authCredentials));
    }

    /**
     * Retrieve a temp {@link AuthCredentials} from the shared preferences.
     */
    private AuthCredentials getTempAuthCredentialsFromPreferences() {
        String authCredentialsJson = SharedPreferencesHelper.getString(PreferenceManager.getDefaultSharedPreferences
                (this), Constants.PREFERENCES_KEY_TEMP_LAST_AUTH_CREDENTIALS, "");

        if (!TextUtils.isEmpty(authCredentialsJson)) {
            try {
                return new Gson().fromJson(authCredentialsJson, AuthCredentials.class);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
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
        return new AuthCredentials.Builder().setEmail(editTextEmail.getText().toString())
                .setUsername(editTextUserName.getText().toString())
                .setPassword(editTextPassword.getText().toString())
                .setImapServer(editTextImapServer.getText().toString())
                .setImapPort(Integer.parseInt(editTextImapPort.getText().toString()))
                .setImapSecurityTypeOption(((SecurityType) spinnerImapSecyrityType.getSelectedItem()).getOption())
                .setSmtpServer(editTextSmtpServer.getText().toString())
                .setSmtpPort(Integer.parseInt(editTextSmtpPort.getText().toString()))
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
    private boolean isAllInformationCorrect() {
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
