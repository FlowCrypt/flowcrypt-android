/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync;

import android.content.Context;
import android.util.Log;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.api.email.FoldersManager;
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.api.email.sync.tasks.CheckIsLoadedMessagesEncryptedSyncTask;
import com.flowcrypt.email.api.email.sync.tasks.CheckNewMessagesSyncTask;
import com.flowcrypt.email.api.email.sync.tasks.LoadContactsSyncTask;
import com.flowcrypt.email.api.email.sync.tasks.LoadMessageDetailsSyncTask;
import com.flowcrypt.email.api.email.sync.tasks.LoadMessagesSyncTask;
import com.flowcrypt.email.api.email.sync.tasks.LoadMessagesToCacheSyncTask;
import com.flowcrypt.email.api.email.sync.tasks.LoadPrivateKeysFromEmailBackupSyncTask;
import com.flowcrypt.email.api.email.sync.tasks.MoveMessagesSyncTask;
import com.flowcrypt.email.api.email.sync.tasks.RefreshMessagesSyncTask;
import com.flowcrypt.email.api.email.sync.tasks.SearchMessagesSyncTask;
import com.flowcrypt.email.api.email.sync.tasks.SendMessageSyncTask;
import com.flowcrypt.email.api.email.sync.tasks.SendMessageWithBackupToKeyOwnerSynsTask;
import com.flowcrypt.email.api.email.sync.tasks.SyncTask;
import com.flowcrypt.email.api.email.sync.tasks.UpdateLabelsSyncTask;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.flowcrypt.email.util.exception.ManualHandledException;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.util.MailConnectException;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.mail.FolderClosedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.event.MessageChangedEvent;
import javax.mail.event.MessageChangedListener;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;

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

    private volatile Future<?> idleSyncRunnableFuture;
    private volatile SyncListener syncListener;
    private volatile AccountDao accountDao;
    private volatile boolean isIdleSupport = true;

    public EmailSyncManager(AccountDao accountDao) {
        this.accountDao = accountDao;
        this.activeSyncTaskBlockingQueue = new LinkedBlockingQueue<>();
        this.passiveSyncTaskBlockingQueue = new LinkedBlockingQueue<>();
        this.executorService = Executors.newFixedThreadPool(MAX_THREADS_COUNT);

        updateLabels(null, 0, activeSyncTaskBlockingQueue);
        loadContactsInfoIfNeed();
    }

    /**
     * Start a synchronization.
     *
     * @param isResetNeeded true if need a reconnect, false otherwise.
     */
    public void beginSync(boolean isResetNeeded) {
        Log.d(TAG, "beginSync | isResetNeeded = " + isResetNeeded);
        if (isResetNeeded) {
            cancelAllSyncTask();
            updateLabels(null, 0, activeSyncTaskBlockingQueue);
            loadContactsInfoIfNeed();
        }

        if (!isThreadAlreadyWork(activeSyncTaskRunnableFuture)) {
            activeSyncTaskRunnableFuture = executorService.submit(new ActiveSyncTaskRunnable());
        }

        if (!isThreadAlreadyWork(passiveSyncTaskRunnableFuture)) {
            passiveSyncTaskRunnableFuture = executorService.submit(new PassiveSyncTaskRunnable());
        }

        runIdleInboxIfNeed();
    }

    /**
     * Stop a synchronization.
     */
    public void stopSync() {
        cancelAllSyncTask();

        if (activeSyncTaskRunnableFuture != null) {
            activeSyncTaskRunnableFuture.cancel(true);
        }

        if (passiveSyncTaskRunnableFuture != null) {
            passiveSyncTaskRunnableFuture.cancel(true);
        }

        if (idleSyncRunnableFuture != null) {
            idleSyncRunnableFuture.cancel(true);
        }

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
     * @param ownerKey       The name of the reply to {@link android.os.Messenger}.
     * @param requestCode    The unique request code for the reply to {@link android.os.Messenger}.
     * @param isInBackground if true we will run this task using the passive queue, else we will use the active queue.
     */
    public void updateLabels(String ownerKey, int requestCode, boolean isInBackground) {
        updateLabels(ownerKey, requestCode,
                isInBackground ? passiveSyncTaskBlockingQueue : activeSyncTaskBlockingQueue);
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
            ExceptionUtil.handleError(e);
        }
    }

    /**
     * Load contacts info from the SENT folder.
     */
    public void loadContactsInfoIfNeed() {
        if (accountDao != null && !accountDao.isContactsLoaded()) {
            //we need to update labels before we can use the SENT folder for retrieve contacts
            updateLabels(null, 0, passiveSyncTaskBlockingQueue);
            try {
                passiveSyncTaskBlockingQueue.put(new LoadContactsSyncTask());
            } catch (InterruptedException e) {
                e.printStackTrace();
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
     * @param folder      A local implementation of the remote folder.
     * @param start       The position of the start.
     * @param end         The position of the end.
     */
    public void loadMessages(String ownerKey, int requestCode, Folder folder, int start, int
            end) {
        try {
            activeSyncTaskBlockingQueue.put(new LoadMessagesSyncTask(ownerKey, requestCode, folder,
                    start, end));
        } catch (InterruptedException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
        }
    }

    /**
     * Start loading new messages to the local cache. This method create a new
     * {@link CheckNewMessagesSyncTask} object and add it to the passive BlockingQueue.
     *
     * @param ownerKey      The name of the reply to {@link android.os.Messenger}.
     * @param requestCode   The unique request code for the reply to {@link android.os.Messenger}.
     * @param folder        A local implementation of the remote folder.
     * @param lastCachedUID The last UID in the local database.
     */
    public void loadNewMessages(String ownerKey, int requestCode, Folder folder, int lastCachedUID) {
        try {
            //passiveSyncTaskBlockingQueue.put(new LoadNewMessagesSyncTask(ownerKey, requestCode, folder, messages));
            passiveSyncTaskBlockingQueue.put(new CheckNewMessagesSyncTask(ownerKey, requestCode, folder,
                    lastCachedUID));
        } catch (InterruptedException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
        }
    }

    /**
     * Add load a messages information task. This method create a new
     * {@link LoadMessagesSyncTask} object and added it to the current synchronization
     * BlockingQueue.
     *
     * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
     * @param requestCode The unique request code for the reply to {@link android.os.Messenger}.
     * @param folder      The local implementation of the remote folder.
     * @param uid         The {@link com.sun.mail.imap.protocol.UID} of {@link Message ).
     */
    public void loadMessageDetails(String ownerKey, int requestCode, Folder folder, int uid) {
        try {
            removeOldTasksFromBlockingQueue(LoadMessageDetailsSyncTask.class, activeSyncTaskBlockingQueue);
            activeSyncTaskBlockingQueue.put(new LoadMessageDetailsSyncTask(ownerKey, requestCode,
                    folder, uid));
        } catch (InterruptedException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
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
     * @param folder                       A local implementation of the remote folder.
     * @param countOfAlreadyLoadedMessages The count of already cached messages in the folder.
     */
    public void loadNextMessages(String ownerKey, int requestCode, Folder folder, int
            countOfAlreadyLoadedMessages) {
        try {
            notifyAboutActionProgress(ownerKey, requestCode, R.id.progress_id_adding_task_to_queue);
            activeSyncTaskBlockingQueue.put(new LoadMessagesToCacheSyncTask(ownerKey, requestCode,
                    folder, countOfAlreadyLoadedMessages));

            if (activeSyncTaskBlockingQueue.size() != 1) {
                notifyAboutActionProgress(ownerKey, requestCode, R.id.progress_id_queue_is_not_empty);
            } else {
                if (activeSyncTaskRunnableFuture.isCancelled() && activeSyncTaskRunnableFuture.isDone()) {
                    notifyAboutActionProgress(ownerKey, requestCode, R.id.progress_id_thread_is_cancalled_and_done);
                } else {
                    if (activeSyncTaskRunnableFuture.isDone()) {
                        notifyAboutActionProgress(ownerKey, requestCode, R.id.progress_id_thread_is_done);
                    }

                    if (activeSyncTaskRunnableFuture.isCancelled()) {
                        notifyAboutActionProgress(ownerKey, requestCode, R.id.progress_id_thread_is_cancalled);
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
        }
    }

    /**
     * Add load a new messages information task. This method create a new
     * {@link RefreshMessagesSyncTask} object and added it to the current synchronization
     * BlockingQueue.
     *
     * @param ownerKey              The name of the reply to {@link android.os.Messenger}.
     * @param requestCode           The unique request code for the reply to {@link android.os.Messenger}.
     * @param folder                A local implementation of the remote folder.
     * @param lastUIDInCache        The UID of the last message of the current folder in the local cache.
     * @param countOfLoadedMessages The UID of the last message of the current folder in the local cache.
     * @param isUseActiveQueue      true if the current call will be ran in the active queue, otherwise false.
     */
    public void refreshMessages(String ownerKey, int requestCode, Folder folder, int lastUIDInCache,
                                int countOfLoadedMessages, boolean isUseActiveQueue) {
        try {
            BlockingQueue<SyncTask> syncTaskBlockingQueue = isUseActiveQueue ? activeSyncTaskBlockingQueue :
                    passiveSyncTaskBlockingQueue;

            removeOldTasksFromBlockingQueue(RefreshMessagesSyncTask.class, syncTaskBlockingQueue);
            syncTaskBlockingQueue.put(new RefreshMessagesSyncTask(ownerKey, requestCode,
                    folder, lastUIDInCache, countOfLoadedMessages));
        } catch (InterruptedException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
        }
    }

    /**
     * Move the message to an another folder.
     *
     * @param ownerKey          The name of the reply to {@link android.os.Messenger}.
     * @param requestCode       The unique request code for identify the current action.
     * @param sourceFolder      A local implementation of the remote folder which is the source.
     * @param destinationFolder A local implementation of the remote folder which is the destination.
     * @param uid               The {@link com.sun.mail.imap.protocol.UID} of {@link javax.mail
     *                          .Message ).
     */
    public void moveMessage(String ownerKey, int requestCode, Folder sourceFolder, Folder
            destinationFolder, int uid) {
        try {
            activeSyncTaskBlockingQueue.put(new MoveMessagesSyncTask(ownerKey, requestCode, sourceFolder,
                    destinationFolder, new long[]{uid}));
        } catch (InterruptedException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
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
            ExceptionUtil.handleError(e);
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
            ExceptionUtil.handleError(e);
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
            ExceptionUtil.handleError(e);
        }
    }

    /**
     * Identify encrypted messages.
     *
     * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
     * @param requestCode The unique request code for identify the current action.
     * @param localFolder The local implementation of the remote folder
     */
    public void identifyEncryptedMessages(String ownerKey, int requestCode, Folder localFolder) {
        try {
            removeOldTasksFromBlockingQueue(CheckIsLoadedMessagesEncryptedSyncTask.class, passiveSyncTaskBlockingQueue);
            passiveSyncTaskBlockingQueue.put(new CheckIsLoadedMessagesEncryptedSyncTask(ownerKey,
                    requestCode, localFolder));
        } catch (InterruptedException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
        }
    }

    public AccountDao getAccountDao() {
        return accountDao;
    }

    public void switchAccount(AccountDao accountDao) {
        this.accountDao = accountDao;
        this.isIdleSupport = true;
        beginSync(true);
    }

    /**
     * Add the task of load information of the next searched messages. This method create a new
     * {@link SearchMessagesSyncTask} object and added it to the current synchronization
     * BlockingQueue.
     *
     * @param ownerKey                     The name of the reply to {@link android.os.Messenger}.
     * @param requestCode                  The unique request code for the reply to {@link android.os.Messenger}.
     * @param folder                       A folder where we do a search.
     * @param countOfAlreadyLoadedMessages The count of already cached messages in the database.
     */
    public void searchMessages(String ownerKey, int requestCode, Folder folder, int countOfAlreadyLoadedMessages) {
        try {
            removeOldTasksFromBlockingQueue(SearchMessagesSyncTask.class, activeSyncTaskBlockingQueue);
            activeSyncTaskBlockingQueue.put(new SearchMessagesSyncTask(ownerKey, requestCode,
                    folder, countOfAlreadyLoadedMessages));
        } catch (InterruptedException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
        }
    }

    /**
     * Run a thread where we will idle INBOX folder.
     */
    private void runIdleInboxIfNeed() {
        if (isIdleSupport && !isThreadAlreadyWork(idleSyncRunnableFuture)) {
            idleSyncRunnableFuture = executorService.submit(new IdleSyncRunnable());
        }
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

    /**
     * This method can be used for debugging. Using this method we can identify a progress of some operation.
     *
     * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
     * @param requestCode The unique request code for the reply to {@link android.os.Messenger}.
     * @param resultCode  The unique result code for the reply which identifies the progress of some request.
     */
    private void notifyAboutActionProgress(String ownerKey, int requestCode, int resultCode) {
        if (syncListener != null) {
            syncListener.onActionProgress(accountDao, ownerKey, requestCode, resultCode);
        }
    }

    private abstract class BaseSyncRunnable implements Runnable {
        protected final String TAG;

        protected Session session;
        protected Store store;

        BaseSyncRunnable() {
            TAG = getClass().getSimpleName();
        }

        void resetConnectionIfNeed(SyncTask syncTask) throws MessagingException, ManualHandledException {
            if (store != null && accountDao != null) {
                if (accountDao.getAuthCredentials() != null) {
                    if (!store.getURLName().getUsername().equalsIgnoreCase(accountDao.getAuthCredentials()
                            .getUsername())) {
                        Log.d(TAG, "Connection was reset!");

                        notifyAboutActionProgress(syncTask.getOwnerKey(), syncTask.getRequestCode(),
                                R.id.progress_id_resetting_connection);

                        if (store != null) {
                            store.close();
                        }
                        session = null;
                    }
                } else if (syncListener != null && syncListener.getContext() != null) {
                    throw new ManualHandledException(syncListener.getContext().getString(R.string
                            .device_not_supported_key_store_error));
                } else {
                    throw new NullPointerException("The context is null");
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
                ExceptionUtil.handleError(e);
                Log.d(TAG, "This exception occurred when we try disconnect from the store.");
            }
        }

        void openConnectionToStore() throws IOException,
                GoogleAuthException, MessagingException {
            patchingSecurityProvider(syncListener.getContext());
            session = OpenStoreHelper.getSessionForAccountDao(syncListener.getContext(), accountDao);
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
                notifyAboutActionProgress(syncTask.getOwnerKey(), syncTask.getRequestCode(),
                        R.id.progress_id_running_task);

                resetConnectionIfNeed(syncTask);

                if (!isConnected()) {
                    Log.d(TAG, "Not connected. Start a reconnection ...");
                    notifyAboutActionProgress(syncTask.getOwnerKey(), syncTask.getRequestCode(),
                            R.id.progress_id_connecting_to_email_server);
                    openConnectionToStore();
                    Log.d(TAG, "Reconnection done");
                }

                Log.d(TAG, "Start a new task = " + syncTask.getClass().getSimpleName() + " for store "
                        + store.toString());

                if (syncTask.isUseSMTP()) {
                    notifyAboutActionProgress(syncTask.getOwnerKey(), syncTask.getRequestCode(),
                            R.id.progress_id_running_smtp_action);
                    syncTask.runSMTPAction(accountDao, session, store, syncListener);
                } else {
                    notifyAboutActionProgress(syncTask.getOwnerKey(), syncTask.getRequestCode(),
                            R.id.progress_id_running_imap_action);
                    syncTask.runIMAPAction(accountDao, session, store, syncListener);
                }
                Log.d(TAG, "The task = " + syncTask.getClass().getSimpleName() + " completed");
            } catch (Exception e) {
                e.printStackTrace();
                ExceptionUtil.handleError(e);
                syncTask.handleException(accountDao, e, syncListener);
            }
        }

        /**
         * To update a device's security provider, use the ProviderInstaller class.
         * <p>
         * When you call installIfNeeded(), the ProviderInstaller does the following:
         * <li>If the device's Provider is successfully updated (or is already up-to-date), the method returns
         * normally.</li>
         * <li>If the device's Google Play services library is out of date, the method throws
         * GooglePlayServicesRepairableException. The app can then catch this exception and show the user an
         * appropriate dialog box to update Google Play services.</li>
         * <li>If a non-recoverable error occurs, the method throws GooglePlayServicesNotAvailableException to indicate
         * that it is unable to update the Provider. The app can then catch the exception and choose an appropriate
         * course of action, such as displaying the standard fix-it flow diagram.</li>
         * <p>
         * If installIfNeeded() needs to install a new Provider, this can take anywhere from 30-50 milliseconds (on
         * more recent devices) to 350 ms (on older devices). If the security provider is already up-to-date, the
         * method takes a negligible amount of time.
         * <p>
         * Details here https://developer.android.com/training/articles/security-gms-provider.html#patching
         *
         * @param context Interface to global information about an application environment;
         */
        private void patchingSecurityProvider(Context context) {
            try {
                ProviderInstaller.installIfNeeded(context);
            } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
                e.printStackTrace();
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

                    runIdleInboxIfNeed();

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

                    runIdleInboxIfNeed();

                    if (syncTask != null) {
                        runSyncTask(syncTask);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            closeConnection();
            Log.d(TAG, " stopped!");
        }
    }

    /**
     * This is a thread where we do a sync of some IMAP folder.
     * <p>
     * P.S. Currently we support only "INBOX" folder.
     */
    private class IdleSyncRunnable extends BaseSyncRunnable implements MessageCountListener, MessageChangedListener {
        private Folder localFolder;
        private com.sun.mail.imap.IMAPFolder remoteFolder;
        private MessageDaoSource messageDaoSource;

        IdleSyncRunnable() {
            this.messageDaoSource = new MessageDaoSource();
        }

        @Override
        public void run() {
            Log.d(TAG, " run!");
            Thread.currentThread().setName(getClass().getSimpleName());

            FoldersManager foldersManager = FoldersManager.fromDatabase(syncListener.getContext(),
                    accountDao.getEmail());
            localFolder = foldersManager.findInboxFolder();

            idle();
            closeConnection();

            Log.d(TAG, " stopped!");
        }

        @Override
        public void messagesAdded(MessageCountEvent e) {
            Log.d(TAG, "messagesAdded: " + e.getMessages().length);
            loadNewMessages(null, 0, localFolder, messageDaoSource.getLastUIDOfMessageInLabel(
                    syncListener.getContext(), accountDao.getEmail(), localFolder.getFolderAlias()));
            //loadNewMessages(null, 0, localFolder, e.getMessages());
            //todo-denbond7 Look at https://github.com/javaee/javamail/issues/319
        }

        @Override
        public void messagesRemoved(MessageCountEvent messageCountEvent) {
            Log.d(TAG, "messagesRemoved");
            syncFolderState();
        }

        @Override
        public void messageChanged(MessageChangedEvent e) {
            Log.d(TAG, "messageChanged");
            Message message = e.getMessage();
            if (message != null && e.getMessageChangeType() == MessageChangedEvent.FLAGS_CHANGED) {
                try {
                    messageDaoSource.updateFlagsForLocalMessage(syncListener.getContext(),
                            accountDao.getEmail(),
                            localFolder.getFolderAlias(),
                            remoteFolder.getUID(message),
                            message.getFlags());

                    if (syncListener != null) {
                        syncListener.onMessageChanged(accountDao, localFolder, remoteFolder, message, null, 0);
                    }
                } catch (MessagingException e1) {
                    e1.printStackTrace();
                }
            }
        }

        void idle() {
            try {
                resetConnectionIfNeed();

                while (!GeneralUtil.isInternetConnectionAvailable(syncListener.getContext())) {
                    try {
                        //wait while a connection will be established
                        TimeUnit.MILLISECONDS.sleep(TimeUnit.SECONDS.toMillis(30));
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }

                if (!isConnected()) {
                    Log.d(TAG, "Not connected. Start a reconnection ...");
                    openConnectionToStore();
                    Log.d(TAG, "Reconnection done");
                }

                Log.d(TAG, "Start idling for store " + store.toString());

                remoteFolder = (IMAPFolder) store.getFolder(localFolder.getServerFullFolderName());
                remoteFolder.open(javax.mail.Folder.READ_ONLY);

                syncFolderState();

                remoteFolder.addMessageCountListener(this);
                remoteFolder.addMessageChangedListener(this);

                while (!Thread.interrupted() && isIdlingAvailable()) {
                    remoteFolder.idle();
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (e instanceof FolderClosedException
                        || e instanceof MailConnectException
                        || e instanceof IOException) {
                    idle();
                } else if (e instanceof MessagingException) {
                    if ("IDLE not supported".equals(e.getMessage())) {
                        Log.d(TAG, "IDLE not supported!");
                        isIdleSupport = false;
                    }
                } else {
                    ExceptionUtil.handleError(e);
                }
            }
        }

        void resetConnectionIfNeed() throws MessagingException, ManualHandledException {
            if (store != null && accountDao != null) {
                if (accountDao.getAuthCredentials() != null) {
                    if (!store.getURLName().getUsername().equalsIgnoreCase(accountDao.getAuthCredentials()
                            .getUsername())) {
                        Log.d(TAG, "Connection was reset!");
                        if (store != null) {
                            store.close();
                        }
                        session = null;
                    }
                } else if (syncListener != null && syncListener.getContext() != null) {
                    throw new ManualHandledException(syncListener.getContext().getString(R.string
                            .device_not_supported_key_store_error));
                } else {
                    throw new NullPointerException("The context is null");
                }
            }
        }

        private boolean isIdlingAvailable() {
            //here we can have a lot of checks which help us decide can we run idling(wifi, 3G, a battery level and
            // etc.)
            return true;
        }

        private void syncFolderState() {
            refreshMessages("", 0, localFolder,
                    messageDaoSource.getLastUIDOfMessageInLabel(
                            syncListener.getContext(),
                            accountDao.getEmail(),
                            localFolder.getFolderAlias()),
                    messageDaoSource.getCountOfMessagesForLabel(syncListener.getContext(),
                            accountDao.getEmail(),
                            localFolder.getFolderAlias()), false);
        }
    }
}
