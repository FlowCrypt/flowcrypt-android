/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.flowcrypt.email.R;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.KeyStoreCryptoManager;
import com.flowcrypt.email.ui.activity.base.BaseActivity;
import com.flowcrypt.email.ui.loader.EncryptAndSavePrivateKeysAsyncTaskLoader;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

import java.util.ArrayList;

/**
 * This class describes checking the received private keys. Here we validate and save encrypted
 * via {@link KeyStoreCryptoManager} keys to the database. If one of received private keys is
 * valid, we will return {@link Activity#RESULT_OK}.
 *
 * @author Denis Bondarenko
 *         Date: 21.07.2017
 *         Time: 9:59
 *         E-mail: DenBond7@gmail.com
 */

public class CheckKeysActivity extends BaseActivity implements View.OnClickListener,
        LoaderManager.LoaderCallbacks<LoaderResult> {

    public static final int RESULT_NEGATIVE = 10;

    public static final String KEY_EXTRA_PRIVATE_KEYS = GeneralUtil.generateUniqueExtraKey(
            "KEY_EXTRA_PRIVATE_KEYS", CheckKeysActivity.class);
    public static final String KEY_EXTRA_BOTTOM_TITLE = GeneralUtil.generateUniqueExtraKey(
            "KEY_EXTRA_BOTTOM_TITLE", CheckKeysActivity.class);
    public static final String KEY_EXTRA_CHECK_BUTTON_TITLE = GeneralUtil.generateUniqueExtraKey(
            "KEY_EXTRA_CHECK_BUTTON_TITLE", CheckKeysActivity.class);
    public static final String KEY_EXTRA_NEGATIVE_ACTION_BUTTON_TITLE = GeneralUtil
            .generateUniqueExtraKey(
                    "KEY_EXTRA_NEGATIVE_ACTION_BUTTON_TITLE", CheckKeysActivity.class);

    private ArrayList<String> privateKeys;
    private EditText editTextKeyPassword;
    private View progressBar;
    private String bottomTitle;
    private String checkButtonTitle;
    private String anotherAccountButtonTitle;
    private boolean isThrowErrorIfDuplicateFound;

    public static Intent newIntent(Context context, ArrayList<String> privateKeys, String
            bottomTitle, String checkButtonTitle, String negativeActionButtonTitle) {
        Intent intent = new Intent(context, CheckKeysActivity.class);
        intent.putExtra(KEY_EXTRA_PRIVATE_KEYS, privateKeys);
        intent.putExtra(KEY_EXTRA_BOTTOM_TITLE, bottomTitle);
        intent.putExtra(KEY_EXTRA_CHECK_BUTTON_TITLE, checkButtonTitle);
        intent.putExtra(KEY_EXTRA_NEGATIVE_ACTION_BUTTON_TITLE, negativeActionButtonTitle);
        return intent;
    }

    @Override
    public boolean isDisplayHomeAsUpEnabled() {
        return false;
    }

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_check_keys;
    }

    @Override
    public View getRootView() {
        return findViewById(R.id.layoutContent);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() != null) {
            this.privateKeys = getIntent().getStringArrayListExtra(KEY_EXTRA_PRIVATE_KEYS);
            this.bottomTitle = getIntent().getStringExtra(KEY_EXTRA_BOTTOM_TITLE);
            this.checkButtonTitle = getIntent().getStringExtra(KEY_EXTRA_CHECK_BUTTON_TITLE);
            this.anotherAccountButtonTitle = getIntent().getStringExtra
                    (KEY_EXTRA_NEGATIVE_ACTION_BUTTON_TITLE);
        }

        initViews();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonCheck:
                UIUtil.hideSoftInput(this, editTextKeyPassword);
                if (privateKeys != null && !privateKeys.isEmpty()) {
                    if (TextUtils.isEmpty(editTextKeyPassword.getText().toString())) {
                        UIUtil.showInfoSnackbar(editTextKeyPassword,
                                getString(R.string.passphrase_must_be_non_empty));
                    } else {
                        getSupportLoaderManager().restartLoader(R.id
                                .loader_id_encrypt_and_save_private_keys_infos, null, this);
                    }
                }
                break;

            case R.id.buttonNegativeAction:
                finish();
                setResult(RESULT_NEGATIVE);
                break;
        }
    }

    @Override
    public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_encrypt_and_save_private_keys_infos:
                progressBar.setVisibility(View.VISIBLE);
                return new EncryptAndSavePrivateKeysAsyncTaskLoader(this, privateKeys,
                        editTextKeyPassword.getText().toString(), isThrowErrorIfDuplicateFound);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<LoaderResult> loader, LoaderResult loaderResult) {
        if (loaderResult != null) {
            if (loaderResult.getResult() != null) {
                handleSuccessLoaderResult(loader.getId(), loaderResult.getResult());
            } else if (loaderResult.getException() != null) {
                handleFailureLoaderResult(loader.getId(), loaderResult.getException());
            } else {
                UIUtil.showInfoSnackbar(getRootView(), getString(R.string.unknown_error));
            }
        } else {
            UIUtil.showInfoSnackbar(getRootView(), getString(R.string.unknown_error));
        }
    }

    @Override
    public void onLoaderReset(Loader<LoaderResult> loader) {

    }

    private void handleFailureLoaderResult(int loaderId, Exception e) {
        switch (loaderId) {
            case R.id.loader_id_encrypt_and_save_private_keys_infos:
                progressBar.setVisibility(View.GONE);
        }
    }

    private void handleSuccessLoaderResult(int loaderId, Object result) {
        switch (loaderId) {
            case R.id.loader_id_encrypt_and_save_private_keys_infos:
                progressBar.setVisibility(View.GONE);
                boolean booleanResult = (boolean) result;
                if (booleanResult) {
                    setResult(Activity.RESULT_OK);
                    finish();
                } else {
                    UIUtil.showInfoSnackbar(getRootView(), getString(R.string
                            .password_is_incorrect));
                }
                break;
        }
    }

    private void initViews() {
        if (findViewById(R.id.buttonCheck) != null) {
            Button buttonCheck = (Button) findViewById(R.id.buttonCheck);
            buttonCheck.setText(checkButtonTitle);
            buttonCheck.setOnClickListener(this);
        }

        if (findViewById(R.id.buttonNegativeAction) != null) {
            Button buttonSelectAnotherAccount =
                    (Button) findViewById(R.id.buttonNegativeAction);
            buttonSelectAnotherAccount.setText(anotherAccountButtonTitle);
            buttonSelectAnotherAccount.setOnClickListener(this);
        }

        TextView textViewCheckKeysTitle = (TextView) findViewById(R.id.textViewCheckKeysTitle);
        if (textViewCheckKeysTitle != null) {
            textViewCheckKeysTitle.setText(bottomTitle);
        }

        editTextKeyPassword = (EditText) findViewById(R.id.editTextKeyPassword);
        progressBar = findViewById(R.id.progressBar);
    }
}
