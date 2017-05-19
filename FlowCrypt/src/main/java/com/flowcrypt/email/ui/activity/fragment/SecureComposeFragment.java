package com.flowcrypt.email.ui.activity.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FilterQueryProvider;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.test.PgpContact;
import com.flowcrypt.email.ui.activity.fragment.base.BaseSendSecurityMessageFragment;
import com.flowcrypt.email.ui.adapter.PgpContactAdapter;
import com.flowcrypt.email.util.UIUtil;
import com.hootsuite.nachos.NachoTextView;
import com.hootsuite.nachos.validator.ChipifyingNachoValidator;

import java.util.ArrayList;
import java.util.List;

/**
 * This fragment describe a logic of sent an encrypted message.
 *
 * @author DenBond7
 *         Date: 08.05.2017
 *         Time: 14:44
 *         E-mail: DenBond7@gmail.com
 */
public class SecureComposeFragment extends BaseSendSecurityMessageFragment {

    private NachoTextView recipientEditTextView;
    private EditText editTextEmailSubject;
    private EditText editTextEmailMessage;
    private View progressBar;
    private View layoutForm;

    public SecureComposeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_secure_compose, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
    }

    @Override
    public View getProgressView() {
        return progressBar;
    }

    @Override
    public View getContentView() {
        return layoutForm;
    }

    @Override
    public OutgoingMessageInfo getOutgoingMessageInfo() {
        OutgoingMessageInfo outgoingMessageInfo = new OutgoingMessageInfo();
        outgoingMessageInfo.setMessage(editTextEmailMessage.getText().toString());
        outgoingMessageInfo.setSubject(editTextEmailSubject.getText().toString());

        List<PgpContact> pgpContacts = new ArrayList<>();

        for (String s : recipientEditTextView.getChipValues()) {
            pgpContacts.add(new PgpContact(s, null));
        }

        outgoingMessageInfo.setToPgpContacts(pgpContacts.toArray(new PgpContact[0]));

        return outgoingMessageInfo;
    }

    @Override
    public boolean isAllInformationCorrect() {
        if (TextUtils.isEmpty(recipientEditTextView.getText().toString())) {
            UIUtil.showInfoSnackbar(recipientEditTextView, getString(R.string
                            .text_must_not_be_empty,
                    getString(R.string.prompt_recipient)));
            recipientEditTextView.requestFocus();
        } else if (isEmailValid()) {
            if (TextUtils.isEmpty(editTextEmailSubject.getText().toString())) {
                UIUtil.showInfoSnackbar(editTextEmailSubject, getString(R.string
                                .text_must_not_be_empty,
                        getString(R.string.prompt_subject)));
                editTextEmailSubject.requestFocus();
            } else if (TextUtils.isEmpty(editTextEmailMessage.getText().toString())) {
                UIUtil.showInfoSnackbar(editTextEmailMessage, getString(R.string
                                .text_must_not_be_empty,
                        getString(R.string.prompt_compose_security_email)));
                editTextEmailMessage.requestFocus();
            } else {
                return true;
            }
        }

        return false;
    }

    /**
     * Init fragment views
     *
     * @param view The root fragment view.
     */
    private void initViews(View view) {
        recipientEditTextView = (NachoTextView) view.findViewById(R.id.editTextRecipient);
        recipientEditTextView.setAdapter(preparePgpContactAdapter());
        recipientEditTextView.setNachoValidator(new ChipifyingNachoValidator());

        editTextEmailSubject = (EditText) view.findViewById(R.id.editTextEmailSubject);
        editTextEmailMessage = (EditText) view.findViewById(R.id.editTextEmailMessage);

        layoutForm = view.findViewById(R.id.layoutForm);
        progressBar = view.findViewById(R.id.progressBar);
    }

    /**
     * Prepare a {@link PgpContactAdapter} for the {@link NachoTextView} object.
     *
     * @return <tt>{@link PgpContactAdapter}</tt>
     */
    private PgpContactAdapter preparePgpContactAdapter() {
        PgpContactAdapter pgpContactAdapter = new PgpContactAdapter(getContext(), null, true);
        //setup a search contacts logic in the database
        pgpContactAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                return getContext().getContentResolver().query(
                        new ContactsDaoSource().getBaseContentUri(),
                        null,
                        ContactsDaoSource.COL_EMAIL + " LIKE ?",
                        new String[]{"%" + constraint + "%"},
                        ContactsDaoSource.COL_EMAIL + " ASC");
            }
        });

        return pgpContactAdapter;
    }

    /**
     * Check is an email valid.
     *
     * @return <tt>boolean</tt> An email validation result.
     */
    private boolean isEmailValid() {
        List<String> emails = recipientEditTextView.getChipAndTokenValues();
        for (String email : emails) {
            if (!js.str_is_email_valid(email)) {
                UIUtil.showInfoSnackbar(recipientEditTextView, getString(R.string
                        .error_some_email_is_not_valid, email));
                recipientEditTextView.requestFocus();
                return false;
            }
        }
        return true;
    }
}
