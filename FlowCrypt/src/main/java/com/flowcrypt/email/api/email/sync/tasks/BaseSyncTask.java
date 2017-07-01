/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import com.flowcrypt.email.api.email.sync.SyncErrorTypes;
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.sun.mail.gimap.GmailSSLStore;
import com.sun.mail.util.MailConnectException;

import javax.mail.Session;

/**
 * The base realization of {@link SyncTask}.
 *
 * @author DenBond7
 *         Date: 23.06.2017
 *         Time: 15:59
 *         E-mail: DenBond7@gmail.com
 */

abstract class BaseSyncTask implements SyncTask {
    String ownerKey;
    int requestCode;

    /**
     * The base constructor.
     *
     * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
     * @param requestCode The unique request code for the reply to {@link android.os.Messenger}.
     */
    BaseSyncTask(String ownerKey, int requestCode) {
        this.ownerKey = ownerKey;
        this.requestCode = requestCode;
    }

    @Override
    public boolean isUseSMTP() {
        return false;
    }

    @Override
    public void run(GmailSSLStore gmailSSLStore, SyncListener syncListener) throws Exception {

    }

    @Override
    public void run(Session session, String userName, String password, SyncListener syncListener)
            throws Exception {
    }

    @Override
    public void handleException(Exception e, SyncListener syncListener) {
        if (syncListener != null) {
            int errorType;

            if (e instanceof MailConnectException) {
                errorType = SyncErrorTypes.CONNECTION_TO_STORE_IS_LOST;
            } else {
                errorType = SyncErrorTypes.TASK_RUNNING_ERROR;
            }

            syncListener.onError(errorType, e, ownerKey, requestCode);
        }
    }

    public String getOwnerKey() {
        return ownerKey;
    }

    public int getRequestCode() {
        return requestCode;
    }
}
