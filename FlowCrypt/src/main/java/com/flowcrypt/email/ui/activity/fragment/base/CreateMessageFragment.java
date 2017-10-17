/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.base;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.Loader;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.TextView;
import android.widget.Toast;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.FoldersManager;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.api.email.model.IncomingMessageInfo;
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.model.MessageEncryptionType;
import com.flowcrypt.email.model.UpdateInfoAboutPgpContactsResult;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.ui.activity.CreateMessageActivity;
import com.flowcrypt.email.ui.activity.ImportPublicKeyActivity;
import com.flowcrypt.email.ui.activity.fragment.dialog.NoPgpFoundDialogFragment;
import com.flowcrypt.email.ui.activity.listeners.OnChangeMessageEncryptedTypeListener;
import com.flowcrypt.email.ui.adapter.PgpContactAdapter;
import com.flowcrypt.email.ui.loader.UpdateInfoAboutPgpContactsAsyncTaskLoader;
import com.flowcrypt.email.ui.widget.CustomChipSpanChipCreator;
import com.flowcrypt.email.ui.widget.PGPContactChipSpan;
import com.flowcrypt.email.ui.widget.SingleCharacterSpanChipTokenizer;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;
import com.hootsuite.nachos.NachoTextView;
import com.hootsuite.nachos.chip.Chip;
import com.hootsuite.nachos.tokenizer.ChipTokenizer;
import com.hootsuite.nachos.validator.ChipifyingNachoValidator;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This fragment describe a logic of sent an encrypted or standard message.
 *
 * @author DenBond7
 *         Date: 10.05.2017
 *         Time: 11:27
 *         E-mail: DenBond7@gmail.com
 */

public class CreateMessageFragment extends BaseGmailFragment implements View.OnFocusChangeListener {
    private static final int REQUEST_CODE_NO_PGP_FOUND_DIALOG = 100;
    private static final int REQUEST_CODE_IMPORT_PUBLIC_KEY = 101;
    private static final int REQUEST_CODE_GET_CONTENT_FOR_SENDING = 102;

    private Js js;
    private OnMessageSendListener onMessageSendListener;
    private OnChangeMessageEncryptedTypeListener onChangeMessageEncryptedTypeListener;
    private List<PgpContact> pgpContacts;
    private ArrayList<AttachmentInfo> attachmentInfoList;
    private NachoTextView editTextRecipients;
    private ContactsDaoSource contactsDaoSource;
    private FoldersManager.FolderType folderType;
    private IncomingMessageInfo incomingMessageInfo;

    private ViewGroup layoutAttachments;
    private EditText editTextEmailSubject;
    private EditText editTextEmailMessage;
    private TextInputLayout textInputLayoutEmailMessage;
    private View layoutContent;
    private View progressBarCheckContactsDetails;

    private boolean isUpdateInfoAboutContactsEnable = true;
    private boolean isUpdatedInfoAboutContactCompleted = true;
    private boolean isMessageSendingNow;
    private boolean isIncomingMessageInfoUsed;

    public CreateMessageFragment() {
        pgpContacts = new ArrayList<>();
        attachmentInfoList = new ArrayList<>();
        contactsDaoSource = new ContactsDaoSource();
    }

    @Override
    public View getContentView() {
        return layoutContent;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnMessageSendListener) {
            this.onMessageSendListener = (OnMessageSendListener) context;
        } else throw new IllegalArgumentException(context.toString() + " must implement " +
                OnMessageSendListener.class.getSimpleName());

        if (context instanceof OnChangeMessageEncryptedTypeListener) {
            this.onChangeMessageEncryptedTypeListener = (OnChangeMessageEncryptedTypeListener) context;
        } else throw new IllegalArgumentException(context.toString() + " must implement " +
                OnChangeMessageEncryptedTypeListener.class.getSimpleName());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        try {
            js = new Js(getContext(), null);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (getActivity().getIntent() != null) {
            this.incomingMessageInfo = getActivity().getIntent().getParcelableExtra
                    (CreateMessageActivity.EXTRA_KEY_INCOMING_MESSAGE_INFO);
            if (incomingMessageInfo != null && incomingMessageInfo.getFolder() != null) {
                this.folderType = FoldersManager.getFolderTypeForImapFodler(
                        incomingMessageInfo.getFolder().getAttributes());
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_message, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        showAttachments();

        if (incomingMessageInfo != null && !isIncomingMessageInfoUsed) {
            this.isIncomingMessageInfoUsed = true;
            updateViews();
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (incomingMessageInfo != null && GeneralUtil.isInternetConnectionAvailable(getContext())
                && onChangeMessageEncryptedTypeListener.getMessageEncryptionType() == MessageEncryptionType.ENCRYPTED) {
            getLoaderManager().restartLoader(R.id.loader_id_update_info_about_pgp_contacts, null, this);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_NO_PGP_FOUND_DIALOG:
                switch (resultCode) {
                    case NoPgpFoundDialogFragment.RESULT_CODE_SWITCH_TO_STANDARD_EMAIL:
                        onChangeMessageEncryptedTypeListener.onMessageEncryptionTypeChange(MessageEncryptionType
                                .STANDARD);
                        break;

                    case NoPgpFoundDialogFragment.RESULT_CODE_IMPORT_THEIR_PUBLIC_KEY:
                        if (data != null) {
                            PgpContact pgpContact = data.getParcelableExtra(NoPgpFoundDialogFragment
                                    .EXTRA_KEY_PGP_CONTACT);

                            if (pgpContact != null) {
                                startActivityForResult(
                                        ImportPublicKeyActivity.newIntent(getContext(),
                                                getString(R.string.import_public_key), pgpContact),

                                        REQUEST_CODE_IMPORT_PUBLIC_KEY);
                            }
                        }

                        break;

                    case NoPgpFoundDialogFragment.RESULT_CODE_REMOVE_CONTACT:
                        if (data != null) {
                            PgpContact pgpContact = data.getParcelableExtra(NoPgpFoundDialogFragment
                                    .EXTRA_KEY_PGP_CONTACT);

                            if (pgpContact != null) {
                                removePgpContactFromRecipientsField(pgpContact);
                            }
                        }

                        break;
                }
                break;

            case REQUEST_CODE_IMPORT_PUBLIC_KEY:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Toast.makeText(getContext(), R.string.key_successfully_imported, Toast.LENGTH_SHORT).show();

                        getLoaderManager().restartLoader(R.id.loader_id_update_info_about_pgp_contacts, null,
                                CreateMessageFragment.this);
                        break;
                }
                break;

            case REQUEST_CODE_GET_CONTENT_FOR_SENDING:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        if (data != null && data.getData() != null) {
                            AttachmentInfo attachmentInfo = getAttachmentInfoFromUri(data.getData());
                            if (isAttachmentCanBeAdded(attachmentInfo)) {
                                attachmentInfoList.add(attachmentInfo);
                                showAttachments();
                            } else {
                                showInfoSnackbar(getView(),
                                        getString(R.string.template_warning_max_total_attachments_size,
                                                FileUtils.byteCountToDisplaySize(
                                                        Constants.MAX_TOTAL_ATTACHMENT_SIZE_IN_BYTES)),
                                        Snackbar.LENGTH_LONG);
                            }
                        } else {
                            showInfoSnackbar(getView(), getString(R.string.can_not_attach_this_file),
                                    Snackbar.LENGTH_LONG);
                        }
                        break;
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
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.setGroupVisible(0, !isMessageSendingNow);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuActionSend:
                if (getSnackBar() != null) {
                    getSnackBar().dismiss();
                }

                if (isUpdatedInfoAboutContactCompleted) {
                    UIUtil.hideSoftInput(getContext(), getView());
                    if (GeneralUtil.isInternetConnectionAvailable(getContext())) {
                        if (isAllInformationCorrect()) {
                            sendMessage();
                        }
                    } else {
                        UIUtil.showInfoSnackbar(getView(), getString(R.string
                                .internet_connection_is_not_available));
                    }
                } else {
                    Toast.makeText(getContext(), R.string.please_wait_while_information_about_contacts_will_be_updated,
                            Toast.LENGTH_SHORT).show();
                }
                return true;

            case R.id.menuActionAttachFile:
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(Intent.createChooser(intent, getString(R.string.choose_attachment)),
                        REQUEST_CODE_GET_CONTENT_FOR_SENDING);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_update_info_about_pgp_contacts:
                pgpContacts.clear();
                progressBarCheckContactsDetails.setVisibility(View.VISIBLE);
                isUpdatedInfoAboutContactCompleted = false;
                return new UpdateInfoAboutPgpContactsAsyncTaskLoader(getContext(),
                        editTextRecipients.getChipAndTokenValues());

            default:
                return null;
        }
    }

    @Override
    public void handleSuccessLoaderResult(int loaderId, Object result) {
        switch (loaderId) {
            case R.id.loader_id_update_info_about_pgp_contacts:
                UpdateInfoAboutPgpContactsResult updateInfoAboutPgpContactsResult
                        = (UpdateInfoAboutPgpContactsResult) result;

                isUpdatedInfoAboutContactCompleted = true;
                progressBarCheckContactsDetails.setVisibility(View.INVISIBLE);

                if (updateInfoAboutPgpContactsResult != null
                        && updateInfoAboutPgpContactsResult.getUpdatedPgpContacts() != null) {
                    pgpContacts = updateInfoAboutPgpContactsResult.getUpdatedPgpContacts();
                }

                if (updateInfoAboutPgpContactsResult == null || !updateInfoAboutPgpContactsResult.isAllInfoReceived()) {
                    Toast.makeText(getContext(), R.string.info_about_some_contacts_not_received,
                            Toast.LENGTH_SHORT).show();
                }

                if (!pgpContacts.isEmpty()) {
                    updateChips();
                }
                break;

            default:
                super.handleSuccessLoaderResult(loaderId, result);
        }
    }

    @Override
    public void handleFailureLoaderResult(int loaderId, Exception e) {
        super.handleFailureLoaderResult(loaderId, e);
        switch (loaderId) {
            case R.id.loader_id_update_info_about_pgp_contacts:
                isUpdatedInfoAboutContactCompleted = true;
                progressBarCheckContactsDetails.setVisibility(View.INVISIBLE);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<LoaderResult> loader) {
        super.onLoaderReset(loader);
    }

    @Override
    public void onErrorOccurred(int requestCode, int errorType, Exception e) {
        notifyUserAboutErrorWhenSendMessage();
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        switch (v.getId()) {
            case R.id.editTextRecipient:
                progressBarCheckContactsDetails.setVisibility(hasFocus ? View.INVISIBLE : View.VISIBLE);
                if (hasFocus) {
                    pgpContacts.clear();
                    getLoaderManager().destroyLoader(R.id.loader_id_update_info_about_pgp_contacts);
                } else {
                    if (isUpdateInfoAboutContactsEnable) {
                        getLoaderManager().restartLoader(R.id.loader_id_update_info_about_pgp_contacts, null, this);
                    } else {
                        progressBarCheckContactsDetails.setVisibility(View.INVISIBLE);
                    }
                }
                break;
        }
    }

    public void onMessageEncryptionTypeChange(MessageEncryptionType messageEncryptionType) {
        String emailMassageHint = null;
        if (messageEncryptionType != null) {
            switch (messageEncryptionType) {
                case ENCRYPTED:
                    emailMassageHint = getString(R.string.prompt_compose_security_email);
                    break;

                case STANDARD:
                    emailMassageHint = getString(R.string.prompt_compose_standard_email);
                    break;
            }
        }
        textInputLayoutEmailMessage.setHint(emailMassageHint);
    }

    /**
     * Notify the user about an error which occurred when we send a message.
     */
    public void notifyUserAboutErrorWhenSendMessage() {
        isMessageSendingNow = false;
        getActivity().invalidateOptionsMenu();
        UIUtil.exchangeViewVisibility(getContext(), false, progressView, getContentView());
        showInfoSnackbar(getView(), getString(R.string.error_occurred_while_sending_message));
    }

    /**
     * Check the message sending status
     *
     * @return true if message was sent, false otherwise.
     */
    public boolean isMessageSendingNow() {
        return isMessageSendingNow;
    }

    /**
     * Generate an outgoing message info from entered information by user.
     *
     * @return <tt>OutgoingMessageInfo</tt> Return a created OutgoingMessageInfo object which
     * contains information about an outgoing message.
     */
    private OutgoingMessageInfo getOutgoingMessageInfo() {
        OutgoingMessageInfo outgoingMessageInfo = new OutgoingMessageInfo();
        outgoingMessageInfo.setMessage(editTextEmailMessage.getText().toString());
        outgoingMessageInfo.setSubject(editTextEmailSubject.getText().toString());

        List<PgpContact> pgpContacts = new ArrayList<>();
        if (incomingMessageInfo != null) {
            outgoingMessageInfo.setRawReplyMessage(
                    incomingMessageInfo.getOriginalRawMessageWithoutAttachments());

        }

        if (onChangeMessageEncryptedTypeListener.getMessageEncryptionType() == MessageEncryptionType.ENCRYPTED) {
            pgpContacts = contactsDaoSource.getPgpContactsListFromDatabase(getContext(),
                    editTextRecipients.getChipValues());
        } else {
            List<String> contacts = editTextRecipients.getChipValues();

            for (String s : contacts) {
                pgpContacts.add(new PgpContact(s, null));
            }
        }

        outgoingMessageInfo.setToPgpContacts(pgpContacts.toArray(new PgpContact[0]));

        if (getActivity() instanceof CreateMessageActivity) {
            CreateMessageActivity createMessageActivity = (CreateMessageActivity) getActivity();
            outgoingMessageInfo.setFromPgpContact(new PgpContact(createMessageActivity.getSenderEmail(), null));
        }

        return outgoingMessageInfo;
    }

    /**
     * Check that all recipients have PGP.
     *
     * @return true if all recipients have PGP, other wise false.
     */
    private boolean isAllRecipientsHavePGP(boolean isShowRemoveAction) {
        for (PgpContact pgpContact : pgpContacts) {
            if (!pgpContact.getHasPgp()) {
                showNoPgpFoundDialog(pgpContact, isShowRemoveAction);
                return false;
            }
        }

        return true;
    }

    /**
     * This method does update chips in the recipients field.
     */
    private void updateChips() {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(editTextRecipients.getText());

        PGPContactChipSpan[] pgpContactChipSpans = spannableStringBuilder.getSpans(0, editTextRecipients.length(),
                PGPContactChipSpan.class);

        if (pgpContactChipSpans.length > 0) {
            for (PgpContact pgpContact : pgpContacts) {
                for (PGPContactChipSpan pgpContactChipSpan : pgpContactChipSpans) {
                    if (pgpContact.getEmail().equalsIgnoreCase(pgpContactChipSpan.getText().toString())) {
                        pgpContactChipSpan.setHasPgp(pgpContact.getHasPgp());
                        break;
                    }
                }
            }
            editTextRecipients.invalidateChips();
        }
    }

    private void initChipsView(View view) {
        editTextRecipients = (NachoTextView) view.findViewById(R.id.editTextRecipient);
        editTextRecipients.setNachoValidator(new ChipifyingNachoValidator());
        editTextRecipients.setIllegalCharacters(',');
        editTextRecipients.setChipTokenizer(new SingleCharacterSpanChipTokenizer(getContext(),
                new CustomChipSpanChipCreator(getContext()), PGPContactChipSpan.class,
                SingleCharacterSpanChipTokenizer.CHIP_SEPARATOR_WHITESPACE));
        editTextRecipients.setAdapter(preparePgpContactAdapter());
        editTextRecipients.setOnFocusChangeListener(this);
    }

    /**
     * Do a lot of checks to validate an outgoing message info.
     *
     * @return <tt>Boolean</tt> true if all information is correct, false otherwise.
     */
    private boolean isAllInformationCorrect() {
        if (TextUtils.isEmpty(editTextRecipients.getText().toString())) {
            showInfoSnackbar(editTextRecipients, getString(R.string.text_must_not_be_empty,
                    getString(R.string.prompt_recipient)));
            editTextRecipients.requestFocus();
        } else if (isEmailValid()) {
            if (TextUtils.isEmpty(editTextEmailSubject.getText().toString())) {
                showInfoSnackbar(editTextEmailSubject, getString(R.string.text_must_not_be_empty,
                        getString(R.string.prompt_subject)));
                editTextEmailSubject.requestFocus();
            } else if (TextUtils.isEmpty(editTextEmailMessage.getText().toString())) {
                showInfoSnackbar(editTextEmailMessage, getString(R.string.sending_message_must_not_be_empty));
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
                                                CreateMessageFragment.this);
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

    /**
     * Remove the current {@link PgpContact} from recipients.
     *
     * @param deleteCandidatePgpContact The {@link PgpContact} which will be removed.
     */
    private void removePgpContactFromRecipientsField(PgpContact deleteCandidatePgpContact) {
        ChipTokenizer chipTokenizer = editTextRecipients.getChipTokenizer();
        for (Chip chip : editTextRecipients.getAllChips()) {
            if (deleteCandidatePgpContact.getEmail().equalsIgnoreCase(chip.getText().toString())
                    && chipTokenizer != null) {
                chipTokenizer.deleteChip(chip, editTextRecipients.getText());
            }

        }

        for (PgpContact pgpContact : pgpContacts) {
            if (deleteCandidatePgpContact.getEmail().equalsIgnoreCase(pgpContact.getEmail())) {
                pgpContacts.remove(pgpContact);
            }
        }
    }

    /**
     * Init fragment views
     *
     * @param view The root fragment view.
     */
    private void initViews(View view) {
        layoutAttachments = (ViewGroup) view.findViewById(R.id.layoutAttachments);
        initChipsView(view);

        editTextEmailSubject = (EditText) view.findViewById(R.id.editTextEmailSubject);
        editTextEmailMessage = (EditText) view.findViewById(R.id.editTextEmailMessage);
        textInputLayoutEmailMessage = (TextInputLayout) view.findViewById(R.id.textInputLayoutEmailMessage);

        layoutContent = view.findViewById(R.id.scrollView);
        progressBarCheckContactsDetails = view.findViewById(R.id.progressBarCheckContactsDetails);
    }

    /**
     * Update views on the screen. This method can be called when we need to update the current
     * screen.
     */
    private void updateViews() {
        onMessageEncryptionTypeChange(onChangeMessageEncryptedTypeListener.getMessageEncryptionType());

        if (incomingMessageInfo != null) {
            if (FoldersManager.FolderType.SENT == folderType) {
                editTextRecipients.setText(prepareRecipients(incomingMessageInfo.getTo()));
            } else {
                editTextRecipients.setText(prepareRecipients(incomingMessageInfo.getFrom()));
            }
            editTextRecipients.chipifyAllUnterminatedTokens();
            editTextEmailSubject.setText(getString(R.string.template_reply_subject, incomingMessageInfo.getSubject()));
            editTextEmailMessage.requestFocus();
        }
    }

    private String prepareRecipients(List<String> recipients) {
        String result = "";
        for (String s : recipients) {
            result += s + " ";
        }

        return result;
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
        List<String> emails = editTextRecipients.getChipAndTokenValues();
        for (String email : emails) {
            if (!js.str_is_email_valid(email)) {
                showInfoSnackbar(editTextRecipients, getString(R.string.error_some_email_is_not_valid, email));
                editTextRecipients.requestFocus();
                return false;
            }
        }
        return true;
    }

    /**
     * Check is attachment can be added to the current message.
     *
     * @param newAttachmentInfo The new attachment which will be maybe added.
     * @return true if the attachment can be added, otherwise false.
     */
    private boolean isAttachmentCanBeAdded(AttachmentInfo newAttachmentInfo) {
        int totalSizeOfAttachments = 0;

        for (AttachmentInfo attachmentInfo : attachmentInfoList) {
            totalSizeOfAttachments += attachmentInfo.getEncodedSize();
        }

        totalSizeOfAttachments += newAttachmentInfo.getEncodedSize();

        return totalSizeOfAttachments < Constants.MAX_TOTAL_ATTACHMENT_SIZE_IN_BYTES;
    }

    /**
     * Generate {@link AttachmentInfo} from the requested information from the file uri.
     *
     * @param uri The file {@link Uri}
     * @return Generated {@link AttachmentInfo}.
     */
    private AttachmentInfo getAttachmentInfoFromUri(Uri uri) {
        AttachmentInfo attachmentInfo = new AttachmentInfo();
        Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                attachmentInfo.setName(cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)));
                attachmentInfo.setEncodedSize(cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE)));
                attachmentInfo.setType(getContext().getContentResolver().getType(uri));
                attachmentInfo.setUri(uri);
            }

            cursor.close();
        }

        return attachmentInfo;
    }

    /**
     * Show a dialog where we can select different actions.
     *
     * @param pgpContact         The {@link PgpContact} which will be used when we select the
     *                           remove action.
     * @param isShowRemoveAction true if we want to show the remove action, false otherwise.
     */
    private void showNoPgpFoundDialog(PgpContact pgpContact, boolean isShowRemoveAction) {
        NoPgpFoundDialogFragment noPgpFoundDialogFragment =
                NoPgpFoundDialogFragment.newInstance(pgpContact, isShowRemoveAction);

        noPgpFoundDialogFragment.setTargetFragment(this, REQUEST_CODE_NO_PGP_FOUND_DIALOG);
        noPgpFoundDialogFragment.show(getFragmentManager(), NoPgpFoundDialogFragment.class.getSimpleName());
    }

    /**
     * Send a message.
     */
    private void sendMessage() {
        dismissCurrentSnackBar();

        isUpdateInfoAboutContactsEnable = false;
        isMessageSendingNow = true;

        getActivity().invalidateOptionsMenu();

        statusView.setVisibility(View.GONE);
        UIUtil.exchangeViewVisibility(getContext(), true, progressView, getContentView());

        OutgoingMessageInfo outgoingMessageInfo = getOutgoingMessageInfo();
        outgoingMessageInfo.setAttachmentInfoArrayList(attachmentInfoList);
        outgoingMessageInfo.setMessageEncryptionType(onChangeMessageEncryptedTypeListener.getMessageEncryptionType());

        if (onMessageSendListener != null) {
            onMessageSendListener.sendMessage(outgoingMessageInfo);
        }
    }

    /**
     * Show attachments which were added.
     */
    private void showAttachments() {
        if (!attachmentInfoList.isEmpty()) {
            layoutAttachments.removeAllViews();
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            for (final AttachmentInfo attachmentInfo : attachmentInfoList) {
                final View rootView = layoutInflater.inflate(R.layout.attachment_item, layoutAttachments, false);

                TextView textViewAttachmentName = (TextView) rootView.findViewById(R.id.textViewAttchmentName);
                textViewAttachmentName.setText(attachmentInfo.getName());

                TextView textViewAttachmentSize = (TextView) rootView.findViewById(R.id.textViewAttachmentSize);
                textViewAttachmentSize.setText(Formatter.formatFileSize(getContext(), attachmentInfo.getEncodedSize()));

                View imageButtonDownloadAttachment = rootView.findViewById(R.id.imageButtonDownloadAttachment);
                imageButtonDownloadAttachment.setVisibility(View.GONE);

                View imageButtonClearAttachment = rootView.findViewById(R.id.imageButtonClearAttachment);
                imageButtonClearAttachment.setVisibility(View.VISIBLE);
                imageButtonClearAttachment.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        attachmentInfoList.remove(attachmentInfo);
                        layoutAttachments.removeView(rootView);
                    }
                });
                layoutAttachments.addView(rootView);
            }
        } else {
            layoutAttachments.removeAllViews();
        }
    }

    /**
     * This interface will be used when we send a message.
     */
    public interface OnMessageSendListener {
        void sendMessage(OutgoingMessageInfo outgoingMessageInfo);

        String getSenderEmail();
    }
}
