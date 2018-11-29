/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;

import com.flowcrypt.email.R;
import com.flowcrypt.email.util.GeneralUtil;

import java.nio.charset.StandardCharsets;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

/**
 * This class can be used to show an info dialog to the user using {@link WebView}.
 *
 * @author Denis Bondarenko
 * Date: 05.02.2018
 * Time: 15:56
 * E-mail: DenBond7@gmail.com
 */

public class WebViewInfoDialogFragment extends DialogFragment implements View.OnClickListener {
  private static final String KEY_INFO_DIALOG_TITLE = GeneralUtil.generateUniqueExtraKey
      ("KEY_INFO_DIALOG_TITLE", WebViewInfoDialogFragment.class);
  private static final String KEY_INFO_DIALOG_MESSAGE = GeneralUtil.generateUniqueExtraKey
      ("KEY_INFO_DIALOG_MESSAGE", WebViewInfoDialogFragment.class);
  private static final String KEY_INFO_IS_CANCELABLE = GeneralUtil.generateUniqueExtraKey
      ("KEY_INFO_IS_CANCELABLE", WebViewInfoDialogFragment.class);

  protected String dialogTitle;
  protected String dialogMessage;

  public WebViewInfoDialogFragment() {
  }

  public static WebViewInfoDialogFragment newInstance(String dialogTitle, String dialogMessage) {
    return newInstance(dialogTitle, dialogMessage, true);
  }

  public static WebViewInfoDialogFragment newInstance(String dialogTitle, String dialogMessage,
                                                      boolean isCancelable) {
    WebViewInfoDialogFragment infoDialogFragment = new WebViewInfoDialogFragment();

    Bundle args = prepareArgs(dialogTitle, dialogMessage, isCancelable);
    infoDialogFragment.setArguments(args);

    return infoDialogFragment;
  }

  @NonNull
  public static Bundle prepareArgs(String dialogTitle, String dialogMessage, boolean isCancelable) {
    Bundle args = new Bundle();
    args.putString(KEY_INFO_DIALOG_TITLE, dialogTitle);
    args.putString(KEY_INFO_DIALOG_MESSAGE, dialogMessage);
    args.putBoolean(KEY_INFO_IS_CANCELABLE, isCancelable);
    return args;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Bundle args = getArguments();

    if (args != null) {
      dialogTitle = args.getString(KEY_INFO_DIALOG_TITLE, getString(R.string.info));
      dialogMessage = args.getString(KEY_INFO_DIALOG_MESSAGE);
      setCancelable(args.getBoolean(KEY_INFO_IS_CANCELABLE, true));
    }
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
    dialog.setTitle(dialogTitle);

    View rootView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_info_in_webview, null);
    rootView.findViewById(R.id.buttonOk).setOnClickListener(this);

    WebView webView = rootView.findViewById(R.id.webView);
    webView.loadDataWithBaseURL(null, dialogMessage, "text/html", StandardCharsets.UTF_8.displayName(), null);
    dialog.setView(rootView);
    return dialog.create();
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.buttonOk:
        dismiss();
        break;
    }
  }
}
