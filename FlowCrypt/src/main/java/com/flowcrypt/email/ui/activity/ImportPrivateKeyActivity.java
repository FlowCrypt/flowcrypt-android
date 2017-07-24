/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.Manifest;
import android.accounts.Account;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;

import com.flowcrypt.email.R;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.model.PrivateKeyDetails;
import com.flowcrypt.email.service.EmailSyncService;
import com.flowcrypt.email.ui.activity.base.BaseBackStackActivity;
import com.flowcrypt.email.ui.activity.fragment.dialog.InfoDialogFragment;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

import java.util.ArrayList;

/**
 * This activity describes a logic of import private keys.
 *
 * @author Denis Bondarenko
 *         Date: 20.07.2017
 *         Time: 16:59
 *         E-mail: DenBond7@gmail.com
 */

public class ImportPrivateKeyActivity extends BaseBackStackActivity
        implements View.OnClickListener {

    public static final String EXTRA_KEY_ACCOUNT = GeneralUtil.generateUniqueExtraKey
            ("EXTRA_KEY_ACCOUNT", ImportPrivateKeyActivity.class);
    private static final int ONE_MB = 1024 * 1024;
    private static final int REQUEST_CODE_CHECK_PRIVATE_KEYS = 10;
    private static final int REQUEST_CODE_SELECT_KEYS_FROM_FILES_SYSTEM = 11;
    private static final int REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE = 12;
    private Account account;
    private ClipboardManager clipboardManager;

    public static Intent newIntent(Context context, Account account) {
        Intent intent = new Intent(context, ImportPrivateKeyActivity.class);
        intent.putExtra(EXTRA_KEY_ACCOUNT, account);
        return intent;
    }

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_import_private_key;
    }

    @Override
    public View getRootView() {
        return findViewById(R.id.layoutContent);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null) {
            this.account = getIntent().getParcelableExtra(EXTRA_KEY_ACCOUNT);
        }

        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        initViews();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_SELECT_KEYS_FROM_FILES_SYSTEM:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        if (data != null) {
                            ArrayList<PrivateKeyDetails> privateKeyDetailsList = new ArrayList<>();
                            if (checkPrivateKeySize(data.getData())) {
                                privateKeyDetailsList.add(new PrivateKeyDetails(
                                        GeneralUtil.getFileNameFromUri(this, data.getData()),
                                        null,
                                        data.getData(),
                                        PrivateKeyDetails.Type.FILE));
                            }

                            if (!privateKeyDetailsList.isEmpty()) {
                                startActivityForResult(CheckKeysActivity.newIntent(this,
                                        privateKeyDetailsList,
                                        getString(R.string.template_check_key_name,
                                                privateKeyDetailsList.get(0).getKeyName()),
                                        getString(R.string.continue_),
                                        getString(R.string.choose_another_key)),
                                        REQUEST_CODE_CHECK_PRIVATE_KEYS);
                            } else {
                                showInfoSnackbar(getRootView(),
                                        getString(R.string.select_correct_private_key));
                            }
                        }
                        break;
                }
                break;

            case REQUEST_CODE_CHECK_PRIVATE_KEYS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        EmailSyncService.startEmailSyncService(this, account);
                        EmailManagerActivity.runEmailManagerActivity(this, account);
                        finish();
                        break;

                    case CheckKeysActivity.RESULT_NEGATIVE:

                        break;
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    runSelectFileIntent();
                } else {
                    UIUtil.showSnackbar(getRootView(),
                            getString(R.string.access_to_read_the_sdcard_id_denied),
                            getString(R.string.change), new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    GeneralUtil.showAppSettingScreen(ImportPrivateKeyActivity.this);
                                }
                            });
                }
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonLoadFromFile:
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    runSelectFileIntent();
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        showAnExplanationForReadSdCard();
                    } else {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE);
                    }
                }
                break;

            case R.id.buttonLoadFromClipboard:
                if (clipboardManager.hasPrimaryClip()) {
                    ClipData.Item item = clipboardManager.getPrimaryClip().getItemAt(0);
                    CharSequence privateKeyFromClipboard = item.getText();
                    if (!TextUtils.isEmpty(privateKeyFromClipboard)) {
                        if (isValidPrivateKey(privateKeyFromClipboard.toString())) {
                            ArrayList<PrivateKeyDetails> privateKeyDetailsList = new ArrayList<>();
                            privateKeyDetailsList.add(new PrivateKeyDetails(null,
                                    privateKeyFromClipboard.toString(),
                                    PrivateKeyDetails.Type.CLIPBOARD));
                            startActivityForResult(CheckKeysActivity.newIntent(this,
                                    privateKeyDetailsList,
                                    getString(R.string.loaded_private_key_from_your_clipboard),
                                    getString(R.string.continue_),
                                    getString(R.string.choose_another_key)),
                                    REQUEST_CODE_CHECK_PRIVATE_KEYS);
                            showInfoSnackbar(getRootView(), privateKeyFromClipboard.toString());
                        } else {
                            InfoDialogFragment infoDialogFragment = InfoDialogFragment.newInstance
                                    (getString(R.string.hint), getString(R.string
                                            .hint_clipboard_has_wrong_structure));
                            infoDialogFragment.show(getSupportFragmentManager(),
                                    InfoDialogFragment.class.getSimpleName());
                        }
                    } else {
                        showClipboardIsEmptyInfoDialog();
                    }
                } else {
                    showClipboardIsEmptyInfoDialog();
                }
                break;
        }
    }

    /**
     * Check that the private key has a valid structure.
     *
     * @param privateKeyFromClipboard The armored private key.
     * @return true if private key has valid structure, otherwise false.
     */
    private boolean isValidPrivateKey(String privateKeyFromClipboard) {
        //todo-denbond7 need to use loader to do this check (for better performance)
        try {
            Js js = new Js(this, null);
            String normalizedArmoredKey = js.crypto_key_normalize(privateKeyFromClipboard);
            PgpKey pgpKey = js.crypto_key_read(normalizedArmoredKey);
            if (!TextUtils.isEmpty(pgpKey.getLongid())
                    && !TextUtils.isEmpty(pgpKey.getFingerprint())
                    && pgpKey.getPrimaryUserId() != null) {
                return pgpKey.isPrivate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Check that the private key size mot bigger then 1 MB.
     *
     * @param data The {@link Uri} of the selected file.
     * @return true if the private key size mot bigger then 1 MB, otherwise false
     */
    private boolean checkPrivateKeySize(Uri data) {
        int fileSize = GeneralUtil.getFileSizeFromUri(this, data);
        return fileSize > 0 && fileSize <= ONE_MB;
    }

    /**
     * Show an explanation to the user for read the sdcard.
     * After the user sees the explanation, we try again to request the permission.
     */
    private void showAnExplanationForReadSdCard() {
        UIUtil.showSnackbar(getRootView(),
                getString(R.string.read_sdcard_permission_explanation_text),
                getString(R.string.do_request), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ActivityCompat.requestPermissions(ImportPrivateKeyActivity.this,
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE);
                    }
                });
    }

    /**
     * Show an explanation to the user for read the sdcard.
     * After the user sees the explanation, we try again to request the permission.
     */
    private void runSelectFileIntent() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(Intent.createChooser(intent,
                getString(R.string.select_key_or_keys)),
                REQUEST_CODE_SELECT_KEYS_FROM_FILES_SYSTEM);
    }

    private void showClipboardIsEmptyInfoDialog() {
        InfoDialogFragment infoDialogFragment = InfoDialogFragment.newInstance
                (getString(R.string.hint), getString(R.string
                        .hint_clipboard_is_empty, getString(R.string.app_name)));
        infoDialogFragment.show(getSupportFragmentManager(),
                InfoDialogFragment.class.getSimpleName());
    }

    private void initViews() {
        if (findViewById(R.id.buttonLoadFromFile) != null) {
            findViewById(R.id.buttonLoadFromFile).setOnClickListener(this);
        }

        if (findViewById(R.id.buttonLoadFromClipboard) != null) {
            findViewById(R.id.buttonLoadFromClipboard).setOnClickListener(this);
        }
    }
}
