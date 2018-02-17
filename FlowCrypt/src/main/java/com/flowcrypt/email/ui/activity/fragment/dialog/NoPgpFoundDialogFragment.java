/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.flowcrypt.email.R;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.model.DialogItem;
import com.flowcrypt.email.ui.adapter.DialogItemAdapter;
import com.flowcrypt.email.util.GeneralUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * This dialog will be used to show for user different options to resolve a PGP not found situation.
 *
 * @author Denis Bondarenko
 *         Date: 01.08.2017
 *         Time: 10:04
 *         E-mail: DenBond7@gmail.com
 */

public class NoPgpFoundDialogFragment extends BaseDialogFragment implements DialogInterface.OnClickListener {
    public static final int RESULT_CODE_SWITCH_TO_STANDARD_EMAIL = 10;
    public static final int RESULT_CODE_IMPORT_THEIR_PUBLIC_KEY = 11;
    public static final int RESULT_CODE_COPY_FROM_OTHER_CONTACT = 12;
    public static final int RESULT_CODE_REMOVE_CONTACT = 13;

    public static final String EXTRA_KEY_PGP_CONTACT = GeneralUtil.generateUniqueExtraKey
            ("EXTRA_KEY_PGP_CONTACT", NoPgpFoundDialogFragment.class);

    private static final String EXTRA_KEY_IS_SHOW_REMOVE_ACTION = GeneralUtil.generateUniqueExtraKey
            ("EXTRA_KEY_IS_SHOW_REMOVE_ACTION", NoPgpFoundDialogFragment.class);

    private PgpContact pgpContact;
    private List<DialogItem> dialogItemList;
    private boolean isShowRemoveAction;

    public static NoPgpFoundDialogFragment newInstance(PgpContact pgpContact, boolean isShowRemoveAction) {
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_KEY_PGP_CONTACT, pgpContact);
        args.putBoolean(EXTRA_KEY_IS_SHOW_REMOVE_ACTION, isShowRemoveAction);
        NoPgpFoundDialogFragment noPgpFoundDialogFragment = new NoPgpFoundDialogFragment();
        noPgpFoundDialogFragment.setArguments(args);
        return noPgpFoundDialogFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            this.pgpContact = getArguments().getParcelable(EXTRA_KEY_PGP_CONTACT);
            this.isShowRemoveAction = getArguments().getBoolean(EXTRA_KEY_IS_SHOW_REMOVE_ACTION);
        }

        dialogItemList = new ArrayList<>();

        dialogItemList.add(new DialogItem(R.mipmap.ic_switch, getString(R.string.switch_to_standard_email),
                RESULT_CODE_SWITCH_TO_STANDARD_EMAIL));
        dialogItemList.add(new DialogItem(R.mipmap.ic_document, getString(R.string.import_their_public_key),
                RESULT_CODE_IMPORT_THEIR_PUBLIC_KEY));
        dialogItemList.add(new DialogItem(R.mipmap.ic_content_copy, getString(R.string.copy_from_other_contact),
                RESULT_CODE_COPY_FROM_OTHER_CONTACT));
        if (isShowRemoveAction) {
            dialogItemList.add(new DialogItem(
                    R.mipmap.ic_remove_recipient, getString(R.string.template_remove_recipient,
                    pgpContact.getEmail()), RESULT_CODE_REMOVE_CONTACT));
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        DialogItemAdapter dialogItemAdapter = new DialogItemAdapter(getContext(), dialogItemList);

        builder.setTitle(R.string.recipient_does_not_use_pgp);
        builder.setAdapter(dialogItemAdapter, this);
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        DialogItem dialogItem = dialogItemList.get(which);
        sendResult(dialogItem.getId());
    }

    private void sendResult(int result) {
        if (getTargetFragment() == null) {
            return;
        }

        Intent returnIntent = new Intent();
        returnIntent.putExtra(EXTRA_KEY_PGP_CONTACT, pgpContact);
        getTargetFragment().onActivityResult(getTargetRequestCode(), result, returnIntent);
    }
}
