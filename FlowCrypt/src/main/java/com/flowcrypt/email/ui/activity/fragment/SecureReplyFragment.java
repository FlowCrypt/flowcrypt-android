package com.flowcrypt.email.ui.activity.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.IncomingMessageInfo;
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo;
import com.flowcrypt.email.test.PgpContact;
import com.flowcrypt.email.ui.activity.fragment.base.BaseSendSecurityMessageFragment;
import com.flowcrypt.email.util.UIUtil;

/**
 * This fragment describe a logic of sent an encrypted message as a reply.
 *
 * @author DenBond7
 *         Date: 10.05.2017
 *         Time: 09:11
 *         E-mail: DenBond7@gmail.com
 */
public class SecureReplyFragment extends BaseSendSecurityMessageFragment {

    /**
     * This constant will be used when we create a reply message subject.
     */
    private static final String SUBJECT_PREFIX_RE = "Re: ";

    private TextView textViewReplyRecipient;
    private EditText editTextReplyEmailMessage;

    private IncomingMessageInfo incomingMessageInfo;
    private View layoutContent;
    private View progressBar;

    public SecureReplyFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_security_reply, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
    }

    @Override
    public OutgoingMessageInfo getOutgoingMessageInfo() {
        OutgoingMessageInfo outgoingMessageInfo = new OutgoingMessageInfo();
        outgoingMessageInfo.setMessage(editTextReplyEmailMessage.getText().toString());
        outgoingMessageInfo.setSubject(SUBJECT_PREFIX_RE + incomingMessageInfo.getSubject());
        outgoingMessageInfo.setToPgpContacts(
                new PgpContact[]{new PgpContact(incomingMessageInfo.getFrom().get(0), null)});

        return outgoingMessageInfo;
    }

    @Override
    public View getProgressView() {
        return progressBar;
    }

    @Override
    public View getContentView() {
        return layoutContent;
    }

    @Override
    public boolean isAllInformationCorrect() {
        if (TextUtils.isEmpty(editTextReplyEmailMessage.getText().toString())) {
            UIUtil.showInfoSnackbar(editTextReplyEmailMessage,
                    getString(R.string.text_must_not_be_empty,
                            getString(R.string.prompt_compose_security_email)));
        } else {
            return true;
        }

        return false;
    }

    /**
     * Update an incoming message info and views on the current screen.
     */
    public void setIncomingMessageInfo(IncomingMessageInfo incomingMessageInfo) {
        this.incomingMessageInfo = incomingMessageInfo;
        updateViews();
    }

    /**
     * Update views on the screen. This method can be called when we need to update the current
     * screen.
     */
    private void updateViews() {
        if (incomingMessageInfo != null) {
            textViewReplyRecipient.setText(incomingMessageInfo.getFrom().get(0));
        }
    }

    private void initViews(View view) {
        this.textViewReplyRecipient = (TextView) view.findViewById(R.id.textViewReplyRecipient);
        this.editTextReplyEmailMessage = (EditText) view.findViewById(R.id
                .editTextReplyEmailMessage);
        this.layoutContent = view.findViewById(R.id.layoutForm);
        this.progressBar = view.findViewById(R.id.progressBar);
    }
}
