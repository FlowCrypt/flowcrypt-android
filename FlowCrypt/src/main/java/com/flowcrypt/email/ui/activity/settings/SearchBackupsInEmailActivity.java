/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.ui.activity.BackupKeysActivity;
import com.flowcrypt.email.ui.activity.base.BaseSettingsBackStackSyncActivity;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.test.espresso.idling.CountingIdlingResource;

/**
 * This activity helps a user to backup his private keys via next methods:
 * <ul>
 * <li>BACKUP AS EMAIL</li>
 * <li>BACKUP AS A FILE</li>
 * </ul>
 *
 * @author DenBond7
 * Date: 30.05.2017
 * Time: 15:27
 * E-mail: DenBond7@gmail.com
 */

public class SearchBackupsInEmailActivity extends BaseSettingsBackStackSyncActivity implements View.OnClickListener {
  public static final int REQUEST_CODE_BACKUP_WITH_OPTION = 100;

  private CountingIdlingResource countingIdlingResource;
  private View progressBar;
  private View layoutContent;
  private View layoutSyncStatus;
  private View layoutBackupFound;
  private View layoutBackupNotFound;
  private TextView textViewBackupFound;

  private List<String> privateKeys;

  private boolean isLoadPrivateKeysRequestSent;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    initViews();

    if (GeneralUtil.isInternetConnectionAvailable(this)) {
      UIUtil.exchangeViewVisibility(this, true, progressBar, layoutContent);
    } else {
      Toast.makeText(this, R.string.internet_connection_is_not_available, Toast.LENGTH_SHORT).show();
      finish();
    }
    countingIdlingResource = new CountingIdlingResource(GeneralUtil.generateNameForIdlingResources
        (SearchBackupsInEmailActivity.class), GeneralUtil.isDebugBuild());
    countingIdlingResource.increment();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_CODE_BACKUP_WITH_OPTION:
        switch (resultCode) {
          case Activity.RESULT_OK:
            Toast.makeText(this, R.string.backed_up_successfully, Toast.LENGTH_SHORT).show();
            finish();
            break;
        }
        break;
      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void onReplyReceived(int requestCode, int resultCode, Object obj) {
    switch (requestCode) {
      case R.id.syns_load_private_keys:
        if (privateKeys == null) {
          UIUtil.exchangeViewVisibility(this, false, progressBar, layoutContent);
          List<String> keys = (List<String>) obj;
          if (keys != null) {
            if (keys.isEmpty()) {
              showNoBackupFoundView();
            } else {
              this.privateKeys = keys;
              showBackupFoundView();
            }
          } else {
            showNoBackupFoundView();
          }
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
        UIUtil.exchangeViewVisibility(this, false, progressBar, layoutSyncStatus);
        if (!countingIdlingResource.isIdleNow()) {
          countingIdlingResource.decrement();
        }
        UIUtil.showSnackbar(getRootView(),
            getString(R.string.error_occurred_while_receiving_private_keys),
            getString(R.string.retry),
            new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                layoutSyncStatus.setVisibility(View.GONE);
                UIUtil.exchangeViewVisibility(SearchBackupsInEmailActivity.this, true,
                    progressBar, layoutContent);
                loadPrivateKeys(R.id.syns_load_private_keys);
              }
            });
        break;
    }
  }

  @Override
  public void onSyncServiceConnected() {
    super.onSyncServiceConnected();
    if (!isLoadPrivateKeysRequestSent) {
      isLoadPrivateKeysRequestSent = true;
      loadPrivateKeys(R.id.syns_load_private_keys);
    }
  }

  @Override
  public int getContentViewResourceId() {
    return R.layout.activity_backup_settings;
  }

  @Override
  public View getRootView() {
    return layoutContent;
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.buttonSeeMoreBackupOptions:
      case R.id.buttonBackupMyKey:
        startActivityForResult(new Intent(this, BackupKeysActivity.class), REQUEST_CODE_BACKUP_WITH_OPTION);
        break;
    }
  }

  @VisibleForTesting
  public CountingIdlingResource getCountingIdlingResource() {
    return countingIdlingResource;
  }

  private void initViews() {
    this.progressBar = findViewById(R.id.progressBar);
    this.layoutContent = findViewById(R.id.layoutContent);
    this.layoutSyncStatus = findViewById(R.id.layoutSyncStatus);
    this.layoutBackupFound = findViewById(R.id.layoutBackupFound);
    this.layoutBackupNotFound = findViewById(R.id.layoutBackupNotFound);
    this.textViewBackupFound = findViewById(R.id.textViewBackupFound);

    if (findViewById(R.id.buttonSeeMoreBackupOptions) != null) {
      findViewById(R.id.buttonSeeMoreBackupOptions).setOnClickListener(this);
    }

    if (findViewById(R.id.buttonBackupMyKey) != null) {
      findViewById(R.id.buttonBackupMyKey).setOnClickListener(this);
    }
  }

  private void showNoBackupFoundView() {
    layoutBackupNotFound.setVisibility(View.VISIBLE);
  }

  private void showBackupFoundView() {
    layoutBackupFound.setVisibility(View.VISIBLE);
    if (textViewBackupFound != null && privateKeys != null) {
      textViewBackupFound.setText(getString(R.string.backups_found_message,
          privateKeys.size()));
    }
  }
}
