/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.api.email.FoldersManager;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.model.IncomingMessageInfo;
import com.flowcrypt.email.api.email.sync.SyncErrorTypes;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.database.dao.source.imap.AttachmentDaoSource;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.model.MessageEncryptionType;
import com.flowcrypt.email.model.messages.MessagePart;
import com.flowcrypt.email.model.messages.MessagePartPgpMessage;
import com.flowcrypt.email.model.messages.MessagePartPgpPublicKey;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.ui.activity.MessageDetailsActivity;
import com.flowcrypt.email.ui.activity.SecureReplyActivity;
import com.flowcrypt.email.ui.activity.base.BaseSendingMessageActivity;
import com.flowcrypt.email.ui.activity.base.BaseSyncActivity;
import com.flowcrypt.email.ui.activity.fragment.base.BaseGmailFragment;
import com.flowcrypt.email.ui.loader.DecryptMessageAsyncTaskLoader;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

import org.apache.commons.io.FileUtils;

import java.util.List;

/**
 * This fragment describe details of some message.
 *
 * @author DenBond7
 *         Date: 03.05.2017
 *         Time: 16:29
 *         E-mail: DenBond7@gmail.com
 */
public class MessageDetailsFragment extends BaseGmailFragment implements View.OnClickListener {
    private TextView textViewSenderAddress;
    private TextView textViewDate;
    private TextView textViewSubject;
    private View viewFooterOfHeader;
    private ViewGroup layoutMessageParts;
    private View layoutContent;
    private View imageButtonReplyAll;
    private View progressBarActionRunning;

    private java.text.DateFormat dateFormat;
    private IncomingMessageInfo incomingMessageInfo;
    private GeneralMessageDetails generalMessageDetails;
    private Folder folder;

    private boolean isAdditionalActionEnable;
    private boolean isDeleteActionEnable;
    private boolean isArchiveActionEnable;
    private boolean isMoveToInboxActionEnable;
    private OnActionListener onActionListener;

    public MessageDetailsFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof BaseSyncActivity) {
            this.onActionListener = (OnActionListener) context;
        } else throw new IllegalArgumentException(context.toString() + " must implement " +
                OnActionListener.class.getSimpleName());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        dateFormat = DateFormat.getTimeFormat(getContext());
        Intent activityIntent = getActivity().getIntent();

        if (activityIntent != null) {
            this.generalMessageDetails = activityIntent.getParcelableExtra(MessageDetailsActivity
                    .EXTRA_KEY_GENERAL_MESSAGE_DETAILS);
            this.folder = activityIntent.getParcelableExtra(MessageDetailsActivity.EXTRA_KEY_FOLDER);
        }

        updateActionsVisibility(folder);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_message_details, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        updateViews();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (!TextUtils.isEmpty(generalMessageDetails.getRawMessageWithoutAttachments())) {
            getLoaderManager().initLoader(R.id.loader_id_load_message_info_from_database, null, this);
        }
    }

    @Override
    public View getContentView() {
        return layoutMessageParts;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_message_details, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem menuItemArchiveMessage = menu.findItem(R.id.menuActionArchiveMessage);
        MenuItem menuItemDeleteMessage = menu.findItem(R.id.menuActionDeleteMessage);
        MenuItem menuActionMoveToInbox = menu.findItem(R.id.menuActionMoveToInbox);

        if (menuItemArchiveMessage != null) {
            menuItemArchiveMessage.setVisible(isArchiveActionEnable && isAdditionalActionEnable);
        }

        if (menuItemDeleteMessage != null) {
            menuItemDeleteMessage.setVisible(isDeleteActionEnable && isAdditionalActionEnable);
        }

        if (menuActionMoveToInbox != null) {
            menuActionMoveToInbox.setVisible(isMoveToInboxActionEnable && isAdditionalActionEnable);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuActionArchiveMessage:
            case R.id.menuActionDeleteMessage:
            case R.id.menuActionMoveToInbox:
                runMessageAction(item.getItemId());
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_load_message_info_from_database:
                UIUtil.exchangeViewVisibility(getContext(), true, progressView, layoutMessageParts);
                return new DecryptMessageAsyncTaskLoader(getContext(), generalMessageDetails
                        .getRawMessageWithoutAttachments());
            default:
                return null;
        }
    }

    @Override
    public void handleSuccessLoaderResult(int loaderId, Object result) {
        switch (loaderId) {
            case R.id.loader_id_load_message_info_from_database:
                imageButtonReplyAll.setVisibility(View.VISIBLE);
                isAdditionalActionEnable = true;
                getActivity().invalidateOptionsMenu();
                this.incomingMessageInfo = (IncomingMessageInfo) result;
                updateMessageBody();
                UIUtil.exchangeViewVisibility(getContext(), false, progressView, layoutMessageParts);
                break;
            default:
                super.handleSuccessLoaderResult(loaderId, result);
        }
    }

    @Override
    public void handleFailureLoaderResult(int loaderId, Exception e) {
        super.handleFailureLoaderResult(loaderId, e);
        isAdditionalActionEnable = true;
        getActivity().invalidateOptionsMenu();
        UIUtil.exchangeViewVisibility(getContext(), false, progressView, layoutMessageParts);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imageButtonReplyAll:
                runSecurityReplyActivity();
                break;
        }
    }

    @Override
    public void onErrorOccurred(final int requestCode, int errorType, Exception e) {
        super.onErrorOccurred(requestCode, errorType, e);
        isAdditionalActionEnable = true;
        getActivity().invalidateOptionsMenu();

        switch (errorType) {
            case SyncErrorTypes.CONNECTION_TO_STORE_IS_LOST:
                showSnackbar(getView(), getString(R.string.failed_load_message_from_email_server),
                        getString(R.string.retry), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                UIUtil.exchangeViewVisibility(getContext(), true, progressView, statusView);
                                ((BaseSyncActivity) getBaseActivity()).loadMessageDetails(
                                        R.id.syns_request_code_load_message_details, folder,
                                        generalMessageDetails.getUid());
                            }
                        });
                break;
        }
    }

    /**
     * Show message details.
     *
     * @param generalMessageDetails This object contains general message details.
     */
    public void showMessageBody(GeneralMessageDetails generalMessageDetails) {
        this.generalMessageDetails = generalMessageDetails;
        getLoaderManager().initLoader(R.id.loader_id_load_message_info_from_database, null, this);
    }

    public void notifyUserAboutActionError(int requestCode) {
        isAdditionalActionEnable = true;
        getActivity().invalidateOptionsMenu();

        UIUtil.exchangeViewVisibility(getContext(), false, progressBarActionRunning, layoutContent);

        switch (requestCode) {
            case R.id.syns_request_archive_message:
                UIUtil.showInfoSnackbar(getView(),
                        getString(R.string.error_occurred_while_archiving_message));
                break;

            case R.id.syns_request_delete_message:
                UIUtil.showInfoSnackbar(getView(),
                        getString(R.string.error_occurred_while_deleting_message));
                break;
        }
    }

    protected void updateMessageBody() {
        if (incomingMessageInfo != null) {
            updateMessageView();
            showAttachmentsIfTheyExist();
        }
    }

    /**
     * Update actions visibility using {@link FoldersManager.FolderType}
     *
     * @param folder The folder where current message exists.
     */
    private void updateActionsVisibility(Folder folder) {
        FoldersManager.FolderType folderType = FoldersManager.getFolderTypeForImapFodler(folder
                .getAttributes());

        if (folderType != null) {
            switch (folderType) {
                case All:
                    isMoveToInboxActionEnable = true;
                    isArchiveActionEnable = false;
                    isDeleteActionEnable = true;
                    break;

                case SENT:
                    isArchiveActionEnable = false;
                    isDeleteActionEnable = true;
                    break;

                case TRASH:
                    isArchiveActionEnable = true;
                    isDeleteActionEnable = false;
                    break;

                case DRAFTS:
                case SPAM:
                    isArchiveActionEnable = false;
                    isDeleteActionEnable = false;
                    break;

                default:
                    isArchiveActionEnable = true;
                    isDeleteActionEnable = true;
                    break;
            }
        } else {
            isArchiveActionEnable = true;
            isMoveToInboxActionEnable = false;
            isDeleteActionEnable = true;
        }

        getActivity().invalidateOptionsMenu();
    }

    /**
     * Run the message action (archive/delete/move to inbox).
     *
     * @param menuId The action menu id.
     */
    private void runMessageAction(final int menuId) {
        if (GeneralUtil.isInternetConnectionAvailable(getContext())) {
            isAdditionalActionEnable = false;
            getActivity().invalidateOptionsMenu();
            statusView.setVisibility(View.GONE);
            UIUtil.exchangeViewVisibility(getContext(), true, progressBarActionRunning, layoutContent);
            switch (menuId) {
                case R.id.menuActionArchiveMessage:
                    onActionListener.onArchiveMessageClicked();
                    break;

                case R.id.menuActionDeleteMessage:
                    onActionListener.onDeleteMessageClicked();
                    break;

                case R.id.menuActionMoveToInbox:
                    onActionListener.onMoveMessageToInboxClicked();
                    break;
            }
        } else {
            showSnackbar(getView(),
                    getString(R.string.internet_connection_is_not_available),
                    getString(R.string.retry), Snackbar.LENGTH_LONG, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            runMessageAction(menuId);
                        }
                    });
        }
    }

    /**
     * Run a screen where the user can start write a reply.
     */
    private void runSecurityReplyActivity() {
        MessageEncryptionType messageEncryptionType = MessageEncryptionType.STANDARD;

        for (MessagePart messagePart : incomingMessageInfo.getMessageParts()) {
            if (messagePart instanceof MessagePartPgpPublicKey
                    || messagePart instanceof MessagePartPgpMessage) {
                messageEncryptionType = MessageEncryptionType.ENCRYPTED;
                break;
            }
        }

        Intent intent = SecureReplyActivity.generateIntent(getContext(), incomingMessageInfo, messageEncryptionType);
        intent.putExtra(BaseSendingMessageActivity.EXTRA_KEY_ACCOUNT_EMAIL, generalMessageDetails.getEmail());
        startActivity(intent);
    }

    private void initViews(View view) {
        textViewSenderAddress = (TextView) view.findViewById(R.id.textViewSenderAddress);
        textViewDate = (TextView) view.findViewById(R.id.textViewDate);
        textViewSubject = (TextView) view.findViewById(R.id.textViewSubject);
        viewFooterOfHeader = view.findViewById(R.id.layoutFooterOfHeader);
        layoutMessageParts = (ViewGroup) view.findViewById(R.id.layoutMessageParts);
        progressBarActionRunning = view.findViewById(R.id.progressBarActionRunning);

        layoutContent = view.findViewById(R.id.layoutContent);
        imageButtonReplyAll = view.findViewById(R.id.imageButtonReplyAll);
        imageButtonReplyAll.setOnClickListener(this);
    }

    private void updateViews() {
        if (generalMessageDetails != null) {
            String subject = TextUtils.isEmpty(generalMessageDetails.getSubject()) ? getString(R.string.no_subject) :
                    generalMessageDetails.getSubject();

            textViewSenderAddress.setText(generalMessageDetails.getFrom()[0]);
            textViewSubject.setText(subject);
            textViewDate.setText(dateFormat.format(generalMessageDetails.getReceivedDateInMillisecond()));
        }

        updateMessageBody();
    }

    private void showAttachmentsIfTheyExist() {
        if (generalMessageDetails != null && generalMessageDetails.isMessageHasAttachment()) {
            List<AttachmentInfo> attachmentInfoList = new AttachmentDaoSource()
                    .getAttachmentInfoList(getContext(), generalMessageDetails.getEmail(),
                            generalMessageDetails.getLabel(), generalMessageDetails.getUid());
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());

            for (AttachmentInfo attachmentInfo : attachmentInfoList) {
                View rootView = layoutInflater.inflate(R.layout.attachment_item, layoutMessageParts, false);

                TextView textViewAttachmentName = (TextView) rootView.findViewById(R.id.textViewAttchmentName);
                textViewAttachmentName.setText(attachmentInfo.getName());

                TextView textViewAttachmentSize = (TextView) rootView.findViewById(R.id.textViewAttachmentSize);
                textViewAttachmentSize.setText(FileUtils.byteCountToDisplaySize(attachmentInfo.getEncodedSize()));

                layoutMessageParts.addView(rootView);
            }
        }
    }

    private void updateMessageView() {
        if (incomingMessageInfo.getMessageParts() != null && !incomingMessageInfo.getMessageParts().isEmpty()) {
            boolean isFirstMessagePartIsText = true;
            for (MessagePart messagePart : incomingMessageInfo.getMessageParts()) {
                LayoutInflater layoutInflater = LayoutInflater.from(getContext());
                if (messagePart != null && !TextUtils.isEmpty(messagePart.getValue())) {
                    switch (messagePart.getMessagePartType()) {
                        case PGP_MESSAGE:
                            layoutMessageParts.addView(generatePgpMessagePart(messagePart, layoutInflater));
                            break;

                        case TEXT:
                            layoutMessageParts.addView(generateTextPart(messagePart, layoutInflater));
                            if (isFirstMessagePartIsText) {
                                viewFooterOfHeader.setVisibility(View.VISIBLE);
                            }
                            break;

                        case PGP_PUBLIC_KEY:
                            layoutMessageParts.addView(generatePublicKeyPart(
                                    (MessagePartPgpPublicKey) messagePart, layoutInflater));
                            break;

                        default:
                            layoutMessageParts.addView(generateMessagePart(messagePart, layoutInflater,
                                    R.layout.message_part_other, layoutMessageParts));
                            break;
                    }
                }
                isFirstMessagePartIsText = false;
            }
        } else {
            layoutMessageParts.removeAllViews();
        }
    }

    /**
     * Generate the public key part. There we can see the public key details and save/update the
     * key owner information to the local database.
     *
     * @param messagePartPgpPublicKey The {@link MessagePartPgpPublicKey} object which contains
     *                                information about a public key and his owner.
     * @param layoutInflater          The {@link LayoutInflater} instance.
     * @return The generated view.
     */
    @NonNull
    private View generatePublicKeyPart(final MessagePartPgpPublicKey messagePartPgpPublicKey,
                                       LayoutInflater layoutInflater) {

        View messagePartPublicKeyView = layoutInflater.inflate(
                R.layout.message_part_public_key, layoutMessageParts, false);

        TextView textViewKeyOwnerTemplate = (TextView) messagePartPublicKeyView
                .findViewById(R.id.textViewKeyOwnerTemplate);
        TextView textViewKeyWordsTemplate = (TextView) messagePartPublicKeyView
                .findViewById(R.id.textViewKeyWordsTemplate);
        TextView textViewFingerprintTemplate = (TextView) messagePartPublicKeyView
                .findViewById(R.id.textViewFingerprintTemplate);
        final TextView textViewPgpPublicKey = (TextView) messagePartPublicKeyView
                .findViewById(R.id.textViewPgpPublicKey);
        Switch switchShowPublicKey = (Switch) messagePartPublicKeyView
                .findViewById(R.id.switchShowPublicKey);

        switchShowPublicKey.setOnCheckedChangeListener(new CompoundButton
                .OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean
                    isChecked) {
                textViewPgpPublicKey.setVisibility(isChecked ? View.VISIBLE : View.GONE);

                buttonView.setText(isChecked ? R.string.hide_the_public_key :
                        R.string.show_the_public_key);
            }
        });

        if (!TextUtils.isEmpty(messagePartPgpPublicKey.getKeyOwner())) {
            textViewKeyOwnerTemplate.setText(
                    getString(R.string.template_message_part_public_key_owner,
                            messagePartPgpPublicKey.getKeyOwner()));
        }

        UIUtil.setHtmlTextToTextView(getString(R.string.template_message_part_public_key_key_words,
                messagePartPgpPublicKey.getKeyWords()), textViewKeyWordsTemplate);

        UIUtil.setHtmlTextToTextView(
                getString(R.string.template_message_part_public_key_fingerprint,
                        GeneralUtil.doSectionsInText(" ",
                                messagePartPgpPublicKey.getFingerprint(), 4)),
                textViewFingerprintTemplate);

        textViewPgpPublicKey.setText(messagePartPgpPublicKey.getValue());

        if (messagePartPgpPublicKey.isPgpContactExists()) {
            if (messagePartPgpPublicKey.isPgpContactCanBeUpdated()) {
                initUpdateContactButton(messagePartPgpPublicKey, messagePartPublicKeyView);
            }
        } else {
            initSaveContactButton(messagePartPgpPublicKey, messagePartPublicKeyView);
        }

        return messagePartPublicKeyView;
    }

    /**
     * Init the save contact button. When we press this button a new contact will be saved to the
     * local database.
     *
     * @param messagePartPgpPublicKey  The {@link MessagePartPgpPublicKey} object which contains
     *                                 information about a public key and his owner.
     * @param messagePartPublicKeyView The public key view container.
     */
    private void initSaveContactButton(final MessagePartPgpPublicKey messagePartPgpPublicKey,
                                       View messagePartPublicKeyView) {
        Button buttonSaveContact = (Button) messagePartPublicKeyView
                .findViewById(R.id.buttonSaveContact);
        if (buttonSaveContact != null) {
            buttonSaveContact.setVisibility(View.VISIBLE);
            buttonSaveContact.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Uri uri = new ContactsDaoSource().addRow(getContext(),
                            new PgpContact(messagePartPgpPublicKey.getKeyOwner(),
                                    null,
                                    messagePartPgpPublicKey.getValue(),
                                    false,
                                    null,
                                    false,
                                    messagePartPgpPublicKey.getFingerprint(),
                                    messagePartPgpPublicKey.getLongId(),
                                    messagePartPgpPublicKey.getKeyWords(), 0));
                    if (uri != null) {
                        Toast.makeText(getContext(),
                                R.string.contact_successfully_saved, Toast.LENGTH_SHORT).show();
                        v.setVisibility(View.GONE);
                    } else {
                        Toast.makeText(getContext(),
                                R.string.error_occurred_while_saving_contact,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    /**
     * Init the update contact button. When we press this button the contact will be updated in the
     * local database.
     *
     * @param messagePartPgpPublicKey  The {@link MessagePartPgpPublicKey} object which contains
     *                                 information about a public key and his owner.
     * @param messagePartPublicKeyView The public key view container.
     */
    private void initUpdateContactButton(final MessagePartPgpPublicKey messagePartPgpPublicKey,
                                         View messagePartPublicKeyView) {
        Button buttonUpdateContact = (Button) messagePartPublicKeyView
                .findViewById(R.id.buttonUpdateContact);
        if (buttonUpdateContact != null) {
            buttonUpdateContact.setVisibility(View.VISIBLE);
            buttonUpdateContact.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean isUpdated = new ContactsDaoSource().updatePgpContact
                            (getContext(),
                                    new PgpContact(messagePartPgpPublicKey.getKeyOwner(),
                                            null,
                                            messagePartPgpPublicKey.getValue(),
                                            false,
                                            null,
                                            false,
                                            messagePartPgpPublicKey.getFingerprint(),
                                            messagePartPgpPublicKey.getLongId(),
                                            messagePartPgpPublicKey.getKeyWords(), 0)) > 0;
                    if (isUpdated) {
                        Toast.makeText(getContext(),
                                R.string.contact_successfully_updated,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(),
                                R.string.error_occurred_while_updating_contact,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    @NonNull
    private TextView generateMessagePart(MessagePart messagePart, LayoutInflater layoutInflater,
                                         int message_part_other, ViewGroup layoutMessageParts) {
        TextView textViewMessagePartOther = (TextView) layoutInflater.inflate(
                message_part_other, layoutMessageParts, false);

        textViewMessagePartOther.setText(messagePart.getValue());
        return textViewMessagePartOther;
    }

    @NonNull
    private TextView generateTextPart(MessagePart messagePart, LayoutInflater layoutInflater) {
        return generateMessagePart(messagePart, layoutInflater, R.layout.message_part_text, layoutMessageParts);
    }

    @NonNull
    private TextView generatePgpMessagePart(MessagePart messagePart,
                                            LayoutInflater layoutInflater) {
        return generateMessagePart(messagePart, layoutInflater, R.layout.message_part_pgp_message, layoutMessageParts);
    }

    public interface OnActionListener {
        void onArchiveMessageClicked();

        void onDeleteMessageClicked();

        void onMoveMessageToInboxClicked();
    }
}
