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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.View;

import com.flowcrypt.email.R;
import com.flowcrypt.email.model.PrivateKeyDetails;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.service.EmailSyncService;
import com.flowcrypt.email.ui.activity.base.BaseBackStackActivity;
import com.flowcrypt.email.ui.activity.fragment.dialog.InfoDialogFragment;
import com.flowcrypt.email.ui.loader.ValidatePrivateKeyAsyncTaskLoader;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * This activity describes a logic of import private keys.
 *
 * @author Denis Bondarenko
 *         Date: 20.07.2017
 *         Time: 16:59
 *         E-mail: DenBond7@gmail.com
 */

public class ImportPrivateKeyActivity extends BaseBackStackActivity
        implements View.OnClickListener, LoaderManager.LoaderCallbacks<LoaderResult> {

    public static final String EXTRA_KEY_ACCOUNT = GeneralUtil.generateUniqueExtraKey
            ("EXTRA_KEY_ACCOUNT", ImportPrivateKeyActivity.class);
    private static final int REQUEST_CODE_CHECK_PRIVATE_KEYS = 10;
    private static final int REQUEST_CODE_SELECT_KEYS_FROM_FILES_SYSTEM = 11;
    private static final int REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE = 12;
    private Account account;
    private ClipboardManager clipboardManager;
    private PrivateKeyDetails privateKeyDetails;
    private View layoutContentView;
    private View layoutProgress;
    private boolean isCheckingPrivateKeyNow;

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
                            privateKeyDetails = new PrivateKeyDetails(
                                    GeneralUtil.getFileNameFromUri(this, data.getData()),
                                    null,
                                    data.getData(),
                                    PrivateKeyDetails.Type.FILE);

                            getSupportLoaderManager().restartLoader(R.id
                                    .loader_id_validate_private_key_from_file, null, this);
                        }
                        break;
                }
                break;

            case REQUEST_CODE_CHECK_PRIVATE_KEYS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        EmailSyncService.startEmailSyncService(this, account);
                        EmailManagerActivity.runEmailManagerActivity(this, account);
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
    public void onBackPressed() {
        if (isCheckingPrivateKeyNow) {
            getSupportLoaderManager().destroyLoader(R.id.loader_id_validate_private_key_from_file);
            getSupportLoaderManager()
                    .destroyLoader(R.id.loader_id_validate_private_key_from_clipboard);
            isCheckingPrivateKeyNow = false;
            UIUtil.exchangeViewVisibility(getApplicationContext(), false, layoutProgress,
                    layoutContentView);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonLoadFromFile:
                dismissSnackBar();
                privateKeyDetails = null;

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
                dismissSnackBar();
                privateKeyDetails = null;

                if (clipboardManager.hasPrimaryClip()) {
                    ClipData.Item item = clipboardManager.getPrimaryClip().getItemAt(0);
                    CharSequence privateKeyFromClipboard = item.getText();
                    if (!TextUtils.isEmpty(privateKeyFromClipboard)) {
                        privateKeyDetails = new PrivateKeyDetails(null,
                                privateKeyFromClipboard.toString(),
                                PrivateKeyDetails.Type.CLIPBOARD);

                        getSupportLoaderManager().restartLoader(R.id
                                .loader_id_validate_private_key_from_clipboard, null, this);
                    } else {
                        showClipboardIsEmptyInfoDialog();
                    }
                } else {
                    showClipboardIsEmptyInfoDialog();
                }
                break;
        }
    }

    @Override
    public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_validate_private_key_from_file:
                isCheckingPrivateKeyNow = true;
                UIUtil.exchangeViewVisibility(getApplicationContext(),
                        true, layoutProgress, layoutContentView);
                return new ValidatePrivateKeyAsyncTaskLoader(getApplicationContext(),
                        privateKeyDetails, true);

            case R.id.loader_id_validate_private_key_from_clipboard:
                isCheckingPrivateKeyNow = true;
                UIUtil.exchangeViewVisibility(getApplicationContext(),
                        true, layoutProgress, layoutContentView);
                return new ValidatePrivateKeyAsyncTaskLoader(getApplicationContext(),
                        privateKeyDetails, false);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<LoaderResult> loader, LoaderResult loaderResult) {
        handleLoaderResult(loader, loaderResult);
    }

    @Override
    public void onLoaderReset(Loader<LoaderResult> loader) {
        switch (loader.getId()) {
            case R.id.loader_id_validate_private_key_from_file:
            case R.id.loader_id_validate_private_key_from_clipboard:
                isCheckingPrivateKeyNow = false;
                UIUtil.exchangeViewVisibility(getApplicationContext(),
                        false, layoutProgress, layoutContentView);
        }
    }

    @Override
    public void handleSuccessLoaderResult(int loaderId, Object result) {
        switch (loaderId) {
            case R.id.loader_id_validate_private_key_from_file:
                isCheckingPrivateKeyNow = false;
                UIUtil.exchangeViewVisibility(getApplicationContext(),
                        false, layoutProgress, layoutContentView);
                if ((boolean) result) {
                    startActivityForResult(CheckKeysActivity.newIntent(this,
                            new ArrayList<>(Arrays.asList(new
                                    PrivateKeyDetails[]{privateKeyDetails})),
                            getString(R.string.template_check_key_name,
                                    privateKeyDetails.getKeyName()),
                            getString(R.string.continue_),
                            getString(R.string.choose_another_key)),
                            REQUEST_CODE_CHECK_PRIVATE_KEYS);
                } else {
                    showInfoSnackbar(getRootView(),
                            getString(R.string.file_has_wrong_pgp_structure));
                }
                break;

            case R.id.loader_id_validate_private_key_from_clipboard:
                isCheckingPrivateKeyNow = false;
                UIUtil.exchangeViewVisibility(getApplicationContext(),
                        false, layoutProgress, layoutContentView);
                if ((boolean) result) {
                    startActivityForResult(CheckKeysActivity.newIntent(this,
                            new ArrayList<>(Arrays.asList(new
                                    PrivateKeyDetails[]{privateKeyDetails})),
                            getString(R.string.loaded_private_key_from_your_clipboard),
                            getString(R.string.continue_),
                            getString(R.string.choose_another_key)),
                            REQUEST_CODE_CHECK_PRIVATE_KEYS);
                } else {
                    showInfoSnackbar(getRootView(),
                            getString(R.string.clipboard_has_wrong_structure));
                }
                break;

            default:
                super.handleSuccessLoaderResult(loaderId, result);
        }
    }

    @Override
    public void handleFailureLoaderResult(int loaderId, Exception e) {
        switch (loaderId) {
            case R.id.loader_id_validate_private_key_from_file:
            case R.id.loader_id_validate_private_key_from_clipboard:
                isCheckingPrivateKeyNow = false;
                UIUtil.exchangeViewVisibility(getApplicationContext(),
                        false, layoutProgress, layoutContentView);
                showInfoSnackbar(getRootView(), e.getMessage());
                break;

            default:
                super.handleFailureLoaderResult(loaderId, e);
        }
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
        layoutContentView = findViewById(R.id.layoutContentView);
        layoutProgress = findViewById(R.id.layoutProgress);

        if (findViewById(R.id.buttonLoadFromFile) != null) {
            findViewById(R.id.buttonLoadFromFile).setOnClickListener(this);
        }

        if (findViewById(R.id.buttonLoadFromClipboard) != null) {
            findViewById(R.id.buttonLoadFromClipboard).setOnClickListener(this);
        }
    }
}
