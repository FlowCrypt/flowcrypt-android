/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.js.PasswordStrength;
import com.flowcrypt.email.js.UiJsManager;
import com.flowcrypt.email.js.core.Js;
import com.flowcrypt.email.ui.activity.fragment.dialog.InfoDialogFragment;
import com.flowcrypt.email.ui.activity.fragment.dialog.WebViewInfoDialogFragment;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;
import com.google.android.material.snackbar.Snackbar;
import com.nulabinc.zxcvbn.Zxcvbn;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * @author Denis Bondarenko
 * Date: 04.08.2018
 * Time: 14:53
 * E-mail: DenBond7@gmail.com
 */
public abstract class BasePassPhraseManagerActivity extends BaseBackStackActivity implements View.OnClickListener,
    TextWatcher {

  public static final String KEY_EXTRA_ACCOUNT_DAO =
      GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_ACCOUNT_DAO", BasePassPhraseManagerActivity.class);

  protected View layoutProgress;
  protected View layoutContentView;
  protected View btnSetPassPhrase;
  protected View layoutSecondPasswordCheck;
  protected View layoutFirstPasswordCheck;
  protected View layoutSuccess;
  protected EditText editTextKeyPassword;
  protected EditText editTextKeyPasswordSecond;
  protected ProgressBar progressBarPasswordQuality;
  protected TextView textViewPasswordQualityInfo;
  protected TextView textViewSuccessTitle;
  protected TextView textViewSuccessSubTitle;
  protected TextView textViewFirstPasswordCheckTitle;
  protected TextView textViewSecondPasswordCheckTitle;
  protected Button btnSuccess;

  protected Js js;
  protected Zxcvbn zxcvbn;
  protected PasswordStrength passwordStrength;
  protected AccountDao account;
  protected boolean isBackEnabled = true;

  public abstract void onConfirmPassPhraseSuccess();

  @Override
  public int getContentViewResourceId() {
    return R.layout.activity_pass_phrase_manager;
  }

  @Override
  public View getRootView() {
    return findViewById(R.id.layoutContent);
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (getIntent() == null) {
      finish();
    }

    this.account = getIntent().getParcelableExtra(KEY_EXTRA_ACCOUNT_DAO);

    if (account == null) {
      finish();
    }

    initViews();

    this.js = UiJsManager.getInstance(this).getJs();
    this.zxcvbn = new Zxcvbn();
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.buttonSetPassPhrase:
        if (TextUtils.isEmpty(editTextKeyPassword.getText().toString())) {
          showInfoSnackbar(getRootView(), getString(R.string.passphrase_must_be_non_empty), Snackbar.LENGTH_LONG);
        } else {
          if (getSnackBar() != null) {
            getSnackBar().dismiss();
          }

          if (passwordStrength != null) {
            switch (passwordStrength.getWord()) {
              case Constants.PASSWORD_QUALITY_WEAK:
              case Constants.PASSWORD_QUALITY_POOR:
                InfoDialogFragment infoDialogFragment = InfoDialogFragment.newInstance("",
                    getString(R.string.select_stronger_pass_phrase));
                infoDialogFragment.show(getSupportFragmentManager(), InfoDialogFragment.class.getSimpleName());
                break;

              default:
                UIUtil.exchangeViewVisibility(this, true, layoutSecondPasswordCheck, layoutFirstPasswordCheck);
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
          webViewInfoDialogFragment.show(getSupportFragmentManager(), WebViewInfoDialogFragment.class.getSimpleName());
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
        UIUtil.exchangeViewVisibility(this, false, layoutSecondPasswordCheck, layoutFirstPasswordCheck);
        break;

      case R.id.buttonConfirmPassPhrases:
        if (TextUtils.isEmpty(editTextKeyPasswordSecond.getText().toString())) {
          showInfoSnackbar(getRootView(), getString(R.string.passphrase_must_be_non_empty), Snackbar.LENGTH_LONG);
        } else {
          if (getSnackBar() != null) {
            getSnackBar().dismiss();
          }

          if (editTextKeyPassword.getText().toString().equals(editTextKeyPasswordSecond.getText().toString())) {
            onConfirmPassPhraseSuccess();
          } else {
            editTextKeyPasswordSecond.setText(null);
            showInfoSnackbar(getRootView(), getString(R.string.pass_phrases_do_not_match), Snackbar.LENGTH_LONG);
          }
        }
        break;

      case R.id.buttonSuccess:
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
    double measure = zxcvbn.measure(editable.toString(), js.crypto_password_weak_words()).getGuesses();
    passwordStrength = js.crypto_password_estimate_strength(measure);

    updatePasswordQualityProgressBar(passwordStrength);
    updatePasswordQualityInfo(passwordStrength);
    updateSetPassButtonBackground();

    if (TextUtils.isEmpty(editable)) {
      textViewPasswordQualityInfo.setText(R.string.passphrase_must_be_non_empty);
    }
  }

  protected void initViews() {
    layoutProgress = findViewById(R.id.layoutProgress);
    layoutContentView = findViewById(R.id.layoutContentView);
    layoutFirstPasswordCheck = findViewById(R.id.layoutFirstPasswordCheck);
    layoutSecondPasswordCheck = findViewById(R.id.layoutSecondPasswordCheck);
    layoutSuccess = findViewById(R.id.layoutSuccess);
    textViewSuccessTitle = findViewById(R.id.textViewSuccessTitle);
    textViewSuccessSubTitle = findViewById(R.id.textViewSuccessSubTitle);
    textViewFirstPasswordCheckTitle = findViewById(R.id.textViewFirstPasswordCheckTitle);
    textViewSecondPasswordCheckTitle = findViewById(R.id.textViewSecondPasswordCheckTitle);
    btnSuccess = findViewById(R.id.buttonSuccess);

    editTextKeyPassword = findViewById(R.id.editTextKeyPassword);
    editTextKeyPassword.addTextChangedListener(this);
    editTextKeyPasswordSecond = findViewById(R.id.editTextKeyPasswordSecond);
    progressBarPasswordQuality = findViewById(R.id.progressBarPasswordQuality);
    textViewPasswordQualityInfo = findViewById(R.id.textViewPasswordQualityInfo);
    btnSetPassPhrase = findViewById(R.id.buttonSetPassPhrase);
    btnSetPassPhrase.setOnClickListener(this);
    findViewById(R.id.imageButtonShowPasswordHint).setOnClickListener(this);
    findViewById(R.id.buttonConfirmPassPhrases).setOnClickListener(this);
    findViewById(R.id.buttonUseAnotherPassPhrase).setOnClickListener(this);
    btnSuccess.setOnClickListener(this);
  }

  private void updateSetPassButtonBackground() {
    switch (passwordStrength.getWord()) {
      case Constants.PASSWORD_QUALITY_WEAK:
      case Constants.PASSWORD_QUALITY_POOR:
        btnSetPassPhrase.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.silver),
            PorterDuff.Mode.MULTIPLY);
        break;

      default:
        btnSetPassPhrase.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.colorPrimary),
            PorterDuff.Mode.MULTIPLY);
        break;
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
