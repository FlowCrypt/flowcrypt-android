/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import android.os.Messenger;

import com.flowcrypt.email.api.email.sync.SyncListener;
import com.sun.mail.gimap.GmailSSLStore;

import javax.mail.Folder;
import javax.mail.MessagingException;

/**
 * This task do job of receiving a Gmail labels list.
 *
 * @author DenBond7
 *         Date: 19.06.2017
 *         Time: 13:34
 *         E-mail: DenBond7@gmail.com
 */

public class UpdateLabelsSyncTask extends BaseSyncTask {

    /**
     * The base constructor.
     *
     * @param ownerKey    The name of the reply to {@link Messenger}.
     * @param requestCode The unique request code for the reply to {@link Messenger}.
     */
    public UpdateLabelsSyncTask(String ownerKey, int requestCode) {
        super(ownerKey, requestCode);
    }

    @Override
    public void run(GmailSSLStore gmailSSLStore, SyncListener syncListener) throws
            MessagingException {
        Folder[] folders = gmailSSLStore.getDefaultFolder().list("*");
        if (syncListener != null) {
            syncListener.onFolderInfoReceived(folders, ownerKey, requestCode);
        }
    }
}
