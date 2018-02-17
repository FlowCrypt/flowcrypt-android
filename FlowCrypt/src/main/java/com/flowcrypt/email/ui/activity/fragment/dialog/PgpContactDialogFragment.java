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
 * This dialog describes actions which can be used for manage some {@link PgpContact}.
 *
 * @author Denis Bondarenko
 *         Date: 12.02.2018
 *         Time: 13:59
 *         E-mail: DenBond7@gmail.com
 */

public class PgpContactDialogFragment extends BaseDialogFragment implements DialogInterface.OnClickListener {
    public static final int RESULT_CODE_COPY_EMAIL = 10;
    public static final int RESULT_CODE_REMOVE_CONTACT = 11;

    public static final String EXTRA_KEY_PGP_CONTACT = GeneralUtil.generateUniqueExtraKey
            ("EXTRA_KEY_PGP_CONTACT", PgpContactDialogFragment.class);

    private PgpContact pgpContact;
    private List<DialogItem> dialogItemList;

    public static PgpContactDialogFragment newInstance(PgpContact pgpContact) {
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_KEY_PGP_CONTACT, pgpContact);
        PgpContactDialogFragment pgpContactDialogFragment = new PgpContactDialogFragment();
        pgpContactDialogFragment.setArguments(args);
        return pgpContactDialogFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            this.pgpContact = getArguments().getParcelable(EXTRA_KEY_PGP_CONTACT);
        }

        dialogItemList = new ArrayList<>();

        dialogItemList.add(new DialogItem(R.mipmap.ic_content_copy,
                getString(R.string.copy), RESULT_CODE_COPY_EMAIL));
        dialogItemList.add(new DialogItem(R.mipmap.ic_delete_grey,
                getString(R.string.remove), RESULT_CODE_REMOVE_CONTACT));
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        DialogItemAdapter dialogItemAdapter = new DialogItemAdapter(getContext(), dialogItemList);

        builder.setTitle(pgpContact.getEmail());
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
