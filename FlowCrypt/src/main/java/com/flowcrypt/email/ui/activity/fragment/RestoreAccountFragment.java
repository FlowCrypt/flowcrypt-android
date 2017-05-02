package com.flowcrypt.email.ui.activity.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.eclipsesource.v8.V8Object;
import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.flowcrypt.email.test.Js;
import com.flowcrypt.email.test.PgpKey;
import com.flowcrypt.email.test.SampleStorageConnector;
import com.flowcrypt.email.ui.activity.EmailManagerActivity;
import com.flowcrypt.email.util.UIUtil;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * This class described restore an account functionality. There we can activate and save to the
 * security storage downloaded keys.
 *
 * @author DenBond7
 *         Date: 05.01.2017
 *         Time: 01:40
 *         E-mail: DenBond7@gmail.com
 */
public class RestoreAccountFragment extends BaseFragment implements View.OnClickListener {
    private static final String KEY_SUCCESS = "success";
    private List<String> keysPathList;
    private EditText editTextKeyPassword;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_restore_account, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonLoadAccount:
                //Todo-DenBond7 Better use loader for this code.
                if (keysPathList != null && !keysPathList.isEmpty()) {
                    if (TextUtils.isEmpty(editTextKeyPassword.getText().toString())) {
                        UIUtil.showInfoSnackbar(editTextKeyPassword,
                                getString(R.string.passphrase_must_be_non_empty));
                    } else {
                        try {
                            boolean isOneOrMoreKeyDecrypted = isOneOrMoreKeyDecrypted();

                            if (isOneOrMoreKeyDecrypted) {
                                startActivity(new Intent(getContext(), EmailManagerActivity.class));
                                getActivity().finish();
                            } else {
                                //Todo-DenBond7 Better to show this notification in some layout.
                                // We can show 2 buttons: 1)"Select another account" 2)"Refresh"
                                UIUtil.showInfoSnackbar(getView(), getString(R.string
                                        .password_is_incorrect));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
        }
    }

    /**
     * Update current list of private keys paths.
     */
    public void updateKeysPathList(List<String> keysPathList) {
        this.keysPathList = keysPathList;
    }

    /**
     * Try to decrypt some key with entered password.
     *
     * @return <tt>Boolean</tt> true if one or more key accepted, false otherwise;
     * @throws IOException
     */
    private boolean isOneOrMoreKeyDecrypted() throws IOException {
        Js js = new Js(getContext(), new SampleStorageConnector(getContext()));
        String passphrase = editTextKeyPassword.getText().toString();
        boolean isOneOrMoreKeySaved = false;
        for (String filePath : keysPathList) {
            File file = new File(filePath);
            try {
                String rawArmoredKey = FileUtils.readFileToString(file,
                        StandardCharsets.UTF_8);

                String normalizedArmoredKey = js.crypto_key_normalize
                        (rawArmoredKey);

                PgpKey pgpKey = js.crypto_key_read(normalizedArmoredKey);
                V8Object v8Object = js.crypto_key_decrypt(
                        pgpKey, passphrase);

                if (pgpKey.isPrivate() && v8Object != null
                        && v8Object.getBoolean(KEY_SUCCESS)) {
                    saveKeyToStorage(file.getParent(), normalizedArmoredKey, passphrase);
                    isOneOrMoreKeySaved = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return isOneOrMoreKeySaved;
    }

    /**
     * Try to decrypt some key with entered password.
     *
     * @param directory            Directory where file will be save;
     * @param normalizedArmoredKey A normalized key;
     * @param passphrase           A passphrase which user entered;
     */
    private void saveKeyToStorage(String directory, String normalizedArmoredKey, String
            passphrase) {
        try {
            String fileName = Constants.PREFIX_PRIVATE_KEY + passphrase;
            FileUtils.writeStringToFile(new File(directory, fileName),
                    normalizedArmoredKey,
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initViews(View view) {
        if (view.findViewById(R.id.buttonLoadAccount) != null) {
            view.findViewById(R.id.buttonLoadAccount).setOnClickListener(this);
        }

        editTextKeyPassword = (EditText) view.findViewById(R.id.editTextKeyPassword);
    }
}
