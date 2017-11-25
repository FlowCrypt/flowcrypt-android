/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.ui.loader.GetOwnPublicKeysAsAttachmentInfoAsyncTaskLoader;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

import java.util.ArrayList;

/**
 * This dialog can be used for collecting information about user public keys.
 *
 * @author Denis Bondarenko
 *         Date: 24.11.2017
 *         Time: 13:13
 *         E-mail: DenBond7@gmail.com
 */

public class PrepareSendUserPublicKeyDialogFragment extends BaseDialogFragment implements View.OnClickListener,
        LoaderManager.LoaderCallbacks<LoaderResult> {
    public static final String KEY_ATTACHMENT_INFO_LIST = GeneralUtil.generateUniqueExtraKey
            ("KEY_ATTACHMENT_INFO_LIST", InfoDialogFragment.class);

    private static final String KEY_FROM_EMAIL = GeneralUtil.generateUniqueExtraKey
            ("KEY_FROM_EMAIL", InfoDialogFragment.class);

    private TextView textViewMessage;
    private TextView emptyView;
    private ListView listViewKeys;
    private View progressBar;
    private View buttonOk;

    private String fromEmail;
    private ArrayList<AttachmentInfo> attachmentInfoList;

    public PrepareSendUserPublicKeyDialogFragment() {
    }

    public static PrepareSendUserPublicKeyDialogFragment newInstance(String fromEmail) {

        Bundle args = new Bundle();
        args.putString(KEY_FROM_EMAIL, fromEmail);

        PrepareSendUserPublicKeyDialogFragment fragment = new PrepareSendUserPublicKeyDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            this.fromEmail = args.getString(KEY_FROM_EMAIL);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.fragment_send_user_public_key, getView() !=
                null & getView() instanceof ViewGroup ? (ViewGroup) getView() : null, false);

        textViewMessage = view.findViewById(R.id.textViewMessage);
        progressBar = view.findViewById(R.id.progressBar);
        emptyView = view.findViewById(R.id.emptyView);
        listViewKeys = view.findViewById(R.id.listViewKeys);
        buttonOk = view.findViewById(R.id.buttonOk);
        buttonOk.setOnClickListener(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(view);

        getLoaderManager().initLoader(R.id.loader_id_load_own_public_keys_as_attachment_info, null, this);

        return builder.create();
    }

    @Override
    public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_load_own_public_keys_as_attachment_info:
                return new GetOwnPublicKeysAsAttachmentInfoAsyncTaskLoader(getContext());

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<LoaderResult> loader, LoaderResult loaderResult) {
        switch (loader.getId()) {
            case R.id.loader_id_load_own_public_keys_as_attachment_info:
                buttonOk.setVisibility(View.VISIBLE);
                handleLoaderResult(loader, loaderResult);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<LoaderResult> loader) {

    }

    @SuppressWarnings("unchecked")
    @Override
    public void handleSuccessLoaderResult(int loaderId, Object result) {
        switch (loaderId) {
            case R.id.loader_id_load_own_public_keys_as_attachment_info:
                UIUtil.exchangeViewVisibility(getContext(), false, progressBar, listViewKeys);
                attachmentInfoList = (ArrayList<AttachmentInfo>) result;

                if (attachmentInfoList != null && !attachmentInfoList.isEmpty()) {
                    String[] strings = new String[attachmentInfoList.size()];
                    for (int i = 0; i < attachmentInfoList.size(); i++) {
                        AttachmentInfo attachmentInfo = attachmentInfoList.get(i);
                        strings[i] = attachmentInfo.getName();
                    }

                    if (attachmentInfoList.size() > 1) {
                        textViewMessage.setText(R.string.tell_sender_to_update_their_settings);
                        textViewMessage.append("\n\n");
                        textViewMessage.append(getString(R.string.select_keys));

                        ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<>(getContext(), android.R
                                .layout.simple_list_item_multiple_choice, strings);

                        listViewKeys.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                        listViewKeys.setAdapter(stringArrayAdapter);

                        for (int i = 0; i < attachmentInfoList.size(); i++) {
                            AttachmentInfo attachmentInfo = attachmentInfoList.get(i);
                            if (fromEmail.equalsIgnoreCase(attachmentInfo.getEmail())) {
                                listViewKeys.setItemChecked(i, true);
                            }
                        }
                    } else {
                        textViewMessage.setText(R.string.tell_sender_to_update_their_settings);
                        textViewMessage.append("\n\n");
                        textViewMessage.append(getString(R.string.your_public_key_will_be_appended));

                        ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<>(getContext(), android.R
                                .layout.simple_list_item_1, strings);

                        listViewKeys.setAdapter(stringArrayAdapter);
                    }
                } else {
                    UIUtil.exchangeViewVisibility(getContext(), true, emptyView, listViewKeys);
                }
                break;

            default:
                super.handleSuccessLoaderResult(loaderId, result);
        }
    }

    @Override
    public void handleFailureLoaderResult(int loaderId, Exception e) {
        switch (loaderId) {
            case R.id.loader_id_load_own_public_keys_as_attachment_info:
                UIUtil.exchangeViewVisibility(getContext(), true, emptyView, progressBar);
                emptyView.setText(e.getMessage());
                break;

            default:
                super.handleFailureLoaderResult(loaderId, e);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonOk:
                if (attachmentInfoList.size() == 1) {
                    sendResult(Activity.RESULT_OK, attachmentInfoList);
                    dismiss();
                } else {
                    ArrayList<AttachmentInfo> selectedAttachmentInfoArrayList = new ArrayList<>();
                    SparseBooleanArray checkedItemPositions = listViewKeys.getCheckedItemPositions();
                    for (int i = 0; i < checkedItemPositions.size(); i++) {
                        int key = checkedItemPositions.keyAt(i);
                        if (checkedItemPositions.get(key)) {
                            selectedAttachmentInfoArrayList.add(attachmentInfoList.get(key));
                        }
                    }

                    if (selectedAttachmentInfoArrayList.isEmpty()) {
                        showToast(getString(R.string.you_should_select_one_or_more_keys));
                    } else {
                        sendResult(Activity.RESULT_OK, selectedAttachmentInfoArrayList);
                        dismiss();
                    }
                }

                break;
        }
    }

    private void sendResult(int result, ArrayList<AttachmentInfo> attachmentInfoList) {
        if (getTargetFragment() == null) {
            return;
        }

        Intent intent = new Intent();
        intent.putParcelableArrayListExtra(KEY_ATTACHMENT_INFO_LIST, attachmentInfoList);

        getTargetFragment().onActivityResult(getTargetRequestCode(), result, intent);
    }
}
