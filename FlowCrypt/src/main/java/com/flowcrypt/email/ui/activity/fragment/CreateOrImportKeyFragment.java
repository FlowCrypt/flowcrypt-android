/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment;

import android.Manifest;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.flowcrypt.email.R;
import com.flowcrypt.email.ui.activity.CreateOrImportKeyActivity;
import com.flowcrypt.email.ui.activity.SplashActivity;
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

import java.io.IOException;
import java.util.ArrayList;

/**
 * This fragment describes a logic for create ot import private keys.
 *
 * @author DenBond7
 *         Date: 23.05.2017
 *         Time: 13:00
 *         E-mail: DenBond7@gmail.com
 */

public class CreateOrImportKeyFragment extends BaseFragment implements View.OnClickListener {
    private static final int REQUEST_CODE_IMPORT_KEYS = 101;
    private static final int REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE = 102;

    private ArrayList<String> privateKeys;
    private OnPrivateKeysSelectedListener onPrivateKeysSelectedListener;

    private boolean isShowAnotherAccountButton = true;

    public CreateOrImportKeyFragment() {
        this.privateKeys = new ArrayList<>();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnPrivateKeysSelectedListener) {
            this.onPrivateKeysSelectedListener = (OnPrivateKeysSelectedListener) context;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity().getIntent() != null && getActivity().getIntent().hasExtra
                (CreateOrImportKeyActivity.KEY_IS_SHOW_USE_ANOTHER_ACCOUNT_BUTTON)) {
            this.isShowAnotherAccountButton = getActivity().getIntent().getBooleanExtra
                    (CreateOrImportKeyActivity.KEY_IS_SHOW_USE_ANOTHER_ACCOUNT_BUTTON, true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_or_import_key, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonImportMyKey:
                if (ContextCompat.checkSelfPermission(getContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    runSelectFileIntent();
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                            Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        showAnExplanationForReadSdCard();
                    } else {
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE);
                    }
                }
                break;

            case R.id.buttonSelectAnotherAccount:
                getActivity().finish();
                startActivity(SplashActivity.getSignOutIntent(getContext()));
                break;
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
                    UIUtil.showSnackbar(getView(),
                            getString(R.string.access_to_read_the_sdcard_id_denied),
                            getString(R.string.change), new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    GeneralUtil.showAppSettingScreen(getContext());
                                }
                            });
                }
                break;
        }
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
                                            (getContext(), data.getData()));
                                } else {
                                    for (int i = 0; i < clipData.getItemCount(); i++) {
                                        ClipData.Item item = clipData.getItemAt(i);
                                        privateKeys.add(GeneralUtil.readFileFromUriToString
                                                (getContext(), item.getUri()));
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                UIUtil.showInfoSnackbar(getView(), e.getMessage());
                            }

                            if (!privateKeys.isEmpty() && onPrivateKeysSelectedListener != null) {
                                onPrivateKeysSelectedListener.onPrivateKeySelected(privateKeys);
                            }
                        }
                        break;
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }

    }

    /**
     * Change visibility of the Select Another Account button.
     *
     * @param showAnotherAccountButton if true the button will be visible, otherwise the button
     *                                 will be invisible.
     */
    public void setShowAnotherAccountButton(boolean showAnotherAccountButton) {
        isShowAnotherAccountButton = showAnotherAccountButton;
    }

    /**
     * Show an explanation to the user for read the sdcard.
     * After the user sees the explanation, we try again to request the permission.
     */
    private void showAnExplanationForReadSdCard() {
        UIUtil.showSnackbar(getView(),
                getString(R.string.read_sdcard_permission_explanation_text),
                getString(R.string.do_request), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
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
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent,
                getString(R.string.select_key_or_keys)), REQUEST_CODE_IMPORT_KEYS);
    }

    private void initViews(View view) {
        if (view.findViewById(R.id.buttonCreateNewKey) != null) {
            view.findViewById(R.id.buttonCreateNewKey).setOnClickListener(this);
        }

        if (view.findViewById(R.id.buttonImportMyKey) != null) {
            view.findViewById(R.id.buttonImportMyKey).setOnClickListener(this);
        }

        if (view.findViewById(R.id.buttonSelectAnotherAccount) != null) {
            if (isShowAnotherAccountButton) {
                view.findViewById(R.id.buttonSelectAnotherAccount).setVisibility(View.VISIBLE);
                view.findViewById(R.id.buttonSelectAnotherAccount).setOnClickListener(this);
            } else {
                view.findViewById(R.id.buttonSelectAnotherAccount).setVisibility(View.GONE);
            }
        }
    }

    /**
     * This listener handles a situation when private keys selected from the file system.
     */
    public interface OnPrivateKeysSelectedListener {
        void onPrivateKeySelected(ArrayList<String> privateKeys);
    }
}
