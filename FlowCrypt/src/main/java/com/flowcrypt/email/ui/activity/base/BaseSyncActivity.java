/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base;

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

    public void updateLabels() {
        if (checkBound()) return;

        Message msg = Message.obtain(null, EmailSyncService.MESSAGE_UPDATE_LABELS, 0, 0);
        try {
            messenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void loadMessage(Folder folder) {
        if (checkBound()) return;

        Message msg = Message.obtain(null, EmailSyncService.MESSAGE_LOAD_MESSAGES, 0, 0, folder);
        try {
            messenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private boolean checkBound() {
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
