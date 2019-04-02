/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
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
import android.widget.Toast;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.retrofit.node.NodeRepository;
import com.flowcrypt.email.api.retrofit.response.model.node.Error;
import com.flowcrypt.email.api.retrofit.response.model.node.Word;
import com.flowcrypt.email.api.retrofit.response.node.NodeResponseWrapper;
import com.flowcrypt.email.api.retrofit.response.node.ZxcvbnStrengthBarResult;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.jetpack.viewmodel.PasswordStrengthViewModel;
import com.flowcrypt.email.ui.activity.fragment.dialog.InfoDialogFragment;
import com.flowcrypt.email.ui.activity.fragment.dialog.WebViewInfoDialogFragment;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;
import com.google.android.material.snackbar.Snackbar;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

/**
 * @author Denis Bondarenko
 * Date: 04.08.2018
 * Time: 14:53
 * E-mail: DenBond7@gmail.com
 */
public abstract class BasePassPhraseManagerActivity extends BaseBackStackActivity implements View.OnClickListener,
    TextWatcher, Observer<NodeResponseWrapper> {

  public static final String KEY_EXTRA_ACCOUNT_DAO =
      GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_ACCOUNT_DAO", BasePassPhraseManagerActivity.class);
  private static final long DELAY = 350;

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

  protected AccountDao account;
  protected boolean isBackEnabled = true;

  private PasswordStrengthViewModel viewModel;
  private ZxcvbnStrengthBarResult strengthBarResult;
  private Timer timer;

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

    timer = new Timer();
    viewModel = ViewModelProviders.of(this).get(PasswordStrengthViewModel.class);
    viewModel.init(new NodeRepository());
    viewModel.getResponsesLiveData().observe(this, this);
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

          if (strengthBarResult != null && strengthBarResult.getWord() != null) {
            switch (strengthBarResult.getWord().getWord()) {
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
    final String passphrase = editable.toString();
    timer.cancel();
    timer = new Timer();
    timer.schedule(
        new TimerTask() {
          @Override
          public void run() {
            runOnUiThread(new TimerTask() {
              @Override
              public void run() {
                viewModel.check(passphrase);
              }
            });
          }
        },
        DELAY
    );

    if (TextUtils.isEmpty(editable)) {
      textViewPasswordQualityInfo.setText(R.string.passphrase_must_be_non_empty);
    }
  }

  @Override
  public void onChanged(NodeResponseWrapper nodeResponseWrapper) {
    switch (nodeResponseWrapper.getRequestCode()) {
      case R.id.live_data_id_check_passphrase_strength:
        switch (nodeResponseWrapper.getStatus()) {
          case SUCCESS:
            strengthBarResult = (ZxcvbnStrengthBarResult) nodeResponseWrapper.getResult();
            updateStrengthViews();
            break;

          case ERROR:
            if (nodeResponseWrapper.getResult() != null) {
              Error error = nodeResponseWrapper.getResult().getError();
              Toast.makeText(this, error.toString(), Toast.LENGTH_SHORT).show();
            }
            break;

          case EXCEPTION:
            if (nodeResponseWrapper.getResult() != null) {
              Throwable throwable = nodeResponseWrapper.getException();
              if (throwable != null) {
                Toast.makeText(this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
              }
            }
            break;
        }
        break;
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

  private void updateStrengthViews() {
    if (strengthBarResult == null || strengthBarResult.getWord() == null) {
      return;
    }

    Word word = strengthBarResult.getWord();

    switch (word.getWord()) {
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

    int color = parseColor();

    progressBarPasswordQuality.setProgress(word.getBar());
    progressBarPasswordQuality.getProgressDrawable().setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);

    String qualityValue = getLocalizedPasswordQualityValue(word);

    Spannable qualityValueSpannable = new SpannableString(qualityValue);
    qualityValueSpannable.setSpan(new ForegroundColorSpan(color), 0, qualityValueSpannable.length(),
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    qualityValueSpannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0,
        qualityValueSpannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    textViewPasswordQualityInfo.setText(qualityValueSpannable);
    textViewPasswordQualityInfo.append(" ");
    textViewPasswordQualityInfo.append(getString(R.string.password_quality_subtext));

    Spannable timeSpannable = new SpannableString(strengthBarResult.getTime());
    timeSpannable.setSpan(new ForegroundColorSpan(color), 0, timeSpannable.length(),
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    textViewPasswordQualityInfo.append(" ");
    textViewPasswordQualityInfo.append(timeSpannable);
    textViewPasswordQualityInfo.append(")");
  }

  private int parseColor() {
    int color;
    try {
      color = Color.parseColor(strengthBarResult.getWord().getColor());
    } catch (IllegalArgumentException e) {
      e.printStackTrace();

      switch (strengthBarResult.getWord().getColor()) {
        case "orange":
          color = Color.parseColor("#FFA500");
          break;

        case "darkorange":
          color = Color.parseColor("#FF8C00");
          break;

        case "darkred":
          color = Color.parseColor("#8B0000");
          break;

        default:
          color = Color.DKGRAY;
      }
    }
    return color;
  }

  private String getLocalizedPasswordQualityValue(Word passwordStrength) {
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
