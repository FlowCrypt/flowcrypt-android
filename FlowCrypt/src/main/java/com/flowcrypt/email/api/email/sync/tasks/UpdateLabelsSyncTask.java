/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import android.os.Messenger;

import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.database.dao.source.AccountDao;

import javax.mail.Folder;
import javax.mail.Session;
import javax.mail.Store;

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
    public void runIMAPAction(AccountDao accountDao, Session session, Store store, SyncListener syncListener)
            throws Exception {
        super.runIMAPAction(accountDao, session, store, syncListener);
        Folder[] folders = store.getDefaultFolder().list("*");
        if (syncListener != null) {
            syncListener.onFolderInfoReceived(accountDao, folders, ownerKey, requestCode);
        }
    }
}
