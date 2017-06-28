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
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
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
        .LoaderCallbacks<Cursor> {
    public static final int RESULT_CODE_MESSAGE_CHANGED = 100;

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
        MessageDetailsFragment messageDetailsFragment = (MessageDetailsFragment)
                getSupportFragmentManager()
                        .findFragmentById(R.id.messageDetailsFragment);

        if (messageDetailsFragment == null || messageDetailsFragment.isBackPressedEnable()) {
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
                if (cursor != null && cursor.moveToFirst()) {
                    if (TextUtils.isEmpty(cursor.getString(cursor.getColumnIndex(MessageDaoSource
                            .COL_RAW_MESSAGE_WITHOUT_ATTACHMENTS)))) {
                        if (isBound) {
                            loadMessageDetails(R.id.syns_request_code_load_message_details, folder,
                                    uid);
                        } else {
                            isNeedToReceiveMessageDetails = true;
                        }
                    } else {
                        isNeedToReceiveMessageDetails = false;
                        generalMessageDetails = new MessageDaoSource().getMessageInfo(cursor);
                        showMessageDetails(generalMessageDetails);
                        setResult(MessageDetailsActivity.RESULT_CODE_MESSAGE_CHANGED, null);
                        cursor.close();
                    }
                } else throw new IllegalArgumentException("The message not exists in the database");
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
    public void onReplyFromSyncServiceReceived(int requestCode, int resultCode) {
        switch (requestCode) {
            case R.id.syns_request_code_load_message_details:
                switch (resultCode) {
                    case EmailSyncService.REPLY_RESULT_CODE_OK:
                        new MessageDaoSource().setSeenStatusForLocalMessage(this, email, folder
                                .getFolderAlias(), uid);
                        setResult(MessageDetailsActivity.RESULT_CODE_MESSAGE_CHANGED, null);
                        getSupportLoaderManager().restartLoader(R.id
                                        .loader_id_load_message_info_from_database,
                                null, this);
                        break;

                    case EmailSyncService.REPLY_RESULT_CODE_ERROR:
                        // TODO-denbond7: 27.06.2017 need to handle error when load message details.
                        break;
                }
                break;
        }
    }

    private void showMessageDetails(GeneralMessageDetails generalMessageDetails) {
        MessageDetailsFragment messageDetailsFragment = (MessageDetailsFragment)
                getSupportFragmentManager()
                        .findFragmentById(R.id.messageDetailsFragment);

        if (messageDetailsFragment != null) {
            messageDetailsFragment.showMessageDetails(generalMessageDetails);
        }
    }

    private void initViews() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(null);
        }
    }

}
