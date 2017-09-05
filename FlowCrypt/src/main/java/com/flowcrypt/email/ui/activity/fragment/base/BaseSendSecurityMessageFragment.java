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
import android.support.v4.content.Loader;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.model.MessageEncryptionType;
import com.flowcrypt.email.model.UpdateInfoAboutPgpContactsResult;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.ui.activity.ImportPublicKeyActivity;
import com.flowcrypt.email.ui.activity.fragment.dialog.NoPgpFoundDialogFragment;
import com.flowcrypt.email.ui.activity.listeners.OnChangeMessageEncryptedTypeListener;
import com.flowcrypt.email.ui.loader.UpdateInfoAboutPgpContactsAsyncTaskLoader;
import com.flowcrypt.email.ui.widget.CustomChipSpanChipCreator;
import com.flowcrypt.email.ui.widget.PGPContactChipSpan;
import com.flowcrypt.email.ui.widget.SingleCharacterSpanChipTokenizer;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;
import com.hootsuite.nachos.NachoTextView;
import com.hootsuite.nachos.validator.ChipifyingNachoValidator;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The base fragment for sending an encrypted message;
 *
 * @author DenBond7
 *         Date: 10.05.2017
 *         Time: 11:27
 *         E-mail: DenBond7@gmail.com
 */

public abstract class BaseSendSecurityMessageFragment extends BaseGmailFragment {
    protected static final int REQUEST_CODE_NO_PGP_FOUND_DIALOG = 100;
    private static final int REQUEST_CODE_IMPORT_PUBLIC_KEY = 101;
    private static final int REQUEST_CODE_GET_CONTENT_FOR_SENDING = 102;

    private static final int MAX_TOTAL_ATTACHMENT_SIZE_IN_BYTES = 1024 * 1024 * 5;

    protected Js js;
    protected OnMessageSendListener onMessageSendListener;
    protected OnChangeMessageEncryptedTypeListener onChangeMessageEncryptedTypeListener;
    protected boolean isUpdateInfoAboutContactsEnable = true;
    protected boolean isUpdatedInfoAboutContactCompleted = true;
    protected boolean isMessageSendingNow;
    protected List<PgpContact> pgpContacts;
    protected NachoTextView editTextRecipients;
    protected ArrayList<AttachmentInfo> attachmentInfoList;
    private ViewGroup layoutAttachments;

    public BaseSendSecurityMessageFragment() {
        pgpContacts = new ArrayList<>();
        attachmentInfoList = new ArrayList<>();
    }

    public abstract void onMessageEncryptionTypeChange(MessageEncryptionType messageEncryptionType);

    /**
     * Generate an outgoing message info from entered information by user.
     *
     * @return <tt>OutgoingMessageInfo</tt> Return a created OutgoingMessageInfo object which
     * contains information about an outgoing message.
     */
    public abstract OutgoingMessageInfo getOutgoingMessageInfo();

    /**
     * Get an update information about contacts progress view.
     *
     * @return {@link View}
     */
    public abstract View getUpdateInfoAboutContactsProgressBar();

    /**
     * Get a list of emails, that will be checked to find an information about public keys.
     *
     * @return A list of emails.
     */
    public abstract List<String> getContactsEmails();

    /**
     * Do a lot of checks to validate an outgoing message info.
     *
     * @return <tt>Boolean</tt> true if all information is correct, false otherwise.
     */
    public abstract boolean isAllInformationCorrect();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnMessageSendListener) {
            this.onMessageSendListener = (OnMessageSendListener) context;
        } else throw new IllegalArgumentException(context.toString() + " must implement " +
                OnMessageSendListener.class.getSimpleName());

        if (context instanceof OnChangeMessageEncryptedTypeListener) {
            this.onChangeMessageEncryptedTypeListener = (OnChangeMessageEncryptedTypeListener)
                    context;
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
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        layoutAttachments = (ViewGroup) view.findViewById(R.id.layoutAttachments);
        initChipsView(view);
        showAttachments();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_NO_PGP_FOUND_DIALOG:
                switch (resultCode) {
                    case NoPgpFoundDialogFragment.RESULT_CODE_SWITCH_TO_STANDARD_EMAIL:
                        switchMessageEncryptionType(MessageEncryptionType.STANDARD);
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
                }
                break;

            case REQUEST_CODE_IMPORT_PUBLIC_KEY:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Toast.makeText(getContext(), R.string.key_successfully_imported,
                                Toast.LENGTH_SHORT).show();

                        getLoaderManager().restartLoader(
                                R.id.loader_id_update_info_about_pgp_contacts, null,
                                BaseSendSecurityMessageFragment.this);
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
                                                FileUtils.byteCountToDisplaySize(MAX_TOTAL_ATTACHMENT_SIZE_IN_BYTES)),
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
                    Toast.makeText(getContext(), R.string
                                    .please_wait_while_information_about_contacts_will_be_updated,
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
                getUpdateInfoAboutContactsProgressBar().setVisibility(View.VISIBLE);
                isUpdatedInfoAboutContactCompleted = false;
                return new UpdateInfoAboutPgpContactsAsyncTaskLoader(getContext(),
                        getContactsEmails());

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
                getUpdateInfoAboutContactsProgressBar().setVisibility(View.INVISIBLE);

                if (updateInfoAboutPgpContactsResult != null
                        && updateInfoAboutPgpContactsResult.getUpdatedPgpContacts() != null) {
                    pgpContacts = updateInfoAboutPgpContactsResult.getUpdatedPgpContacts();
                }

                if (updateInfoAboutPgpContactsResult == null
                        || !updateInfoAboutPgpContactsResult.isAllInfoReceived()) {
                    Toast.makeText(getContext(),
                            R.string.info_about_some_contacts_not_received,
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
                getUpdateInfoAboutContactsProgressBar().setVisibility(View.INVISIBLE);
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
     * Switch the message encryption type.
     *
     * @param messageEncryptionType The new message encryption type.
     */
    protected void switchMessageEncryptionType(MessageEncryptionType messageEncryptionType) {
        onChangeMessageEncryptedTypeListener.onMessageEncryptionTypeChange(messageEncryptionType);
    }

    /**
     * Check that all recipients have PGP.
     *
     * @return true if all recipients have PGP, other wise false.
     */
    protected boolean isAllRecipientsHavePGP(boolean isShowRemoveAction) {
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
    protected void updateChips() {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder
                (editTextRecipients.getText());

        PGPContactChipSpan[] pgpContactChipSpans = spannableStringBuilder.getSpans(0,
                editTextRecipients.length(), PGPContactChipSpan.class);

        if (pgpContactChipSpans.length > 0) {
            for (PgpContact pgpContact : pgpContacts) {
                for (PGPContactChipSpan pgpContactChipSpan : pgpContactChipSpans) {
                    if (pgpContact.getEmail().equalsIgnoreCase(pgpContactChipSpan.getText()
                            .toString())) {
                        pgpContactChipSpan.setHasPgp(pgpContact.getHasPgp());
                        break;
                    }
                }
            }
            editTextRecipients.invalidateChips();
        }
    }

    protected void initChipsView(View view) {
        editTextRecipients = (NachoTextView) view.findViewById(R.id.editTextRecipient);
        editTextRecipients.setNachoValidator(new ChipifyingNachoValidator());
        editTextRecipients.setIllegalCharacters(',');
        editTextRecipients.setChipTokenizer(
                new SingleCharacterSpanChipTokenizer(getContext(), new CustomChipSpanChipCreator
                        (getContext()),
                        PGPContactChipSpan.class, SingleCharacterSpanChipTokenizer
                        .CHIP_SEPARATOR_WHITESPACE));
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

        return totalSizeOfAttachments < MAX_TOTAL_ATTACHMENT_SIZE_IN_BYTES;
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
        noPgpFoundDialogFragment.show(getFragmentManager(), NoPgpFoundDialogFragment.class
                .getSimpleName());
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
        outgoingMessageInfo.setMessageEncryptionType(onChangeMessageEncryptedTypeListener
                .getMessageEncryptionType());

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
                textViewAttachmentSize.setText(FileUtils.byteCountToDisplaySize(attachmentInfo.getEncodedSize()));

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
