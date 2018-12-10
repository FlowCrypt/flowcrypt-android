/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.js.UiJsManager;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.ui.activity.base.BasePassPhraseManagerActivity;
import com.flowcrypt.email.ui.loader.CreatePrivateKeyAsyncTaskLoader;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

/**
 * @author Denis Bondarenko
 * Date: 08.01.2018
 * Time: 15:58
 * E-mail: DenBond7@gmail.com
 */

public class CreatePrivateKeyActivity extends BasePassPhraseManagerActivity implements
    LoaderManager.LoaderCallbacks<LoaderResult> {

  private static final String KEY_CREATED_PRIVATE_KEY_LONG_ID =
      GeneralUtil.generateUniqueExtraKey("KEY_CREATED_PRIVATE_KEY_LONG_ID", CreatePrivateKeyActivity.class);

  private String createdPrivateKeyLongId;
  private boolean isBackEnabled = true;

  public static Intent newIntent(Context context, AccountDao account) {
    Intent intent = new Intent(context, CreatePrivateKeyActivity.class);
    intent.putExtra(KEY_EXTRA_ACCOUNT_DAO, account);
    return intent;
  }

  @Override
  public void onConfirmPassPhraseSuccess() {
    LoaderManager.getInstance(this).restartLoader(R.id.loader_id_create_private_key, null, this);
  }

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

    if (savedInstanceState != null) {
      this.createdPrivateKeyLongId = savedInstanceState.getString(KEY_CREATED_PRIVATE_KEY_LONG_ID);
    }

    if (getIntent() == null) {
      finish();
    }
  }

  @Override
  public void onBackPressed() {
    if (isBackEnabled) {
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
      case R.id.buttonSuccess:
        setResult(Activity.RESULT_OK);
        finish();
        break;

      default:
        super.onClick(v);
    }
  }

  @Override
  @NonNull
  public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
    switch (id) {
      case R.id.loader_id_create_private_key:
        if (TextUtils.isEmpty(createdPrivateKeyLongId)) {
          isBackEnabled = false;
          UIUtil.exchangeViewVisibility(this, true, layoutProgress, layoutContentView);
          return new CreatePrivateKeyAsyncTaskLoader(this, account, editTextKeyPassword.getText().toString());
        } else {
          return new Loader<>(this);
        }

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
    switch (loader.getId()) {
      case R.id.loader_id_create_private_key:
        isBackEnabled = true;
        break;
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void onSuccess(int loaderId, Object result) {
    switch (loaderId) {
      case R.id.loader_id_create_private_key:
        isBackEnabled = true;
        createdPrivateKeyLongId = (String) result;
        layoutSecondPasswordCheck.setVisibility(View.GONE);
        layoutSuccess.setVisibility(View.VISIBLE);
        UIUtil.exchangeViewVisibility(this, false, layoutProgress, layoutContentView);
        UiJsManager.getInstance(this).getJs().getStorageConnector().refresh(this);
        restartJsService();
        break;

      default:
        super.onSuccess(loaderId, result);
        break;
    }
  }

  @Override
  public void onError(int loaderId, Exception e) {
    switch (loaderId) {
      case R.id.loader_id_create_private_key:
        isBackEnabled = true;
        editTextKeyPasswordSecond.setText(null);
        UIUtil.exchangeViewVisibility(this, false, layoutProgress, layoutContentView);
        showInfoSnackbar(getRootView(), e.getMessage());
        break;

      default:
        super.onError(loaderId, e);
        break;
    }
  }

  @Override
  protected void initViews() {
    super.initViews();

    textViewFirstPasswordCheckTitle.setText(R.string.set_up_flow_crypt);
    textViewSecondPasswordCheckTitle.setText(R.string.set_up_flow_crypt);

    textViewSuccessTitle.setText(R.string.you_are_all_set);
    textViewSuccessSubTitle.setText(R.string.you_can_send_and_receive_encrypted_emails);
    btnSuccess.setText(R.string.continue_);

    if (!TextUtils.isEmpty(this.createdPrivateKeyLongId)) {
      layoutProgress.setVisibility(View.GONE);
      layoutFirstPasswordCheck.setVisibility(View.GONE);
      layoutSecondPasswordCheck.setVisibility(View.GONE);
      layoutSuccess.setVisibility(View.VISIBLE);
      layoutContentView.setVisibility(View.VISIBLE);
    }
  }
}
