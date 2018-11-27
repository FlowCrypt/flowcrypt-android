/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import com.flowcrypt.email.R;
import com.flowcrypt.email.util.GeneralUtil;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

/**
 * This dialog can be used if we need to show a simple info dialog which has two buttons (negative and positive).
 *
 * @author Denis Bondarenko
 * Date: 28.08.2018
 * Time: 15:28
 * E-mail: DenBond7@gmail.com
 */
public class TwoWayDialogFragment extends DialogFragment {
  private static final String KEY_DIALOG_TITLE = GeneralUtil.generateUniqueExtraKey("KEY_DIALOG_TITLE",
      TwoWayDialogFragment.class);
  private static final String KEY_DIALOG_MESSAGE = GeneralUtil.generateUniqueExtraKey("KEY_DIALOG_MESSAGE",
      TwoWayDialogFragment.class);
  private static final String KEY_POSITIVE_BUTTON_TITLE = GeneralUtil.generateUniqueExtraKey
      ("KEY_POSITIVE_BUTTON_TITLE", TwoWayDialogFragment.class);
  private static final String KEY_NEGATIVE_BUTTON_TITLE = GeneralUtil.generateUniqueExtraKey
      ("KEY_NEGATIVE_BUTTON_TITLE", TwoWayDialogFragment.class);
  private static final String KEY_IS_CANCELABLE = GeneralUtil.generateUniqueExtraKey
      ("KEY_IS_CANCELABLE", TwoWayDialogFragment.class);

  private String dialogTitle;
  private String dialogMessage;
  private String positiveButtonTitle;
  private String negativeButtonTitle;
  private OnTwoWayDialogListener onTwoWayDialogListener;

  public TwoWayDialogFragment() {
  }

  public static TwoWayDialogFragment newInstance(String dialogTitle, String dialogMessage) {
    return newInstance(dialogTitle, dialogMessage, false);
  }

  public static TwoWayDialogFragment newInstance(String dialogTitle, String dialogMessage, boolean isCancelable) {
    return newInstance(dialogTitle, dialogMessage, null, null, isCancelable);
  }

  public static TwoWayDialogFragment newInstance(String dialogTitle, String dialogMessage, String positiveButtonTitle,
                                                 String negativeButtonTitle, boolean isCancelable) {
    Bundle args = new Bundle();
    args.putString(KEY_DIALOG_TITLE, dialogTitle);
    args.putString(KEY_DIALOG_MESSAGE, dialogMessage);
    args.putString(KEY_POSITIVE_BUTTON_TITLE, positiveButtonTitle);
    args.putString(KEY_NEGATIVE_BUTTON_TITLE, negativeButtonTitle);
    args.putBoolean(KEY_IS_CANCELABLE, isCancelable);
    TwoWayDialogFragment infoDialogFragment = new TwoWayDialogFragment();
    infoDialogFragment.setArguments(args);

    return infoDialogFragment;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    if (context instanceof OnTwoWayDialogListener) {
      this.onTwoWayDialogListener = (OnTwoWayDialogListener) context;
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Bundle args = getArguments();

    if (args != null) {
      dialogTitle = args.getString(KEY_DIALOG_TITLE);
      dialogMessage = args.getString(KEY_DIALOG_MESSAGE);
      positiveButtonTitle = args.getString(KEY_POSITIVE_BUTTON_TITLE, getString(R.string.yes));
      negativeButtonTitle = args.getString(KEY_NEGATIVE_BUTTON_TITLE, getString(R.string.no));
      setCancelable(args.getBoolean(KEY_IS_CANCELABLE, false));
    }
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());

    if (dialogTitle != null) {
      dialog.setTitle(dialogTitle);
    } else {
      dialog.setTitle(R.string.info);
    }
    dialog.setMessage(dialogMessage);

    dialog.setPositiveButton(positiveButtonTitle,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            sendResult(Activity.RESULT_OK);

            if (onTwoWayDialogListener != null) {
              onTwoWayDialogListener.onTwoWayDialogButtonClick(Activity.RESULT_OK);
            }
          }
        }
    );

    dialog.setNegativeButton(negativeButtonTitle,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            sendResult(Activity.RESULT_CANCELED);

            if (onTwoWayDialogListener != null) {
              onTwoWayDialogListener.onTwoWayDialogButtonClick(Activity.RESULT_CANCELED);
            }
          }
        }
    );

    return dialog.create();
  }

  private void sendResult(int result) {
    if (getTargetFragment() == null) {
      return;
    }
    getTargetFragment().onActivityResult(getTargetRequestCode(), result, null);
  }

  /**
   * This interface can be used by an activity to receive results from the dialog when a user choose some button.
   */
  public interface OnTwoWayDialogListener {
    /**
     * @param result Can be {@link Activity#RESULT_OK} if the user clicks the positive button, or
     *               {@link Activity#RESULT_CANCELED} if the user clicks the negative button.
     */
    void onTwoWayDialogButtonClick(int result);
  }
}
