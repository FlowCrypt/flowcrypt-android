/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.SecurityType;
import com.flowcrypt.email.ui.activity.base.BaseActivity;
import com.flowcrypt.email.util.GeneralUtil;

import java.util.ArrayList;

/**
 * This activity describes a logic of adding a new account of other email providers.
 *
 * @author Denis Bondarenko
 *         Date: 12.09.2017
 *         Time: 17:21
 *         E-mail: DenBond7@gmail.com
 */

public class AddNewAccountActivity extends BaseActivity implements CompoundButton.OnCheckedChangeListener,
        AdapterView.OnItemSelectedListener, View.OnClickListener, TextWatcher {
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
        return null;
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
                editTextImapPort.setText(String.valueOf(securityTypeForImap.getImapPort()));
                break;

            case R.id.spinnerSmtpSecyrityType:
                SecurityType securityTypeForSmtp = (SecurityType) parent.getAdapter().getItem(position);
                editTextSmtpPort.setText(String.valueOf(securityTypeForSmtp.getSmtpPort()));
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
        checkBoxRequireSignInForSmtp = (CheckBox) findViewById(R.id.checkBoxRequireSignInForSmtp);
        checkBoxRequireSignInForSmtp.setOnCheckedChangeListener(this);

        spinnerImapSecyrityType = (Spinner) findViewById(R.id.spinnerImapSecurityType);
        spinnerSmtpSecyrityType = (Spinner) findViewById(R.id.spinnerSmtpSecyrityType);

        ArrayAdapter<SecurityType> userAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, generateAvailableSecurityTypes());

        spinnerImapSecyrityType.setAdapter(userAdapter);
        spinnerImapSecyrityType.setOnItemSelectedListener(this);
        spinnerSmtpSecyrityType.setAdapter(userAdapter);
        spinnerSmtpSecyrityType.setOnItemSelectedListener(this);

        if (findViewById(R.id.buttonTryToConnect) != null) {
            findViewById(R.id.buttonTryToConnect).setOnClickListener(this);
        }
    }

    @NonNull
    private ArrayList<SecurityType> generateAvailableSecurityTypes() {
        ArrayList<SecurityType> securityTypes = new ArrayList<>();
        securityTypes.add(new SecurityType("None", 143, 25));
        securityTypes.add(new SecurityType("SSL/TLS", 993, 465));
        securityTypes.add(new SecurityType("SSL/TLS (All certificates)", 993, 465));
        securityTypes.add(new SecurityType("STARTLS", 143, 25));
        securityTypes.add(new SecurityType("STARTLS (All certificates)", 143, 25));
        return securityTypes;
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
                            getString(R.string.smtp_sign_in_username)));
                    editTextSmtpUsername.requestFocus();
                } else if (TextUtils.isEmpty(editTextSmtpPassword.getText())) {
                    showInfoSnackbar(editTextSmtpPassword, getString(R.string.text_must_not_be_empty,
                            getString(R.string.smtp_sign_in_password)));
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
