/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.os.Bundle;
import android.view.View;

import com.flowcrypt.email.R;
import com.flowcrypt.email.ui.activity.base.BaseActivity;
import com.flowcrypt.email.ui.activity.fragment.RestoreAccountFragment;
import com.flowcrypt.email.util.GeneralUtil;

/**
 * This class described restore an account functionality.
 *
 * @author DenBond7
 *         Date: 05.01.2017
 *         Time: 01:37
 *         E-mail: DenBond7@gmail.com
 */
public class LoadPrivateKeysBackupFromGmailActivity extends BaseActivity
        implements
        RestoreAccountFragment.OnRunEmailManagerActivityListener {

    public static final String KEY_EXTRA_ACCOUNT = GeneralUtil.generateUniqueExtraKey(
            "KEY_EXTRA_ACCOUNT", LoadPrivateKeysBackupFromGmailActivity.class);

    @Override
    public View getRootView() {
        return null;
    }

    @Override
    public boolean isDisplayHomeAsUpEnabled() {
        return false;
    }

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_restore_account;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       /*  if (getIntent() != null) {
           this.account = getIntent().getParcelableExtra(KEY_EXTRA_ACCOUNT);
        }

        initViews();
        if (account != null) {
            getSupportLoaderManager().initLoader(R.id.loader_id_load_gmail_backups, null, this);
        } else {
            finish();
        }*/
    }

    @Override
    public void onRunEmailManageActivity() {
        /*Intent startEmailServiceIntent = new Intent(this, EmailSyncService.class);
        startEmailServiceIntent.putExtra(EmailSyncService.EXTRA_KEY_GMAIL_ACCOUNT,
                account);
        startService(startEmailServiceIntent);

        Intent intentRunEmailManagerActivity = new Intent(this, EmailManagerActivity.class);
        intentRunEmailManagerActivity.putExtra(EmailManagerActivity.EXTRA_KEY_ACCOUNT,
                account);
        startActivity(intentRunEmailManagerActivity);
        finish();*/
    }
}
