/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync;

import android.util.Log;

import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.api.email.sync.tasks.LoadMessagesSyncTask;
import com.flowcrypt.email.api.email.sync.tasks.SyncTask;
import com.flowcrypt.email.api.email.sync.tasks.UpdateLabelsSyncTask;
import com.google.android.gms.auth.GoogleAuthException;
import com.sun.mail.gimap.GmailSSLStore;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import javax.mail.MessagingException;
import javax.mail.event.ConnectionEvent;
import javax.mail.event.ConnectionListener;

/**
 * This class describes a logic of work with {@link GmailSSLStore} for the single account. Via
 * this class we can retrieve a new information from the server and send a data to the server.
 * Here we open a new connection to the {@link GmailSSLStore} and keep it alive. This class does
 * all job to communicate with IMAP server.
 *
 * @author DenBond7
 *         Date: 14.06.2017
 *         Time: 10:31
 *         E-mail: DenBond7@gmail.com
 */

public class GmailSynsManager {
    private static final String TAG = GmailSynsManager.class.getSimpleName();

    private static final GmailSynsManager ourInstance = new GmailSynsManager();

    private BlockingQueue<SyncTask> syncTaskBlockingQueue;
    private ExecutorService executorService;
    private Future<?> syncTaskFuture;

    /**
     * This fields created as volatile because will be used in different threads.
     */
    private volatile SyncListener syncListener;
    private volatile GmailSSLStore gmailSSLStore;

    private GmailSynsManager() {
        this.syncTaskBlockingQueue = new LinkedBlockingQueue<>();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Get the single instance of {@link GmailSynsManager}.
     *
     * @return The instance of {@link GmailSynsManager}.
     */
    public static GmailSynsManager getInstance() {
        return ourInstance;
    }

    /**
     * Start a synchronization.
     *
     * @param isNeedReset true if need a reconnect, false otherwise.
     */
    public void beginSync(boolean isNeedReset) {
        Log.d(TAG, "beginSync | isNeedReset = " + isNeedReset);
        if (isNeedReset) {
            cancelAllJobs();
            disconnect();
        }

        if (!isSyncThreadAlreadyWork()) {
            syncTaskFuture = executorService.submit(new SyncTaskRunnable());
        }
    }

    /**
     * Check a sync thread state.
     *
     * @return true if already work, otherwise false.
     */
    public boolean isSyncThreadAlreadyWork() {
        return syncTaskFuture != null && !syncTaskFuture.isCancelled() &&
                !syncTaskFuture.isDone();
    }

    /**
     * Stop a synchronization.
     */
    public void disconnect() {
        if (syncTaskFuture != null) {
            syncTaskFuture.cancel(true);
        }
    }

    /**
     * Clear the queue of sync tasks.
     */
    public void cancelAllJobs() {
        syncTaskBlockingQueue.clear();
    }

    /**
     * Set the {@link SyncListener} for current {@link GmailSynsManager}
     *
     * @param syncListener A new listener.
     */
    public void setSyncListener(SyncListener syncListener) {
        this.syncListener = syncListener;
    }

    /**
     * Run update a folders list.
     */
    public void updateLabels() {
        try {
            syncTaskBlockingQueue.put(new UpdateLabelsSyncTask());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void loadMessages(String folderName, int from, int to) {
        try {
            syncTaskBlockingQueue.put(new LoadMessagesSyncTask(folderName, from, to));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class SyncTaskRunnable implements Runnable, ConnectionListener {
        private final String TAG = SyncTaskRunnable.class.getSimpleName();

        @Override
        public void run() {
            Thread.currentThread().setName(SyncTaskRunnable.class.getCanonicalName());
            while (!Thread.interrupted()) {
                try {
                    Log.d(TAG, "SyncTaskBlockingQueue size = " + syncTaskBlockingQueue.size());
                    SyncTask syncTask = syncTaskBlockingQueue.take();

                    if (syncTask != null) {
                        if (!isConnected()) {
                            openConnectionToGmailStore();
                            Log.d(TAG, "Reconnection done");
                        }

                        Log.d(TAG, "Start a new task = " + syncTask.getClass().getSimpleName());
                        syncTask.run(gmailSSLStore, syncListener);
                        Log.d(TAG, "The task = " + syncTask.getClass().getSimpleName()
                                + " completed");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void opened(ConnectionEvent e) {
            Log.d(TAG, "Connection to IMAP opened" + e);
        }

        @Override
        public void disconnected(ConnectionEvent e) {
            Log.d(TAG, "Connection to IMAP disconnected" + e);
        }

        @Override
        public void closed(ConnectionEvent e) {
            Log.d(TAG, "Connection to IMAP closed" + e);
        }

        private void openConnectionToGmailStore() throws IOException,
                GoogleAuthException, MessagingException {
            gmailSSLStore = OpenStoreHelper.openAndConnectToGimapsStore(getValidToken(),
                    getEmail());
            gmailSSLStore.addConnectionListener(this);
        }

        /**
         * Check available connection to the gmail store.
         * Must be called from non-main thread.
         *
         * @return trus if connected, false otherwise.
         */
        private boolean isConnected() {
            return gmailSSLStore != null && gmailSSLStore.isConnected();
        }

        /**
         * Check available connection to the gmail store. If connection does not exists try to
         * reconnect.
         * Must be called from non-main thread.
         *
         * @return true if connection available, false otherwise.
         */
        private void checkConnection() throws GoogleAuthException, IOException, MessagingException {
            if (!isConnected()) {
                openConnectionToGmailStore();
            }
        }

        private void handleException(int exceptionType, Exception e) {
            if (syncListener != null) {
                syncListener.onError(exceptionType, e);
            }
        }

        private String getValidToken() throws IOException, GoogleAuthException {
            if (syncListener != null) {
                return syncListener.getValidToken();
            } else
                throw new IllegalArgumentException("You must specify"
                        + SyncListener.class.getSimpleName() + " to use this method");
        }

        private String getEmail() throws IOException, GoogleAuthException {
            if (syncListener != null) {
                return syncListener.getEmail();
            } else
                throw new IllegalArgumentException("You must specify"
                        + SyncListener.class.getSimpleName() + " to use this method");
        }
    }
}
