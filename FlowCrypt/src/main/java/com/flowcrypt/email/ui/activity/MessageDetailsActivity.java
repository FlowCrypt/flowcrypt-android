/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.api.email.FoldersManager;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.model.IncomingMessageInfo;
import com.flowcrypt.email.api.email.sync.SyncErrorTypes;
import com.flowcrypt.email.database.MessageState;
import com.flowcrypt.email.database.dao.source.imap.AttachmentDaoSource;
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource;
import com.flowcrypt.email.service.EmailSyncService;
import com.flowcrypt.email.ui.activity.base.BaseBackStackSyncActivity;
import com.flowcrypt.email.ui.activity.fragment.MessageDetailsFragment;
import com.flowcrypt.email.util.GeneralUtil;

/**
 * This activity describe details of some message.
 *
 * @author DenBond7
 *         Date: 03.05.2017
 *         Time: 16:29
 *         E-mail: DenBond7@gmail.com
 */
public class MessageDetailsActivity extends BaseBackStackSyncActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, MessageDetailsFragment.OnActionListener {
    public static final int RESULT_CODE_UPDATE_LIST = 100;

    public static final String EXTRA_KEY_FOLDER = GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_FOLDER",
            MessageDetailsActivity.class);
    public static final String EXTRA_KEY_GENERAL_MESSAGE_DETAILS = GeneralUtil.generateUniqueExtraKey
            ("EXTRA_KEY_GENERAL_MESSAGE_DETAILS", MessageDetailsActivity.class);

    private GeneralMessageDetails generalMessageDetails;
    private Folder folder;
    private boolean isNeedToReceiveMessageBody;
    private boolean isBackEnable = true;
    private boolean isRequestMessageDetailsStarted;

    public static Intent getIntent(Context context, Folder folder, GeneralMessageDetails generalMessageDetails) {
        Intent intent = new Intent(context, MessageDetailsActivity.class);
        intent.putExtra(EXTRA_KEY_FOLDER, folder);
        intent.putExtra(EXTRA_KEY_GENERAL_MESSAGE_DETAILS, generalMessageDetails);
        return intent;
    }

    @Override
    public View getRootView() {
        return null;
    }

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_message_details;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() != null) {
            this.folder = getIntent().getParcelableExtra(EXTRA_KEY_FOLDER);
            this.generalMessageDetails = getIntent().getParcelableExtra(EXTRA_KEY_GENERAL_MESSAGE_DETAILS);
        }

        initViews();

        if (TextUtils.isEmpty(generalMessageDetails.getRawMessageWithoutAttachments())) {
            getSupportLoaderManager().initLoader(R.id.loader_id_load_message_info_from_database, null, this);
        }
    }

    @Override
    public void onBackPressed() {
        if (isBackEnable) {
            super.onBackPressed();
        } else {
            Toast.makeText(this, R.string.please_wait_while_action_will_be_completed,
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_load_message_info_from_database:
                return new CursorLoader(this, new MessageDaoSource().
                        getBaseContentUri(),
                        null,
                        MessageDaoSource.COL_EMAIL + "= ? AND " + MessageDaoSource.COL_FOLDER + " = ? AND "
                                + MessageDaoSource.COL_UID + " = ? ",
                        new String[]{generalMessageDetails.getEmail(), folder.getFolderAlias(),
                                String.valueOf(generalMessageDetails.getUid())}, null);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case R.id.loader_id_load_message_info_from_database:
                if (TextUtils.isEmpty(generalMessageDetails.getRawMessageWithoutAttachments())) {
                    if (cursor != null && cursor.moveToFirst()) {
                        if (TextUtils.isEmpty(cursor.getString(cursor.getColumnIndex
                                (MessageDaoSource.COL_RAW_MESSAGE_WITHOUT_ATTACHMENTS)))) {
                            if (isBoundToSyncService && !isRequestMessageDetailsStarted) {
                                this.isRequestMessageDetailsStarted = true;
                                loadMessageDetails(R.id.syns_request_code_load_message_details,
                                        folder, generalMessageDetails.getUid());
                            } else {
                                isNeedToReceiveMessageBody = true;
                            }
                        } else {
                            isNeedToReceiveMessageBody = false;
                            MessageDaoSource messageDaoSource = new MessageDaoSource();
                            messageDaoSource.setSeenStatusForLocalMessage(this, generalMessageDetails.getEmail(),
                                    folder.getFolderAlias(), generalMessageDetails.getUid());
                            this.generalMessageDetails = messageDaoSource.getMessageInfo(cursor);
                            updateMessageDetails(generalMessageDetails);
                            setResult(MessageDetailsActivity.RESULT_CODE_UPDATE_LIST, null);

                            decryptMessage(R.id.js_decrypt_message, generalMessageDetails
                                    .getRawMessageWithoutAttachments());
                        }
                    }
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case R.id.loader_id_load_message_info_from_database:
                break;
        }
    }


    @Override
    public void onSyncServiceConnected() {
        super.onSyncServiceConnected();
        if (isNeedToReceiveMessageBody) {
            loadMessageDetails(R.id.syns_request_code_load_message_details, folder, generalMessageDetails.getUid());
        }
    }

    @Override
    public void onReplyFromServiceReceived(int requestCode, int resultCode, Object obj) {
        switch (requestCode) {
            case R.id.syns_request_code_load_message_details:
                isRequestMessageDetailsStarted = false;
                switch (resultCode) {
                    case EmailSyncService.REPLY_RESULT_CODE_ACTION_OK:
                        new MessageDaoSource().setSeenStatusForLocalMessage(this, generalMessageDetails.getEmail(),
                                folder.getFolderAlias(), generalMessageDetails.getUid());
                        setResult(MessageDetailsActivity.RESULT_CODE_UPDATE_LIST, null);
                        getSupportLoaderManager().restartLoader(R.id.loader_id_load_message_info_from_database,
                                null, this);
                        break;

                    case EmailSyncService.REPLY_RESULT_CODE_ACTION_ERROR_MESSAGE_NOT_FOUND:
                        messageNotAvailableInFolder();
                        break;
                }
                break;

            case R.id.syns_request_archive_message:
            case R.id.syns_request_delete_message:
            case R.id.syns_request_move_message_to_inbox:
                isBackEnable = true;
                switch (resultCode) {
                    case EmailSyncService.REPLY_RESULT_CODE_ACTION_OK:
                        int toastMessageResourcesId = 0;

                        switch (requestCode) {
                            case R.id.syns_request_archive_message:
                                toastMessageResourcesId = R.string.message_was_archived;
                                break;

                            case R.id.syns_request_delete_message:
                                toastMessageResourcesId = R.string.message_was_deleted;
                                break;

                            case R.id.syns_request_move_message_to_inbox:
                                toastMessageResourcesId = R.string.message_was_moved_to_inbox;
                                break;
                        }

                        Toast.makeText(this, toastMessageResourcesId, Toast.LENGTH_SHORT).show();

                        new MessageDaoSource().deleteMessageFromFolder(this, generalMessageDetails.getEmail(),
                                folder.getFolderAlias(), generalMessageDetails.getUid());
                        new AttachmentDaoSource().deleteAttachments(this, generalMessageDetails.getEmail(),
                                folder.getFolderAlias(), generalMessageDetails.getUid());
                        setResult(MessageDetailsActivity.RESULT_CODE_UPDATE_LIST, null);
                        finish();
                        break;

                    case EmailSyncService.REPLY_RESULT_CODE_ACTION_ERROR_MESSAGE_NOT_EXISTS:
                        messageNotAvailableInFolder();
                        break;
                }
                break;

            case R.id.js_decrypt_message:
                if (obj instanceof IncomingMessageInfo) {
                    IncomingMessageInfo incomingMessageInfo = (IncomingMessageInfo) obj;
                    MessageDetailsFragment messageDetailsFragment = (MessageDetailsFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.messageDetailsFragment);

                    if (messageDetailsFragment != null) {
                        messageDetailsFragment.showIncomingMessageInfo(incomingMessageInfo);
                    }
                }
                break;
        }
    }

    @Override
    public void onJsServiceConnected() {
        super.onJsServiceConnected();
        if (!TextUtils.isEmpty(generalMessageDetails.getRawMessageWithoutAttachments())) {
            decryptMessage(R.id.js_decrypt_message, generalMessageDetails.getRawMessageWithoutAttachments());
        }
    }

    @Override
    public void onErrorFromServiceReceived(int requestCode, int errorType, Exception e) {
        switch (requestCode) {
            case R.id.syns_request_code_load_message_details:
                isRequestMessageDetailsStarted = false;
                notifyMessageDetailsFragmentAboutError(requestCode, errorType, e);
                break;

            case R.id.syns_request_archive_message:
            case R.id.syns_request_delete_message:
            case R.id.syns_request_move_message_to_inbox:
                isBackEnable = true;
                notifyMessageDetailsFragmentAboutError(requestCode, errorType, e);
                break;

            default:
                notifyMessageDetailsFragmentAboutError(requestCode, errorType, e);
                break;
        }
    }

    @Override
    public void onArchiveMessageClicked() {
        isBackEnable = false;
        FoldersManager foldersManager = FoldersManager.fromDatabase(this, generalMessageDetails.getEmail());
        moveMessage(R.id.syns_request_archive_message, folder,
                foldersManager.getFolderArchive(), generalMessageDetails.getUid());
    }

    @Override
    public void onDeleteMessageClicked() {
        isBackEnable = false;
        if (JavaEmailConstants.FOLDER_OUTBOX.equalsIgnoreCase(generalMessageDetails.getLabel())) {
            MessageDaoSource messageDaoSource = new MessageDaoSource();
            GeneralMessageDetails generalMessageDetails = messageDaoSource.getMessage(this, this
                    .generalMessageDetails.getEmail(), this.generalMessageDetails.getLabel(), this
                    .generalMessageDetails.getUid());

            if (generalMessageDetails == null || generalMessageDetails.getMessageState() == MessageState.SENDING) {
                Toast.makeText(this, generalMessageDetails == null ? R.string.can_not_delete_sent_message
                        : R.string.can_not_delete_sending_message, Toast.LENGTH_LONG).show();
            } else {
                int deletedRows = new MessageDaoSource().deleteOutgoingMessage(this, generalMessageDetails);
                if (deletedRows > 0) {
                    Toast.makeText(this, R.string.message_was_deleted, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.can_not_delete_sent_message, Toast.LENGTH_LONG).show();
                }
            }

            setResult(MessageDetailsActivity.RESULT_CODE_UPDATE_LIST, null);
            finish();
        } else {
            FoldersManager foldersManager = FoldersManager.fromDatabase(this, generalMessageDetails.getEmail());
            moveMessage(R.id.syns_request_delete_message, folder,
                    foldersManager.getFolderTrash(), generalMessageDetails.getUid());
        }
    }

    @Override
    public void onMoveMessageToInboxClicked() {
        isBackEnable = false;
        FoldersManager foldersManager = FoldersManager.fromDatabase(this, generalMessageDetails.getEmail());
        moveMessage(R.id.syns_request_move_message_to_inbox, folder,
                foldersManager.getFolderInbox(), generalMessageDetails.getUid());
    }

    private void messageNotAvailableInFolder() {
        new MessageDaoSource().deleteMessageFromFolder(this, generalMessageDetails.getEmail(),
                folder.getFolderAlias(), generalMessageDetails.getUid());
        new AttachmentDaoSource().deleteAttachments(this, generalMessageDetails.getEmail(),
                folder.getFolderAlias(), generalMessageDetails.getUid());
        setResult(MessageDetailsActivity.RESULT_CODE_UPDATE_LIST, null);
        Toast.makeText(this, R.string.email_does_not_available_in_this_folder,
                Toast.LENGTH_LONG).show();
        finish();
    }

    private void notifyUserAboutError(int requestCode) {
        MessageDetailsFragment messageDetailsFragment = (MessageDetailsFragment)
                getSupportFragmentManager()
                        .findFragmentById(R.id.messageDetailsFragment);

        if (messageDetailsFragment != null) {
            messageDetailsFragment.notifyUserAboutActionError(requestCode);
        }
    }

    /**
     * Handle an error from the sync service.
     *
     * @param requestCode The unique request code for the reply to {@link android.os.Messenger}.
     * @param errorType   The {@link SyncErrorTypes}
     * @param e           The exception which happened.
     */
    private void notifyMessageDetailsFragmentAboutError(int requestCode, int errorType, Exception e) {
        MessageDetailsFragment messageDetailsFragment = (MessageDetailsFragment)
                getSupportFragmentManager().findFragmentById(R.id.messageDetailsFragment);

        if (messageDetailsFragment != null) {
            messageDetailsFragment.onErrorOccurred(requestCode, errorType, e);
        }
    }

    private void updateMessageDetails(GeneralMessageDetails generalMessageDetails) {
        MessageDetailsFragment messageDetailsFragment = (MessageDetailsFragment) getSupportFragmentManager()
                .findFragmentById(R.id.messageDetailsFragment);

        if (messageDetailsFragment != null) {
            messageDetailsFragment.updateMessageDetails(generalMessageDetails);
        }
    }

    private void initViews() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(null);
        }
    }
}
