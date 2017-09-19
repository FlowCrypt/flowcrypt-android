/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import com.flowcrypt.email.api.email.sync.EmailSyncManager;
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.sun.mail.gimap.GmailSSLStore;

import javax.mail.Session;

/**
 * The sync task which will be run by {@link EmailSyncManager}
 *
 * @author DenBond7
 *         Date: 16.06.2017
 *         Time: 16:12
 *         E-mail: DenBond7@gmail.com
 */

public interface SyncTask {
    /**
     * Check is this task use the SMTP protocol to communicate with a server.
     *
     * @return true if wiil be used SMTP, false if will be used IMAP.
     */
    boolean isUseSMTP();

    /**
     * Run current task in the separate thread.
     *
     * @param gmailSSLStore The connected and opened {@link GmailSSLStore} object.
     * @param syncListener  The listener of synchronization.
     * @throws Exception
     */
    void run(GmailSSLStore gmailSSLStore, SyncListener syncListener) throws Exception;

    /**
     * Run current task in the separate thread.
     *
     * @param session      The {@link Session} object.
     * @param userName     The user name which will be used of connection.
     * @param password     The password which will be used of connection.
     * @param syncListener The listener of synchronization.
     * @throws Exception
     */
    void run(Session session, String userName, String password, SyncListener syncListener) throws
            Exception;

    /**
     * This method will be called when an exception occurred while current task running.
     *
     * @param e            The occurred exception.
     * @param syncListener The listener of synchronization.
     */
    void handleException(Exception e, SyncListener syncListener);
}
