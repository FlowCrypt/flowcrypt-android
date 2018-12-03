/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
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
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.JsForUiManager;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.security.SecurityStorageConnector;
import com.flowcrypt.email.ui.activity.base.BaseImportKeyActivity;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
  private List<String> privateKeys;
  private Js js;

  private View progressBarLoadingBackups;
  private View layoutContent;
  private View layoutSyncStatus;
  private Button buttonImportBackup;

  private boolean isLoadPrivateKeysRequestSent;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.js = JsForUiManager.getInstance(this).getJs();

    if (isSyncEnable() && GeneralUtil.isInternetConnectionAvailable(this)) {
      UIUtil.exchangeViewVisibility(this, true, progressBarLoadingBackups, layoutContent);
      countingIdlingResource = new CountingIdlingResource(GeneralUtil.generateNameForIdlingResources
          (ImportPrivateKeyActivity.class), GeneralUtil.isDebugBuild());
    } else {
      hideImportButton();
      UIUtil.exchangeViewVisibility(this, false, progressBarLoadingBackups, layoutContent);
    }
  }

  @Override
  protected void initViews() {
    super.initViews();

    this.progressBarLoadingBackups = findViewById(R.id.progressBarLoadingBackups);
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
  public void onReplyFromServiceReceived(int requestCode, int resultCode, Object obj) {
    switch (requestCode) {
      case R.id.syns_load_private_keys:
        if (privateKeys == null) {
          List<String> keys = (List<String>) obj;
          if (keys != null) {
            if (!keys.isEmpty()) {
              this.privateKeys = keys;

              SecurityStorageConnector securityStorageConnector =
                  (SecurityStorageConnector) js.getStorageConnector();

              Iterator<String> iterator = privateKeys.iterator();
              Set<String> uniqueKeysLongIds = new HashSet<>();

              while (iterator.hasNext()) {
                String privateKey = iterator.next();
                PgpKey pgpKey = js.crypto_key_read(privateKey);
                uniqueKeysLongIds.add(pgpKey.getLongid());
                if (securityStorageConnector.getPgpPrivateKey(pgpKey.getLongid()) != null) {
                  iterator.remove();
                  uniqueKeysLongIds.remove(pgpKey.getLongid());
                }
              }

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
          UIUtil.exchangeViewVisibility(this, false, progressBarLoadingBackups, layoutContent);
        }
        if (!countingIdlingResource.isIdleNow()) {
          countingIdlingResource.decrement();
        }
        break;
    }
  }

  @Override
  public void onErrorFromServiceReceived(int requestCode, int errorType, Exception e) {
    switch (requestCode) {
      case R.id.syns_load_private_keys:
        hideImportButton();
        UIUtil.exchangeViewVisibility(this, false, progressBarLoadingBackups, layoutSyncStatus);
        UIUtil.showSnackbar(getRootView(),
            getString(R.string.error_occurred_while_receiving_private_keys),
            getString(android.R.string.ok),
            new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                layoutSyncStatus.setVisibility(View.GONE);
                UIUtil.exchangeViewVisibility(ImportPrivateKeyActivity.this,
                    false, progressBarLoadingBackups, layoutContent);
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
        if (privateKeys != null && !privateKeys.isEmpty()) {
          ArrayList<KeyDetails> keyDetails = new ArrayList<>();

          for (String key : privateKeys) {
            keyDetails.add(new KeyDetails(key, KeyDetails.Type.EMAIL));
          }

          startActivityForResult(CheckKeysActivity.newIntent(this,
              keyDetails,
              null,
              getString(R.string.continue_),
              getString(R.string.choose_another_key)),
              REQUEST_CODE_CHECK_PRIVATE_KEYS);
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
        isCheckClipboardFromServiceEnable = false;

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
  public void onKeyValidated(KeyDetails.Type type) {
    switch (type) {
      case FILE:
        startActivityForResult(CheckKeysActivity.newIntent(this, keyDetailsList,
            getResources().getQuantityString(R.plurals.file_contains_some_amount_of_keys,
                keyDetailsList.size(), GeneralUtil.getFileNameFromUri(this,
                    keyImportModel.getFileUri()), keyDetailsList.size()),
            getString(R.string.continue_), null,
            getString(R.string.choose_another_key), true),
            REQUEST_CODE_CHECK_PRIVATE_KEYS);
        break;

      case CLIPBOARD:
        startActivityForResult(CheckKeysActivity.newIntent(this,
            keyDetailsList,
            getResources().getQuantityString(R.plurals.loaded_private_keys_from_clipboard,
                keyDetailsList.size(), keyDetailsList.size()),
            getString(R.string.continue_), null,
            getString(R.string.choose_another_key), true),
            REQUEST_CODE_CHECK_PRIVATE_KEYS);
        break;
    }
  }

  @Override
  public boolean isPrivateKeyChecking() {
    return true;
  }

  @VisibleForTesting
  public CountingIdlingResource getCountingIdlingResource() {
    return countingIdlingResource;
  }

  public void hideImportButton() {
    buttonImportBackup.setVisibility(View.GONE);
    ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams)
        buttonLoadFromFile.getLayoutParams();
    marginLayoutParams.topMargin = getResources().getDimensionPixelSize(R.dimen
        .margin_top_first_button);
    buttonLoadFromFile.requestLayout();
  }
}
