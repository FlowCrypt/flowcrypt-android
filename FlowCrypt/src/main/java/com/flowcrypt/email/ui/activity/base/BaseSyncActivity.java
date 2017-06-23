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
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.Toast;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.service.EmailSyncService;

/**
 * This class describes a bind to the email sync service logic.
 *
 * @author DenBond7
 *         Date: 16.06.2017
 *         Time: 11:30
 *         E-mail: DenBond7@gmail.com
 */

public abstract class BaseSyncActivity extends BaseActivity implements ServiceConnection {
    /**
     * Messenger for communicating with the service.
     */
    protected Messenger messenger;
    /**
     * Flag indicating whether we have called bind on the service.
     */
    protected boolean isBound;

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
        if (isBound) {
            unbindService(this);
            isBound = false;
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        messenger = new Messenger(service);
        isBound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        messenger = null;
        isBound = false;
    }

    /**
     * Load messages from some folder in some range.
     *
     * @param folder {@link Folder} object.
     * @param start  The position of the start.
     * @param end    The position of the end.
     */
    public void loadMessages(Folder folder, int start, int end) {
        if (checkBound()) return;

        Message msg = Message.obtain(null, EmailSyncService.MESSAGE_LOAD_MESSAGES, start, end,
                folder);
        try {
            messenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Start a job to load message to cache.
     *
     * @param folder                       {@link Folder} object.
     * @param countOfAlreadyLoadedMessages The count of already loaded messages in the folder.
     */
    public void loadNextMessages(Folder folder, int countOfAlreadyLoadedMessages) {
        if (checkBound()) return;

        Message msg = Message.obtain(null, EmailSyncService.MESSAGE_LOAD_NEXT_MESSAGES,
                countOfAlreadyLoadedMessages, 0,
                folder);
        try {
            messenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Run update a folders list.
     */
    public void updateLabels() {
        if (checkBound()) return;

        Message msg = Message.obtain(null, EmailSyncService.MESSAGE_UPDATE_LABELS, 0, 0);
        try {
            messenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load the last messages which not exist in the database.
     *
     * @param currentFolder  {@link Folder} object.
     * @param lastUIDInCache The UID of the last message of the current folder in the local cache.
     */
    public void loadNewMessagesManually(Folder currentFolder, int lastUIDInCache) {
        if (checkBound()) return;

        Message msg = Message.obtain(null, EmailSyncService.MESSAGE_LOAD_NEW_MESSAGES_MANUALLY,
                lastUIDInCache, 0, currentFolder);
        try {
            messenger.send(msg);
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
                Toast.makeText(this, "Activity not connected to the service.", Toast.LENGTH_SHORT)
                        .show();
            }
            return true;
        }
        return false;
    }
}
