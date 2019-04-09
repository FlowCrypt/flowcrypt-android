/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.database.dao.source.ActionQueueDaoSource;
import com.flowcrypt.email.jobscheduler.ForwardedAttachmentsDownloaderJobService;
import com.flowcrypt.email.jobscheduler.MessagesSenderJobService;
import com.flowcrypt.email.security.SecurityUtils;
import com.flowcrypt.email.service.EmailSyncService;
import com.flowcrypt.email.service.actionqueue.actions.EncryptPrivateKeysIfNeededAction;
import com.flowcrypt.email.ui.activity.base.BaseActivity;
import com.flowcrypt.email.util.SharedPreferencesHelper;

import androidx.preference.PreferenceManager;

/**
 * This is a launcher {@link Activity}
 *
 * @author Denis Bondarenko
 * Date: 3/5/19
 * Time: 9:57 AM
 * E-mail: DenBond7@gmail.com
 */
public class LauncherActivity extends BaseActivity {
  private AccountDao account;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    PreferenceManager.setDefaultValues(this, R.xml.preferences_notifications_settings, false);
    ForwardedAttachmentsDownloaderJobService.schedule(getApplicationContext());
    MessagesSenderJobService.schedule(getApplicationContext());

    account = new AccountDaoSource().getActiveAccountInformation(this);
    if (account != null && isNodeReady()) {
      showEmailManagerActivity();
    }
  }

  @Override
  protected void onNodeStateChanged(boolean isReady) {
    super.onNodeStateChanged(isReady);
    if (account != null) {
      showEmailManagerActivity();
    } else {
      showSignInActivity();
    }
  }

  @Override
  public boolean isDisplayHomeAsUpEnabled() {
    return false;
  }

  @Override
  public View getRootView() {
    return findViewById(R.id.layoutContent);
  }

  @Override
  public int getContentViewResourceId() {
    return R.layout.activity_launcher;
  }


  private void showSignInActivity() {
    startActivity(new Intent(this, SignInActivity.class));
    finish();
  }

  private void showEmailManagerActivity() {
    if (SecurityUtils.hasBackup(this)) {
      boolean isCheckKeysNeeded = SharedPreferencesHelper.getBoolean(PreferenceManager
          .getDefaultSharedPreferences(this), Constants.PREFERENCES_KEY_IS_CHECK_KEYS_NEEDED, true);

      if (isCheckKeysNeeded) {
        new ActionQueueDaoSource().addAction(this, new EncryptPrivateKeysIfNeededAction(account.getEmail()));
      }

      EmailSyncService.startEmailSyncService(this);
      EmailManagerActivity.runEmailManagerActivity(this);
      finish();
    }
  }
}
