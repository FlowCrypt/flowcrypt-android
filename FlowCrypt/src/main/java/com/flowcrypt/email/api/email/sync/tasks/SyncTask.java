/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import com.flowcrypt.email.api.email.sync.EmailSyncManager;
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.database.dao.source.AccountDao;

import javax.mail.Session;
import javax.mail.Store;

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
     * @param accountDao   The account information which will be used of connection.
     * @param session      The {@link Session} object.
     * @param store        The connected and opened {@link Store} object.
     * @param syncListener The listener of synchronization.
     * @throws Exception Different exceptions can be throw when we work with {@link Store}
     */
    void runIMAPAction(AccountDao accountDao, Session session, Store store, SyncListener syncListener) throws Exception;

    /**
     * Run current task in the separate thread.
     *
     * @param accountDao   The account information which will be used of connection.
     * @param session      The {@link Session} object.
     * @param store        The connected and opened {@link Store} object.
     * @param syncListener The listener of synchronization.
     * @throws Exception Different exceptions can be throw when we work with {@link Session} or {@link Store}
     */
    void runSMTPAction(AccountDao accountDao, Session session, Store store, SyncListener syncListener) throws Exception;

    /**
     * This method will be called when an exception occurred while current task running.
     *
     * @param accountDao   The account information which will be used of connection.
     * @param e            The occurred exception.
     * @param syncListener The listener of synchronization.
     */
    void handleException(AccountDao accountDao, Exception e, SyncListener syncListener);

    /**
     * Get the task owner key.
     *
     * @return The task owner key. Can be used to identify who created this task.
     */
    String getOwnerKey();

    /**
     * Get the task request code.
     *
     * @return The task request id. Can be used to identify current task between other.
     */
    int getRequestCode();
}
