/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FilterQueryProvider;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.model.MessageEncryptionType;
import com.flowcrypt.email.ui.activity.base.BaseSendingMessageActivity;
import com.flowcrypt.email.ui.activity.fragment.base.BaseSendSecurityMessageFragment;
import com.flowcrypt.email.ui.activity.fragment.dialog.NoPgpFoundDialogFragment;
import com.flowcrypt.email.ui.adapter.PgpContactAdapter;
import com.flowcrypt.email.ui.widget.CustomChipSpanChipCreator;
import com.flowcrypt.email.ui.widget.SingleCharacterSpanChipTokenizer;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;
import com.hootsuite.nachos.NachoTextView;
import com.hootsuite.nachos.chip.Chip;
import com.hootsuite.nachos.chip.ChipSpan;
import com.hootsuite.nachos.tokenizer.ChipTokenizer;
import com.hootsuite.nachos.validator.ChipifyingNachoValidator;

import java.util.List;

/**
 * This fragment describe a logic of sent an encrypted or standard message.
 *
 * @author DenBond7
 *         Date: 08.05.2017
 *         Time: 14:44
 *         E-mail: DenBond7@gmail.com
 */
public class ComposeFragment extends BaseSendSecurityMessageFragment implements View
        .OnFocusChangeListener {
    private NachoTextView recipientEditTextView;
    private EditText editTextEmailSubject;
    private EditText editTextEmailMessage;
    private TextInputLayout textInputLayoutEmailMessage;
    private View layoutContent;
    private View progressBarCheckContactsDetails;

    private ContactsDaoSource contactsDaoSource;

    public ComposeFragment() {
        contactsDaoSource = new ContactsDaoSource();
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_NO_PGP_FOUND_DIALOG:
                switch (resultCode) {
                    case NoPgpFoundDialogFragment.RESULT_CODE_REMOVE_CONTACT:
                        if (data != null) {
                            PgpContact pgpContact = data.getParcelableExtra(NoPgpFoundDialogFragment
                                    .EXTRA_KEY_PGP_CONTACT);

                            if (pgpContact != null) {
                                removePgpContactFromRecipientsField(pgpContact);
                            }
                        }

                        break;

                    default:
                        super.onActivityResult(requestCode, resultCode, data);
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_secure_compose, menu);
    }

    @Override
    public View getContentView() {
        return layoutContent;
    }

    @Override
    public OutgoingMessageInfo getOutgoingMessageInfo() {
        OutgoingMessageInfo outgoingMessageInfo = new OutgoingMessageInfo();
        outgoingMessageInfo.setMessage(editTextEmailMessage.getText().toString());
        outgoingMessageInfo.setSubject(editTextEmailSubject.getText().toString());

        List<PgpContact> pgpContacts = contactsDaoSource.getPgpContactsListFromDatabase
                (getContext(), recipientEditTextView.getChipValues());

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
        return recipientEditTextView.getChipAndTokenValues();
    }

    @Override
    public boolean isAllInformationCorrect() {
        if (TextUtils.isEmpty(recipientEditTextView.getText().toString())) {
            showInfoSnackbar(recipientEditTextView, getString(R.string
                            .text_must_not_be_empty,
                    getString(R.string.prompt_recipient)));
            recipientEditTextView.requestFocus();
        } else if (isEmailValid()) {
            if (TextUtils.isEmpty(editTextEmailSubject.getText().toString())) {
                showInfoSnackbar(editTextEmailSubject, getString(R.string
                                .text_must_not_be_empty,
                        getString(R.string.prompt_subject)));
                editTextEmailSubject.requestFocus();
            } else if (TextUtils.isEmpty(editTextEmailMessage.getText().toString())) {
                showInfoSnackbar(editTextEmailMessage, getString(R.string
                                .text_must_not_be_empty,
                        getString(R.string.prompt_compose_security_email)));
                editTextEmailMessage.requestFocus();
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
                                                ComposeFragment.this);
                                    } else {
                                        showInfoSnackbar(getView(), getString(R.string
                                                .internet_connection_is_not_available));
                                    }
                                }
                            });
                } else if (isAllRecipientsHavePGP(true)) {
                    return true;
                }
            } else {
                return true;
            }
        }

        return false;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        switch (v.getId()) {
            case R.id.editTextRecipient:
                progressBarCheckContactsDetails.setVisibility(
                        hasFocus ? View.INVISIBLE : View.VISIBLE);
                if (hasFocus) {
                    pgpContacts.clear();
                    getLoaderManager().destroyLoader(R.id.loader_id_update_info_about_pgp_contacts);
                } else {
                    if (isUpdateInfoAboutContactsEnable) {
                        getLoaderManager().restartLoader(R.id
                                .loader_id_update_info_about_pgp_contacts, null, this);
                    } else {
                        progressBarCheckContactsDetails.setVisibility(View.INVISIBLE);
                    }
                }
                break;
        }
    }

    @Override
    public void handleSuccessLoaderResult(int loaderId, Object result) {
        super.handleSuccessLoaderResult(loaderId, result);
        switch (loaderId) {
            case R.id.loader_id_update_info_about_pgp_contacts:
                if (!pgpContacts.isEmpty()) {
                    updateChips();
                }
                break;
        }
    }

    @Override
    public void onMessageEncryptionTypeChange(MessageEncryptionType messageEncryptionType) {
        String emailMassageHint = null;
        switch (messageEncryptionType) {
            case ENCRYPTED:
                emailMassageHint = getString(R.string.prompt_compose_security_email);
                break;

            case STANDARD:
                emailMassageHint = getString(R.string.prompt_compose_standard_email);
                break;
        }
        textInputLayoutEmailMessage.setHint(emailMassageHint);
    }

    /**
     * Remove the current {@link PgpContact} from recipients.
     *
     * @param deleteCandidatePgpContact The {@link PgpContact} which will be removed.
     */
    private void removePgpContactFromRecipientsField(PgpContact deleteCandidatePgpContact) {
        ChipTokenizer chipTokenizer = recipientEditTextView.getChipTokenizer();
        for (Chip chip : recipientEditTextView.getAllChips()) {
            if (deleteCandidatePgpContact.getEmail().equalsIgnoreCase(chip.getText().toString())
                    && chipTokenizer != null) {
                chipTokenizer.deleteChip(chip, recipientEditTextView.getText());
            }

        }

        for (PgpContact pgpContact : pgpContacts) {
            if (deleteCandidatePgpContact.getEmail().equalsIgnoreCase(pgpContact.getEmail())) {
                pgpContacts.remove(pgpContact);
            }
        }
    }

    private void updateChips() {
        SpannableStringBuilder spannableStringBuilder
                = new SpannableStringBuilder(recipientEditTextView.getText());

        ChipSpan[] chipSpans = spannableStringBuilder.getSpans(0,
                recipientEditTextView.length(), ChipSpan.class);

        if (chipSpans.length > 0) {
            for (PgpContact pgpContact : pgpContacts) {
                for (ChipSpan chipSpan : chipSpans) {
                    if (pgpContact.getEmail().equalsIgnoreCase(chipSpan.getText().toString())) {
                        CustomChipSpanChipCreator.updateChipSpanBackground(getContext(), chipSpan,
                                pgpContact.getHasPgp());
                        break;
                    }
                }
            }
            recipientEditTextView.setText(spannableStringBuilder);
        }
    }

    /**
     * Init fragment views
     *
     * @param view The root fragment view.
     */
    private void initViews(View view) {
        initChipsView(view);

        editTextEmailSubject = (EditText) view.findViewById(R.id.editTextEmailSubject);
        editTextEmailMessage = (EditText) view.findViewById(R.id.editTextEmailMessage);
        textInputLayoutEmailMessage = (TextInputLayout) view.findViewById(R.id
                .textInputLayoutEmailMessage);

        layoutContent = view.findViewById(R.id.scrollView);
        progressBarCheckContactsDetails = view.findViewById(R.id.progressBarCheckContactsDetails);
    }

    private void initChipsView(View view) {
        recipientEditTextView = (NachoTextView) view.findViewById(R.id.editTextRecipient);
        recipientEditTextView.setAdapter(preparePgpContactAdapter());
        recipientEditTextView.setNachoValidator(new ChipifyingNachoValidator());
        recipientEditTextView.setIllegalCharacters(',');
        recipientEditTextView.setChipTokenizer(
                new SingleCharacterSpanChipTokenizer(getContext(),
                        new CustomChipSpanChipCreator(getContext()), ChipSpan.class,
                        SingleCharacterSpanChipTokenizer.CHIP_SEPARATOR_WHITESPACE));
        recipientEditTextView.setOnFocusChangeListener(this);
        recipientEditTextView.setChipBackground(
                ColorStateList.valueOf(UIUtil.getColor(getContext(), R.color.aluminum)));
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
                showInfoSnackbar(recipientEditTextView, getString(R.string
                        .error_some_email_is_not_valid, email));
                recipientEditTextView.requestFocus();
                return false;
            }
        }
        return true;
    }
}
