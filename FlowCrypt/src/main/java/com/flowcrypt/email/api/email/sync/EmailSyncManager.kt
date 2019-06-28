/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync

import android.content.Context
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper
import com.flowcrypt.email.api.email.sync.tasks.CheckIsLoadedMessagesEncryptedSyncTask
import com.flowcrypt.email.api.email.sync.tasks.CheckNewMessagesSyncTask
import com.flowcrypt.email.api.email.sync.tasks.LoadContactsSyncTask
import com.flowcrypt.email.api.email.sync.tasks.LoadMessageDetailsSyncTask
import com.flowcrypt.email.api.email.sync.tasks.LoadMessagesSyncTask
import com.flowcrypt.email.api.email.sync.tasks.LoadMessagesToCacheSyncTask
import com.flowcrypt.email.api.email.sync.tasks.LoadPrivateKeysFromEmailBackupSyncTask
import com.flowcrypt.email.api.email.sync.tasks.MoveMessagesSyncTask
import com.flowcrypt.email.api.email.sync.tasks.RefreshMessagesSyncTask
import com.flowcrypt.email.api.email.sync.tasks.SearchMessagesSyncTask
import com.flowcrypt.email.api.email.sync.tasks.SendMessageWithBackupToKeyOwnerSynsTask
import com.flowcrypt.email.api.email.sync.tasks.SyncTask
import com.flowcrypt.email.api.email.sync.tasks.UpdateLabelsSyncTask
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource
import com.flowcrypt.email.jobscheduler.ForwardedAttachmentsDownloaderJobService
import com.flowcrypt.email.jobscheduler.MessagesSenderJobService
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.ManualHandledException
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller
import com.sun.mail.iap.ConnectionException
import com.sun.mail.imap.IMAPFolder
import com.sun.mail.util.MailConnectException
import java.io.IOException
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import javax.mail.FolderClosedException
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Session
import javax.mail.Store
import javax.mail.event.MessageChangedEvent
import javax.mail.event.MessageChangedListener
import javax.mail.event.MessageCountEvent
import javax.mail.event.MessageCountListener

/**
 * This class describes a logic of work with [Store] for the single account. Via
 * this class we can retrieve a new information from the server and send a data to the server.
 * Here we open a new connection to the [Store] and keep it alive. This class does
 * all job to communicate with IMAP server.
 *
 * @author DenBond7
 * Date: 14.06.2017
 * Time: 10:31
 * E-mail: DenBond7@gmail.com
 */

class EmailSyncManager(account: AccountDao, listener: SyncListener) {

  private val activeQueue: BlockingQueue<SyncTask>
  private val passiveQueue: BlockingQueue<SyncTask>
  private val executorService: ExecutorService
  private var activeFuture: Future<*>? = null
  private var passiveFuture: Future<*>? = null

  /**
   * This fields created as volatile because will be used in different threads.
   */

  @Volatile
  private var idleFuture: Future<*>? = null
  @Volatile
  private var listener: SyncListener
  @Volatile
  var accountDao: AccountDao
    private set
  @Volatile
  private var isIdleSupported = true

  init {
    this.accountDao = account
    this.listener = listener
    this.activeQueue = LinkedBlockingQueue()
    this.passiveQueue = LinkedBlockingQueue()
    this.executorService = Executors.newFixedThreadPool(MAX_THREADS_COUNT)

    updateLabels("", 0, activeQueue)
    loadContactsInfoIfNeeded()
  }

  /**
   * Start a synchronization.
   *
   * @param isResetNeeded true if need a reconnect, false otherwise.
   */
  fun beginSync(isResetNeeded: Boolean) {
    LogsUtil.d(TAG, "beginSync | isResetNeeded = $isResetNeeded")
    if (isResetNeeded) {
      cancelAllSyncTasks()
      updateLabels("", 0, activeQueue)
      loadContactsInfoIfNeeded()
    }

    if (!isThreadAlreadyWorking(activeFuture)) {
      activeFuture = executorService.submit(ActiveSyncTaskRunnable())
    }

    if (!isThreadAlreadyWorking(passiveFuture)) {
      passiveFuture = executorService.submit(PassiveSyncTaskRunnable())
    }

    runIdleInboxIfNeeded()

    ForwardedAttachmentsDownloaderJobService.schedule(listener.context)
    MessagesSenderJobService.schedule(listener.context)
  }

  /**
   * Stop a synchronization.
   */
  fun stopSync() {
    cancelAllSyncTasks()

    activeFuture?.cancel(true)
    passiveFuture?.cancel(true)
    idleFuture?.cancel(true)
    executorService.shutdown()
  }

  /**
   * Clear the queue of sync tasks.
   */
  fun cancelAllSyncTasks() {
    activeQueue.clear()
    passiveQueue.clear()
  }

  /**
   * Run update a folders list.
   *
   * @param ownerKey    The name of the reply to [android.os.Messenger].
   * @param requestCode The unique request code for the reply to [android.os.Messenger].
   * @param queue       The queue where [UpdateLabelsSyncTask] will be run.
   */
  fun updateLabels(ownerKey: String, requestCode: Int, queue: BlockingQueue<SyncTask>) {
    try {
      removeOldTasks(UpdateLabelsSyncTask::class.java, queue)
      queue.put(UpdateLabelsSyncTask(ownerKey, requestCode))
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }
  }

  /**
   * Run update a folders list.
   *
   * @param ownerKey       The name of the reply to [android.os.Messenger].
   * @param requestCode    The unique request code for the reply to [android.os.Messenger].
   * @param isInBackground if true we will run this task using the passive queue, else we will use the active queue.
   */
  fun updateLabels(ownerKey: String, requestCode: Int, isInBackground: Boolean) {
    updateLabels(ownerKey, requestCode, if (isInBackground) passiveQueue else activeQueue)
  }

  /**
   * Add load a messages information task. This method create a new
   * [LoadMessagesSyncTask] object and added it to the current synchronization
   * BlockingQueue.
   *
   * @param ownerKey    The name of the reply to [android.os.Messenger].
   * @param requestCode The unique request code for the reply to [android.os.Messenger].
   * @param localFolder A local implementation of the remote localFolder.
   * @param start       The position of the start.
   * @param end         The position of the end.
   */
  fun loadMsgs(ownerKey: String, requestCode: Int, localFolder: LocalFolder, start: Int, end: Int) {
    try {
      activeQueue.put(LoadMessagesSyncTask(ownerKey, requestCode, localFolder, start, end))
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }
  }

  /**
   * Start loading new messages to the local cache. This method create a new
   * [CheckNewMessagesSyncTask] object and add it to the passive BlockingQueue.
   *
   * @param ownerKey    The name of the reply to [android.os.Messenger].
   * @param requestCode The unique request code for the reply to [android.os.Messenger].
   * @param localFolder A local implementation of the remote localFolder.
   */
  fun loadNewMsgs(ownerKey: String, requestCode: Int, localFolder: LocalFolder) {
    try {
      removeOldTasks(CheckNewMessagesSyncTask::class.java, passiveQueue)
      passiveQueue.put(CheckNewMessagesSyncTask(ownerKey, requestCode, localFolder))
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }
  }

  /**
   * Add load a messages information task. This method create a new
   * [LoadMessagesSyncTask] object and added it to the current synchronization
   * BlockingQueue.
   *
   * @param ownerKey    The name of the reply to [android.os.Messenger].
   * @param requestCode The unique request code for the reply to [android.os.Messenger].
   * @param localFolder The local implementation of the remote localFolder.
   * @param uid         The [com.sun.mail.imap.protocol.UID] of [).][Message]
   */
  fun loadMsgDetails(ownerKey: String, requestCode: Int, localFolder: LocalFolder, uid: Int) {
    try {
      removeOldTasks(LoadMessageDetailsSyncTask::class.java, activeQueue)
      activeQueue.put(LoadMessageDetailsSyncTask(ownerKey, requestCode, localFolder, uid.toLong()))
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }
  }

  /**
   * Add the task of load information of the next messages. This method create a new
   * [LoadMessagesToCacheSyncTask] object and added it to the current synchronization
   * BlockingQueue.
   *
   * @param ownerKey               The name of the reply to [android.os.Messenger].
   * @param requestCode            The unique request code for the reply to
   * [android.os.Messenger].
   * @param localFolder            A local implementation of the remote localFolder.
   * @param alreadyLoadedMsgsCount The count of already cached messages in the localFolder.
   */
  fun loadNextMsgs(ownerKey: String, requestCode: Int, localFolder: LocalFolder, alreadyLoadedMsgsCount: Int) {
    try {
      notifyActionProgress(ownerKey, requestCode, R.id.progress_id_adding_task_to_queue)
      removeOldTasks(LoadMessagesToCacheSyncTask::class.java, activeQueue)
      activeQueue.put(LoadMessagesToCacheSyncTask(ownerKey, requestCode, localFolder, alreadyLoadedMsgsCount))

      if (activeQueue.size != 1) {
        notifyActionProgress(ownerKey, requestCode, R.id.progress_id_queue_is_not_empty)
      } else {
        val future = activeFuture ?: return

        if (future.isCancelled && future.isDone) {
          notifyActionProgress(ownerKey, requestCode, R.id.progress_id_thread_is_cancalled_and_done)
        } else {
          if (future.isDone) {
            notifyActionProgress(ownerKey, requestCode, R.id.progress_id_thread_is_done)
          }

          if (future.isCancelled) {
            notifyActionProgress(ownerKey, requestCode, R.id.progress_id_thread_is_cancalled)
          }
        }
      }
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }
  }

  /**
   * Add load a new messages information task. This method create a new
   * [RefreshMessagesSyncTask] object and added it to the current synchronization
   * BlockingQueue.
   *
   * @param ownerKey          The name of the reply to [android.os.Messenger].
   * @param requestCode       The unique request code for the reply to [android.os.Messenger].
   * @param localFolder       A local implementation of the remote localFolder.
   * @param isActiveQueueUsed true if the current call will be ran in the active queue, otherwise false.
   */
  fun refreshMsgs(ownerKey: String, requestCode: Int, localFolder: LocalFolder, isActiveQueueUsed: Boolean) {
    try {
      val syncTaskBlockingQueue = if (isActiveQueueUsed) activeQueue else passiveQueue

      removeOldTasks(RefreshMessagesSyncTask::class.java, syncTaskBlockingQueue)
      syncTaskBlockingQueue.put(RefreshMessagesSyncTask(ownerKey, requestCode, localFolder))
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }
  }

  /**
   * Move the message to an another folder.
   *
   * @param ownerKey    The name of the reply to [android.os.Messenger].
   * @param requestCode The unique request code for identify the current action.
   * @param srcFolder   A local implementation of the remote folder which is the source.
   * @param destFolder  A local implementation of the remote folder which is the destination.
   * @param uid         The [com.sun.mail.imap.protocol.UID] of [                    .Message ).][javax.mail]
   */
  fun moveMsg(ownerKey: String, requestCode: Int, srcFolder: LocalFolder, destFolder: LocalFolder, uid: Int) {
    try {
      activeQueue.put(MoveMessagesSyncTask(ownerKey, requestCode, srcFolder, destFolder, longArrayOf(uid.toLong())))
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }
  }

  /**
   * Load the private keys from the INBOX folder.
   *
   * @param ownerKey    The name of the reply to [android.os.Messenger].
   * @param requestCode The unique request code for identify the current action.
   */
  fun loadPrivateKeys(ownerKey: String, requestCode: Int) {
    try {
      activeQueue.put(LoadPrivateKeysFromEmailBackupSyncTask(ownerKey, requestCode))
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }
  }

  /**
   * Send a message with a backup to the key owner.
   *
   * @param ownerKey    The name of the reply to [android.os.Messenger].
   * @param requestCode The unique request code for identify the current action.
   */
  fun sendMsgWithBackup(ownerKey: String, requestCode: Int) {
    try {
      activeQueue.put(SendMessageWithBackupToKeyOwnerSynsTask(ownerKey, requestCode))
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }
  }

  /**
   * Identify encrypted messages.
   *
   * @param ownerKey    The name of the reply to [android.os.Messenger].
   * @param requestCode The unique request code for identify the current action.
   * @param localFolder The local implementation of the remote folder
   */
  fun identifyEncryptedMsgs(ownerKey: String, requestCode: Int, localFolder: LocalFolder) {
    try {
      removeOldTasks(CheckIsLoadedMessagesEncryptedSyncTask::class.java, passiveQueue)
      passiveQueue.put(CheckIsLoadedMessagesEncryptedSyncTask(ownerKey, requestCode, localFolder))
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }
  }

  fun switchAccount(account: AccountDao) {
    this.accountDao = account
    this.isIdleSupported = true
    beginSync(true)
  }

  /**
   * Add the task of load information of the next searched messages. This method create a new
   * [SearchMessagesSyncTask] object and added it to the current synchronization
   * BlockingQueue.
   *
   * @param ownerKey               The name of the reply to [android.os.Messenger].
   * @param requestCode            The unique request code for the reply to [android.os.Messenger].
   * @param localFolder            A localFolder where we do a search.
   * @param alreadyLoadedMsgsCount The count of already cached messages in the database.
   */
  fun searchMsgs(ownerKey: String, requestCode: Int, localFolder: LocalFolder, alreadyLoadedMsgsCount: Int) {
    try {
      removeOldTasks(SearchMessagesSyncTask::class.java, activeQueue)
      activeQueue.put(SearchMessagesSyncTask(ownerKey, requestCode, localFolder, alreadyLoadedMsgsCount))
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }
  }

  /**
   * Load contacts info from the SENT folder.
   */
  private fun loadContactsInfoIfNeeded() {
    if (!accountDao.areContactsLoaded) {
      //we need to update labels before we can use the SENT folder for retrieve contacts
      updateLabels("", 0, passiveQueue)
      try {
        passiveQueue.put(LoadContactsSyncTask())
      } catch (e: InterruptedException) {
        e.printStackTrace()
      }
    }
  }

  /**
   * Run a thread where we will idle INBOX folder.
   */
  private fun runIdleInboxIfNeeded() {
    if (isIdleSupported && !isThreadAlreadyWorking(idleFuture)) {
      idleFuture = executorService.submit(IdleSyncRunnable())
    }
  }

  /**
   * Check a sync thread state.
   *
   * @return true if already work, otherwise false.
   */
  private fun isThreadAlreadyWorking(future: Future<*>?): Boolean {
    return future != null && !future.isCancelled && !future.isDone
  }

  /**
   * Remove the old tasks from the queue of synchronization.
   *
   * @param cls   The task type.
   * @param queue The queue of the tasks.
   */
  private fun removeOldTasks(cls: Class<*>, queue: BlockingQueue<SyncTask>) {
    val iterator = queue.iterator()
    while (iterator.hasNext()) {
      if (cls.isInstance(iterator.next())) {
        iterator.remove()
      }
    }
  }

  /**
   * This method can be used for debugging. Using this method we can identify a progress of some operation.
   *
   * @param ownerKey    The name of the reply to [android.os.Messenger].
   * @param requestCode The unique request code for the reply to [android.os.Messenger].
   * @param resultCode  The unique result code for the reply which identifies the progress of some request.
   */
  private fun notifyActionProgress(ownerKey: String, requestCode: Int, resultCode: Int) {
    listener.onActionProgress(accountDao, ownerKey, requestCode, resultCode)
  }

  private abstract inner class BaseSyncRunnable internal constructor() : Runnable {
    protected val tag: String = javaClass.simpleName

    protected var sess: Session? = null
    protected var store: Store? = null

    /**
     * Check available connection to the store.
     * Must be called from non-main thread.
     *
     * @return trus if connected, false otherwise.
     */
    internal val isConnected: Boolean
      get() = store != null && store!!.isConnected

    internal fun resetConnIfNeeded(task: SyncTask?) {
      val activeStore = store ?: return

      if (accountDao.authCreds != null) {
        if (!activeStore.urlName.username.equals(accountDao.authCreds!!.username, ignoreCase = true)) {
          LogsUtil.d(tag, "Connection was reset!")

          if (task != null) {
            notifyActionProgress(task.ownerKey, task.requestCode, R.id.progress_id_resetting_connection)
          }

          activeStore.close()
          sess = null
        }
      } else {
        throw ManualHandledException(listener.context.getString(R.string.device_not_supported_key_store_error))
      }
    }

    internal fun closeConn() {
      try {
        val activeStore = store ?: return
        activeStore.close()
      } catch (e: MessagingException) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
        LogsUtil.d(tag, "This exception occurred when we try disconnect from the store.")
      }

    }

    internal fun openConnToStore() {
      patchingSecurityProvider(listener.context)
      sess = OpenStoreHelper.getAccountSess(listener.context, accountDao)
      store = OpenStoreHelper.openStore(listener.context, accountDao, sess!!)
    }

    /**
     * Run the incoming [SyncTask]
     *
     * @param isRetryEnabled true if want to retry a task if it was fail
     * @param task           The incoming [SyncTask]
     */
    internal fun runSyncTask(task: SyncTask?, isRetryEnabled: Boolean) {

      task?.let {
        try {
          notifyActionProgress(task.ownerKey, task.requestCode, R.id.progress_id_running_task)

          resetConnIfNeeded(task)

          if (!isConnected) {
            LogsUtil.d(tag, "Not connected. Start a reconnection ...")
            notifyActionProgress(task.ownerKey, task.requestCode, R.id.progress_id_connecting_to_email_server)
            openConnToStore()
            LogsUtil.d(tag, "Reconnection done")
          }

          val activeStore = store ?: return
          val activeSess = sess ?: return

          LogsUtil.d(tag, "Start a new task = " + task.javaClass.simpleName + " for store " + activeStore.toString())

          if (task.isSMTPRequired) {
            notifyActionProgress(task.ownerKey, task.requestCode, R.id.progress_id_running_smtp_action)
            task.runSMTPAction(accountDao, activeSess, activeStore, listener)
          } else {
            notifyActionProgress(task.ownerKey, task.requestCode, R.id.progress_id_running_imap_action)
            task.runIMAPAction(accountDao, activeSess, activeStore, listener)
          }
          LogsUtil.d(tag, "The task = " + task.javaClass.simpleName + " completed")
        } catch (e: Exception) {
          e.printStackTrace()
          if (e is ConnectionException) {
            if (isRetryEnabled) {
              runSyncTask(task, false)
            } else {
              ExceptionUtil.handleError(e)
              task.handleException(accountDao, e, listener)
            }
          } else {
            ExceptionUtil.handleError(e)
            task.handleException(accountDao, e, listener)
          }
        }
      }
    }

    /**
     * To update a device's security provider, use the ProviderInstaller class.
     *
     *
     * When you call installIfNeeded(), the ProviderInstaller does the following:
     *  * If the device's Provider is successfully updated (or is already up-to-date), the method returns
     * normally.
     *  * If the device's Google Play services library is out of date, the method throws
     * GooglePlayServicesRepairableException. The app can then catch this exception and show the user an
     * appropriate dialog box to update Google Play services.
     *  * If a non-recoverable error occurs, the method throws GooglePlayServicesNotAvailableException to indicate
     * that it is unable to update the Provider. The app can then catch the exception and choose an appropriate
     * course of action, such as displaying the standard fix-it flow diagram.
     *
     *
     * If installIfNeeded() needs to install a new Provider, this can take anywhere from 30-50 milliseconds (on
     * more recent devices) to 350 ms (on older devices). If the security provider is already up-to-date, the
     * method takes a negligible amount of time.
     *
     *
     * Details here https://developer.android.com/training/articles/security-gms-provider.html#patching
     *
     * @param context Interface to global information about an application environment;
     */
    private fun patchingSecurityProvider(context: Context) {
      try {
        ProviderInstaller.installIfNeeded(context)
      } catch (e: GooglePlayServicesRepairableException) {
        e.printStackTrace()
      } catch (e: GooglePlayServicesNotAvailableException) {
        e.printStackTrace()
      }
    }
  }

  private inner class PassiveSyncTaskRunnable : BaseSyncRunnable() {
    private val timeoutWaitNextTask = 30

    override fun run() {
      LogsUtil.d(tag, " run!")
      Thread.currentThread().name = javaClass.simpleName

      while (!Thread.interrupted()) {
        try {
          LogsUtil.d(tag, "PassiveSyncTaskBlockingQueue size = " + passiveQueue.size)
          var syncTask: SyncTask? = passiveQueue.poll(timeoutWaitNextTask.toLong(), TimeUnit.SECONDS)

          if (syncTask == null) {
            closeConn()
            LogsUtil.d(tag, "Disconnected. Wait new tasks.")
            syncTask = passiveQueue.take()
          }

          runIdleInboxIfNeeded()
          runSyncTask(syncTask, true)
        } catch (e: InterruptedException) {
          e.printStackTrace()
        }
      }

      closeConn()
      LogsUtil.d(tag, " stopped!")
    }
  }

  private inner class ActiveSyncTaskRunnable : BaseSyncRunnable() {
    override fun run() {
      LogsUtil.d(tag, " run!")
      Thread.currentThread().name = javaClass.simpleName
      while (!Thread.interrupted()) {
        try {
          LogsUtil.d(tag, "ActiveSyncTaskBlockingQueue size = " + activeQueue.size)
          val syncTask = activeQueue.take()

          runIdleInboxIfNeeded()
          runSyncTask(syncTask, true)
        } catch (e: InterruptedException) {
          e.printStackTrace()
        }

      }

      closeConn()
      LogsUtil.d(tag, " stopped!")
    }
  }

  /**
   * This is a thread where we do a sync of some IMAP folder.
   *
   *
   * P.S. Currently we support only "INBOX" folder.
   */
  private inner class IdleSyncRunnable internal constructor() : BaseSyncRunnable(), MessageCountListener,
      MessageChangedListener {
    private var localFolder: LocalFolder? = null
    private var remoteFolder: IMAPFolder? = null
    private val msgDaoSource: MessageDaoSource = MessageDaoSource()
    /**
     * here we can have a lot of checks which help us decide can we run idling(wifi, 3G, a battery level and etc.)
     */
    private val isIdlingAvailable: Boolean
      get() = true

    override fun run() {
      LogsUtil.d(tag, " run!")
      Thread.currentThread().name = javaClass.simpleName

      val foldersManager = FoldersManager.fromDatabase(listener.context, accountDao.email)
      localFolder = foldersManager.findInboxFolder() ?: return

      idle()
      closeConn()

      LogsUtil.d(tag, " stopped!")
    }

    override fun messagesAdded(e: MessageCountEvent) {
      LogsUtil.d(tag, "messagesAdded: " + e.messages.size)
      localFolder?.let { loadNewMsgs("", 0, it) }
    }

    override fun messagesRemoved(messageCountEvent: MessageCountEvent) {
      LogsUtil.d(tag, "messagesRemoved")
      syncFolderState()
    }

    override fun messageChanged(e: MessageChangedEvent) {
      LogsUtil.d(tag, "messageChanged")
      val local = localFolder ?: return
      val remote = remoteFolder ?: return
      val msg = e.message
      if (msg != null && e.messageChangeType == MessageChangedEvent.FLAGS_CHANGED) {
        try {
          msgDaoSource.updateLocalMsgFlags(listener.context, accountDao.email, local.folderAlias,
              remote.getUID(msg), msg.flags)
          listener.onMsgChanged(accountDao, local, remote, msg, "", 0)
        } catch (msgException: MessagingException) {
          msgException.printStackTrace()
        }
      }
    }

    internal fun idle() {
      try {
        resetConnIfNeeded(null)
        while (!GeneralUtil.isConnected(listener.context)) {
          try {
            //wait while a connection will be established
            TimeUnit.MILLISECONDS.sleep(TimeUnit.SECONDS.toMillis(30))
          } catch (interruptedException: InterruptedException) {
            interruptedException.printStackTrace()
          }
        }

        if (!isConnected) {
          LogsUtil.d(tag, "Not connected. Start a reconnection ...")
          openConnToStore()
          LogsUtil.d(tag, "Reconnection done")
        }

        val activeStore = store ?: return

        LogsUtil.d(tag, "Start idling for store $activeStore")

        remoteFolder = activeStore.getFolder(localFolder!!.fullName) as IMAPFolder
        remoteFolder!!.open(javax.mail.Folder.READ_ONLY)

        syncFolderState()

        remoteFolder!!.addMessageCountListener(this)
        remoteFolder!!.addMessageChangedListener(this)

        while (!Thread.interrupted() && isIdlingAvailable) {
          remoteFolder!!.idle()
        }
      } catch (e: Exception) {
        e.printStackTrace()
        if (e is FolderClosedException || e is MailConnectException || e is IOException) {
          idle()
        } else if (e is MessagingException) {
          if ("IDLE not supported" == e.message) {
            LogsUtil.d(tag, "IDLE not supported!")
            isIdleSupported = false
          }
        } else {
          ExceptionUtil.handleError(e)
        }
      }
    }

    private fun syncFolderState() {
      localFolder?.let {
        refreshMsgs("", 0, it, false)
      }
    }
  }

  companion object {
    private const val MAX_THREADS_COUNT = 3
    private val TAG = EmailSyncManager::class.java.simpleName
  }
}
