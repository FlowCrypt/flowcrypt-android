/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo;
import com.flowcrypt.email.service.BaseService;
import com.flowcrypt.email.service.EmailSyncService;
import com.flowcrypt.email.util.exception.ExceptionUtil;

/**
 * This class describes a bind to the email sync service logic.
 *
 * @author DenBond7
 *         Date: 16.06.2017
 *         Time: 11:30
 *         E-mail: DenBond7@gmail.com
 */

public abstract class BaseSyncActivity extends BaseActivity {
    // Messengers for communicating with the service.
    protected Messenger syncServiceMessenger;
    protected Messenger syncServiceReplyMessenger;

    /**
     * Flag indicating whether we have called bind on the {@link EmailSyncService}.
     */
    protected boolean isBoundToSyncService;

    private ServiceConnection serviceConnectionSyncService = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Activity connected to " + name.getClassName());
            syncServiceMessenger = new Messenger(service);
            isBoundToSyncService = true;

            registerReplyMessenger(EmailSyncService.MESSAGE_ADD_REPLY_MESSENGER, syncServiceMessenger,
                    syncServiceReplyMessenger);
            onSyncServiceConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Activity disconnected from " + name.getClassName());
            syncServiceMessenger = null;
            isBoundToSyncService = false;
        }
    };

    public BaseSyncActivity() {
        super();
        syncServiceReplyMessenger = new Messenger(new ReplyHandler(this));
    }

    /**
     * Check is a sync enable.
     *
     * @return true - if sync enable, false - otherwise.
     */
    public abstract boolean isSyncEnable();

    public abstract void onSyncServiceConnected();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isSyncEnable()) {
            bindToService(EmailSyncService.class, serviceConnectionSyncService);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isSyncEnable() && isBoundToSyncService) {
            if (syncServiceMessenger != null) {
                unregisterReplyMessenger(EmailSyncService.MESSAGE_REMOVE_REPLY_MESSENGER, syncServiceMessenger,
                        syncServiceReplyMessenger);
            }

            unbindFromService(EmailSyncService.class, serviceConnectionSyncService);
            isBoundToSyncService = false;
        }
    }

    @Override
    public void onJsServiceConnected() {

    }

    /**
     * Send a message with a backup to the key owner.
     *
     * @param requestCode The unique request code for identify the current action.
     * @param accountName The account name.
     */
    public void sendMessageWithPrivateKeyBackup(int requestCode, String accountName) {
        if (checkServiceBound(isBoundToSyncService)) return;

        BaseService.Action action = new BaseService.Action(getReplyMessengerName(),
                requestCode, accountName);

        Message message = Message.obtain(null, EmailSyncService.MESSAGE_SEND_MESSAGE_WITH_BACKUP,
                action);

        message.replyTo = syncServiceReplyMessenger;
        try {
            syncServiceMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
        }
    }

    /**
     * Request the active account
     *
     * @param requestCode The unique request code for identify the current action.
     */
    public void requestActiveAccount(int requestCode) {
        if (checkServiceBound(isBoundToSyncService)) return;
        try {
            BaseService.Action action = new BaseService.Action(getReplyMessengerName(),
                    requestCode, null);

            Message message = Message.obtain(null, EmailSyncService.MESSAGE_GET_ACTIVE_ACCOUNT,
                    action);
            message.replyTo = syncServiceReplyMessenger;

            syncServiceMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
        }
    }

    /**
     * Load the user private keys.
     *
     * @param requestCode The unique request code for identify the current action.
     */
    public void loadPrivateKeys(int requestCode) {
        if (checkServiceBound(isBoundToSyncService)) return;
        try {
            BaseService.Action action = new BaseService.Action(getReplyMessengerName(), requestCode, null);

            Message message = Message.obtain(null, EmailSyncService.MESSAGE_LOAD_PRIVATE_KEYS,
                    action);
            message.replyTo = syncServiceReplyMessenger;

            syncServiceMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
        }
    }

    /**
     * Load messages from some folder in some range.
     *
     * @param requestCode The unique request code for identify the current action.
     * @param folder      {@link Folder} object.
     * @param start       The position of the start.
     * @param end         The position of the end.
     */
    public void loadMessages(int requestCode, Folder folder, int start, int end) {
        if (checkServiceBound(isBoundToSyncService)) return;

        BaseService.Action action = new BaseService.Action(getReplyMessengerName(),
                requestCode, folder);

        Message message = Message.obtain(null, EmailSyncService.MESSAGE_LOAD_MESSAGES, start, end,
                action);
        message.replyTo = syncServiceReplyMessenger;
        try {
            syncServiceMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
        }
    }

    /**
     * Start a job to load message to cache.
     *
     * @param requestCode                  The unique request code for identify the current action.
     * @param folder                       {@link Folder} object.
     * @param countOfAlreadyLoadedMessages The count of already loaded messages in the folder.
     */
    public void loadNextMessages(int requestCode, Folder folder, int countOfAlreadyLoadedMessages) {
        if (checkServiceBound(isBoundToSyncService)) return;

        onProgressReplyFromServiceReceived(requestCode, R.id.progress_id_start_of_loading_new_messages, null);

        BaseService.Action action = new BaseService.Action(getReplyMessengerName(),
                requestCode, folder);

        Message message = Message.obtain(null, EmailSyncService.MESSAGE_LOAD_NEXT_MESSAGES,
                countOfAlreadyLoadedMessages, 0,
                action);

        message.replyTo = syncServiceReplyMessenger;
        try {
            syncServiceMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
        }
    }

    /**
     * Run update a folders list.
     *
     * @param requestCode    The unique request code for identify the current action.
     * @param isInBackground if true we will run this task using the passive queue, else we will use the active queue.
     */
    public void updateLabels(int requestCode, boolean isInBackground) {
        if (checkServiceBound(isBoundToSyncService)) return;

        BaseService.Action action = new BaseService.Action(getReplyMessengerName(),
                requestCode, null);

        Message message = Message.obtain(null, EmailSyncService.MESSAGE_UPDATE_LABELS,
                isInBackground ? 1 : 0, 0, action);
        message.replyTo = syncServiceReplyMessenger;
        try {
            syncServiceMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
        }
    }

    /**
     * Load the last messages which not exist in the database.
     *
     * @param requestCode           The unique request code for identify the current action.
     * @param currentFolder         {@link Folder} object.
     * @param lastUIDInCache        The UID of the last message of the current folder in the local cache.
     * @param countOfLoadedMessages The UID of the last message of the current folder in the local cache.
     */
    public void refreshMessages(int requestCode, Folder currentFolder, int lastUIDInCache, int countOfLoadedMessages) {
        if (checkServiceBound(isBoundToSyncService)) return;

        BaseService.Action action = new BaseService.Action(getReplyMessengerName(),
                requestCode, currentFolder);

        Message message = Message.obtain(null, EmailSyncService.MESSAGE_REFRESH_MESSAGES,
                lastUIDInCache, countOfLoadedMessages, action);
        message.replyTo = syncServiceReplyMessenger;
        try {
            syncServiceMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
        }
    }

    /**
     * Start a job to load message details.
     *
     * @param requestCode The unique request code for identify the current action.
     * @param folder      {@link Folder} object.
     * @param uid         The {@link com.sun.mail.imap.protocol.UID} of {@link javax.mail.Message ).
     */
    public void loadMessageDetails(int requestCode, Folder folder, int uid) {
        if (checkServiceBound(isBoundToSyncService)) return;

        BaseService.Action action = new BaseService.Action(getReplyMessengerName(),
                requestCode, folder);

        Message message = Message.obtain(null, EmailSyncService.MESSAGE_LOAD_MESSAGE_DETAILS,
                uid, 0, action);

        message.replyTo = syncServiceReplyMessenger;
        try {
            syncServiceMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
        }
    }

    /**
     * Move the message to an another folder.
     *
     * @param requestCode       The unique request code for identify the current action.
     * @param sourcesFolder     The message {@link Folder} object.
     * @param destinationFolder The new destionation {@link Folder} object.
     * @param uid               The {@link com.sun.mail.imap.protocol.UID} of {@link javax.mail
     *                          .Message ).
     */
    public void moveMessage(int requestCode, Folder sourcesFolder,
                            Folder destinationFolder, int uid) {
        if (checkServiceBound(isBoundToSyncService)) return;

        Folder[] folders = new Folder[]{sourcesFolder, destinationFolder};
        BaseService.Action action = new BaseService.Action(getReplyMessengerName(),
                requestCode, folders);

        Message message = Message.obtain(null, EmailSyncService.MESSAGE_MOVE_MESSAGE,
                uid, 0, action);

        message.replyTo = syncServiceReplyMessenger;
        try {
            syncServiceMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
        }
    }

    /**
     * Send a message.
     *
     * @param requestCode         The unique request code for identify the current action.
     * @param outgoingMessageInfo The {@link OutgoingMessageInfo} which contains information about an outgoing
     *                            message.
     */
    public void sendMessage(int requestCode, OutgoingMessageInfo outgoingMessageInfo) {
        if (checkServiceBound(isBoundToSyncService)) return;

        BaseService.Action action = new BaseService.Action(getReplyMessengerName(),
                requestCode, outgoingMessageInfo);

        Message message = Message.obtain(null, EmailSyncService.MESSAGE_SEND_MESSAGE, action);

        message.replyTo = syncServiceReplyMessenger;
        try {
            syncServiceMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
        }
    }
}
