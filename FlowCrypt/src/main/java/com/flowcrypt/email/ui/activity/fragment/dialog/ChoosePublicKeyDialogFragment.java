/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
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
import com.flowcrypt.email.api.retrofit.node.NodeRepository;
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails;
import com.flowcrypt.email.api.retrofit.response.node.NodeResponseWrapper;
import com.flowcrypt.email.api.retrofit.response.node.ParseKeysResult;
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeysViewModel;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;
import com.google.android.gms.common.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

/**
 * This dialog can be used for collecting information about user public keys.
 *
 * @author Denis Bondarenko
 * Date: 24.11.2017
 * Time: 13:13
 * E-mail: DenBond7@gmail.com
 */

public class ChoosePublicKeyDialogFragment extends BaseDialogFragment implements View.OnClickListener,
    Observer<NodeResponseWrapper> {
  public static final String KEY_ATTACHMENT_INFO_LIST = GeneralUtil.generateUniqueExtraKey
      ("KEY_ATTACHMENT_INFO_LIST", InfoDialogFragment.class);

  public static final String KEY_TO = GeneralUtil.generateUniqueExtraKey
      ("KEY_TO", InfoDialogFragment.class);

  private ArrayList<AttachmentInfo> atts;
  private ListView listViewKeys;
  private TextView textViewMsg;
  private View progressBar;
  private View content;
  private String to;

  public ChoosePublicKeyDialogFragment() {
  }

  public static ChoosePublicKeyDialogFragment newInstance(String to) {
    Bundle args = new Bundle();
    args.putString(KEY_TO, to);

    ChoosePublicKeyDialogFragment fragment = new ChoosePublicKeyDialogFragment();
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (getArguments() != null) {
      this.to = getArguments().getString(KEY_TO);
    }

    this.atts = new ArrayList<>();
    PrivateKeysViewModel viewModel = ViewModelProviders.of(this).get(PrivateKeysViewModel.class);
    viewModel.init(new NodeRepository());
    viewModel.getResponsesLiveData().observe(this, this);
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    View view = LayoutInflater.from(getContext()).inflate(R.layout.fragment_send_user_public_key, getView() !=
        null & getView() instanceof ViewGroup ? (ViewGroup) getView() : null, false);

    textViewMsg = view.findViewById(R.id.textViewMessage);
    progressBar = view.findViewById(R.id.progressBar);
    listViewKeys = view.findViewById(R.id.listViewKeys);
    content = view.findViewById(R.id.groupContent);
    View buttonOk = view.findViewById(R.id.buttonOk);
    buttonOk.setOnClickListener(this);

    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
    builder.setView(view);

    return builder.create();
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.buttonOk:
        if (atts != null) {
          if (atts.size() == 1) {
            sendResult(Activity.RESULT_OK, atts);
            dismiss();
          } else {
            if (!atts.isEmpty()) {
              sendResult();
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

  @Override
  public void onChanged(NodeResponseWrapper nodeResponseWrapper) {
    switch (nodeResponseWrapper.getRequestCode()) {
      case R.id.live_data_id_fetch_keys:
        switch (nodeResponseWrapper.getStatus()) {
          case LOADING:
            UIUtil.exchangeViewVisibility(getContext(), true, progressBar, content);
            break;

          case SUCCESS:
            ParseKeysResult parseKeysResult = (ParseKeysResult) nodeResponseWrapper.getResult();
            List<NodeKeyDetails> nodeKeyDetailsList = parseKeysResult.getNodeKeyDetails();
            if (CollectionUtils.isEmpty(nodeKeyDetailsList)) {
              textViewMsg.setText(getString(R.string.no_pub_keys));
            } else {
              for (NodeKeyDetails nodeKeyDetails : nodeKeyDetailsList) {
                AttachmentInfo att = EmailUtil.genAttInfoFromPubKey(nodeKeyDetails);
                if (att != null) {
                  atts.add(att);
                }
              }

              UIUtil.exchangeViewVisibility(getContext(), false, progressBar, content);

              NodeKeyDetails matchedDetail = getMatchedKey(nodeKeyDetailsList);
              if (matchedDetail != null) {
                AttachmentInfo att = EmailUtil.genAttInfoFromPubKey(matchedDetail);
                if (att != null) {
                  atts.clear();
                  atts.add(att);
                }
              }

              if (atts.size() > 1) {
                textViewMsg.setText(R.string.tell_sender_to_update_their_settings);
                textViewMsg.append("\n\n");
                textViewMsg.append(getString(R.string.select_key));

                String[] strings = new String[atts.size()];
                for (int i = 0; i < atts.size(); i++) {
                  AttachmentInfo att = atts.get(i);
                  strings[i] = att.getEmail();
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_list_item_single_choice, strings);

                listViewKeys.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                listViewKeys.setAdapter(adapter);
              } else {
                textViewMsg.setText(R.string.tell_sender_to_update_their_settings);
                listViewKeys.setVisibility(View.GONE);
              }
            }
            break;

          case ERROR:
            UIUtil.exchangeViewVisibility(getContext(), false, progressBar, textViewMsg);
            textViewMsg.setText(nodeResponseWrapper.getResult().getError().toString());
            break;

          case EXCEPTION:
            UIUtil.exchangeViewVisibility(getContext(), false, progressBar, textViewMsg);
            textViewMsg.setText(nodeResponseWrapper.getException().getMessage());
            break;
        }
        break;
    }
  }

  private void sendResult() {
    ArrayList<AttachmentInfo> selectedAtts = new ArrayList<>();
    SparseBooleanArray checkedItemPositions = listViewKeys.getCheckedItemPositions();
    if (checkedItemPositions != null) {
      for (int i = 0; i < checkedItemPositions.size(); i++) {
        int key = checkedItemPositions.keyAt(i);
        if (checkedItemPositions.get(key)) {
          selectedAtts.add(atts.get(key));
        }
      }
    }

    if (selectedAtts.isEmpty()) {
      showToast(getString(R.string.please_select_key));
    } else {
      sendResult(Activity.RESULT_OK, selectedAtts);
      dismiss();
    }
  }

  private void sendResult(int result, ArrayList<AttachmentInfo> atts) {
    if (getTargetFragment() == null) {
      return;
    }

    Intent intent = new Intent();
    intent.putParcelableArrayListExtra(KEY_ATTACHMENT_INFO_LIST, atts);

    getTargetFragment().onActivityResult(getTargetRequestCode(), result, intent);
  }

  /**
   * Get the matched {@link NodeKeyDetails}. If the sender email matched to the email from {@link PgpContact} which got
   * from the private key than we return a relevant public key.
   *
   * @return A matched {@link NodeKeyDetails} or null.
   */
  private NodeKeyDetails getMatchedKey(List<NodeKeyDetails> nodeKeyDetailsList) {
    for (NodeKeyDetails nodeKeyDetails : nodeKeyDetailsList) {
      PgpContact primaryUserId = nodeKeyDetails.getPrimaryPgpContact();
      if (primaryUserId.getEmail().equalsIgnoreCase(to)) {
        return nodeKeyDetails;
      }
    }

    return null;
  }
}
