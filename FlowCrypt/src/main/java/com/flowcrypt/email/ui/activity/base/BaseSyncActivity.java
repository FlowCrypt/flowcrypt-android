/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo;
import com.flowcrypt.email.api.email.sync.SyncErrorTypes;
import com.flowcrypt.email.service.BaseService;
import com.flowcrypt.email.service.EmailSyncService;
import com.flowcrypt.email.service.JsBackgroundService;
import com.flowcrypt.email.util.exception.ExceptionUtil;

import java.lang.ref.WeakReference;

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
    protected Messenger jsServiceMessenger;
    protected Messenger jsServiceReplyMessenger;

    /**
     * Flag indicating whether we have called bind on the {@link EmailSyncService}.
     */
    protected boolean isBoundToSyncService;
    /**
     * Flag indicating whether we have called bind on the {@link JsBackgroundService}.
     */
    protected boolean isBoundToJsService;

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

    private ServiceConnection serviceConnectionJsService = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Activity connected to " + name.getClassName());
            jsServiceMessenger = new Messenger(service);
            isBoundToJsService = true;

            registerReplyMessenger(JsBackgroundService.MESSAGE_ADD_REPLY_MESSENGER, jsServiceMessenger,
                    jsServiceReplyMessenger);
            onJsServiceConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Activity disconnected from " + name.getClassName());
            jsServiceMessenger = null;
            isBoundToJsService = false;
        }
    };

    public BaseSyncActivity() {
        syncServiceReplyMessenger = new Messenger(new ReplyHandler(this));
        jsServiceReplyMessenger = new Messenger(new ReplyHandler(this));
    }

    /**
     * In this method we can handle response after run some action via {@link BaseService}
     *
     * @param requestCode The unique request code for identifies the some action. Must be unique
     *                    over all project.
     * @param resultCode  The result code of a run action.
     * @param obj         The object which returned from the service.
     */
    public abstract void onReplyFromServiceReceived(int requestCode, int resultCode, Object obj);

    /**
     * Check is a sync enable.
     *
     * @return true - if sync enable, false - otherwise.
     */
    public abstract boolean isSyncEnable();

    /**
     * In this method we can handle a progress state after run some action via {@link BaseService}
     *
     * @param requestCode The unique request code for identifies the some action. Must be unique
     *                    over all project.
     * @param resultCode  The result code of a run action.
     * @param obj         The object which returned from the service.
     */
    public abstract void onProgressReplyFromServiceReceived(int requestCode, int resultCode, Object obj);

    /**
     * In this method we can handle en error after run some action via {@link BaseService}
     *
     * @param requestCode The unique request code for identifies the some action. Must be unique
     *                    over all project.
     * @param errorType   The {@link SyncErrorTypes}.
     * @param e           The exception which occurred.
     */
    public abstract void onErrorFromServiceReceived(int requestCode, int errorType, Exception e);

    public abstract void onSyncServiceConnected();

    public abstract void onJsServiceConnected();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isSyncEnable()) {
            bindToService(EmailSyncService.class, serviceConnectionSyncService);
        }

        bindToService(JsBackgroundService.class, serviceConnectionJsService);
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

        if (isBoundToJsService) {
            if (jsServiceMessenger != null) {
                unregisterReplyMessenger(JsBackgroundService.MESSAGE_REMOVE_REPLY_MESSENGER, jsServiceMessenger,
                        jsServiceReplyMessenger);
            }

            unbindFromService(JsBackgroundService.class, serviceConnectionJsService);
            isBoundToJsService = false;
        }
    }

    public String getReplyMessengerName() {
        return getClass().getSimpleName() + "_" + hashCode();
    }

    /**
     * Send a message with a backup to the key owner.
     *
     * @param requestCode The unique request code for identify the current action.
     * @param accountName The account name.
     */
    public void sendMessageWithPrivateKeyBackup(int requestCode, String accountName) {
        if (checkIsSyncServiceBound()) return;

        EmailSyncService.Action action = new EmailSyncService.Action(getReplyMessengerName(),
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
        if (checkIsSyncServiceBound()) return;
        try {
            EmailSyncService.Action action = new EmailSyncService.Action(getReplyMessengerName(),
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
        if (checkIsSyncServiceBound()) return;
        try {
            EmailSyncService.Action action = new EmailSyncService.Action(getReplyMessengerName(), requestCode, null);

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
        if (checkIsSyncServiceBound()) return;

        EmailSyncService.Action action = new EmailSyncService.Action(getReplyMessengerName(),
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
        if (checkIsSyncServiceBound()) return;

        onProgressReplyFromServiceReceived(requestCode, R.id.progress_id_start_of_loading_new_messages, null);

        EmailSyncService.Action action = new EmailSyncService.Action(getReplyMessengerName(),
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
     * @param requestCode The unique request code for identify the current action.
     */
    public void updateLabels(int requestCode) {
        if (checkIsSyncServiceBound()) return;

        EmailSyncService.Action action = new EmailSyncService.Action(getReplyMessengerName(),
                requestCode, null);

        Message message = Message.obtain(null, EmailSyncService.MESSAGE_UPDATE_LABELS, 0, 0,
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
     * Load the last messages which not exist in the database.
     *
     * @param requestCode           The unique request code for identify the current action.
     * @param currentFolder         {@link Folder} object.
     * @param lastUIDInCache        The UID of the last message of the current folder in the local cache.
     * @param countOfLoadedMessages The UID of the last message of the current folder in the local cache.
     */
    public void refreshMessages(int requestCode, Folder currentFolder, int lastUIDInCache, int countOfLoadedMessages) {
        if (checkIsSyncServiceBound()) return;

        EmailSyncService.Action action = new EmailSyncService.Action(getReplyMessengerName(),
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
        if (checkIsSyncServiceBound()) return;

        EmailSyncService.Action action = new EmailSyncService.Action(getReplyMessengerName(),
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
        if (checkIsSyncServiceBound()) return;

        Folder[] folders = new Folder[]{sourcesFolder, destinationFolder};
        EmailSyncService.Action action = new EmailSyncService.Action(getReplyMessengerName(),
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
        if (checkIsSyncServiceBound()) return;

        EmailSyncService.Action action = new EmailSyncService.Action(getReplyMessengerName(),
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

    /**
     * Check is current {@link Activity} connected to {@link EmailSyncService}
     *
     * @return true if current activity connected to the service, otherwise false.
     */
    protected boolean checkIsSyncServiceBound() {
        if (!isBoundToSyncService) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Activity not connected to the service");
            }
            return true;
        }
        return false;
    }

    private void bindToService(Class<?> cls, ServiceConnection serviceConnection) {
        bindService(new Intent(this, cls), serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "bind to " + cls.getSimpleName());
    }

    /**
     * Disconnect from a service
     */
    private void unbindFromService(Class<?> cls, ServiceConnection serviceConnection) {
        unbindService(serviceConnection);
        Log.d(TAG, "unbind from " + cls.getSimpleName());
    }

    /**
     * Register a reply {@link Messenger} to receive notifications from some service.
     *
     * @param what             A {@link Message#what}}
     * @param serviceMessenger A service {@link Messenger}
     * @param replyToMessenger A reply to {@link Messenger}
     */
    private void registerReplyMessenger(int what, Messenger serviceMessenger, Messenger replyToMessenger) {
        EmailSyncService.Action action = new EmailSyncService.Action(getReplyMessengerName(), -1, null);

        Message message = Message.obtain(null, what, action);
        message.replyTo = replyToMessenger;
        try {
            serviceMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
        }
    }

    /**
     * Unregister a reply {@link Messenger} from some service.
     *
     * @param what             A {@link Message#what}}
     * @param serviceMessenger A service {@link Messenger}
     * @param replyToMessenger A reply to {@link Messenger}
     */
    private void unregisterReplyMessenger(int what, Messenger serviceMessenger, Messenger replyToMessenger) {
        EmailSyncService.Action action = new EmailSyncService.Action(getReplyMessengerName(), -1, null);

        Message message = Message.obtain(null, what, action);
        message.replyTo = replyToMessenger;
        try {
            serviceMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
        }
    }

    /**
     * The incoming handler realization. This handler will be used to communicate with a service and other Android
     * components.
     */
    private static class ReplyHandler extends Handler {
        private final WeakReference<BaseSyncActivity> baseSyncActivityWeakReference;

        ReplyHandler(BaseSyncActivity baseSyncActivity) {
            this.baseSyncActivityWeakReference = new WeakReference<>(baseSyncActivity);
        }

        @Override
        public void handleMessage(Message message) {
            if (baseSyncActivityWeakReference.get() != null) {
                BaseSyncActivity baseSyncActivity = baseSyncActivityWeakReference.get();
                switch (message.what) {
                    case BaseService.REPLY_OK:
                        baseSyncActivity.onReplyFromServiceReceived(message.arg1, message.arg2, message.obj);
                        break;

                    case BaseService.REPLY_ERROR:
                        Exception exception = null;

                        if (message.obj instanceof Exception) {
                            exception = (Exception) message.obj;
                        }

                        baseSyncActivity.onErrorFromServiceReceived(message.arg1, message.arg2, exception);
                        break;

                    case BaseService.REPLY_ACTION_PROGRESS:
                        baseSyncActivity.onProgressReplyFromServiceReceived(message.arg1, message.arg2, message.obj);
                        break;
                }
            }
        }
    }
}
