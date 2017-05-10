package com.flowcrypt.email.ui.activity.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo;
import com.flowcrypt.email.test.PgpContact;
import com.flowcrypt.email.ui.activity.fragment.base.BaseSendSecurityMessageFragment;
import com.flowcrypt.email.util.UIUtil;

/**
 * This fragment describe a logic of sent an encrypted message.
 *
 * @author DenBond7
 *         Date: 08.05.2017
 *         Time: 14:44
 *         E-mail: DenBond7@gmail.com
 */
public class SecureComposeFragment extends BaseSendSecurityMessageFragment {

    private EditText editTextRecipient;
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
        outgoingMessageInfo.setToPgpContacts(
                new PgpContact[]{new PgpContact(editTextRecipient.getText().toString(), null)});

        return outgoingMessageInfo;
    }

    @Override
    public boolean isAllInformationCorrect() {
        if (TextUtils.isEmpty(editTextRecipient.getText().toString())) {
            UIUtil.showInfoSnackbar(editTextRecipient, getString(R.string
                            .text_must_not_be_empty,
                    getString(R.string.prompt_recipient)));
        } else if (!isEmailValid()) {
            UIUtil.showInfoSnackbar(editTextRecipient, getString(R.string
                    .error_email_is_not_valid));
        } else if (TextUtils.isEmpty(editTextEmailSubject.getText().toString())) {
            UIUtil.showInfoSnackbar(editTextEmailSubject, getString(R.string
                            .text_must_not_be_empty,
                    getString(R.string.prompt_subject)));
        } else if (TextUtils.isEmpty(editTextEmailMessage.getText().toString())) {
            UIUtil.showInfoSnackbar(editTextEmailMessage, getString(R.string
                            .text_must_not_be_empty,
                    getString(R.string.prompt_compose_security_email)));
        } else {
            return true;
        }

        return false;
    }

    /**
     * Init fragment views
     *
     * @param view The root fragment view.
     */
    private void initViews(View view) {
        editTextRecipient = (EditText) view.findViewById(R.id.editTextRecipient);
        editTextEmailSubject = (EditText) view.findViewById(R.id.editTextEmailSubject);
        editTextEmailMessage = (EditText) view.findViewById(R.id.editTextEmailMessage);

        layoutForm = view.findViewById(R.id.layoutForm);
        progressBar = view.findViewById(R.id.progressBar);
    }

    /**
     * Check is an email valid.
     *
     * @return <tt>boolean</tt> An email validation result.
     */
    private boolean isEmailValid() {
        return js.str_is_email_valid(editTextRecipient.getText().toString());
    }
}
