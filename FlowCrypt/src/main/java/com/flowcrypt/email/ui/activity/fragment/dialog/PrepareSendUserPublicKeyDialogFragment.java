/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.JsForUiManager;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.js.PgpKeyInfo;
import com.flowcrypt.email.util.GeneralUtil;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

/**
 * This dialog can be used for collecting information about user public keys.
 *
 * @author Denis Bondarenko
 * Date: 24.11.2017
 * Time: 13:13
 * E-mail: DenBond7@gmail.com
 */

public class PrepareSendUserPublicKeyDialogFragment extends BaseDialogFragment implements View.OnClickListener {
  public static final String KEY_ATTACHMENT_INFO_LIST = GeneralUtil.generateUniqueExtraKey
      ("KEY_ATTACHMENT_INFO_LIST", InfoDialogFragment.class);

  private ArrayList<AttachmentInfo> attachmentInfoList;
  private ListView listViewKeys;

  public PrepareSendUserPublicKeyDialogFragment() {
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.attachmentInfoList = new ArrayList<>();
    prepareAttachments();
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    View view = LayoutInflater.from(getContext()).inflate(R.layout.fragment_send_user_public_key, getView() !=
        null & getView() instanceof ViewGroup ? (ViewGroup) getView() : null, false);

    TextView textViewMessage = view.findViewById(R.id.textViewMessage);
    listViewKeys = view.findViewById(R.id.listViewKeys);
    View buttonOk = view.findViewById(R.id.buttonOk);
    buttonOk.setOnClickListener(this);

    if (attachmentInfoList.size() > 1) {
      textViewMessage.setText(R.string.tell_sender_to_update_their_settings);
      textViewMessage.append("\n\n");
      textViewMessage.append(getString(R.string.select_key));

      String[] strings = new String[attachmentInfoList.size()];
      for (int i = 0; i < attachmentInfoList.size(); i++) {
        AttachmentInfo attachmentInfo = attachmentInfoList.get(i);
        strings[i] = attachmentInfo.getEmail();
      }

      ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<>(getContext(), android.R
          .layout.simple_list_item_single_choice, strings);

      listViewKeys.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
      listViewKeys.setAdapter(stringArrayAdapter);
    } else {
      textViewMessage.setText(R.string.tell_sender_to_update_their_settings);
      listViewKeys.setVisibility(View.GONE);
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
    builder.setView(view);

    return builder.create();
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.buttonOk:
        if (attachmentInfoList != null) {
          if (attachmentInfoList.size() == 1) {
            sendResult(Activity.RESULT_OK, attachmentInfoList);
            dismiss();
          } else {
            if (attachmentInfoList.size() != 0) {
              ArrayList<AttachmentInfo> selectedAttachmentInfoArrayList = new ArrayList<>();
              SparseBooleanArray checkedItemPositions = listViewKeys.getCheckedItemPositions();
              if (checkedItemPositions != null) {
                for (int i = 0; i < checkedItemPositions.size(); i++) {
                  int key = checkedItemPositions.keyAt(i);
                  if (checkedItemPositions.get(key)) {
                    selectedAttachmentInfoArrayList.add(attachmentInfoList.get(key));
                  }
                }
              }

              if (selectedAttachmentInfoArrayList.isEmpty()) {
                showToast(getString(R.string.please_select_key));
              } else {
                sendResult(Activity.RESULT_OK, selectedAttachmentInfoArrayList);
                dismiss();
              }
            } else {
              dismiss();
            }
          }
        } else {
          dismiss();
        }
        break;
    }
  }

  public void prepareAttachments() {
    Js js = JsForUiManager.getInstance(getContext()).getJs();
    PgpKeyInfo[] pgpKeyInfoArray = js.getStorageConnector().getAllPgpPrivateKeys();
    for (PgpKeyInfo pgpKeyInfo : pgpKeyInfoArray) {
      PgpKey pgpKey = js.crypto_key_read(pgpKeyInfo.getPrivate());
      if (pgpKey != null) {
        PgpKey publicKey = pgpKey.toPublic();
        if (publicKey != null) {
          PgpContact primaryUserId = pgpKey.getPrimaryUserId();
          if (primaryUserId != null) {
            attachmentInfoList.add(EmailUtil.generateAttachmentInfoFromPublicKey(publicKey));
          }
        }
      }
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
