/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.flowcrypt.email.R;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.KeyStoreCryptoManager;
import com.flowcrypt.email.ui.activity.SplashActivity;
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment;
import com.flowcrypt.email.ui.loader.EncryptAndSavePrivateKeysAsyncTaskLoader;
import com.flowcrypt.email.util.UIUtil;

import java.util.List;

/**
 * This class described restore an account functionality. Here we validate and save downloaded
 * and encrypted via {@link KeyStoreCryptoManager} keys to the database.
 *
 * @author DenBond7
 *         Date: 05.01.2017
 *         Time: 01:40
 *         E-mail: DenBond7@gmail.com
 */
public class RestoreAccountFragment extends BaseFragment implements View.OnClickListener,
        LoaderManager.LoaderCallbacks<LoaderResult> {
    private OnRunEmailManagerActivityListener onRunEmailManagerActivityListener;
    private List<String> privateKeys;
    private EditText editTextKeyPassword;
    private View progressBar;
    private boolean isThrowErrorIfDuplicateFound;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnRunEmailManagerActivityListener) {
            onRunEmailManagerActivityListener = (OnRunEmailManagerActivityListener) context;
        } else throw new IllegalArgumentException(context.toString() + " must implement " +
                RestoreAccountFragment.OnRunEmailManagerActivityListener.class.getSimpleName());
    }

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
                if (privateKeys != null && !privateKeys.isEmpty()) {
                    if (TextUtils.isEmpty(editTextKeyPassword.getText().toString())) {
                        UIUtil.showInfoSnackbar(editTextKeyPassword,
                                getString(R.string.passphrase_must_be_non_empty));
                    } else {
                        getLoaderManager().restartLoader(R.id
                                .loader_id_encrypt_and_save_private_keys_infos, null, this);
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
    public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_encrypt_and_save_private_keys_infos:
                progressBar.setVisibility(View.VISIBLE);
                return new EncryptAndSavePrivateKeysAsyncTaskLoader(getContext(), privateKeys,
                        editTextKeyPassword.getText().toString(), isThrowErrorIfDuplicateFound);

            default:
                return null;
        }
    }

    @Override
    public void handleSuccessLoaderResult(int loaderId, Object result) {
        switch (loaderId) {
            case R.id.loader_id_encrypt_and_save_private_keys_infos:
                progressBar.setVisibility(View.GONE);
                boolean booleanResult = (boolean) result;
                if (booleanResult) {
                    if (onRunEmailManagerActivityListener != null) {
                        onRunEmailManagerActivityListener.onRunEmailManageActivity();
                    }
                } else {
                    UIUtil.showInfoSnackbar(getView(), getString(R.string
                            .password_is_incorrect));
                }
                break;

            default:
                super.handleSuccessLoaderResult(loaderId, result);
        }
    }

    @Override
    public void handleFailureLoaderResult(int loaderId, Exception e) {
        super.handleFailureLoaderResult(loaderId, e);
        switch (loaderId) {
            case R.id.loader_id_encrypt_and_save_private_keys_infos:
                progressBar.setVisibility(View.GONE);
        }
    }

    /**
     * Update current list of private keys paths.
     */
    public void setPrivateKeys(List<String> keysPathList, boolean isThrowErrorIfDuplicateFound) {
        this.privateKeys = keysPathList;
        this.isThrowErrorIfDuplicateFound = isThrowErrorIfDuplicateFound;
    }

    private void initViews(View view) {
        if (view.findViewById(R.id.buttonLoadAccount) != null) {
            view.findViewById(R.id.buttonLoadAccount).setOnClickListener(this);
        }

        if (view.findViewById(R.id.buttonSelectAnotherAccount) != null) {
            view.findViewById(R.id.buttonSelectAnotherAccount).setOnClickListener(this);
        }

        editTextKeyPassword = (EditText) view.findViewById(R.id.editTextKeyPassword);
        progressBar = view.findViewById(R.id.progressBar);
    }

    public interface OnRunEmailManagerActivityListener {
        void onRunEmailManageActivity();
    }
}
