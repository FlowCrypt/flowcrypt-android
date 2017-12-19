/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import com.flowcrypt.email.R;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.JsForUiManager;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.security.SecurityStorageConnector;
import com.flowcrypt.email.service.EmailSyncService;
import com.flowcrypt.email.ui.activity.base.BaseImportKeyActivity;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * This activity describes a logic of import private keys.
 *
 * @author Denis Bondarenko
 *         Date: 20.07.2017
 *         Time: 16:59
 *         E-mail: DenBond7@gmail.com
 */

public class ImportPrivateKeyActivity extends BaseImportKeyActivity {

    private static final int REQUEST_CODE_CHECK_PRIVATE_KEYS = 100;

    private List<String> privateKeys;
    private Js js;

    private View progressBarLoadingBackups;
    private View layoutContent;
    private View layoutSyncStatus;
    private View buttonImportBackup;

    private boolean isLoadPrivateKeysRequestSent;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.js = JsForUiManager.getInstance(this).getJs();

        if (GeneralUtil.isInternetConnectionAvailable(this)) {
            UIUtil.exchangeViewVisibility(this, true, progressBarLoadingBackups, layoutContent);
        } else {
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
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);

        if (name.getClassName().equalsIgnoreCase(EmailSyncService.class.getName())) {
            if (!isLoadPrivateKeysRequestSent) {
                isLoadPrivateKeysRequestSent = true;
                loadPrivateKeys(R.id.syns_load_private_keys);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onReplyFromSyncServiceReceived(int requestCode, int resultCode, Object obj) {
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

                            while (iterator.hasNext()) {
                                String privateKey = iterator.next();
                                PgpKey pgpKey = js.crypto_key_read(privateKey);
                                if (securityStorageConnector.getPgpPrivateKey(pgpKey.getLongid()) != null) {
                                    iterator.remove();
                                }
                            }

                            if (this.privateKeys.isEmpty()) {
                                hideImportButton();
                            } else {
                                textViewImportKeyTitle.setText(getResources().getQuantityString(
                                        R.plurals.you_have_backups_that_was_not_imported, this.privateKeys.size()));
                            }
                        }
                    }
                    UIUtil.exchangeViewVisibility(this, false, progressBarLoadingBackups, layoutContent);
                }
                break;
        }
    }

    @Override
    public void onErrorFromSyncServiceReceived(int requestCode, int errorType, Exception e) {
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
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonImportBackup:
                if (privateKeys.size() > 0) {
                    this.keyDetails = new KeyDetails(privateKeys.get(0), KeyDetails.Type.EMAIL);
                    startActivityForResult(CheckKeysActivity.newIntent(this,
                            new ArrayList<>(Arrays.asList(new KeyDetails[]{keyDetails})),
                            getString(R.string.found_backup_of_your_account_key),
                            getString(R.string.continue_),
                            getString(R.string.choose_another_key), isThrowErrorIfDuplicateFound),
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
                startActivityForResult(CheckKeysActivity.newIntent(this,
                        new ArrayList<>(Arrays.asList(new KeyDetails[]{keyDetails})),
                        getString(R.string.template_check_key_name,
                                keyDetails.getKeyName()),
                        getString(R.string.continue_),
                        getString(R.string.choose_another_key),
                        isThrowErrorIfDuplicateFound),
                        REQUEST_CODE_CHECK_PRIVATE_KEYS);
                break;

            case CLIPBOARD:
                startActivityForResult(CheckKeysActivity.newIntent(this,
                        new ArrayList<>(Arrays.asList(new KeyDetails[]{keyDetails})),
                        getString(R.string.loaded_private_key_from_your_clipboard),
                        getString(R.string.continue_),
                        getString(R.string.choose_another_key),
                        isThrowErrorIfDuplicateFound),
                        REQUEST_CODE_CHECK_PRIVATE_KEYS);
                break;
        }
    }

    @Override
    public boolean isPrivateKeyChecking() {
        return true;
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
