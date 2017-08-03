/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.IncomingMessageInfo;
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.model.MessageEncryptionType;
import com.flowcrypt.email.ui.activity.SecureReplyActivity;
import com.flowcrypt.email.ui.activity.base.BaseSendingMessageActivity;
import com.flowcrypt.email.ui.activity.fragment.base.BaseSendSecurityMessageFragment;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

import java.util.List;

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
    private View progressBarCheckContactsDetails;

    public SecureReplyFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity().getIntent() != null) {
            this.incomingMessageInfo = getActivity().getIntent().getParcelableExtra
                    (SecureReplyActivity.EXTRA_KEY_INCOMING_MESSAGE_INFO);
        }
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

        if (incomingMessageInfo != null) {
            updateViews();
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (GeneralUtil.isInternetConnectionAvailable(getContext())) {
            getLoaderManager().restartLoader(R.id
                    .loader_id_update_info_about_pgp_contacts, null, this);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_secure_reply, menu);
    }

    @Override
    public OutgoingMessageInfo getOutgoingMessageInfo() {
        OutgoingMessageInfo outgoingMessageInfo = new OutgoingMessageInfo();
        outgoingMessageInfo.setMessage(editTextReplyEmailMessage.getText().toString());
        outgoingMessageInfo.setSubject(SUBJECT_PREFIX_RE + incomingMessageInfo.getSubject());
        outgoingMessageInfo.setRawReplyMessage(
                incomingMessageInfo.getOriginalRawMessageWithoutAttachments());
        List<PgpContact> pgpContacts = new ContactsDaoSource().getPgpContactsListFromDatabase
                (getContext(), incomingMessageInfo.getFrom());

        outgoingMessageInfo.setToPgpContacts(pgpContacts.toArray(new PgpContact[0]));

        if (getActivity() instanceof BaseSendingMessageActivity) {
            BaseSendingMessageActivity baseSendingMessageActivity = (BaseSendingMessageActivity)
                    getActivity();
            outgoingMessageInfo.setFromPgpContact(new PgpContact(baseSendingMessageActivity
                    .getSenderEmail(), null));
        }

        return outgoingMessageInfo;
    }

    @Override
    public View getUpdateInfoAboutContactsProgressBar() {
        return progressBarCheckContactsDetails;
    }

    @Override
    public List<String> getContactsEmails() {
        return incomingMessageInfo.getFrom();
    }

    @Override
    public View getContentView() {
        return layoutContent;
    }

    @Override
    public boolean isAllInformationCorrect() {
        if (TextUtils.isEmpty(editTextReplyEmailMessage.getText().toString())) {
            UIUtil.showInfoSnackbar(editTextReplyEmailMessage,
                    getString(R.string.sending_message_must_not_be_empty));
        } else if (onChangeMessageEncryptedTypeListener.getMessageEncryptionType() ==
                MessageEncryptionType.ENCRYPTED) {
            if (pgpContacts.isEmpty()) {
                showSnackbar(getView(),
                        getString(R.string.please_update_information_about_contacts),
                        getString(R.string.update), Snackbar.LENGTH_LONG,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (GeneralUtil.isInternetConnectionAvailable(getContext())) {
                                    getLoaderManager().restartLoader(
                                            R.id.loader_id_update_info_about_pgp_contacts, null,
                                            SecureReplyFragment.this);
                                } else {
                                    showInfoSnackbar(getView(), getString(R.string
                                            .internet_connection_is_not_available));
                                }
                            }
                        });
            } else if (isAllRecipientsHavePGP(false)) {
                return true;
            }
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
        this.progressBarCheckContactsDetails = view.findViewById(R.id
                .progressBarCheckContactsDetails);
    }
}
