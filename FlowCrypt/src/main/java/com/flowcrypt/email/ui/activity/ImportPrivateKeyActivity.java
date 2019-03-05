/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails;
import com.flowcrypt.email.js.UiJsManager;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.security.SecurityStorageConnector;
import com.flowcrypt.email.ui.activity.base.BaseImportKeyActivity;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;
import com.google.android.gms.common.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.test.espresso.idling.CountingIdlingResource;

/**
 * This activity describes a logic of import private keys.
 *
 * @author Denis Bondarenko
 * Date: 20.07.2017
 * Time: 16:59
 * E-mail: DenBond7@gmail.com
 */

public class ImportPrivateKeyActivity extends BaseImportKeyActivity {

  private static final int REQUEST_CODE_CHECK_PRIVATE_KEYS = 100;
  private CountingIdlingResource countingIdlingResource;
  private ArrayList<NodeKeyDetails> privateKeys;

  private View progressBar;
  private View layoutContent;
  private View layoutSyncStatus;
  private Button buttonImportBackup;

  private boolean isLoadPrivateKeysRequestSent;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (isSyncEnabled() && GeneralUtil.isConnected(this)) {
      UIUtil.exchangeViewVisibility(this, true, progressBar, layoutContent);
      countingIdlingResource = new CountingIdlingResource(GeneralUtil.genIdlingResourcesName
          (ImportPrivateKeyActivity.class), GeneralUtil.isDebugBuild());
    } else {
      hideImportButton();
      UIUtil.exchangeViewVisibility(this, false, progressBar, layoutContent);
    }
  }

  @Override
  protected void initViews() {
    super.initViews();

    this.progressBar = findViewById(R.id.progressBarLoadingBackups);
    this.layoutContent = findViewById(R.id.layoutContent);
    this.layoutSyncStatus = findViewById(R.id.layoutSyncStatus);
    this.buttonImportBackup = findViewById(R.id.buttonImportBackup);
    this.buttonImportBackup.setOnClickListener(this);
  }

  @Override
  public int getContentViewResourceId() {
    return R.layout.activity_import_private_key;
  }

  @Override
  public void onSyncServiceConnected() {
    if (!isLoadPrivateKeysRequestSent) {
      isLoadPrivateKeysRequestSent = true;
      loadPrivateKeys(R.id.syns_load_private_keys);

      if (countingIdlingResource != null) {
        countingIdlingResource.increment();
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void onReplyReceived(int requestCode, int resultCode, Object obj) {
    switch (requestCode) {
      case R.id.syns_load_private_keys:
        if (privateKeys == null) {
          ArrayList<NodeKeyDetails> keys = (ArrayList<NodeKeyDetails>) obj;
          if (keys != null) {
            if (!keys.isEmpty()) {
              this.privateKeys = keys;

              Set<String> uniqueKeysLongIds = filterKeys();

              if (this.privateKeys.isEmpty()) {
                hideImportButton();
              } else {
                buttonImportBackup.setText(getResources().getQuantityString(
                    R.plurals.import_keys, uniqueKeysLongIds.size()));
                textViewTitle.setText(getResources().getQuantityString(
                    R.plurals.you_have_backups_that_was_not_imported, uniqueKeysLongIds.size()));
              }
            } else {
              hideImportButton();
            }
          } else {
            hideImportButton();
          }
          UIUtil.exchangeViewVisibility(this, false, progressBar, layoutContent);
        }
        if (!countingIdlingResource.isIdleNow()) {
          countingIdlingResource.decrement();
        }
        break;
    }
  }

  @Override
  public void onErrorHappened(int requestCode, int errorType, Exception e) {
    switch (requestCode) {
      case R.id.syns_load_private_keys:
        hideImportButton();
        UIUtil.exchangeViewVisibility(this, false, progressBar, layoutSyncStatus);
        UIUtil.showSnackbar(getRootView(), getString(R.string.error_occurred_while_receiving_private_keys),
            getString(android.R.string.ok), new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                layoutSyncStatus.setVisibility(View.GONE);
                UIUtil.exchangeViewVisibility(ImportPrivateKeyActivity.this,
                    false, progressBar, layoutContent);
              }
            });
        if (!countingIdlingResource.isIdleNow()) {
          countingIdlingResource.decrement();
        }
        break;
    }
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.buttonImportBackup:
        if (!CollectionUtils.isEmpty(privateKeys)) {
          startActivityForResult(CheckKeysActivity.newIntent(this, privateKeys, KeyDetails.Type.EMAIL, null,
              getString(R.string.continue_), getString(R.string.choose_another_key)), REQUEST_CODE_CHECK_PRIVATE_KEYS);
        }
        break;

      default:
        super.onClick(v);
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_CODE_CHECK_PRIVATE_KEYS:
        isCheckingClipboardEnabled = false;

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

  @Override
  public void onKeyFound(KeyDetails.Type type, ArrayList<NodeKeyDetails> keyDetailsList) {
    switch (type) {
      case FILE:
        String fileName = GeneralUtil.getFileNameFromUri(this, keyImportModel.getFileUri());
        String bottomTitle = getResources().getQuantityString(R.plurals.file_contains_some_amount_of_keys,
            keyDetailsList.size(), fileName, keyDetailsList.size());
        String posBtnTitle = getString(R.string.continue_);
        Intent intent = CheckKeysActivity.newIntent(this, keyDetailsList, KeyDetails.Type.FILE,
            bottomTitle, posBtnTitle, null, getString(R.string.choose_another_key), true);
        startActivityForResult(intent, REQUEST_CODE_CHECK_PRIVATE_KEYS);
        break;

      case CLIPBOARD:
        String title = getResources().getQuantityString(R.plurals.loaded_private_keys_from_clipboard,
            keyDetailsList.size(), keyDetailsList.size());
        Intent clipboardIntent = CheckKeysActivity.newIntent(this, keyDetailsList, KeyDetails.Type.CLIPBOARD, title,
            getString(R.string.continue_), null, getString(R.string.choose_another_key), true);
        startActivityForResult(clipboardIntent,
            REQUEST_CODE_CHECK_PRIVATE_KEYS);
        break;
    }
  }

  @Override
  public boolean isPrivateKeyMode() {
    return true;
  }

  @VisibleForTesting
  public CountingIdlingResource getCountingIdlingResource() {
    return countingIdlingResource;
  }

  public void hideImportButton() {
    buttonImportBackup.setVisibility(View.GONE);
    ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) buttonLoadFromFile
        .getLayoutParams();
    marginLayoutParams.topMargin = getResources().getDimensionPixelSize(R.dimen
        .margin_top_first_button);
    buttonLoadFromFile.requestLayout();
  }

  private Set<String> filterKeys() {
    SecurityStorageConnector connector = UiJsManager.getInstance(this).getSecurityStorageConnector();

    Iterator<NodeKeyDetails> iterator = privateKeys.iterator();
    Set<String> uniqueKeysLongIds = new HashSet<>();

    while (iterator.hasNext()) {
      NodeKeyDetails privateKey = iterator.next();
      uniqueKeysLongIds.add(privateKey.getLongId());
      if (connector.getPgpPrivateKey(privateKey.getLongId()) != null) {
        iterator.remove();
        uniqueKeysLongIds.remove(privateKey.getLongId());
      }
    }
    return uniqueKeysLongIds;
  }
}
