/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.service.EmailSyncService;

import java.lang.ref.WeakReference;

/**
 * This class describes a bind to the email sync service logic.
 *
 * @author DenBond7
 *         Date: 16.06.2017
 *         Time: 11:30
 *         E-mail: DenBond7@gmail.com
 */

public abstract class BaseSyncActivity extends BaseActivity implements ServiceConnection {
    private static final String TAG = BaseSyncActivity.class.getSimpleName();
    /**
     * Messenger for communicating with the service.
     */
    protected Messenger syncServiceMessenger;

    protected Messenger replyMessenger;

    /**
     * Flag indicating whether we have called bind on the service.
     */
    protected boolean isBound;

    public BaseSyncActivity() {
        replyMessenger = new Messenger(new ReplyHandler(this));
    }

    /**
     * In this method we can handle response after run some action via {@link EmailSyncService}
     *
     * @param requestCode The unique request code for identifies the some action. Must be unique
     *                    over all project.
     * @param resultCode  The result code of a run action.
     */
    public abstract void onReplyFromSyncServiceReceived(int requestCode, int resultCode);

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to the service
        bindService(new Intent(this, EmailSyncService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        unbindFromService();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        syncServiceMessenger = new Messenger(service);
        isBound = true;

        registerReplyMessenger();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        syncServiceMessenger = null;
        isBound = false;
    }

    public String getReplyMessengerName() {
        return getClass().getSimpleName();
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
        if (checkBound()) return;

        EmailSyncService.Action action = new EmailSyncService.Action(getReplyMessengerName(),
                requestCode, folder);

        Message message = Message.obtain(null, EmailSyncService.MESSAGE_LOAD_MESSAGES, start, end,
                action);
        message.replyTo = replyMessenger;
        try {
            syncServiceMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
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
        if (checkBound()) return;

        EmailSyncService.Action action = new EmailSyncService.Action(getReplyMessengerName(),
                requestCode, folder);

        Message message = Message.obtain(null, EmailSyncService.MESSAGE_LOAD_NEXT_MESSAGES,
                countOfAlreadyLoadedMessages, 0,
                action);

        message.replyTo = replyMessenger;
        try {
            syncServiceMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Run update a folders list.
     *
     * @param requestCode The unique request code for identify the current action.
     */
    public void updateLabels(int requestCode) {
        if (checkBound()) return;

        EmailSyncService.Action action = new EmailSyncService.Action(getReplyMessengerName(),
                requestCode, null);

        Message message = Message.obtain(null, EmailSyncService.MESSAGE_UPDATE_LABELS, 0, 0,
                action);
        message.replyTo = replyMessenger;
        try {
            syncServiceMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load the last messages which not exist in the database.
     *
     * @param requestCode    The unique request code for identify the current action.
     * @param currentFolder  {@link Folder} object.
     * @param lastUIDInCache The UID of the last message of the current folder in the local cache.
     */
    public void loadNewMessagesManually(int requestCode, Folder currentFolder, int lastUIDInCache) {
        if (checkBound()) return;

        EmailSyncService.Action action = new EmailSyncService.Action(getReplyMessengerName(),
                requestCode, currentFolder);

        Message message = Message.obtain(null, EmailSyncService.MESSAGE_LOAD_NEW_MESSAGES_MANUALLY,
                lastUIDInCache, 0, action);
        message.replyTo = replyMessenger;
        try {
            syncServiceMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check is current {@link Activity} connected to {@link EmailSyncService}
     *
     * @return true if current activity connected to the service, otherwise false.
     */
    protected boolean checkBound() {
        if (!isBound) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Activity not connected to the service");
            }
            return true;
        }
        return false;
    }

    /**
     * Disconnect from the {@link EmailSyncService}
     */
    private void unbindFromService() {
        if (isBound) {

            if (syncServiceMessenger != null) {
                unregisterReplyMessenger();
            }

            unbindService(this);
            isBound = false;
        }
    }

    /**
     * Register a reply {@link Messenger} to receive notifications from the
     * {@link EmailSyncService}.
     */
    private void registerReplyMessenger() {
        EmailSyncService.Action action = new EmailSyncService.Action(getReplyMessengerName(),
                -1, null);

        Message message = Message.obtain(null,
                EmailSyncService.MESSAGE_ADD_REPLY_MESSENGER, action);
        message.replyTo = replyMessenger;
        try {
            syncServiceMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Unregister a reply {@link Messenger} from the {@link EmailSyncService}.
     */
    private void unregisterReplyMessenger() {
        EmailSyncService.Action action = new EmailSyncService.Action(getReplyMessengerName(),
                -1, null);

        Message message = Message.obtain(null,
                EmailSyncService.MESSAGE_REMOVE_REPLY_MESSENGER, action);
        message.replyTo = replyMessenger;
        try {
            syncServiceMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * The incoming handler realization. This handler will be used to communicate with current
     * service and other Android components.
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
                    case EmailSyncService.REPLY_WHAT:
                        baseSyncActivity.onReplyFromSyncServiceReceived(message.arg1, message.arg2);
                        break;
                }
            }
        }
    }
}
