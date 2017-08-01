/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
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
import com.flowcrypt.email.model.NoPgpFoundDialogAction;
import com.flowcrypt.email.ui.adapter.NoPgpFoundDialogAdapter;
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

public class NoPgpFoundDialogFragment extends BaseDialogFragment
        implements DialogInterface.OnClickListener {
    public static final int RESULT_CODE_SWITCH_TO_STANDARD_EMAIL = 10;
    public static final int RESULT_CODE_IMPORT_THEIR_PUBLIC_KEY = 11;
    public static final int RESULT_CODE_REMOVE_CONTACT = 12;

    public static final String EXTRA_KEY_PGP_CONTACT = GeneralUtil.generateUniqueExtraKey
            ("EXTRA_KEY_PGP_CONTACT", NoPgpFoundDialogFragment.class);

    private PgpContact pgpContact;
    private List<NoPgpFoundDialogAction> noPgpFoundDialogActionList;

    public static NoPgpFoundDialogFragment newInstance(PgpContact pgpContact) {
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_KEY_PGP_CONTACT, pgpContact);
        NoPgpFoundDialogFragment noPgpFoundDialogFragment = new NoPgpFoundDialogFragment();
        noPgpFoundDialogFragment.setArguments(args);
        return noPgpFoundDialogFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            this.pgpContact = getArguments().getParcelable(EXTRA_KEY_PGP_CONTACT);
        }

        noPgpFoundDialogActionList = new ArrayList<>();

        noPgpFoundDialogActionList.add(new NoPgpFoundDialogAction(
                R.mipmap.ic_switch, getString(R.string.switch_to_standard_email),
                RESULT_CODE_SWITCH_TO_STANDARD_EMAIL));
        noPgpFoundDialogActionList.add(new NoPgpFoundDialogAction(
                R.mipmap.ic_document, getString(R.string.import_their_public_key),
                RESULT_CODE_IMPORT_THEIR_PUBLIC_KEY));
        noPgpFoundDialogActionList.add(new NoPgpFoundDialogAction(
                R.mipmap.ic_remove_recipient, getString(R.string.template_remove_recipient,
                pgpContact.getEmail()), RESULT_CODE_REMOVE_CONTACT));
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        NoPgpFoundDialogAdapter noPgpFoundDialogAdapter = new NoPgpFoundDialogAdapter(getContext
                (), noPgpFoundDialogActionList);

        builder.setTitle(R.string.recipient_does_not_use_pgp);
        builder.setAdapter(noPgpFoundDialogAdapter, this);
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        NoPgpFoundDialogAction noPgpFoundDialogAction = noPgpFoundDialogActionList.get(which);
        sendResult(noPgpFoundDialogAction.getId());
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
