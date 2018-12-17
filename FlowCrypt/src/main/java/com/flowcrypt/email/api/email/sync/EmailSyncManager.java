/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync;

import android.content.Context;
import android.util.Log;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.FoldersManager;
import com.flowcrypt.email.api.email.LocalFolder;
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
import com.flowcrypt.email.api.email.sync.tasks.SendMessageWithBackupToKeyOwnerSynsTask;
import com.flowcrypt.email.api.email.sync.tasks.SyncTask;
import com.flowcrypt.email.api.email.sync.tasks.UpdateLabelsSyncTask;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource;
import com.flowcrypt.email.jobscheduler.ForwardedAttachmentsDownloaderJobService;
import com.flowcrypt.email.jobscheduler.MessagesSenderJobService;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.flowcrypt.email.util.exception.ManualHandledException;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;
import com.sun.mail.iap.ConnectionException;
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
 * Date: 14.06.2017
 * Time: 10:31
 * E-mail: DenBond7@gmail.com
 */

public class EmailSyncManager {
  private static final int MAX_THREADS_COUNT = 3;
  private static final String TAG = EmailSyncManager.class.getSimpleName();

  private BlockingQueue<SyncTask> activeQueue;
  private BlockingQueue<SyncTask> passiveQueue;
  private ExecutorService executorService;
  private Future<?> activeFuture;
  private Future<?> passiveFuture;

  /**
   * This fields created as volatile because will be used in different threads.
   */

  private volatile Future<?> idleFuture;
  private volatile SyncListener listener;
  private volatile AccountDao account;
  private volatile boolean isIdleSupported = true;

  public EmailSyncManager(AccountDao account) {
    this.account = account;
    this.activeQueue = new LinkedBlockingQueue<>();
    this.passiveQueue = new LinkedBlockingQueue<>();
    this.executorService = Executors.newFixedThreadPool(MAX_THREADS_COUNT);

    updateLabels(null, 0, activeQueue);
    loadContactsInfoIfNeeded();
  }

  /**
   * Start a synchronization.
   *
   * @param isResetNeeded true if need a reconnect, false otherwise.
   */
  public void beginSync(boolean isResetNeeded) {
    Log.d(TAG, "beginSync | isResetNeeded = " + isResetNeeded);
    if (isResetNeeded) {
      cancelAllSyncTasks();
      updateLabels(null, 0, activeQueue);
      loadContactsInfoIfNeeded();
    }

    if (!isThreadAlreadyWorking(activeFuture)) {
      activeFuture = executorService.submit(new ActiveSyncTaskRunnable());
    }

    if (!isThreadAlreadyWorking(passiveFuture)) {
      passiveFuture = executorService.submit(new PassiveSyncTaskRunnable());
    }

    runIdleInboxIfNeeded();

    if (listener != null && listener.getContext() != null) {
      ForwardedAttachmentsDownloaderJobService.schedule(listener.getContext());
      MessagesSenderJobService.schedule(listener.getContext());
    }
  }

  /**
   * Stop a synchronization.
   */
  public void stopSync() {
    cancelAllSyncTasks();

    if (activeFuture != null) {
      activeFuture.cancel(true);
    }

    if (passiveFuture != null) {
      passiveFuture.cancel(true);
    }

    if (idleFuture != null) {
      idleFuture.cancel(true);
    }

    if (executorService != null) {
      executorService.shutdown();
    }
  }

  /**
   * Clear the queue of sync tasks.
   */
  public void cancelAllSyncTasks() {
    if (activeQueue != null) {
      activeQueue.clear();
    }

    if (passiveQueue != null) {
      passiveQueue.clear();
    }
  }

  /**
   * Set the {@link SyncListener} for current {@link EmailSyncManager}
   *
   * @param syncListener A new listener.
   */
  public void setSyncListener(SyncListener syncListener) {
    this.listener = syncListener;
  }

  /**
   * Run update a folders list.
   *
   * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
   * @param requestCode The unique request code for the reply to {@link android.os.Messenger}.
   * @param queue       The queue where {@link UpdateLabelsSyncTask} will be run.
   */
  public void updateLabels(String ownerKey, int requestCode, BlockingQueue<SyncTask> queue) {
    try {
      removeOldTasks(UpdateLabelsSyncTask.class, queue);
      queue.put(new UpdateLabelsSyncTask(ownerKey, requestCode));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * Run update a folders list.
   *
   * @param ownerKey       The name of the reply to {@link android.os.Messenger}.
   * @param requestCode    The unique request code for the reply to {@link android.os.Messenger}.
   * @param isInBackground if true we will run this task using the passive queue, else we will use the active queue.
   */
  public void updateLabels(String ownerKey, int requestCode, boolean isInBackground) {
    updateLabels(ownerKey, requestCode, isInBackground ? passiveQueue : activeQueue);
  }

  /**
   * Add load a messages information task. This method create a new
   * {@link LoadMessagesSyncTask} object and added it to the current synchronization
   * BlockingQueue.
   *
   * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
   * @param requestCode The unique request code for the reply to {@link android.os.Messenger}.
   * @param localFolder      A local implementation of the remote localFolder.
   * @param start       The position of the start.
   * @param end         The position of the end.
   */
  public void loadMsgs(String ownerKey, int requestCode, LocalFolder localFolder, int start, int end) {
    try {
      activeQueue.put(new LoadMessagesSyncTask(ownerKey, requestCode, localFolder, start, end));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * Start loading new messages to the local cache. This method create a new
   * {@link CheckNewMessagesSyncTask} object and add it to the passive BlockingQueue.
   *
   * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
   * @param requestCode The unique request code for the reply to {@link android.os.Messenger}.
   * @param localFolder      A local implementation of the remote localFolder.
   */
  public void loadNewMsgs(String ownerKey, int requestCode, LocalFolder localFolder) {
    try {
      removeOldTasks(CheckNewMessagesSyncTask.class, passiveQueue);
      passiveQueue.put(new CheckNewMessagesSyncTask(ownerKey, requestCode, localFolder));
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
   * @param localFolder      The local implementation of the remote localFolder.
   * @param uid         The {@link com.sun.mail.imap.protocol.UID} of {@link Message ).
   */
  public void loadMsgDetails(String ownerKey, int requestCode, LocalFolder localFolder, int uid) {
    try {
      removeOldTasks(LoadMessageDetailsSyncTask.class, activeQueue);
      activeQueue.put(new LoadMessageDetailsSyncTask(ownerKey, requestCode, localFolder, uid));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * Add the task of load information of the next messages. This method create a new
   * {@link LoadMessagesToCacheSyncTask} object and added it to the current synchronization
   * BlockingQueue.
   *
   * @param ownerKey                   The name of the reply to {@link android.os.Messenger}.
   * @param requestCode                The unique request code for the reply to
   *                                   {@link android.os.Messenger}.
   * @param localFolder                     A local implementation of the remote localFolder.
   * @param alreadyLoadedMsgsCount The count of already cached messages in the localFolder.
   */
  public void loadNextMsgs(String ownerKey, int requestCode, LocalFolder localFolder, int alreadyLoadedMsgsCount) {
    try {
      notifyActionProgress(ownerKey, requestCode, R.id.progress_id_adding_task_to_queue);
      removeOldTasks(LoadMessagesToCacheSyncTask.class, activeQueue);
      activeQueue.put(new LoadMessagesToCacheSyncTask(ownerKey, requestCode, localFolder, alreadyLoadedMsgsCount));

      if (activeQueue.size() != 1) {
        notifyActionProgress(ownerKey, requestCode, R.id.progress_id_queue_is_not_empty);
      } else {
        if (activeFuture.isCancelled() && activeFuture.isDone()) {
          notifyActionProgress(ownerKey, requestCode, R.id.progress_id_thread_is_cancalled_and_done);
        } else {
          if (activeFuture.isDone()) {
            notifyActionProgress(ownerKey, requestCode, R.id.progress_id_thread_is_done);
          }

          if (activeFuture.isCancelled()) {
            notifyActionProgress(ownerKey, requestCode, R.id.progress_id_thread_is_cancalled);
          }
        }
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * Add load a new messages information task. This method create a new
   * {@link RefreshMessagesSyncTask} object and added it to the current synchronization
   * BlockingQueue.
   *
   * @param ownerKey          The name of the reply to {@link android.os.Messenger}.
   * @param requestCode       The unique request code for the reply to {@link android.os.Messenger}.
   * @param localFolder            A local implementation of the remote localFolder.
   * @param isActiveQueueUsed true if the current call will be ran in the active queue, otherwise false.
   */
  public void refreshMsgs(String ownerKey, int requestCode, LocalFolder localFolder, boolean isActiveQueueUsed) {
    try {
      BlockingQueue<SyncTask> syncTaskBlockingQueue = isActiveQueueUsed ? activeQueue : passiveQueue;

      removeOldTasks(RefreshMessagesSyncTask.class, syncTaskBlockingQueue);
      syncTaskBlockingQueue.put(new RefreshMessagesSyncTask(ownerKey, requestCode, localFolder));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * Move the message to an another folder.
   *
   * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
   * @param requestCode The unique request code for identify the current action.
   * @param srcFolder   A local implementation of the remote folder which is the source.
   * @param destFolder  A local implementation of the remote folder which is the destination.
   * @param uid         The {@link com.sun.mail.imap.protocol.UID} of {@link javax.mail
   *                    .Message ).
   */
  public void moveMsg(String ownerKey, int requestCode, LocalFolder srcFolder, LocalFolder destFolder, int uid) {
    try {
      activeQueue.put(new MoveMessagesSyncTask(ownerKey, requestCode, srcFolder, destFolder, new long[]{uid}));
    } catch (InterruptedException e) {
      e.printStackTrace();
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
      activeQueue.put(new LoadPrivateKeysFromEmailBackupSyncTask(ownerKey, requestCode));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * Send a message with a backup to the key owner.
   *
   * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
   * @param requestCode The unique request code for identify the current action.
   */
  public void sendMsgWithBackup(String ownerKey, int requestCode) {
    try {
      activeQueue.put(new SendMessageWithBackupToKeyOwnerSynsTask(ownerKey, requestCode));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * Identify encrypted messages.
   *
   * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
   * @param requestCode The unique request code for identify the current action.
   * @param localFolder The local implementation of the remote folder
   */
  public void identifyEncryptedMsgs(String ownerKey, int requestCode, LocalFolder localFolder) {
    try {
      removeOldTasks(CheckIsLoadedMessagesEncryptedSyncTask.class, passiveQueue);
      passiveQueue.put(new CheckIsLoadedMessagesEncryptedSyncTask(ownerKey, requestCode, localFolder));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public AccountDao getAccountDao() {
    return account;
  }

  public void switchAccount(AccountDao account) {
    this.account = account;
    if (account != null) {
      this.isIdleSupported = true;
      beginSync(true);
    } else {
      stopSync();
    }
  }

  /**
   * Add the task of load information of the next searched messages. This method create a new
   * {@link SearchMessagesSyncTask} object and added it to the current synchronization
   * BlockingQueue.
   *
   * @param ownerKey                   The name of the reply to {@link android.os.Messenger}.
   * @param requestCode                The unique request code for the reply to {@link android.os.Messenger}.
   * @param localFolder                     A localFolder where we do a search.
   * @param alreadyLoadedMsgsCount The count of already cached messages in the database.
   */
  public void searchMsgs(String ownerKey, int requestCode, LocalFolder localFolder, int alreadyLoadedMsgsCount) {
    try {
      removeOldTasks(SearchMessagesSyncTask.class, activeQueue);
      activeQueue.put(new SearchMessagesSyncTask(ownerKey, requestCode, localFolder, alreadyLoadedMsgsCount));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * Load contacts info from the SENT folder.
   */
  private void loadContactsInfoIfNeeded() {
    if (account != null && !account.areContactsLoaded()) {
      //we need to update labels before we can use the SENT folder for retrieve contacts
      updateLabels(null, 0, passiveQueue);
      try {
        passiveQueue.put(new LoadContactsSyncTask());
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Run a thread where we will idle INBOX folder.
   */
  private void runIdleInboxIfNeeded() {
    if (isIdleSupported && !isThreadAlreadyWorking(idleFuture)) {
      idleFuture = executorService.submit(new IdleSyncRunnable());
    }
  }

  /**
   * Check a sync thread state.
   *
   * @return true if already work, otherwise false.
   */
  private boolean isThreadAlreadyWorking(Future<?> future) {
    return future != null && !future.isCancelled() && !future.isDone();
  }

  /**
   * Remove the old tasks from the queue of synchronization.
   *
   * @param cls   The task type.
   * @param queue The queue of the tasks.
   */
  private void removeOldTasks(Class<?> cls, BlockingQueue<SyncTask> queue) {
    Iterator<?> iterator = queue.iterator();
    while (iterator.hasNext()) {
      if (cls.isInstance(iterator.next())) {
        iterator.remove();
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
  private void notifyActionProgress(String ownerKey, int requestCode, int resultCode) {
    if (listener != null) {
      listener.onActionProgress(account, ownerKey, requestCode, resultCode);
    }
  }

  private abstract class BaseSyncRunnable implements Runnable {
    protected final String tag;

    protected Session sess;
    protected Store store;

    BaseSyncRunnable() {
      tag = getClass().getSimpleName();
    }

    void resetConnIfNeeded(SyncTask task) throws MessagingException, ManualHandledException {
      if (store != null && account != null) {
        if (account.getAuthCreds() != null) {
          if (!store.getURLName().getUsername().equalsIgnoreCase(account.getAuthCreds().getUsername())) {
            Log.d(tag, "Connection was reset!");

            if (task != null) {
              notifyActionProgress(task.getOwnerKey(), task.getRequestCode(), R.id.progress_id_resetting_connection);
            }

            if (store != null) {
              store.close();
            }
            sess = null;
          }
        } else if (listener != null && listener.getContext() != null) {
          throw new ManualHandledException(listener.getContext().getString(R.string
              .device_not_supported_key_store_error));
        } else {
          throw new NullPointerException("The context is null");
        }
      }
    }

    void closeConn() {
      try {
        if (store != null) {
          store.close();
        }
      } catch (MessagingException e) {
        e.printStackTrace();
        ExceptionUtil.handleError(e);
        Log.d(tag, "This exception occurred when we try disconnect from the store.");
      }
    }

    void openConnToStore() throws IOException, GoogleAuthException, MessagingException {
      patchingSecurityProvider(listener.getContext());
      sess = OpenStoreHelper.getAccountSess(listener.getContext(), account);
      store = OpenStoreHelper.openStore(listener.getContext(), account, sess);
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
     * @param isRetryEnabled true if want to retry a task if it was fail
     * @param task           The incoming {@link SyncTask}
     */
    void runSyncTask(SyncTask task, boolean isRetryEnabled) {
      try {
        notifyActionProgress(task.getOwnerKey(), task.getRequestCode(), R.id.progress_id_running_task);

        resetConnIfNeeded(task);

        if (!isConnected()) {
          Log.d(tag, "Not connected. Start a reconnection ...");
          notifyActionProgress(task.getOwnerKey(), task.getRequestCode(), R.id.progress_id_connecting_to_email_server);
          openConnToStore();
          Log.d(tag, "Reconnection done");
        }

        Log.d(tag, "Start a new task = " + task.getClass().getSimpleName() + " for store " + store.toString());

        if (task.isSMTPRequired()) {
          notifyActionProgress(task.getOwnerKey(), task.getRequestCode(), R.id.progress_id_running_smtp_action);
          task.runSMTPAction(account, sess, store, listener);
        } else {
          notifyActionProgress(task.getOwnerKey(), task.getRequestCode(), R.id.progress_id_running_imap_action);
          task.runIMAPAction(account, sess, store, listener);
        }
        Log.d(tag, "The task = " + task.getClass().getSimpleName() + " completed");
      } catch (Exception e) {
        e.printStackTrace();
        if (e instanceof ConnectionException) {
          if (isRetryEnabled) {
            runSyncTask(task, false);
          } else {
            ExceptionUtil.handleError(e);
            task.handleException(account, e, listener);
          }
        } else {
          ExceptionUtil.handleError(e);
          task.handleException(account, e, listener);
        }
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
      Log.d(tag, " run!");
      Thread.currentThread().setName(getClass().getSimpleName());

      while (!Thread.interrupted()) {
        try {
          Log.d(tag, "PassiveSyncTaskBlockingQueue size = " + passiveQueue.size());
          SyncTask syncTask = passiveQueue.poll(TIMEOUT_WAIT_NEXT_TASK, TimeUnit.SECONDS);

          if (syncTask == null) {
            closeConn();
            Log.d(tag, "Disconnected. Wait new tasks.");
            syncTask = passiveQueue.take();
          }

          runIdleInboxIfNeeded();

          if (account != null) {
            runSyncTask(syncTask, true);
          } else {
            //There is no an active account. Finishing a work.
            break;
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

      closeConn();
      Log.d(tag, " stopped!");
    }
  }

  private class ActiveSyncTaskRunnable extends BaseSyncRunnable {
    @Override
    public void run() {
      Log.d(tag, " run!");
      Thread.currentThread().setName(getClass().getSimpleName());
      while (!Thread.interrupted()) {
        try {
          Log.d(tag, "ActiveSyncTaskBlockingQueue size = " + activeQueue.size());
          SyncTask syncTask = activeQueue.take();

          runIdleInboxIfNeeded();

          if (account != null) {
            runSyncTask(syncTask, true);
          } else {
            //There is no an active account. Finishing a work.
            break;
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

      closeConn();
      Log.d(tag, " stopped!");
    }
  }

  /**
   * This is a thread where we do a sync of some IMAP folder.
   * <p>
   * P.S. Currently we support only "INBOX" folder.
   */
  private class IdleSyncRunnable extends BaseSyncRunnable implements MessageCountListener, MessageChangedListener {
    private LocalFolder localFolder;
    private com.sun.mail.imap.IMAPFolder remoteFolder;
    private MessageDaoSource msgDaoSource;

    IdleSyncRunnable() {
      this.msgDaoSource = new MessageDaoSource();
    }

    @Override
    public void run() {
      Log.d(tag, " run!");
      Thread.currentThread().setName(getClass().getSimpleName());

      FoldersManager foldersManager = FoldersManager.fromDatabase(listener.getContext(), account.getEmail());
      localFolder = foldersManager.findInboxFolder();

      if (localFolder == null) {
        return;
      }

      idle();
      closeConn();

      Log.d(tag, " stopped!");
    }

    @Override
    public void messagesAdded(MessageCountEvent e) {
      Log.d(tag, "messagesAdded: " + e.getMessages().length);
      loadNewMsgs(null, 0, localFolder);
    }

    @Override
    public void messagesRemoved(MessageCountEvent messageCountEvent) {
      Log.d(tag, "messagesRemoved");
      syncFolderState();
    }

    @Override
    public void messageChanged(MessageChangedEvent e) {
      Log.d(tag, "messageChanged");
      Message msg = e.getMessage();
      if (msg != null && e.getMessageChangeType() == MessageChangedEvent.FLAGS_CHANGED) {
        try {
          msgDaoSource.updateLocalMsgFlags(listener.getContext(),
              account.getEmail(),
              localFolder.getFolderAlias(),
              remoteFolder.getUID(msg),
              msg.getFlags());

          if (listener != null) {
            listener.onMsgChanged(account, localFolder, remoteFolder, msg, null, 0);
          }
        } catch (MessagingException msgException) {
          msgException.printStackTrace();
        }
      }
    }

    void idle() {
      try {
        resetConnIfNeeded(null);

        while (!GeneralUtil.isConnected(listener.getContext())) {
          try {
            //wait while a connection will be established
            TimeUnit.MILLISECONDS.sleep(TimeUnit.SECONDS.toMillis(30));
          } catch (InterruptedException interruptedException) {
            interruptedException.printStackTrace();
          }
        }

        if (!isConnected()) {
          Log.d(tag, "Not connected. Start a reconnection ...");
          openConnToStore();
          Log.d(tag, "Reconnection done");
        }

        Log.d(tag, "Start idling for store " + store.toString());

        remoteFolder = (IMAPFolder) store.getFolder(localFolder.getFullName());
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
            Log.d(tag, "IDLE not supported!");
            isIdleSupported = false;
          }
        } else {
          ExceptionUtil.handleError(e);
        }
      }
    }

    private boolean isIdlingAvailable() {
      //here we can have a lot of checks which help us decide can we run idling(wifi, 3G, a battery level and
      // etc.)
      return true;
    }

    private void syncFolderState() {
      refreshMsgs("", 0, localFolder, false);
    }
  }
}
