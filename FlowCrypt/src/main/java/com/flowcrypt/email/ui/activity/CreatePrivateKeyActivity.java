/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
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

import com.flowcrypt.email.R;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.JsForUiManager;
import com.flowcrypt.email.js.PasswordStrength;
import com.flowcrypt.email.ui.activity.base.BaseBackStackActivity;
import com.flowcrypt.email.ui.activity.fragment.dialog.InfoDialogFragment;
import com.nulabinc.zxcvbn.Zxcvbn;

/**
 * @author Denis Bondarenko
 *         Date: 08.01.2018
 *         Time: 15:58
 *         E-mail: DenBond7@gmail.com
 */

public class CreatePrivateKeyActivity extends BaseBackStackActivity implements View.OnClickListener, TextWatcher {

    private static final String PASSWORD_QUALITY_PERFECT = "perfect";
    private static final String PASSWORD_QUALITY_GREAT = "great";
    private static final String PASSWORD_QUALITY_GOOD = "good";
    private static final String PASSWORD_QUALITY_REASONABLE = "reasonable";
    private static final String PASSWORD_QUALITY_WEAK = "weak";
    private static final String PASSWORD_QUALITY_POOR = "poor";

    private View progressBar;
    private View buttonSetPassPhrase;
    private EditText editTextKeyPassword;
    private ProgressBar progressBarPasswordQuality;
    private TextView textViewPasswordQualityInfo;

    private Js js;
    private Zxcvbn zxcvbn;
    private PasswordStrength passwordStrength;

    public static Intent newIntent(Context context) {
        return new Intent(context, CreatePrivateKeyActivity.class);
    }

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_create_private_key;
    }

    @Override
    public View getRootView() {
        return null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initViews();

        js = JsForUiManager.getInstance(this).getJs();
        zxcvbn = new Zxcvbn();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonSetPassPhrase:
                if (TextUtils.isEmpty(editTextKeyPassword.getText().toString())) {
                    showInfoSnackbar(editTextKeyPassword, getString(R.string.passphrase_must_be_non_empty),
                            Snackbar.LENGTH_LONG);
                } else {
                    if (getSnackBar() != null) {
                        getSnackBar().dismiss();
                    }

                    if (passwordStrength != null) {
                        switch (passwordStrength.getWord()) {
                            case PASSWORD_QUALITY_WEAK:
                            case PASSWORD_QUALITY_POOR:
                                InfoDialogFragment infoDialogFragment = InfoDialogFragment.newInstance(
                                        getString(R.string.hint),
                                        getString(R.string.select_stronger_pass_phrase));
                                infoDialogFragment.show(getSupportFragmentManager(),
                                        InfoDialogFragment.class.getSimpleName());
                                break;

                            default:
                                /*getSupportLoaderManager().restartLoader(R.id
                                        .loader_id_, null, this);*/
                                break;
                        }
                    }
                }
                break;

            case R.id.imageButtonShowPasswordHint:
                InfoDialogFragment infoDialogFragment = InfoDialogFragment.newInstance(
                        getString(R.string.hint), getString(R.string.password_recommendation),
                        null, false, true, true);
                infoDialogFragment.show(getSupportFragmentManager(),
                        InfoDialogFragment.class.getSimpleName());
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
    }

    private void updateBackgroundOfSetPassPhraseButton() {
        switch (passwordStrength.getWord()) {
            case PASSWORD_QUALITY_WEAK:
            case PASSWORD_QUALITY_POOR:
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
        progressBar = findViewById(R.id.progressBar);
        editTextKeyPassword = findViewById(R.id.editTextKeyPassword);
        progressBarPasswordQuality = findViewById(R.id.progressBarPasswordQuality);
        textViewPasswordQualityInfo = findViewById(R.id.textViewPasswordQualityInfo);
        buttonSetPassPhrase = findViewById(R.id.buttonSetPassPhrase);

        editTextKeyPassword.addTextChangedListener(this);
        buttonSetPassPhrase.setOnClickListener(this);
        findViewById(R.id.imageButtonShowPasswordHint).setOnClickListener(this);
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
                case PASSWORD_QUALITY_PERFECT:
                    qualityValue = getString(R.string.password_quality_perfect);
                    break;

                case PASSWORD_QUALITY_GREAT:
                    qualityValue = getString(R.string.password_quality_great);
                    break;

                case PASSWORD_QUALITY_GOOD:
                    qualityValue = getString(R.string.password_quality_good);
                    break;

                case PASSWORD_QUALITY_REASONABLE:
                    qualityValue = getString(R.string.password_quality_reasonable);
                    break;

                case PASSWORD_QUALITY_POOR:
                    qualityValue = getString(R.string.password_quality_poor);
                    break;

                case PASSWORD_QUALITY_WEAK:
                    qualityValue = getString(R.string.password_quality_weak);
                    break;
            }

            qualityValue = qualityValue.toUpperCase();
        }

        return qualityValue;
    }
}
