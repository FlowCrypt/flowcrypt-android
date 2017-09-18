/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
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
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.AuthCredentials;
import com.flowcrypt.email.api.email.model.SecurityType;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.ui.activity.base.BaseActivity;
import com.flowcrypt.email.ui.loader.AddNewAccountAsyncTaskLoader;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

/**
 * This activity describes a logic of adding a new account of other email providers.
 *
 * @author Denis Bondarenko
 *         Date: 12.09.2017
 *         Time: 17:21
 *         E-mail: DenBond7@gmail.com
 */

public class AddNewAccountActivity extends BaseActivity implements CompoundButton.OnCheckedChangeListener,
        AdapterView.OnItemSelectedListener, View.OnClickListener, TextWatcher, LoaderManager
                .LoaderCallbacks<LoaderResult> {
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
        return contentView;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initViews();
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
                editTextImapPort.setText(String.valueOf(securityTypeForImap.getDefaultImapPort()));
                break;

            case R.id.spinnerSmtpSecyrityType:
                SecurityType securityTypeForSmtp = (SecurityType) parent.getAdapter().getItem(position);
                editTextSmtpPort.setText(String.valueOf(securityTypeForSmtp.getDefaultSmtpPort()));
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
                if (isAllInformationCorrect()) {
                    getSupportLoaderManager().restartLoader(R.id.loader_id_add_new_account, null, this);
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
            case R.id.loader_id_add_new_account:
                UIUtil.exchangeViewVisibility(this, true, progressView, contentView);

                AuthCredentials authCredentials = generateAuthCredentials();
                return new AddNewAccountAsyncTaskLoader(this, authCredentials);

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
            case R.id.loader_id_add_new_account:
                Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
                UIUtil.exchangeViewVisibility(this, false, progressView, contentView);
                break;

            default:
                super.handleSuccessLoaderResult(loaderId, result);
                break;
        }
    }

    @Override
    public void handleFailureLoaderResult(int loaderId, Exception e) {
        switch (loaderId) {
            case R.id.loader_id_add_new_account:
                UIUtil.exchangeViewVisibility(this, false, progressView, contentView);
                showInfoSnackbar(getRootView(), e != null && !TextUtils.isEmpty(e.getMessage()) ? e.getMessage()
                        : getString(R.string.unknown_error), Snackbar.LENGTH_LONG);
                break;

            default:
                super.handleFailureLoaderResult(loaderId, e);
                break;
        }
    }

    protected void initViews() {
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
        spinnerImapSecyrityType.setOnItemSelectedListener(this);
        spinnerSmtpSecyrityType.setAdapter(userAdapter);
        spinnerSmtpSecyrityType.setOnItemSelectedListener(this);

        if (findViewById(R.id.buttonTryToConnect) != null) {
            findViewById(R.id.buttonTryToConnect).setOnClickListener(this);
        }
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
                .setImapSecurityType((SecurityType) spinnerImapSecyrityType.getSelectedItem())
                .setSmtpServer(editTextSmtpServer.getText().toString())
                .setSmtpPort(Integer.parseInt(editTextSmtpPort.getText().toString()))
                .setSmtpSecurityType((SecurityType) spinnerSmtpSecyrityType.getSelectedItem())
                .setIsRequireSignInForSmtp(checkBoxRequireSignInForSmtp.isChecked())
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
