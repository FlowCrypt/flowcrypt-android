/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync;

import android.util.Log;

import com.flowcrypt.email.api.email.model.OutgoingMessageInfo;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.api.email.sync.tasks.LoadMessageDetailsSyncTask;
import com.flowcrypt.email.api.email.sync.tasks.LoadMessagesSyncTask;
import com.flowcrypt.email.api.email.sync.tasks.LoadMessagesToCacheSyncTask;
import com.flowcrypt.email.api.email.sync.tasks.LoadNewMessagesSyncTask;
import com.flowcrypt.email.api.email.sync.tasks.LoadPrivateKeysFromEmailBackupSyncTask;
import com.flowcrypt.email.api.email.sync.tasks.MoveMessagesSyncTask;
import com.flowcrypt.email.api.email.sync.tasks.SendMessageSyncTask;
import com.flowcrypt.email.api.email.sync.tasks.SendMessageWithBackupToKeyOwnerSynsTask;
import com.flowcrypt.email.api.email.sync.tasks.SyncTask;
import com.flowcrypt.email.api.email.sync.tasks.UpdateLabelsSyncTask;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.google.android.gms.auth.GoogleAuthException;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

/**
 * This class describes a logic of work with {@link Store} for the single account. Via
 * this class we can retrieve a new information from the server and send a data to the server.
 * Here we open a new connection to the {@link Store} and keep it alive. This class does
 * all job to communicate with IMAP server.
 *
 * @author DenBond7
 *         Date: 14.06.2017
 *         Time: 10:31
 *         E-mail: DenBond7@gmail.com
 */

public class EmailSyncManager {
    private static final String TAG = EmailSyncManager.class.getSimpleName();

    private BlockingQueue<SyncTask> syncTaskBlockingQueue;
    private ExecutorService executorService;
    private Future<?> syncTaskRunnableFuture;

    /**
     * This fields created as volatile because will be used in different threads.
     */
    private volatile SyncListener syncListener;
    private volatile Session session;
    private volatile Store store;
    private volatile AccountDao accountDao;
    private boolean isNeedToResetConnection;

    public EmailSyncManager(AccountDao accountDao) {
        this.accountDao = accountDao;
        this.syncTaskBlockingQueue = new LinkedBlockingQueue<>();
        this.executorService = Executors.newSingleThreadExecutor();
        updateLabels(null, 0);
    }

    /**
     * Start a synchronization.
     *
     * @param isNeedReset true if need a reconnect, false otherwise.
     */
    public void beginSync(boolean isNeedReset) {
        Log.d(TAG, "beginSync | isNeedReset = " + isNeedReset);
        if (isNeedReset) {
            resetSync();
        }

        if (!isSyncThreadAlreadyWork()) {
            syncTaskRunnableFuture = executorService.submit(new SyncTaskRunnable());
        }
    }

    /**
     * Check a sync thread state.
     *
     * @return true if already work, otherwise false.
     */
    public boolean isSyncThreadAlreadyWork() {
        return syncTaskRunnableFuture != null && !syncTaskRunnableFuture.isCancelled() &&
                !syncTaskRunnableFuture.isDone();
    }

    /**
     * Stop a synchronization.
     */
    public void stopSync() {
        resetSync();

        if (executorService != null) {
            executorService.shutdown();
        }
    }

    /**
     * Clear the queue of sync tasks.
     */
    public void cancelAllSyncTask() {
        if (syncTaskBlockingQueue != null) {
            syncTaskBlockingQueue.clear();
        }
    }

    /**
     * Set the {@link SyncListener} for current {@link EmailSyncManager}
     *
     * @param syncListener A new listener.
     */
    public void setSyncListener(SyncListener syncListener) {
        this.syncListener = syncListener;
    }

    /**
     * Run update a folders list.
     *
     * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
     * @param requestCode The unique request code for the reply to {@link android.os.Messenger}.
     */
    public void updateLabels(String ownerKey, int requestCode) {
        try {
            removeOldTasks(UpdateLabelsSyncTask.class);
            syncTaskBlockingQueue.put(new UpdateLabelsSyncTask(ownerKey, requestCode));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add load a messages information task. This method create a new
     * {@link LoadMessagesSyncTask} object and added it to the current synchronization
     * BlockingQueue.
     *
     * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
     * @param requestCode The unique request code for the reply to {@link android.os.Messenger}.
     * @param folderName  A server folder name.
     * @param start       The position of the start.
     * @param end         The position of the end.
     */
    public void loadMessages(String ownerKey, int requestCode, String folderName, int start, int
            end) {
        try {
            syncTaskBlockingQueue.put(new LoadMessagesSyncTask(ownerKey, requestCode, folderName,
                    start, end));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add load a messages information task. This method create a new
     * {@link LoadMessagesSyncTask} object and added it to the current synchronization
     * BlockingQueue.
     *
     * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
     * @param requestCode The unique request code for the reply to {@link android.os.Messenger}.
     * @param folderName  A server folder name.
     * @param uid         The {@link com.sun.mail.imap.protocol.UID} of {@link Message ).
     */
    public void loadMessageDetails(String ownerKey, int requestCode, String folderName, int uid) {
        try {
            removeOldTasks(LoadMessageDetailsSyncTask.class);
            syncTaskBlockingQueue.put(new LoadMessageDetailsSyncTask(ownerKey, requestCode,
                    folderName, uid));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add the task of load information of the next messages. This method create a new
     * {@link LoadMessagesToCacheSyncTask} object and added it to the current synchronization
     * BlockingQueue.
     *
     * @param ownerKey                     The name of the reply to {@link android.os.Messenger}.
     * @param requestCode                  The unique request code for the reply to
     *                                     {@link android.os.Messenger}.
     * @param folderName                   A server folder name.
     * @param countOfAlreadyLoadedMessages The count of already cached messages in the folder.
     */
    public void loadNextMessages(String ownerKey, int requestCode, String folderName, int
            countOfAlreadyLoadedMessages) {
        try {
            syncTaskBlockingQueue.put(new LoadMessagesToCacheSyncTask(ownerKey, requestCode,
                    folderName, countOfAlreadyLoadedMessages));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add load a new messages information task. This method create a new
     * {@link LoadNewMessagesSyncTask} object and added it to the current synchronization
     * BlockingQueue.
     *
     * @param ownerKey       The name of the reply to {@link android.os.Messenger}.
     * @param requestCode    The unique request code for the reply to {@link android.os.Messenger}.
     * @param folderName     A server folder name.
     * @param lastUIDInCache The UID of the last message of the current folder in the local cache.
     */
    public void loadNewMessagesManually(String ownerKey, int requestCode, String folderName, int
            lastUIDInCache) {
        try {
            removeOldTasks(LoadNewMessagesSyncTask.class);
            syncTaskBlockingQueue.put(new LoadNewMessagesSyncTask(ownerKey, requestCode,
                    folderName, lastUIDInCache));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Move the message to an another folder.
     *
     * @param ownerKey              The name of the reply to {@link android.os.Messenger}.
     * @param requestCode           The unique request code for identify the current action.
     * @param sourceFolderName      The source folder name.
     * @param destinationFolderName The destination folder name.
     * @param uid                   The {@link com.sun.mail.imap.protocol.UID} of {@link javax.mail
     *                              .Message ).
     */
    public void moveMessage(String ownerKey, int requestCode, String sourceFolderName, String
            destinationFolderName, int uid) {
        try {
            syncTaskBlockingQueue.put(new MoveMessagesSyncTask(ownerKey, requestCode,
                    sourceFolderName, destinationFolderName, new long[]{uid}));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Move the message to an another folder.
     *
     * @param ownerKey            The name of the reply to {@link android.os.Messenger}.
     * @param requestCode         The unique request code for identify the current action.
     * @param outgoingMessageInfo The {@link OutgoingMessageInfo} which contains information about an outgoing
     *                            message.
     */
    public void sendMessage(String ownerKey, int requestCode, OutgoingMessageInfo outgoingMessageInfo) {
        try {
            syncTaskBlockingQueue.put(new SendMessageSyncTask(ownerKey, requestCode, outgoingMessageInfo));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load the private keys from the INBOX folder.
     *
     * @param ownerKey         The name of the reply to {@link android.os.Messenger}.
     * @param requestCode      The unique request code for identify the current action.
     * @param searchTermString The search phrase.
     */
    public void loadPrivateKeys(String ownerKey, int requestCode, String searchTermString) {
        try {
            syncTaskBlockingQueue.put(new LoadPrivateKeysFromEmailBackupSyncTask(searchTermString,
                    ownerKey, requestCode));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send a message with a backup to the key owner.
     *
     * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
     * @param requestCode The unique request code for identify the current action.
     * @param accountName The account name.
     */
    public void sendMessageWithBackup(String ownerKey, int requestCode, String accountName) {
        try {
            syncTaskBlockingQueue.put(new SendMessageWithBackupToKeyOwnerSynsTask(ownerKey,
                    requestCode, accountName));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public AccountDao getAccountDao() {
        return accountDao;
    }

    public void setAccount(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

    /**
     * Reset a synchronization.
     */
    private void resetSync() {
        cancelAllSyncTask();

        if (syncTaskRunnableFuture != null) {
            syncTaskRunnableFuture.cancel(true);
        }

        isNeedToResetConnection = true;
    }

    /**
     * Remove the old tasks from the queue of synchronization.
     *
     * @param cls The task type.
     */
    private void removeOldTasks(Class<?> cls) {
        Iterator<?> syncTaskBlockingQueueIterator = syncTaskBlockingQueue.iterator();
        while (syncTaskBlockingQueueIterator.hasNext()) {
            if (cls.isInstance(syncTaskBlockingQueueIterator.next())) {
                syncTaskBlockingQueueIterator.remove();
            }
        }
    }

    private class SyncTaskRunnable implements Runnable {
        private final String TAG = SyncTaskRunnable.class.getSimpleName();

        @Override
        public void run() {
            Log.d(TAG, "SyncTaskRunnable run");
            Thread.currentThread().setName(SyncTaskRunnable.class.getCanonicalName());
            while (!Thread.interrupted()) {
                try {
                    Log.d(TAG, "SyncTaskBlockingQueue size = " + syncTaskBlockingQueue.size());
                    SyncTask syncTask = syncTaskBlockingQueue.take();

                    if (syncTask != null) {
                        try {
                            if (isNeedToResetConnection) {
                                isNeedToResetConnection = false;
                                if (store != null) {
                                    store.close();
                                }
                                session = null;
                            }

                            if (!isConnected()) {
                                Log.d(TAG, "Not connected. Start a reconnection ...");
                                openConnectionToStore();
                                Log.d(TAG, "Reconnection done");
                            }

                            Log.d(TAG, "Start a new task = " + syncTask.getClass().getSimpleName());
                            if (syncTask.isUseSMTP()) {
                                syncTask.runSMTPAction(accountDao, session, store, syncListener);
                            } else {
                                syncTask.runIMAPAction(accountDao, store, syncListener);
                            }
                            Log.d(TAG, "The task = " + syncTask.getClass().getSimpleName() + " completed");
                        } catch (Exception e) {
                            e.printStackTrace();
                            syncTask.handleException(accountDao, e, syncListener);
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            try {
                store.close();
            } catch (MessagingException e) {
                e.printStackTrace();
                Log.d(TAG, "This exception occurred when we try disconnect from the GMAIL store.");
            }

            Log.d(TAG, "SyncTaskRunnable stop");
        }

        private void openConnectionToStore() throws IOException,
                GoogleAuthException, MessagingException {
            session = OpenStoreHelper.getSessionForAccountDao(accountDao);
            store = OpenStoreHelper.openAndConnectToStore(syncListener.getContext(), accountDao, session);
        }

        /**
         * Check available connection to the store.
         * Must be called from non-main thread.
         *
         * @return trus if connected, false otherwise.
         */
        private boolean isConnected() {
            return store != null && store.isConnected();
        }
    }
}
