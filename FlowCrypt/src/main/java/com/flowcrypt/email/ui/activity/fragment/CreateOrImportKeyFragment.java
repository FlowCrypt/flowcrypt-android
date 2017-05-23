package com.flowcrypt.email.ui.activity.fragment;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.flowcrypt.email.R;
import com.flowcrypt.email.model.SignInType;
import com.flowcrypt.email.ui.activity.base.BaseAuthenticationActivity;
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment;
import com.flowcrypt.email.util.UIUtil;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    private ArrayList<String> privateKeys;
    private OnPrivateKeysSelectedListener onPrivateKeysSelectedListener;

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
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(intent, REQUEST_CODE_IMPORT_KEYS);
                break;

            case R.id.buttonSelectAnotherAccount:
                if (getActivity() instanceof BaseAuthenticationActivity) {
                    BaseAuthenticationActivity baseAuthenticationActivity =
                            (BaseAuthenticationActivity) getActivity();
                    baseAuthenticationActivity.signOut(SignInType.GMAIL);
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
                                    privateKeys.add(readFileFromUriToString(data.getData()));
                                } else {
                                    for (int i = 0; i < clipData.getItemCount(); i++) {
                                        ClipData.Item item = clipData.getItemAt(i);
                                        privateKeys.add(readFileFromUriToString(item.getUri()));
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

    private void initViews(View view) {
        if (view.findViewById(R.id.buttonCreateNewKey) != null) {
            view.findViewById(R.id.buttonCreateNewKey).setOnClickListener(this);
        }

        if (view.findViewById(R.id.buttonImportMyKey) != null) {
            view.findViewById(R.id.buttonImportMyKey).setOnClickListener(this);
        }

        if (view.findViewById(R.id.buttonSelectAnotherAccount) != null) {
            view.findViewById(R.id.buttonSelectAnotherAccount).setOnClickListener(this);
        }
    }

    /**
     * Read a file by his Uri and return him as {@link String}.
     *
     * @param uri The {@link Uri} of the file.
     * @return <tt>{@link String}</tt> which contains a file.
     * @throws IOException will thrown for example if the file not found
     */
    private String readFileFromUriToString(Uri uri) throws IOException {
        return IOUtils.toString(getContext().getContentResolver().openInputStream(uri),
                StandardCharsets.UTF_8);
    }

    /**
     * This listener handles a situation when private keys selected from the file system.
     */
    public interface OnPrivateKeysSelectedListener {
        void onPrivateKeySelected(ArrayList<String> privateKeys);
    }
}
