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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.flowcrypt.email.ui.activity.base.BaseBackStackActivity;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

import java.io.IOException;
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

    private static final int REQUEST_CODE_IMPORT_KEYS = 101;
    private static final int REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE = 102;
    private ArrayList<String> privateKeys;
    private Account account;

    public ImportPrivateKeyActivity() {
        this.privateKeys = new ArrayList<>();
    }

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

        initViews();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_IMPORT_KEYS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        if (data != null) {
                            try {
                                ClipData clipData = data.getClipData();
                                if (clipData == null) {
                                    privateKeys.add(GeneralUtil.readFileFromUriToString
                                            (getApplicationContext(), data.getData()));
                                } else {
                                    for (int i = 0; i < clipData.getItemCount(); i++) {
                                        ClipData.Item item = clipData.getItemAt(i);
                                        privateKeys.add(GeneralUtil.readFileFromUriToString
                                                (getApplicationContext(), item.getUri()));
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                UIUtil.showInfoSnackbar(getRootView(), e.getMessage());
                            }

                            if (!privateKeys.isEmpty()) {

                            }
                        }
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
        intent.setType(Constants.MIME_TYPE_PGP_KEY);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent,
                getString(R.string.select_key_or_keys)), REQUEST_CODE_IMPORT_KEYS);
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
