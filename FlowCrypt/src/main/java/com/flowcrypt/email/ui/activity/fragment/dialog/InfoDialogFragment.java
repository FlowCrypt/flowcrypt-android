/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;

import com.flowcrypt.email.R;
import com.flowcrypt.email.util.GeneralUtil;

/**
 * This class can be used to show an info dialog to the user.
 *
 * @author Denis Bondarenko
 *         Date: 24.07.2017
 *         Time: 17:34
 *         E-mail: DenBond7@gmail.com
 */

public class InfoDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {
    private static final String KEY_INFO_DIALOG_TITLE = GeneralUtil.generateUniqueExtraKey
            ("KEY_INFO_DIALOG_TITLE", InfoDialogFragment.class);
    private static final String KEY_INFO_DIALOG_MESSAGE = GeneralUtil.generateUniqueExtraKey
            ("KEY_INFO_DIALOG_MESSAGE", InfoDialogFragment.class);
    private static final String KEY_INFO_BUTTON_TITLE = GeneralUtil.generateUniqueExtraKey
            ("KEY_INFO_BUTTON_TITLE", InfoDialogFragment.class);
    private static final String KEY_INFO_IS_POP_BACK_STACK = GeneralUtil.generateUniqueExtraKey
            ("KEY_INFO_IS_POP_BACK_STACK", InfoDialogFragment.class);
    private static final String KEY_INFO_IS_CANCELABLE = GeneralUtil.generateUniqueExtraKey
            ("KEY_INFO_IS_CANCELABLE", InfoDialogFragment.class);

    private String dialogTitle;
    private String dialogMessage;
    private String buttonTitle;
    private boolean isPopBackStack;
    private OnInfoDialogButtonClickListener onInfoDialogButtonClickListener;

    public InfoDialogFragment() {
    }

    public static InfoDialogFragment newInstance(String dialogTitle, String dialogMessage) {
        return newInstance(dialogTitle, dialogMessage, null, false, false);
    }

    public static InfoDialogFragment newInstance(String dialogTitle, String dialogMessage,
                                                 boolean isCancelable) {
        return newInstance(dialogTitle, dialogMessage, null, false, isCancelable);
    }

    public static InfoDialogFragment newInstance(String dialogTitle, String dialogMessage,
                                                 String buttonTitle, boolean isCancelable) {
        return newInstance(dialogTitle, dialogMessage, buttonTitle, false, isCancelable);
    }

    public static InfoDialogFragment newInstance(String dialogTitle, String dialogMessage,
                                                 String buttonTitle,
                                                 boolean isPopBackStack, boolean isCancelable) {
        InfoDialogFragment infoDialogFragment = new InfoDialogFragment();

        Bundle args = new Bundle();
        args.putString(KEY_INFO_DIALOG_TITLE, dialogTitle);
        args.putString(KEY_INFO_DIALOG_MESSAGE, dialogMessage);
        args.putString(KEY_INFO_BUTTON_TITLE, buttonTitle);
        args.putBoolean(KEY_INFO_IS_POP_BACK_STACK, isPopBackStack);
        args.putBoolean(KEY_INFO_IS_CANCELABLE, isCancelable);
        infoDialogFragment.setArguments(args);

        return infoDialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();

        if (args != null) {
            dialogTitle = args.getString(KEY_INFO_DIALOG_TITLE, getString(R.string.info));
            dialogMessage = args.getString(KEY_INFO_DIALOG_MESSAGE);
            buttonTitle = args.getString(KEY_INFO_BUTTON_TITLE,
                    getString(android.R.string.ok));
            isPopBackStack = args.getBoolean(KEY_INFO_IS_POP_BACK_STACK, false);
            setCancelable(args.getBoolean(KEY_INFO_IS_CANCELABLE, false));
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
        dialog.setTitle(dialogTitle);
        dialog.setMessage(dialogMessage);
        dialog.setPositiveButton(buttonTitle, this);
        return dialog.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case Dialog.BUTTON_POSITIVE:
                sendResult(Activity.RESULT_OK);

                if (onInfoDialogButtonClickListener != null) {
                    onInfoDialogButtonClickListener.onInfoDialogButtonClick();
                }

                if (isPopBackStack) {
                    FragmentManager fragmentManager = getActivity()
                            .getSupportFragmentManager();
                    fragmentManager.popBackStackImmediate();
                }
                break;
        }
    }

    public void setOnInfoDialogButtonClickListener(OnInfoDialogButtonClickListener
                                                           OnInfoDialogButtonClickListener) {
        this.onInfoDialogButtonClickListener = OnInfoDialogButtonClickListener;
    }

    private void sendResult(int result) {
        if (getTargetFragment() == null) {
            return;
        }

        getTargetFragment().onActivityResult(getTargetRequestCode(), result, null);
    }

    public interface OnInfoDialogButtonClickListener {
        void onInfoDialogButtonClick();
    }
}
