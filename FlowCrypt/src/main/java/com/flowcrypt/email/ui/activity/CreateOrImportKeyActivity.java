/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.model.KeyImportModel;
import com.flowcrypt.email.security.SecurityUtils;
import com.flowcrypt.email.ui.activity.base.BaseCheckClipboardBackStackActivity;
import com.flowcrypt.email.util.GeneralUtil;

import androidx.annotation.Nullable;

/**
 * This activity describes a logic for create ot import private keys.
 *
 * @author DenBond7
 * Date: 23.05.2017.
 * Time: 16:15.
 * E-mail: DenBond7@gmail.com
 */
public class CreateOrImportKeyActivity extends BaseCheckClipboardBackStackActivity implements View.OnClickListener {
  public static final int RESULT_CODE_USE_ANOTHER_ACCOUNT = 10;
  public static final String EXTRA_KEY_ACCOUNT_DAO = GeneralUtil.generateUniqueExtraKey
      ("EXTRA_KEY_ACCOUNT_DAO", CreateOrImportKeyActivity.class);

  private static final int REQUEST_CODE_IMPORT_ACTIVITY = 11;
  private static final int REQUEST_CODE_CREATE_KEY_ACTIVITY = 12;
  private static final String KEY_IS_SHOW_USE_ANOTHER_ACCOUNT_BUTTON =
      GeneralUtil.generateUniqueExtraKey("KEY_IS_SHOW_USE_ANOTHER_ACCOUNT_BUTTON", CreateOrImportKeyActivity.class);
  private boolean isShowAnotherAccountButton = true;
  private AccountDao account;

  public static Intent newIntent(Context context, AccountDao account, boolean isShowAnotherAccount) {
    Intent intent = new Intent(context, CreateOrImportKeyActivity.class);
    intent.putExtra(EXTRA_KEY_ACCOUNT_DAO, account);
    intent.putExtra(KEY_IS_SHOW_USE_ANOTHER_ACCOUNT_BUTTON, isShowAnotherAccount);
    return intent;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getIntent() != null) {
      this.isShowAnotherAccountButton = getIntent().getBooleanExtra
          (CreateOrImportKeyActivity.KEY_IS_SHOW_USE_ANOTHER_ACCOUNT_BUTTON, true);
      this.account = getIntent().getParcelableExtra(CreateOrImportKeyActivity.EXTRA_KEY_ACCOUNT_DAO);
    }

    initViews();
  }

  @Override
  public View getRootView() {
    return findViewById(R.id.layoutContent);
  }

  @Override
  public boolean isDisplayHomeAsUpEnabled() {
    return false;
  }

  @Override
  public int getContentViewResourceId() {
    return R.layout.activity_create_or_import_key;
  }

  @Override
  public boolean isPrivateKeyChecking() {
    return true;
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.buttonCreateNewKey:
        startActivityForResult(CreatePrivateKeyActivity.newIntent(this, account), REQUEST_CODE_CREATE_KEY_ACTIVITY);
        break;

      case R.id.buttonImportMyKey:
        KeyImportModel keyImportModel = null;
        if (isBound) {
          keyImportModel = service.getKeyImportModel();
        }

        startActivityForResult(ImportPrivateKeyActivity.newIntent(this, false, getString(R.string.import_private_key),
            keyImportModel, true, ImportPrivateKeyActivity.class), REQUEST_CODE_IMPORT_ACTIVITY);
        break;

      case R.id.buttonSelectAnotherAccount:
        Intent intent = new Intent();
        intent.putExtra(EXTRA_KEY_ACCOUNT_DAO, account);
        setResult(RESULT_CODE_USE_ANOTHER_ACCOUNT, intent);
        finish();
        break;

      case R.id.buttonSkipSetup:
        setResult(Activity.RESULT_OK);
        finish();
        break;
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_CODE_IMPORT_ACTIVITY:
      case REQUEST_CODE_CREATE_KEY_ACTIVITY:
        switch (resultCode) {
          case Activity.RESULT_OK:
            setResult(Activity.RESULT_OK);
            finish();
            break;
        }
        break;

      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

  private void initViews() {
    if (findViewById(R.id.buttonCreateNewKey) != null) {
      findViewById(R.id.buttonCreateNewKey).setOnClickListener(this);
    }

    if (findViewById(R.id.buttonImportMyKey) != null) {
      findViewById(R.id.buttonImportMyKey).setOnClickListener(this);
    }

    if (findViewById(R.id.buttonSelectAnotherAccount) != null) {
      if (isShowAnotherAccountButton) {
        findViewById(R.id.buttonSelectAnotherAccount).setVisibility(View.VISIBLE);
        findViewById(R.id.buttonSelectAnotherAccount).setOnClickListener(this);
      } else {
        findViewById(R.id.buttonSelectAnotherAccount).setVisibility(View.GONE);
      }
    }

    if (findViewById(R.id.buttonSkipSetup) != null) {
      View buttonSkipSetup = findViewById(R.id.buttonSkipSetup);
      if (SecurityUtils.isKeysBackupExist(this)) {
        buttonSkipSetup.setVisibility(View.VISIBLE);
        buttonSkipSetup.setOnClickListener(this);
      } else {
        buttonSkipSetup.setVisibility(View.GONE);
      }
    }
  }
}
