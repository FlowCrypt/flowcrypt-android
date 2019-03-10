/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment;
import com.flowcrypt.email.ui.activity.fragment.dialog.InfoDialogFragment;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.google.android.material.snackbar.Snackbar;

import org.apache.commons.io.FilenameUtils;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * This {@link Fragment} helps to show details about the given key.
 *
 * @author Denis Bondarenko
 * Date: 20.11.18
 * Time: 12:43
 * E-mail: DenBond7@gmail.com
 */
public class KeyDetailsFragment extends BaseFragment implements View.OnClickListener {
  private static final String KEY_NODE_KEY_DETAILS = GeneralUtil.generateUniqueExtraKey("KEY_NODE_KEY_DETAILS",
      KeyDetailsFragment.class);
  private static final int REQUEST_CODE_GET_URI_FOR_SAVING_KEY = 1;

  private NodeKeyDetails details;

  public static KeyDetailsFragment newInstance(NodeKeyDetails details) {
    KeyDetailsFragment keyDetailsFragment = new KeyDetailsFragment();
    Bundle args = new Bundle();
    args.putParcelable(KEY_NODE_KEY_DETAILS, details);
    keyDetailsFragment.setArguments(args);
    return keyDetailsFragment;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Bundle args = getArguments();
    if (args != null) {
      details = args.getParcelable(KEY_NODE_KEY_DETAILS);
    }

    if (details == null) {
      getFragmentManager().popBackStack();
    }
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle bundle) {
    return inflater.inflate(R.layout.fragment_key_details, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    initViews(view);
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    if (getSupportActionBar() != null) {
      getSupportActionBar().setTitle(R.string.my_public_key);
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case REQUEST_CODE_GET_URI_FOR_SAVING_KEY:
        switch (resultCode) {
          case Activity.RESULT_OK:
            if (data != null && data.getData() != null) {
              saveKey(data);
            }
            break;
        }
        break;
    }
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.btnShowPubKey:
        InfoDialogFragment dialogFragment = InfoDialogFragment.newInstance("", details.getPublicKey());
        dialogFragment.show(getFragmentManager(), InfoDialogFragment.class.getSimpleName());
        break;

      case R.id.btnCopyToClipboard:
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("pubKey", details.getPublicKey()));
        Toast.makeText(getContext(), getString(R.string.copied), Toast.LENGTH_SHORT).show();
        break;

      case R.id.btnSaveToFile:
        chooseDest();
        break;

      case R.id.btnShowPrKey:
        Toast.makeText(getContext(), getString(R.string.see_backups_to_save_your_private_keys),
            Toast.LENGTH_SHORT).show();
        break;
    }
  }

  private void saveKey(Intent data) {
    try {
      GeneralUtil.writeFileFromStringToUri(getContext(), data.getData(), details.getPublicKey());
      String fileName = GeneralUtil.getFileNameFromUri(getContext(), data.getData());
      String newFileName = null;

      if (!TextUtils.isEmpty(fileName)) {
        newFileName = FilenameUtils.removeExtension(fileName) + ".asc";
      }

      if (!fileName.equals(newFileName)) {
        DocumentsContract.renameDocument(getContext().getContentResolver(), data.getData(), fileName);
      }

      Toast.makeText(getContext(), getString(R.string.saved), Toast.LENGTH_SHORT).show();
    } catch (Exception e) {
      e.printStackTrace();
      String error = TextUtils.isEmpty(e.getMessage()) ? getString(R.string.unknown_error) : e.getMessage();

      if (e instanceof IllegalStateException) {
        if (e.getMessage() != null && e.getMessage().startsWith("Already exists")) {
          error = getString(R.string.not_saved_file_already_exists);
          showInfoSnackbar(getView(), error, Snackbar.LENGTH_LONG);

          try {
            DocumentsContract.deleteDocument(getContext().getContentResolver(), data.getData());
          } catch (FileNotFoundException fileNotFound) {
            fileNotFound.printStackTrace();
          }

          return;
        } else {
          ExceptionUtil.handleError(e);
        }
      } else {
        ExceptionUtil.handleError(e);
      }

      showInfoSnackbar(getView(), error);
    }
  }

  private void initViews(View view) {
    List<PgpContact> pgpContacts = details.getPgpContacts();
    ArrayList<String> emails = new ArrayList<>();

    for (PgpContact pgpContact : pgpContacts) {
      emails.add(pgpContact.getEmail());
    }

    TextView textViewKeyWords = view.findViewById(R.id.textViewKeyWords);
    UIUtil.setHtmlTextToTextView(getString(R.string.template_key_words, details.getKeywords()), textViewKeyWords);

    TextView textViewFingerprint = view.findViewById(R.id.textViewFingerprint);
    UIUtil.setHtmlTextToTextView(getString(R.string.template_fingerprint,
        GeneralUtil.doSectionsInText(" ", details.getFingerprint(), 4)), textViewFingerprint);

    TextView textViewLongId = view.findViewById(R.id.textViewLongId);
    textViewLongId.setText(getString(R.string.template_longid, details.getLongId()));

    TextView textViewDate = view.findViewById(R.id.textViewDate);
    textViewDate.setText(getString(R.string.template_date,
        DateFormat.getMediumDateFormat(getContext()).format(new Date(details.getCreated()))));

    TextView textViewUsers = view.findViewById(R.id.textViewUsers);
    textViewUsers.setText(getString(R.string.template_users, TextUtils.join(", ", emails)));

    initButtons(view);
  }

  private void initButtons(View view) {
    if (view.findViewById(R.id.btnShowPubKey) != null) {
      view.findViewById(R.id.btnShowPubKey).setOnClickListener(this);
    }

    if (view.findViewById(R.id.btnCopyToClipboard) != null) {
      view.findViewById(R.id.btnCopyToClipboard).setOnClickListener(this);
    }

    if (view.findViewById(R.id.btnSaveToFile) != null) {
      view.findViewById(R.id.btnSaveToFile).setOnClickListener(this);
    }

    if (view.findViewById(R.id.btnShowPrKey) != null) {
      view.findViewById(R.id.btnShowPrKey).setOnClickListener(this);
    }
  }

  private void chooseDest() {
    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType(Constants.MIME_TYPE_PGP_KEY);
    intent.putExtra(Intent.EXTRA_TITLE, "0x" + details.getLongId());
    startActivityForResult(intent, REQUEST_CODE_GET_URI_FOR_SAVING_KEY);
  }
}
