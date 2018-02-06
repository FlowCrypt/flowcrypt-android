/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.JsForUiManager;
import com.flowcrypt.email.js.PasswordStrength;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.ui.activity.base.BaseBackStackActivity;
import com.flowcrypt.email.ui.activity.fragment.dialog.InfoDialogFragment;
import com.flowcrypt.email.ui.activity.fragment.dialog.WebViewInfoDialogFragment;
import com.flowcrypt.email.ui.loader.CreatePrivateKeyAsyncTaskLoader;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;
import com.nulabinc.zxcvbn.Zxcvbn;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author Denis Bondarenko
 *         Date: 08.01.2018
 *         Time: 15:58
 *         E-mail: DenBond7@gmail.com
 */

public class CreatePrivateKeyActivity extends BaseBackStackActivity implements View.OnClickListener, TextWatcher,
        LoaderManager.LoaderCallbacks<LoaderResult> {

    public static final String KEY_EXTRA_ACCOUNT_DAO =
            GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_ACCOUNT_DAO", CreatePrivateKeyActivity.class);

    private static final String KEY_CREATED_PRIVATE_KEY_LONG_ID =
            GeneralUtil.generateUniqueExtraKey("KEY_CREATED_PRIVATE_KEY_LONG_ID", CreatePrivateKeyActivity.class);

    private View layoutProgress;
    private View layoutContentView;
    private View buttonSetPassPhrase;
    private View layoutSecondPasswordCheck;
    private View layoutFirstPasswordCheck;
    private View layoutSuccess;
    private EditText editTextKeyPassword;
    private EditText editTextKeyPasswordSecond;
    private ProgressBar progressBarPasswordQuality;
    private TextView textViewPasswordQualityInfo;

    private Js js;
    private Zxcvbn zxcvbn;
    private PasswordStrength passwordStrength;
    private AccountDao accountDao;

    private String createdPrivateKeyLongId;
    private boolean isBackEnable = true;

    public static Intent newIntent(Context context, AccountDao accountDao) {
        Intent intent = new Intent(context, CreatePrivateKeyActivity.class);
        intent.putExtra(KEY_EXTRA_ACCOUNT_DAO, accountDao);
        return intent;
    }

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_create_private_key;
    }

    @Override
    public View getRootView() {
        return findViewById(R.id.layoutContent);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            this.createdPrivateKeyLongId = savedInstanceState.getString(KEY_CREATED_PRIVATE_KEY_LONG_ID);
        }

        if (getIntent() == null) {
            finish();
        }

        this.accountDao = getIntent().getParcelableExtra(KEY_EXTRA_ACCOUNT_DAO);

        if (accountDao == null) {
            finish();
        }

        initViews();

        this.js = JsForUiManager.getInstance(this).getJs();
        this.zxcvbn = new Zxcvbn();
    }

    @Override
    public void onBackPressed() {
        if (isBackEnable) {
            if (TextUtils.isEmpty(createdPrivateKeyLongId)) {
                super.onBackPressed();
            } else {
                setResult(Activity.RESULT_OK);
                finish();
            }
        } else {
            Toast.makeText(this, R.string.please_wait_while_key_will_be_created, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_CREATED_PRIVATE_KEY_LONG_ID, createdPrivateKeyLongId);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonSetPassPhrase:
                if (TextUtils.isEmpty(editTextKeyPassword.getText().toString())) {
                    showInfoSnackbar(getRootView(), getString(R.string.passphrase_must_be_non_empty),
                            Snackbar.LENGTH_LONG);
                } else {
                    if (getSnackBar() != null) {
                        getSnackBar().dismiss();
                    }

                    if (passwordStrength != null) {
                        switch (passwordStrength.getWord()) {
                            case Constants.PASSWORD_QUALITY_WEAK:
                            case Constants.PASSWORD_QUALITY_POOR:
                                InfoDialogFragment infoDialogFragment = InfoDialogFragment.newInstance(
                                        "",
                                        getString(R.string.select_stronger_pass_phrase));
                                infoDialogFragment.show(getSupportFragmentManager(),
                                        InfoDialogFragment.class.getSimpleName());
                                break;

                            default:
                                UIUtil.exchangeViewVisibility(this, true, layoutSecondPasswordCheck,
                                        layoutFirstPasswordCheck);
                                break;
                        }
                    }
                }
                break;

            case R.id.imageButtonShowPasswordHint:
                if (getSnackBar() != null) {
                    getSnackBar().dismiss();
                }

                try {
                    WebViewInfoDialogFragment webViewInfoDialogFragment = WebViewInfoDialogFragment.newInstance("",
                            IOUtils.toString(getAssets().open("html/pass_phrase_hint.htm"), StandardCharsets.UTF_8));
                    webViewInfoDialogFragment.show(getSupportFragmentManager(), WebViewInfoDialogFragment.class
                            .getSimpleName());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case R.id.buttonUseAnotherPassPhrase:
                if (getSnackBar() != null) {
                    getSnackBar().dismiss();
                }

                editTextKeyPasswordSecond.setText(null);
                editTextKeyPassword.setText(null);
                UIUtil.exchangeViewVisibility(this, false, layoutSecondPasswordCheck,
                        layoutFirstPasswordCheck);
                break;

            case R.id.buttonConfirmPassPhrases:
                if (TextUtils.isEmpty(editTextKeyPasswordSecond.getText().toString())) {
                    showInfoSnackbar(getRootView(), getString(R.string.passphrase_must_be_non_empty),
                            Snackbar.LENGTH_LONG);
                } else {
                    if (getSnackBar() != null) {
                        getSnackBar().dismiss();
                    }

                    if (editTextKeyPassword.getText().toString().equals(
                            editTextKeyPasswordSecond.getText().toString())) {
                        getSupportLoaderManager().restartLoader(R.id.loader_id_create_private_key, null, this);
                    } else {
                        editTextKeyPasswordSecond.setText(null);
                        showInfoSnackbar(getRootView(), getString(R.string.pass_phrases_do_not_match),
                                Snackbar.LENGTH_LONG);
                    }
                }
                break;

            case R.id.buttonContinue:
                setResult(Activity.RESULT_OK);
                finish();
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
        passwordStrength = js.crypto_password_estimate_strength(
                zxcvbn.measure(editable.toString(), js.crypto_password_weak_words()).getGuesses());

        updatePasswordQualityProgressBar(passwordStrength);
        updatePasswordQualityInfo(passwordStrength);
        updateBackgroundOfSetPassPhraseButton();

        if (TextUtils.isEmpty(editable)) {
            textViewPasswordQualityInfo.setText(R.string.passphrase_must_be_non_empty);
        }
    }

    @Override
    public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_create_private_key:
                if (TextUtils.isEmpty(createdPrivateKeyLongId)) {
                    isBackEnable = false;
                    UIUtil.exchangeViewVisibility(this, true, layoutProgress, layoutContentView);
                    return new CreatePrivateKeyAsyncTaskLoader(this, accountDao,
                            editTextKeyPassword.getText().toString());
                } else {
                    return null;
                }

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
        switch (loader.getId()) {
            case R.id.loader_id_create_private_key:
                isBackEnable = true;
                break;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handleSuccessLoaderResult(int loaderId, Object result) {
        switch (loaderId) {
            case R.id.loader_id_create_private_key:
                isBackEnable = true;
                createdPrivateKeyLongId = (String) result;
                layoutSecondPasswordCheck.setVisibility(View.GONE);
                layoutSuccess.setVisibility(View.VISIBLE);
                UIUtil.exchangeViewVisibility(this, false, layoutProgress, layoutContentView);
                JsForUiManager.getInstance(this).getJs().getStorageConnector().refresh(this);
                break;

            default:
                super.handleSuccessLoaderResult(loaderId, result);
                break;
        }
    }

    @Override
    public void handleFailureLoaderResult(int loaderId, Exception e) {
        switch (loaderId) {
            case R.id.loader_id_create_private_key:
                isBackEnable = true;
                editTextKeyPasswordSecond.setText(null);
                UIUtil.exchangeViewVisibility(this, false, layoutProgress, layoutContentView);
                showInfoSnackbar(getRootView(), e.getMessage());
                break;

            default:
                super.handleFailureLoaderResult(loaderId, e);
                break;
        }
    }

    private void updateBackgroundOfSetPassPhraseButton() {
        switch (passwordStrength.getWord()) {
            case Constants.PASSWORD_QUALITY_WEAK:
            case Constants.PASSWORD_QUALITY_POOR:
                buttonSetPassPhrase.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.silver),
                        PorterDuff.Mode.MULTIPLY);
                break;

            default:
                buttonSetPassPhrase.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.colorPrimary),
                        PorterDuff.Mode.MULTIPLY);
                break;
        }
    }

    private void initViews() {
        layoutProgress = findViewById(R.id.layoutProgress);
        layoutContentView = findViewById(R.id.layoutContentView);
        layoutFirstPasswordCheck = findViewById(R.id.layoutFirstPasswordCheck);
        layoutSecondPasswordCheck = findViewById(R.id.layoutSecondPasswordCheck);
        layoutSuccess = findViewById(R.id.layoutSuccess);

        editTextKeyPassword = findViewById(R.id.editTextKeyPassword);
        editTextKeyPassword.addTextChangedListener(this);
        editTextKeyPasswordSecond = findViewById(R.id.editTextKeyPasswordSecond);
        progressBarPasswordQuality = findViewById(R.id.progressBarPasswordQuality);
        textViewPasswordQualityInfo = findViewById(R.id.textViewPasswordQualityInfo);
        buttonSetPassPhrase = findViewById(R.id.buttonSetPassPhrase);
        buttonSetPassPhrase.setOnClickListener(this);
        findViewById(R.id.imageButtonShowPasswordHint).setOnClickListener(this);
        findViewById(R.id.buttonConfirmPassPhrases).setOnClickListener(this);
        findViewById(R.id.buttonUseAnotherPassPhrase).setOnClickListener(this);
        findViewById(R.id.buttonContinue).setOnClickListener(this);

        if (!TextUtils.isEmpty(this.createdPrivateKeyLongId)) {
            layoutProgress.setVisibility(View.GONE);
            layoutFirstPasswordCheck.setVisibility(View.GONE);
            layoutSecondPasswordCheck.setVisibility(View.GONE);
            layoutSuccess.setVisibility(View.VISIBLE);
            layoutContentView.setVisibility(View.VISIBLE);
        }
    }

    private void updatePasswordQualityProgressBar(PasswordStrength passwordStrength) {
        progressBarPasswordQuality.setProgress(passwordStrength.getBar());
        progressBarPasswordQuality.getProgressDrawable().setColorFilter(Color.parseColor(passwordStrength.getColor()),
                android.graphics.PorterDuff.Mode.SRC_IN);
    }

    private void updatePasswordQualityInfo(PasswordStrength passwordStrength) {
        int color = Color.parseColor(passwordStrength.getColor());

        String qualityValue = getLocalizedPasswordQualityValue(passwordStrength);

        Spannable qualityValueSpannable = new SpannableString(qualityValue);
        qualityValueSpannable.setSpan(new ForegroundColorSpan(color), 0, qualityValueSpannable.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        qualityValueSpannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0,
                qualityValueSpannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        textViewPasswordQualityInfo.setText(qualityValueSpannable);
        textViewPasswordQualityInfo.append(" ");
        textViewPasswordQualityInfo.append(getString(R.string.password_quality_subtext));

        Spannable timeSpannable = new SpannableString(passwordStrength.getTime());
        timeSpannable.setSpan(new ForegroundColorSpan(color), 0, timeSpannable.length(), Spannable
                .SPAN_EXCLUSIVE_EXCLUSIVE);
        textViewPasswordQualityInfo.append(" ");
        textViewPasswordQualityInfo.append(timeSpannable);
        textViewPasswordQualityInfo.append(")");
    }

    private String getLocalizedPasswordQualityValue(PasswordStrength passwordStrength) {
        String qualityValue = passwordStrength.getWord();

        if (qualityValue != null) {
            switch (qualityValue) {
                case Constants.PASSWORD_QUALITY_PERFECT:
                    qualityValue = getString(R.string.password_quality_perfect);
                    break;

                case Constants.PASSWORD_QUALITY_GREAT:
                    qualityValue = getString(R.string.password_quality_great);
                    break;

                case Constants.PASSWORD_QUALITY_GOOD:
                    qualityValue = getString(R.string.password_quality_good);
                    break;

                case Constants.PASSWORD_QUALITY_REASONABLE:
                    qualityValue = getString(R.string.password_quality_reasonable);
                    break;

                case Constants.PASSWORD_QUALITY_POOR:
                    qualityValue = getString(R.string.password_quality_poor);
                    break;

                case Constants.PASSWORD_QUALITY_WEAK:
                    qualityValue = getString(R.string.password_quality_weak);
                    break;
            }

            qualityValue = qualityValue.toUpperCase();
        }

        return qualityValue;
    }
}
