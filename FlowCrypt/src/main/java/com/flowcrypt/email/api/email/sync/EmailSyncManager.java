/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
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
import com.flowcrypt.email.api.email.sync.tasks.LoadPrivateKeysFromEmailBackupSyncTask;
import com.flowcrypt.email.api.email.sync.tasks.MoveMessagesSyncTask;
import com.flowcrypt.email.api.email.sync.tasks.RefreshMessagesSyncTask;
import com.flowcrypt.email.api.email.sync.tasks.SendMessageSyncTask;
import com.flowcrypt.email.api.email.sync.tasks.SendMessageWithBackupToKeyOwnerSynsTask;
import com.flowcrypt.email.api.email.sync.tasks.SyncTask;
import com.flowcrypt.email.api.email.sync.tasks.UpdateLabelsSyncTask;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.google.android.gms.auth.GoogleAuthException;

import org.acra.ACRA;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

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
    private static final int MAX_THREADS_COUNT = 3;
    private static final String TAG = EmailSyncManager.class.getSimpleName();

    private BlockingQueue<SyncTask> activeSyncTaskBlockingQueue;
    private BlockingQueue<SyncTask> passiveSyncTaskBlockingQueue;
    private ExecutorService executorService;
    private Future<?> activeSyncTaskRunnableFuture;
    private Future<?> passiveSyncTaskRunnableFuture;

    /**
     * This fields created as volatile because will be used in different threads.
     */
    private volatile SyncListener syncListener;
    private volatile AccountDao accountDao;

    public EmailSyncManager(AccountDao accountDao) {
        this.accountDao = accountDao;
        this.activeSyncTaskBlockingQueue = new LinkedBlockingQueue<>();
        this.passiveSyncTaskBlockingQueue = new LinkedBlockingQueue<>();
        this.executorService = Executors.newFixedThreadPool(MAX_THREADS_COUNT);

        updateLabels(null, 0, activeSyncTaskBlockingQueue);
    }

    /**
     * Start a synchronization.
     *
     * @param isResetNeeded true if need a reconnect, false otherwise.
     */
    public void beginSync(boolean isResetNeeded) {
        Log.d(TAG, "beginSync | isResetNeeded = " + isResetNeeded);
        if (isResetNeeded) {
            resetSync();
            updateLabels(null, 0, activeSyncTaskBlockingQueue);
        }

        if (!isThreadAlreadyWork(activeSyncTaskRunnableFuture)) {
            activeSyncTaskRunnableFuture = executorService.submit(new ActiveSyncTaskRunnable());
        }

        if (!isThreadAlreadyWork(passiveSyncTaskRunnableFuture)) {
            passiveSyncTaskRunnableFuture = executorService.submit(new PassiveSyncTaskRunnable());
        }
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
        if (activeSyncTaskBlockingQueue != null) {
            activeSyncTaskBlockingQueue.clear();
        }

        if (passiveSyncTaskBlockingQueue != null) {
            passiveSyncTaskBlockingQueue.clear();
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
        updateLabels(ownerKey, requestCode, passiveSyncTaskBlockingQueue);
    }

    /**
     * Run update a folders list.
     *
     * @param ownerKey              The name of the reply to {@link android.os.Messenger}.
     * @param requestCode           The unique request code for the reply to {@link android.os.Messenger}.
     * @param syncTaskBlockingQueue The queue where {@link UpdateLabelsSyncTask} will be run.
     */
    public void updateLabels(String ownerKey, int requestCode, BlockingQueue<SyncTask> syncTaskBlockingQueue) {
        try {
            removeOldTasksFromBlockingQueue(UpdateLabelsSyncTask.class, syncTaskBlockingQueue);
            syncTaskBlockingQueue.put(new UpdateLabelsSyncTask(ownerKey, requestCode));
        } catch (InterruptedException e) {
            e.printStackTrace();
            if (ACRA.isInitialised()) {
                ACRA.getErrorReporter().handleException(e);
            }
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
            activeSyncTaskBlockingQueue.put(new LoadMessagesSyncTask(ownerKey, requestCode, folderName,
                    start, end));
        } catch (InterruptedException e) {
            e.printStackTrace();
            if (ACRA.isInitialised()) {
                ACRA.getErrorReporter().handleException(e);
            }
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
            removeOldTasksFromBlockingQueue(LoadMessageDetailsSyncTask.class, activeSyncTaskBlockingQueue);
            activeSyncTaskBlockingQueue.put(new LoadMessageDetailsSyncTask(ownerKey, requestCode,
                    folderName, uid));
        } catch (InterruptedException e) {
            e.printStackTrace();
            if (ACRA.isInitialised()) {
                ACRA.getErrorReporter().handleException(e);
            }
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
            activeSyncTaskBlockingQueue.put(new LoadMessagesToCacheSyncTask(ownerKey, requestCode,
                    folderName, countOfAlreadyLoadedMessages));
        } catch (InterruptedException e) {
            e.printStackTrace();
            if (ACRA.isInitialised()) {
                ACRA.getErrorReporter().handleException(e);
            }
        }
    }

    /**
     * Add load a new messages information task. This method create a new
     * {@link RefreshMessagesSyncTask} object and added it to the current synchronization
     * BlockingQueue.
     *
     * @param ownerKey              The name of the reply to {@link android.os.Messenger}.
     * @param requestCode           The unique request code for the reply to {@link android.os.Messenger}.
     * @param folderName            A server folder name.
     * @param lastUIDInCache        The UID of the last message of the current folder in the local cache.
     * @param countOfLoadedMessages The UID of the last message of the current folder in the local cache.
     */
    public void refreshMessages(String ownerKey, int requestCode, String folderName, int lastUIDInCache,
                                int countOfLoadedMessages) {
        try {
            removeOldTasksFromBlockingQueue(RefreshMessagesSyncTask.class, activeSyncTaskBlockingQueue);
            activeSyncTaskBlockingQueue.put(new RefreshMessagesSyncTask(ownerKey, requestCode,
                    folderName, lastUIDInCache, countOfLoadedMessages));
        } catch (InterruptedException e) {
            e.printStackTrace();
            if (ACRA.isInitialised()) {
                ACRA.getErrorReporter().handleException(e);
            }
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
            activeSyncTaskBlockingQueue.put(new MoveMessagesSyncTask(ownerKey, requestCode,
                    sourceFolderName, destinationFolderName, new long[]{uid}));
        } catch (InterruptedException e) {
            e.printStackTrace();
            if (ACRA.isInitialised()) {
                ACRA.getErrorReporter().handleException(e);
            }
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
            activeSyncTaskBlockingQueue.put(new SendMessageSyncTask(ownerKey, requestCode, outgoingMessageInfo));
        } catch (InterruptedException e) {
            e.printStackTrace();
            if (ACRA.isInitialised()) {
                ACRA.getErrorReporter().handleException(e);
            }
        }
    }

    /**
     * Load the private keys from the INBOX folder.
     *
     * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
     * @param requestCode The unique request code for identify the current action.
     */
    public void loadPrivateKeys(String ownerKey, int requestCode) {
        try {
            activeSyncTaskBlockingQueue.put(new LoadPrivateKeysFromEmailBackupSyncTask(ownerKey, requestCode));
        } catch (InterruptedException e) {
            e.printStackTrace();
            if (ACRA.isInitialised()) {
                ACRA.getErrorReporter().handleException(e);
            }
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
            activeSyncTaskBlockingQueue.put(new SendMessageWithBackupToKeyOwnerSynsTask(ownerKey,
                    requestCode, accountName));
        } catch (InterruptedException e) {
            e.printStackTrace();
            if (ACRA.isInitialised()) {
                ACRA.getErrorReporter().handleException(e);
            }
        }
    }

    public AccountDao getAccountDao() {
        return accountDao;
    }

    public void switchAccount(AccountDao accountDao) {
        this.accountDao = accountDao;
        beginSync(true);
    }

    /**
     * Check a sync thread state.
     *
     * @return true if already work, otherwise false.
     */
    private boolean isThreadAlreadyWork(Future<?> future) {
        return future != null && !future.isCancelled() && !future.isDone();
    }

    /**
     * Reset a synchronization.
     */
    private void resetSync() {
        cancelAllSyncTask();

        if (activeSyncTaskRunnableFuture != null) {
            activeSyncTaskRunnableFuture.cancel(true);
        }

        if (passiveSyncTaskRunnableFuture != null) {
            passiveSyncTaskRunnableFuture.cancel(true);
        }
    }

    /**
     * Remove the old tasks from the queue of synchronization.
     *
     * @param cls                   The task type.
     * @param syncTaskBlockingQueue The queue of the tasks.
     */
    private void removeOldTasksFromBlockingQueue(Class<?> cls, BlockingQueue<SyncTask> syncTaskBlockingQueue) {
        Iterator<?> syncTaskBlockingQueueIterator = syncTaskBlockingQueue.iterator();
        while (syncTaskBlockingQueueIterator.hasNext()) {
            if (cls.isInstance(syncTaskBlockingQueueIterator.next())) {
                syncTaskBlockingQueueIterator.remove();
            }
        }
    }

    private abstract class BaseSyncRunnable implements Runnable {
        protected final String TAG;

        protected Session session;
        protected Store store;

        BaseSyncRunnable() {
            TAG = getClass().getSimpleName();
        }

        void resetConnectionIfNeed() throws MessagingException {
            if (store != null && accountDao != null) {
                store.getURLName().getUsername();
                if (!store.getURLName().getUsername().equalsIgnoreCase(accountDao.getAuthCredentials().getUsername())) {
                    Log.d(TAG, "Connection was reset!");
                    if (store != null) {
                        store.close();
                    }
                    session = null;
                }
            }
        }

        void closeConnection() {
            try {
                if (store != null) {
                    store.close();
                }
            } catch (MessagingException e) {
                e.printStackTrace();
                if (ACRA.isInitialised()) {
                    ACRA.getErrorReporter().handleException(e);
                }
                Log.d(TAG, "This exception occurred when we try disconnect from the GMAIL store.");
            }
        }

        void openConnectionToStore() throws IOException,
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
        boolean isConnected() {
            return store != null && store.isConnected();
        }

        /**
         * Run the incoming {@link SyncTask}
         *
         * @param syncTask The incoming {@link SyncTask}
         */
        void runSyncTask(SyncTask syncTask) {
            try {
                resetConnectionIfNeed();

                if (!isConnected()) {
                    Log.d(TAG, "Not connected. Start a reconnection ...");
                    openConnectionToStore();
                    Log.d(TAG, "Reconnection done");
                }

                Log.d(TAG, "Start a new task = " + syncTask.getClass().getSimpleName()
                        + " for store " + store.toString());

                if (syncTask.isUseSMTP()) {
                    syncTask.runSMTPAction(accountDao, session, store, syncListener);
                } else {
                    syncTask.runIMAPAction(accountDao, store, syncListener);
                }
                Log.d(TAG, "The task = " + syncTask.getClass().getSimpleName() + " completed");
            } catch (Exception e) {
                e.printStackTrace();
                if (ACRA.isInitialised()) {
                    ACRA.getErrorReporter().handleException(e);
                }
                syncTask.handleException(accountDao, e, syncListener);
            }
        }
    }

    private class PassiveSyncTaskRunnable extends BaseSyncRunnable {
        private static final int TIMEOUT_WAIT_NEXT_TASK = 30;

        @Override
        public void run() {
            Log.d(TAG, " run!");
            Thread.currentThread().setName(getClass().getSimpleName());

            while (!Thread.interrupted()) {
                try {
                    Log.d(TAG, "PassiveSyncTaskBlockingQueue size = " + passiveSyncTaskBlockingQueue.size());
                    SyncTask syncTask = passiveSyncTaskBlockingQueue.poll(TIMEOUT_WAIT_NEXT_TASK, TimeUnit.SECONDS);

                    if (syncTask == null) {
                        closeConnection();
                        Log.d(TAG, "Disconnected. Wait new tasks.");
                        syncTask = passiveSyncTaskBlockingQueue.take();
                    }

                    runSyncTask(syncTask);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            closeConnection();
            Log.d(TAG, " stopped!");
        }
    }

    private class ActiveSyncTaskRunnable extends BaseSyncRunnable {
        @Override
        public void run() {
            Log.d(TAG, " run!");
            Thread.currentThread().setName(getClass().getSimpleName());
            while (!Thread.interrupted()) {
                try {
                    Log.d(TAG, "ActiveSyncTaskBlockingQueue size = " + activeSyncTaskBlockingQueue.size());
                    SyncTask syncTask = activeSyncTaskBlockingQueue.take();

                    if (syncTask != null) {
                        runSyncTask(syncTask);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    if (ACRA.isInitialised()) {
                        ACRA.getErrorReporter().handleException(e);
                    }
                }
            }

            closeConnection();
            Log.d(TAG, " stopped!");
        }
    }
}
