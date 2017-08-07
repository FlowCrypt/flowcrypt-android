/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.api.email.FoldersManager;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.sync.SyncErrorTypes;
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
public class MessageDetailsActivity extends BaseBackStackSyncActivity implements LoaderManager
        .LoaderCallbacks<Cursor>, MessageDetailsFragment.OnActionListener {
    public static final int RESULT_CODE_UPDATE_LIST = 100;

    public static final String EXTRA_KEY_EMAIL =
            GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_EMAIL", MessageDetailsActivity.class);
    public static final String EXTRA_KEY_FOLDER =
            GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_FOLDER", MessageDetailsActivity.class);
    public static final String EXTRA_KEY_UID =
            GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_UID", MessageDetailsActivity.class);

    private GeneralMessageDetails generalMessageDetails;
    private String email;
    private Folder folder;
    private int uid;
    private boolean isNeedToReceiveMessageDetails;
    private boolean isBackEnable = true;

    public static Intent getIntent(Context context, String email, Folder folder, int uid) {
        Intent intent = new Intent(context, MessageDetailsActivity.class);
        intent.putExtra(EXTRA_KEY_EMAIL, email);
        intent.putExtra(EXTRA_KEY_FOLDER, folder);
        intent.putExtra(EXTRA_KEY_UID, uid);
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
            this.email = getIntent().getStringExtra(EXTRA_KEY_EMAIL);
            this.folder = getIntent().getParcelableExtra((EXTRA_KEY_FOLDER));
            this.uid = getIntent().getIntExtra(EXTRA_KEY_UID, -1);
        }

        initViews();

        getSupportLoaderManager().initLoader(
                R.id.loader_id_load_message_info_from_database, null, this);
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
                        MessageDaoSource.COL_EMAIL + "= ? AND "
                                + MessageDaoSource.COL_FOLDER + " = ? AND "
                                + MessageDaoSource.COL_UID + " = ? ",
                        new String[]{email, folder.getFolderAlias(), String.valueOf(uid)}, null);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case R.id.loader_id_load_message_info_from_database:
                if (generalMessageDetails == null) {
                    if (cursor != null && cursor.moveToFirst()) {
                        if (TextUtils.isEmpty(cursor.getString(cursor.getColumnIndex
                                (MessageDaoSource
                                        .COL_RAW_MESSAGE_WITHOUT_ATTACHMENTS)))) {
                            if (isBound) {
                                loadMessageDetails(R.id.syns_request_code_load_message_details,
                                        folder,
                                        uid);
                            } else {
                                isNeedToReceiveMessageDetails = true;
                            }
                        } else {
                            isNeedToReceiveMessageDetails = false;
                            MessageDaoSource messageDaoSource = new MessageDaoSource();
                            messageDaoSource.setSeenStatusForLocalMessage(this, email, folder
                                    .getFolderAlias(), uid);
                            generalMessageDetails = messageDaoSource.getMessageInfo(cursor);
                            showMessageDetails(generalMessageDetails, folder);
                            setResult(MessageDetailsActivity.RESULT_CODE_UPDATE_LIST, null);
                        }
                    } else
                        throw new IllegalArgumentException("The message not exists in the " +
                                "database");
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
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        if (isNeedToReceiveMessageDetails && generalMessageDetails == null) {
            loadMessageDetails(R.id.syns_request_code_load_message_details, folder,
                    uid);
        }
    }

    @Override
    public void onReplyFromSyncServiceReceived(int requestCode, int resultCode, Object obj) {
        switch (requestCode) {
            case R.id.syns_request_code_load_message_details:
                switch (resultCode) {
                    case EmailSyncService.REPLY_RESULT_CODE_ACTION_OK:
                        new MessageDaoSource().setSeenStatusForLocalMessage(this, email, folder
                                .getFolderAlias(), uid);
                        setResult(MessageDetailsActivity.RESULT_CODE_UPDATE_LIST, null);
                        getSupportLoaderManager().restartLoader(R.id
                                        .loader_id_load_message_info_from_database,
                                null, this);
                        break;

                    case EmailSyncService.REPLY_RESULT_CODE_ACTION_ERROR:
                        // TODO-denbond7: 27.06.2017 need to handle error when load message details.
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

                        new MessageDaoSource().deleteMessageFromFolder(this, email,
                                folder.getFolderAlias(), uid);
                        setResult(MessageDetailsActivity.RESULT_CODE_UPDATE_LIST, null);
                        finish();
                        break;

                    case EmailSyncService.REPLY_RESULT_CODE_ACTION_ERROR:
                        notifyUserAboutError(requestCode);
                        break;
                }
                break;
        }
    }

    @Override
    public void onErrorFromSyncServiceReceived(int requestCode, int errorType, Exception e) {
        switch (requestCode) {
            default:
                notifyMessageDetailsFragmentAboutError(requestCode, errorType);
                break;
        }
    }

    @Override
    public void onArchiveMessageClicked() {
        isBackEnable = false;
        FoldersManager foldersManager = FoldersManager.fromDatabase(this, email);
        moveMessage(R.id.syns_request_archive_message, folder,
                foldersManager.getFolderArchive(), uid);
    }

    @Override
    public void onDeleteMessageClicked() {
        isBackEnable = false;
        FoldersManager foldersManager = FoldersManager.fromDatabase(this, email);
        moveMessage(R.id.syns_request_delete_message, folder,
                foldersManager.getFolderTrash(), uid);
    }

    @Override
    public void onMoveMessageToInboxClicked() {
        isBackEnable = false;
        FoldersManager foldersManager = FoldersManager.fromDatabase(this, email);
        moveMessage(R.id.syns_request_move_message_to_inbox, folder,
                foldersManager.getFolderInbox(), uid);
    }

    public String getEmail() {
        return email;
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
     */
    private void notifyMessageDetailsFragmentAboutError(int requestCode, int errorType) {
        MessageDetailsFragment messageDetailsFragment = (MessageDetailsFragment)
                getSupportFragmentManager().findFragmentById(R.id.messageDetailsFragment);

        if (messageDetailsFragment != null) {
            messageDetailsFragment.onErrorOccurred(requestCode, errorType);
        }
    }

    private void showMessageDetails(GeneralMessageDetails generalMessageDetails, Folder folder) {
        MessageDetailsFragment messageDetailsFragment = (MessageDetailsFragment)
                getSupportFragmentManager()
                        .findFragmentById(R.id.messageDetailsFragment);

        if (messageDetailsFragment != null) {
            messageDetailsFragment.showMessageDetails(generalMessageDetails, folder);
        }
    }

    private void initViews() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(null);
        }
    }
}
