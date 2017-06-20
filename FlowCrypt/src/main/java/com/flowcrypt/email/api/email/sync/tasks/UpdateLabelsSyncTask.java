/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

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

public class UpdateLabelsSyncTask implements SyncTask {
    @Override
    public void run(GmailSSLStore gmailSSLStore, SyncListener syncListener) throws
            MessagingException {
        Folder[] folders = gmailSSLStore.getDefaultFolder().list("*");
        if (syncListener != null) {
            syncListener.onFolderInfoReceived(folders);
        }
    }
}
