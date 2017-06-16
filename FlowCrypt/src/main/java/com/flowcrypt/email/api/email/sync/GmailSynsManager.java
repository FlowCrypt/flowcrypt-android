/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync;

import android.util.Log;

import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.google.android.gms.auth.GoogleAuthException;
import com.sun.mail.gimap.GmailSSLStore;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.mail.Folder;
import javax.mail.Message;
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

public class GmailSynsManager implements ConnectionListener {
    private static final String TAG = GmailSynsManager.class.getSimpleName();

    private static final GmailSynsManager ourInstance = new GmailSynsManager();
    private static final int MAX_ACTION_THREADS = 3;

    private ExecutorService singleThreadExecutorService;
    private ExecutorService actionsExecutorService;
    private Future<?> connectionTaskFuture;
    private Future<?> disconnectionTaskFuture;
    private GmailSyncListener gmailSyncListener;

    /**
     * This field created as volatile because will be used in different threads.
     */
    private volatile GmailSSLStore gmailSSLStore;

    private GmailSynsManager() {
        this.singleThreadExecutorService = Executors.newSingleThreadExecutor();
        this.actionsExecutorService = Executors.newFixedThreadPool(MAX_ACTION_THREADS);
    }

    /**
     * Get the single instance of {@link GmailSynsManager}.
     *
     * @return
     */
    public static GmailSynsManager getInstance() {
        return ourInstance;
    }

    @Override
    public void opened(ConnectionEvent e) {
        Log.d(TAG, "opened" + e);
    }

    @Override
    public void disconnected(ConnectionEvent e) {
        Log.d(TAG, "disconnected" + e);
    }

    @Override
    public void closed(ConnectionEvent e) {
        Log.d(TAG, "closed" + e);
    }

    /**
     * Start a synchronization.
     */
    public void beginSync() {
        connect(true);
    }

    /**
     * Stop a synchronization.
     */
    public void disconnect() {
        if (!singleThreadExecutorService.isShutdown()) {
            singleThreadExecutorService.execute(new DisconnectTask());
        }
    }

    /**
     * Check a connecting state.
     *
     * @return true if connecting, otherwise false.
     */
    public boolean isConnecting() {
        return connectionTaskFuture != null && !connectionTaskFuture.isCancelled() &&
                !connectionTaskFuture.isDone();
    }

    /**
     * Stop all running jobs.
     */
    public void cancelAllJobs() {
        if (connectionTaskFuture != null) {
            connectionTaskFuture.cancel(true);
        }

        if (disconnectionTaskFuture != null) {
            disconnectionTaskFuture.cancel(true);
        }
    }

    /**
     * Set the {@link GmailSyncListener} for current {@link GmailSynsManager}
     *
     * @param gmailSyncListener A new listener.
     */
    public void setGmailSyncListener(GmailSyncListener gmailSyncListener) {
        this.gmailSyncListener = gmailSyncListener;
    }

    /**
     * Run update a folders list.
     */
    public void updateLabels() {
        actionsExecutorService.submit(new UpdateLabelsTask());
    }

    private void connect(boolean isNeedReset) {
        if (isNeedReset) {
            cancelAllJobs();
            connectionTaskFuture = singleThreadExecutorService.submit(new ConnectionTask());
        } else {
            connectionTaskFuture = singleThreadExecutorService.submit(new ConnectionTask());
        }
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
     * @return trus if connection available, false otherwise.
     */
    private void checkConnection() throws GoogleAuthException, IOException, MessagingException {
        if (!isConnected()) {
            openConnectionToGmailStore();
        }
    }

    private void handleException(int exceptionType, Exception e) {
        if (gmailSyncListener != null) {
            gmailSyncListener.onError(exceptionType, e);
        }
    }

    private String getValidToken() throws IOException, GoogleAuthException {
        if (gmailSyncListener != null) {
            return gmailSyncListener.getValidToken();
        } else
            throw new IllegalArgumentException("You must specify" + GmailSyncListener.class
                    .getSimpleName
                            () + " to use this method");
    }

    private String getEmail() throws IOException, GoogleAuthException {
        if (gmailSyncListener != null) {
            return gmailSyncListener.getEmail();
        } else
            throw new IllegalArgumentException("You must specify" + GmailSyncListener.class
                    .getSimpleName
                            () + " to use this method");
    }

    private void openConnectionToGmailStore() throws IOException,
            GoogleAuthException, MessagingException {
        gmailSSLStore = OpenStoreHelper.openAndConnectToGimapsStore(getValidToken(), getEmail());
        gmailSSLStore.addConnectionListener(GmailSynsManager.this);
    }

    /**
     * This class can bu used for communication with {@link GmailSynsManager}
     */
    public interface GmailSyncListener {
        void onMessageReceived(Message[] messages);

        void onFolderInfoReceived(Folder[] folders);

        void onError(int errorType, Exception e);

        String getValidToken() throws IOException, GoogleAuthException;

        String getEmail();
    }

    private class ConnectionTask implements Runnable {
        @Override
        public void run() {
            Thread.currentThread().setName(ConnectionTask.class.getCanonicalName());
            try {
                if (!isConnected()) {
                    openConnectionToGmailStore();
                }

                updateLabels();

                while (!Thread.interrupted()) {
                    //keep this thread active
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class UpdateLabelsTask implements Runnable {
        @Override
        public void run() {
            Thread.currentThread().setName(UpdateLabelsTask.class.getCanonicalName());
            try {
                checkConnection();
                Folder[] folders = gmailSSLStore.getDefaultFolder().list("*");
                if (gmailSyncListener != null) {
                    gmailSyncListener.onFolderInfoReceived(folders);
                }
            } catch (Exception e) {
                e.printStackTrace();
                handleException(SyncErrorTypes.CONNECTION_TO_STORE_IS_LOST, e);
            }
        }
    }

    private class DisconnectTask implements Runnable {
        @Override
        public void run() {
            Thread.currentThread().setName(DisconnectTask.class.getCanonicalName());
            try {
                if (gmailSSLStore != null && gmailSSLStore.isConnected()) {
                    gmailSSLStore.close();
                }
            } catch (MessagingException e) {
                e.printStackTrace();
                handleException(-1, e);
            }
        }
    }
}
