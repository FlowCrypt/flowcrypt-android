/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.security.SecurityUtils;
import com.flowcrypt.email.ui.activity.base.BaseBackStackSyncActivity;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

import java.util.List;

/**
 * This activity helps a user to backup his private keys via next methods:
 * <ul>
 * <li>BACKUP AS EMAIL</li>
 * <li>BACKUP AS A FILE</li>
 * </ul>
 *
 * @author DenBond7
 *         Date: 30.05.2017
 *         Time: 15:27
 *         E-mail: DenBond7@gmail.com
 */

public class BackupSettingsActivity extends BaseBackStackSyncActivity implements View
        .OnClickListener,
        RadioGroup.OnCheckedChangeListener {

    private static final int REQUEST_CODE_GET_URI_FOR_SAVING_PRIVATE_KEY = 10;

    private View progressBar;
    private View layoutContent;
    private View layoutSyncStatus;
    private View layoutBackupFound;
    private View layoutBackupNotFound;
    private View layoutBackupOptions;
    private TextView textViewBackupFound;
    private TextView textViewOptionsHint;
    private RadioGroup radioGroupBackupsVariants;
    private Button buttonBackupAction;

    private List<String> privateKeys;
    private String account;
    private boolean isBackEnable = true;
    private boolean isLoadPrivateKeysRequestSent;

    @SuppressWarnings("unchecked")
    @Override
    public void onReplyFromSyncServiceReceived(int requestCode, int resultCode, Object obj) {
        switch (requestCode) {
            case R.id.syns_get_active_account:
                account = (String) obj;
                loadPrivateKeys(R.id.syns_load_private_keys, account);
                break;

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
                break;

            case R.id.syns_send_backup_with_private_key_to_key_owner:
                isBackEnable = true;
                layoutSyncStatus.setVisibility(View.GONE);
                UIUtil.exchangeViewVisibility(
                        BackupSettingsActivity.this, false, progressBar, layoutContent);
                UIUtil.showInfoSnackbar(getRootView(), getString(R.string
                        .backup_was_sent_successfully));
                break;
        }
    }

    @Override
    public void onErrorFromSyncServiceReceived(int requestCode, int errorType, Exception e) {
        switch (requestCode) {
            case R.id.syns_load_private_keys:
                UIUtil.exchangeViewVisibility(this, false, progressBar, layoutSyncStatus);
                UIUtil.showSnackbar(getRootView(),
                        getString(R.string.error_occurred_while_receiving_private_keys),
                        getString(R.string.retry),
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                layoutSyncStatus.setVisibility(View.GONE);
                                UIUtil.exchangeViewVisibility(
                                        BackupSettingsActivity.this, true,
                                        progressBar, layoutContent);
                                loadPrivateKeys(R.id.syns_load_private_keys, account);
                            }
                        });
                break;

            case R.id.syns_send_backup_with_private_key_to_key_owner:
                isBackEnable = true;
                UIUtil.exchangeViewVisibility(
                        BackupSettingsActivity.this, false, progressBar, layoutSyncStatus);

                UIUtil.showSnackbar(getRootView(),
                        getString(R.string.backup_was_not_sent),
                        getString(R.string.retry),
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                layoutSyncStatus.setVisibility(View.GONE);
                                UIUtil.exchangeViewVisibility(
                                        BackupSettingsActivity.this, true,
                                        progressBar, layoutContent);
                                sendMessageWithPrivateKeyBackup(R.id
                                        .syns_send_backup_with_private_key_to_key_owner, account);
                            }
                        });
                break;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initViews();
        if (GeneralUtil.isInternetConnectionAvailable(this)) {
            UIUtil.exchangeViewVisibility(this, true, progressBar, layoutContent);
        } else {
            finish();
            Toast.makeText(this, R.string.internet_connection_is_not_available, Toast
                    .LENGTH_SHORT).show();
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        if (!isLoadPrivateKeysRequestSent) {
            isLoadPrivateKeysRequestSent = true;
            requestActiveAccount(R.id.syns_get_active_account);
        }
    }

    @Override
    public void onBackPressed() {
        if (isBackEnable) {
            super.onBackPressed();
        } else {
            Toast.makeText(this, R.string.please_wait_while_message_will_be_sent, Toast
                    .LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuActionHelp:
                startActivity(new Intent(this, FeedbackActivity.class));
                return true;

            default:
                return super.onOptionsItemSelected(item);
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
                layoutBackupFound.setVisibility(View.GONE);
                layoutBackupOptions.setVisibility(View.VISIBLE);
                break;

            case R.id.buttonBackupAction:
                switch (radioGroupBackupsVariants.getCheckedRadioButtonId()) {
                    case R.id.radioButtonEmail:
                        if (GeneralUtil.isInternetConnectionAvailable(this)) {
                            isBackEnable = false;
                            UIUtil.exchangeViewVisibility(this, true, progressBar, layoutContent);
                            sendMessageWithPrivateKeyBackup(R.id
                                    .syns_send_backup_with_private_key_to_key_owner, account);
                        } else {
                            UIUtil.showInfoSnackbar(getRootView(), getString(R.string
                                    .internet_connection_is_not_available));
                        }
                        break;

                    case R.id.radioButtonDownload:
                        runActivityToChooseDestinationForExportedKey();
                        break;
                }
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        switch (group.getId()) {
            case R.id.radioGroupBackupsVariants:
                switch (checkedId) {
                    case R.id.radioButtonEmail:
                        if (textViewOptionsHint != null) {
                            textViewOptionsHint.setText(R.string.backup_as_email_hint);
                            buttonBackupAction.setText(R.string.backup_as_email);
                        }
                        break;

                    case R.id.radioButtonDownload:
                        if (textViewOptionsHint != null) {
                            textViewOptionsHint.setText(R.string.backup_as_download_hint);
                            buttonBackupAction.setText(R.string.backup_as_a_file);
                        }
                        break;
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_GET_URI_FOR_SAVING_PRIVATE_KEY:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        if (data != null && data.getData() != null) {
                            try {
                                GeneralUtil.writeFileFromStringToUri(this, data.getData(),
                                        SecurityUtils
                                                .getPrivateKeysInfo(this).get(0).getPgpKeyInfo()
                                                .getArmored());
                                Toast.makeText(this, R.string.key_successfully_saved, Toast
                                        .LENGTH_SHORT).show();
                            } catch (Exception e) {
                                e.printStackTrace();
                                UIUtil.showInfoSnackbar(getRootView(), e.getMessage());
                            }
                        }
                        break;
                }
                break;
        }
    }

    private void initViews() {
        this.progressBar = findViewById(R.id.progressBar);
        this.layoutContent = findViewById(R.id.layoutContent);
        this.layoutSyncStatus = findViewById(R.id.layoutSyncStatus);
        this.layoutBackupFound = findViewById(R.id.layoutBackupFound);
        this.layoutBackupNotFound = findViewById(R.id.layoutBackupNotFound);
        this.layoutBackupOptions = findViewById(R.id.layoutBackupOptions);
        this.textViewBackupFound = (TextView) findViewById(R.id.textViewBackupFound);
        this.textViewOptionsHint = (TextView) findViewById(R.id.textViewOptionsHint);
        this.radioGroupBackupsVariants = (RadioGroup) findViewById(R.id.radioGroupBackupsVariants);

        if (radioGroupBackupsVariants != null) {
            radioGroupBackupsVariants.setOnCheckedChangeListener(this);
        }

        if (findViewById(R.id.buttonSeeMoreBackupOptions) != null) {
            findViewById(R.id.buttonSeeMoreBackupOptions).setOnClickListener(this);
        }

        buttonBackupAction = (Button) findViewById(R.id.buttonBackupAction);
        if (buttonBackupAction != null) {
            buttonBackupAction.setOnClickListener(this);
        }
    }

    private void showNoBackupFoundView() {
        layoutBackupNotFound.setVisibility(View.VISIBLE);
        layoutBackupOptions.setVisibility(View.VISIBLE);
    }

    private void showBackupFoundView() {
        layoutBackupFound.setVisibility(View.VISIBLE);
        if (textViewBackupFound != null && privateKeys != null) {
            textViewBackupFound.setText(getString(R.string.backups_found_message,
                    privateKeys.size()));
        }
    }

    /**
     * Start a new Activity with return results to choose a destination for an exported key.
     */
    private void runActivityToChooseDestinationForExportedKey() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("plain/text");
        intent.putExtra(Intent.EXTRA_TITLE, SecurityUtils.generateNameForPrivateKey(account));
        startActivityForResult(intent, REQUEST_CODE_GET_URI_FOR_SAVING_PRIVATE_KEY);
    }
}
