/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.attachment

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.text.TextUtils
import android.widget.Toast
import androidx.core.content.FileProvider
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.protocol.ImapProtocolUtil
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper
import com.flowcrypt.email.api.retrofit.node.NodeRetrofitHelper
import com.flowcrypt.email.api.retrofit.node.NodeService
import com.flowcrypt.email.api.retrofit.request.node.DecryptFileRequest
import com.flowcrypt.email.api.retrofit.response.node.DecryptedFileResult
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.database.dao.source.imap.ImapLabelsDaoSource
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.FlowCryptLimitException
import com.flowcrypt.email.util.exception.ManualHandledException
import com.google.android.gms.common.util.CollectionUtils
import com.sun.mail.imap.IMAPFolder
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import javax.mail.Folder

/**
 * This service will be use to download email attachments. To start load an attachment just run service via the intent
 * [AttachmentDownloadManagerService.newIntent]
 *
 *
 * This service can do:
 *
 *
 *
 *  * Can download simultaneously 3 attachments. The other attachments will be added to the queue.
 *  * All loading attachments will visible in the status bar
 *  * The user can stop loading an attachment at any time.
 *
 *
 * @author Denis Bondarenko
 * Date: 16.08.2017
 * Time: 10:29
 * E-mail: DenBond7@gmail.com
 */

class AttachmentDownloadManagerService : Service() {

  @Volatile
  private var looper: Looper? = null
  @Volatile
  private lateinit var workerHandler: ServiceWorkerHandler

  private val replyMessenger: Messenger
  private lateinit var attsNotificationManager: AttachmentNotificationManager

  init {
    this.replyMessenger = Messenger(ReplyHandler(this))
  }

  override fun onCreate() {
    super.onCreate()
    LogsUtil.d(TAG, "onCreate")

    val handlerThread = HandlerThread(TAG)
    handlerThread.start()

    looper = handlerThread.looper
    workerHandler = ServiceWorkerHandler(looper!!, replyMessenger)
    attsNotificationManager = AttachmentNotificationManager(applicationContext)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    LogsUtil.d(TAG, "onStartCommand |intent =$intent|flags = $flags|startId = $startId")
    if (intent != null && !TextUtils.isEmpty(intent.action)) {
      val attInfo = intent.getParcelableExtra<AttachmentInfo>(EXTRA_KEY_ATTACHMENT_INFO)
      when (intent.action) {
        ACTION_CANCEL_DOWNLOAD_ATTACHMENT -> cancelDownloadAtt(attInfo)

        ACTION_RETRY_DOWNLOAD_ATTACHMENT, ACTION_START_DOWNLOAD_ATTACHMENT -> if (attInfo != null) {
          addDownloadTaskToQueue(applicationContext, attInfo)
        }

        else -> checkAndStopIfNeeded()
      }
    }

    return super.onStartCommand(intent, flags, startId)
  }

  override fun onDestroy() {
    super.onDestroy()
    LogsUtil.d(TAG, "onDestroy")
    releaseResources()
  }

  override fun onBind(intent: Intent): IBinder? {
    return null
  }

  private fun stopService() {
    stopSelf()
    LogsUtil.d(TAG, "Trying to stop the service")
  }

  private fun releaseResources() {
    val msg = workerHandler.obtainMessage()
    msg.what = ServiceWorkerHandler.MESSAGE_RELEASE_RESOURCES
    workerHandler.sendMessage(msg)
  }

  private fun cancelDownloadAtt(attInfo: AttachmentInfo) {
    attsNotificationManager.loadingCanceledByUser(attInfo)
    val msg = workerHandler.obtainMessage()
    msg.what = ServiceWorkerHandler.MESSAGE_CANCEL_DOWNLOAD
    msg.obj = attInfo
    workerHandler.sendMessage(msg)
  }

  private fun addDownloadTaskToQueue(context: Context, attInfo: AttachmentInfo) {
    val msg = workerHandler.obtainMessage()
    msg.what = ServiceWorkerHandler.MESSAGE_START_DOWNLOAD
    msg.obj = DownloadAttachmentTaskRequest(context, attInfo)
    workerHandler.sendMessage(msg)
  }

  private fun checkAndStopIfNeeded() {
    val msg = workerHandler.obtainMessage()
    msg.what = ServiceWorkerHandler.MESSAGE_CHECK_AND_STOP_IF_NEEDED
    workerHandler.sendMessage(msg)
  }

  private interface OnDownloadAttachmentListener {
    fun onAttDownloaded(attInfo: AttachmentInfo, uri: Uri)

    fun onCanceled(attInfo: AttachmentInfo)

    fun onError(attInfo: AttachmentInfo, e: Exception)

    fun onProgress(attInfo: AttachmentInfo, progressInPercentage: Int, timeLeft: Long)
  }

  /**
   * The incoming handler realization. This handler will be used to communicate with current
   * service and the worker thread.
   */
  private class ReplyHandler internal constructor(attDownloadManagerService: AttachmentDownloadManagerService)
    : Handler() {

    private val weakRef: WeakReference<AttachmentDownloadManagerService> = WeakReference(attDownloadManagerService)

    override fun handleMessage(message: Message) {
      if (weakRef.get() != null) {
        val attDownloadManagerService = weakRef.get()
        val notificationManager = attDownloadManagerService?.attsNotificationManager

        val (attInfo, exception, uri, progressInPercentage, timeLeft, isLast)
            = message.obj as DownloadAttachmentTaskResult

        when (message.what) {
          MESSAGE_EXCEPTION_HAPPENED -> notificationManager?.errorHappened(attDownloadManagerService, attInfo!!,
              exception!!)

          MESSAGE_TASK_ALREADY_EXISTS -> {
            val msg = attDownloadManagerService?.getString(R.string.template_attachment_already_loading, attInfo!!.name)
            Toast.makeText(attDownloadManagerService, msg, Toast.LENGTH_SHORT).show()
          }

          MESSAGE_ATTACHMENT_DOWNLOAD -> {
            notificationManager?.downloadCompleted(attDownloadManagerService, attInfo!!, uri!!)
            LogsUtil.d(TAG, attInfo!!.name!! + " is downloaded")
          }

          MESSAGE_ATTACHMENT_ADDED_TO_QUEUE ->
            notificationManager?.attachmentAddedToLoadQueue(attDownloadManagerService, attInfo!!)

          MESSAGE_PROGRESS -> notificationManager?.updateLoadingProgress(attDownloadManagerService, attInfo!!,
              progressInPercentage, timeLeft)

          MESSAGE_RELEASE_RESOURCES -> attDownloadManagerService?.looper!!.quit()

          MESSAGE_DOWNLOAD_CANCELED -> {
            notificationManager?.loadingCanceledByUser(attInfo!!)
            LogsUtil.d(TAG, attInfo!!.name!! + " was canceled")
          }

          MESSAGE_STOP_SERVICE -> attDownloadManagerService?.stopService()
        }

        if (isLast) {
          attDownloadManagerService?.stopService()
        }
      }
    }

    companion object {
      internal const val MESSAGE_EXCEPTION_HAPPENED = 1
      internal const val MESSAGE_TASK_ALREADY_EXISTS = 2
      internal const val MESSAGE_ATTACHMENT_DOWNLOAD = 3
      internal const val MESSAGE_DOWNLOAD_STARTED = 4
      internal const val MESSAGE_ATTACHMENT_ADDED_TO_QUEUE = 5
      internal const val MESSAGE_PROGRESS = 6
      internal const val MESSAGE_RELEASE_RESOURCES = 8
      internal const val MESSAGE_DOWNLOAD_CANCELED = 9
      internal const val MESSAGE_STOP_SERVICE = 10
    }
  }

  /**
   * This handler will be used by the instance of [HandlerThread] to receive message from
   * the UI thread.
   */
  private class ServiceWorkerHandler internal constructor(looper: Looper, private val messenger: Messenger)
    : Handler(looper), OnDownloadAttachmentListener {
    private val executorService: ExecutorService = Executors.newFixedThreadPool(QUEUE_SIZE)

    @Volatile
    private var attsInfoMap: HashMap<String, AttachmentInfo> = HashMap()
    @Volatile
    private var futureMap: HashMap<String, Future<*>> = HashMap()

    private val isLast: Boolean
      get() = CollectionUtils.isEmpty(futureMap.values)

    override fun handleMessage(msg: Message) {
      when (msg.what) {
        MESSAGE_START_DOWNLOAD -> {
          val taskRequest = msg.obj as DownloadAttachmentTaskRequest

          val attInfo = taskRequest.attInfo
          val context = taskRequest.context

          try {
            if (attsInfoMap[attInfo.id] == null) {
              attsInfoMap[attInfo.id!!] = attInfo
              val attDownloadRunnable = AttDownloadRunnable(context, attInfo)
              attDownloadRunnable.setListener(this)
              futureMap[attInfo.uniqueStringId] = executorService.submit(attDownloadRunnable)
              val result = DownloadAttachmentTaskResult(attInfo)
              messenger.send(Message.obtain(null, ReplyHandler.MESSAGE_ATTACHMENT_ADDED_TO_QUEUE, result))
            } else {
              taskAlreadyExists(attInfo)
            }
          } catch (e: Exception) {
            e.printStackTrace()
            ExceptionUtil.handleError(e)
            try {
              val result = DownloadAttachmentTaskResult(attInfo, e)
              messenger.send(Message.obtain(null, ReplyHandler.MESSAGE_EXCEPTION_HAPPENED, result))
            } catch (remoteException: RemoteException) {
              remoteException.printStackTrace()
            }
          }
        }

        MESSAGE_CANCEL_DOWNLOAD -> {
          val canceledAttInfo = msg.obj as AttachmentInfo
          val future = futureMap[canceledAttInfo.uniqueStringId]
          if (future != null) {
            if (!future.isDone) {
              //if this thread hasn't run yet
              onCanceled(canceledAttInfo)
            }

            future.cancel(true)
          }

          attsInfoMap.remove(canceledAttInfo.id)
        }

        MESSAGE_RELEASE_RESOURCES -> {
          executorService.shutdown()

          try {
            val result = DownloadAttachmentTaskResult()
            messenger.send(Message.obtain(null, ReplyHandler.MESSAGE_RELEASE_RESOURCES, result))
          } catch (e: RemoteException) {
            e.printStackTrace()
            ExceptionUtil.handleError(e)
          }

        }

        MESSAGE_CHECK_AND_STOP_IF_NEEDED -> if (CollectionUtils.isEmpty(futureMap.values)) {
          try {
            val result = DownloadAttachmentTaskResult()
            messenger.send(Message.obtain(null, ReplyHandler.MESSAGE_STOP_SERVICE, result))
          } catch (e: RemoteException) {
            e.printStackTrace()
            ExceptionUtil.handleError(e)
          }

        }
      }
    }

    override fun onError(attInfo: AttachmentInfo, e: Exception) {
      attsInfoMap.remove(attInfo.id)
      futureMap.remove(attInfo.uniqueStringId)
      try {
        val result = DownloadAttachmentTaskResult(attInfo, e, isLast = isLast)
        messenger.send(Message.obtain(null, ReplyHandler.MESSAGE_EXCEPTION_HAPPENED, result))
      } catch (remoteException: RemoteException) {
        remoteException.printStackTrace()
      }

    }

    override fun onProgress(attInfo: AttachmentInfo, progressInPercentage: Int, timeLeft: Long) {
      try {
        val result = DownloadAttachmentTaskResult(attInfo, progressInPercentage = progressInPercentage,
            timeLeft = timeLeft)
        messenger.send(Message.obtain(null, ReplyHandler.MESSAGE_PROGRESS, result))
      } catch (remoteException: RemoteException) {
        remoteException.printStackTrace()
      }

    }

    override fun onAttDownloaded(attInfo: AttachmentInfo, uri: Uri) {
      attsInfoMap.remove(attInfo.id)
      futureMap.remove(attInfo.uniqueStringId)
      try {
        val result = DownloadAttachmentTaskResult(attInfo, uri = uri, isLast = isLast)
        messenger.send(Message.obtain(null, ReplyHandler.MESSAGE_ATTACHMENT_DOWNLOAD, result))
      } catch (remoteException: RemoteException) {
        remoteException.printStackTrace()
      }

    }

    override fun onCanceled(attInfo: AttachmentInfo) {
      attsInfoMap.remove(attInfo.id)
      futureMap.remove(attInfo.uniqueStringId)
      try {
        val result = DownloadAttachmentTaskResult(attInfo, isLast = isLast)
        messenger.send(Message.obtain(null, ReplyHandler.MESSAGE_DOWNLOAD_CANCELED, result))
      } catch (remoteException: RemoteException) {
        remoteException.printStackTrace()
      }

    }

    private fun taskAlreadyExists(attInfo: AttachmentInfo) {
      try {
        val result = DownloadAttachmentTaskResult(attInfo, isLast = isLast)
        messenger.send(Message.obtain(null, ReplyHandler.MESSAGE_TASK_ALREADY_EXISTS, result))
      } catch (e: RemoteException) {
        e.printStackTrace()
      }

    }

    companion object {
      internal const val MESSAGE_START_DOWNLOAD = 1
      internal const val MESSAGE_CANCEL_DOWNLOAD = 2
      internal const val MESSAGE_RELEASE_RESOURCES = 3
      internal const val MESSAGE_CHECK_AND_STOP_IF_NEEDED = 4
      /**
       * Maximum number of simultaneous downloads
       */
      private const val QUEUE_SIZE = 3
    }
  }

  private class AttDownloadRunnable internal constructor(private val context: Context,
                                                         private val att: AttachmentInfo) : Runnable {
    private var listener: OnDownloadAttachmentListener? = null

    override fun run() {
      if (GeneralUtil.isDebugBuild()) {
        Thread.currentThread().name = AttDownloadRunnable::class.java.simpleName + "|" + att.name
      }

      var attFile: File = prepareAttFile()

      try {
        checkFileSize()

        if (att.uri != null) {
          val inputStream = context.contentResolver.openInputStream(att.uri!!)
          if (inputStream != null) {
            FileUtils.copyInputStreamToFile(inputStream, attFile)
            attFile = decryptFileIfNeeded(context, attFile)

            att.name = attFile.name

            if (!Thread.currentThread().isInterrupted) {
              if (listener != null) {
                val uri = FileProvider.getUriForFile(context, Constants.FILE_PROVIDER_AUTHORITY, attFile)
                listener!!.onAttDownloaded(att, uri)
              }
            }

            return
          }
        }

        val source = AccountDaoSource()
        val account = source.getAccountInformation(context, att.email!!)

        if (account == null) {
          if (listener != null) {
            listener!!.onCanceled(this.att)
            return
          }
        }

        val session = OpenStoreHelper.getAttsSess(context, account)
        val store = OpenStoreHelper.openStore(context, account, session)

        val (fullName) = ImapLabelsDaoSource().getFolder(context, att.email!!, att.folder!!)
            ?: if (source.getAccountInformation(context, att.email!!) == null) {
              if (listener != null) {
                listener!!.onCanceled(this.att)

                store.close()
              }
              return
            } else {
              throw ManualHandledException("Folder \"" + att.folder + "\" not found in the local cache")
            }

        val remoteFolder = store.getFolder(fullName) as IMAPFolder
        remoteFolder.open(Folder.READ_ONLY)

        val msg = remoteFolder.getMessageByUID(att.uid.toLong())
            ?: throw ManualHandledException(context.getString(R.string.no_message_with_this_attachment))

        val att = ImapProtocolUtil.getAttPartById(remoteFolder, msg.messageNumber, msg, this.att.id!!)

        if (att != null) {
          val inputStream = att.inputStream
          downloadFile(attFile, inputStream)

          if (Thread.currentThread().isInterrupted) {
            removeNotCompletedAtt(attFile)
            listener?.onCanceled(this.att)
          } else {
            attFile = decryptFileIfNeeded(context, attFile)
            this.att.name = attFile.name

            if (Thread.currentThread().isInterrupted) {
              removeNotCompletedAtt(attFile)
              listener?.onCanceled(this.att)
            } else {
              val uri = FileProvider.getUriForFile(context, Constants.FILE_PROVIDER_AUTHORITY, attFile)
              listener?.onAttDownloaded(this.att, uri)
            }
          }
        } else throw ManualHandledException(context.getString(R.string.attachment_not_found))

        remoteFolder.close(false)
        store.close()
      } catch (e: Exception) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
        removeNotCompletedAtt(attFile)
        if (listener != null) {
          listener!!.onError(att, e)
        }
      }

    }

    internal fun setListener(listener: OnDownloadAttachmentListener) {
      this.listener = listener
    }

    private fun downloadFile(attFile: File, inputStream: InputStream) {
      try {
        FileUtils.openOutputStream(attFile).use { outputStream ->
          val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
          var count = 0.0
          val size = this.att.encodedSize.toDouble()
          var numberOfReadBytes: Int
          var lastPercentage = 0
          var currentPercentage = 0
          val startTime: Long
          var elapsedTime: Long
          startTime = System.currentTimeMillis()
          var lastUpdateTime = startTime
          updateProgress(currentPercentage, 0)
          while (true) {
            numberOfReadBytes = inputStream.read(buffer)

            if (IOUtils.EOF == numberOfReadBytes) {
              break
            }

            if (!Thread.currentThread().isInterrupted) {
              outputStream.write(buffer, 0, numberOfReadBytes)
              count += numberOfReadBytes.toDouble()
              currentPercentage = (count / size * 100f).toInt()
              val isUpdateNeeded = System.currentTimeMillis() - lastUpdateTime >= MIN_UPDATE_PROGRESS_INTERVAL
              if (currentPercentage - lastPercentage >= 1 && isUpdateNeeded) {
                lastPercentage = currentPercentage
                lastUpdateTime = System.currentTimeMillis()
                elapsedTime = lastUpdateTime - startTime
                val predictLoadingTime = (elapsedTime * size / count).toLong()
                updateProgress(currentPercentage, predictLoadingTime - elapsedTime)
              }
            } else {
              break
            }
          }

          updateProgress(100, 0)
        }
      } finally {
      }
    }

    /**
     * Check is decrypted file has size not more than
     * [Constants.MAX_ATTACHMENT_SIZE_WHICH_CAN_BE_DECRYPTED]. If the file greater then
     * [Constants.MAX_ATTACHMENT_SIZE_WHICH_CAN_BE_DECRYPTED] we throw an exception. This is only for files
     * with the "pgp" extension.
     */
    private fun checkFileSize() {
      if ("pgp".equals(FilenameUtils.getExtension(att.name), ignoreCase = true)) {
        if (att.encodedSize > Constants.MAX_ATTACHMENT_SIZE_WHICH_CAN_BE_DECRYPTED) {
          val errorMsg = context.getString(R.string.template_warning_max_attachments_size_for_decryption,
              FileUtils.byteCountToDisplaySize(Constants.MAX_ATTACHMENT_SIZE_WHICH_CAN_BE_DECRYPTED.toLong()))
          throw FlowCryptLimitException(errorMsg)
        }
      }
    }

    /**
     * Do decryption of the downloaded file if it need.
     *
     * @param context Interface to global information about an application environment;
     * @param file    The downloaded file which can be encrypted.
     * @return The decrypted or the original file.
     */
    private fun decryptFileIfNeeded(context: Context, file: File): File {
      if (!file.exists()) {
        throw NullPointerException("Error. The file is missing")
      }

      if (!"pgp".equals(FilenameUtils.getExtension(file.name), ignoreCase = true)) {
        return file
      }

      FileInputStream(file).use { inputStream ->
        val (_, _, _, decryptedBytes) = getDecryptedFileResult(context, inputStream)

        val decryptedFile = File(file.parent, file.name.substring(0, file.name.lastIndexOf(".")))

        var isInnerExceptionHappened = false

        try {
          FileUtils.openOutputStream(decryptedFile).use { outputStream ->
            IOUtils.write(decryptedBytes, outputStream)
            return decryptedFile
          }
        } catch (e: IOException) {
          if (!decryptedFile.delete()) {
            LogsUtil.d(TAG, "Cannot delete file: $file")
          }

          isInnerExceptionHappened = true
          throw e
        } finally {
          if (!isInnerExceptionHappened) {
            if (!file.delete()) {
              LogsUtil.d(TAG, "Cannot delete file: $file")
            }
          }
        }
      }
    }

    private fun getDecryptedFileResult(context: Context, inputStream: InputStream): DecryptedFileResult {
      val keysStorage = KeysStorageImpl.getInstance(context)
      val pgpKeyInfoList = keysStorage.getAllPgpPrivateKeys()
      val nodeService = NodeRetrofitHelper.getRetrofit()!!.create(NodeService::class.java)
      val request = DecryptFileRequest(IOUtils.toByteArray(inputStream), pgpKeyInfoList)
      val response = nodeService.decryptFile(request).execute()
      val result = response.body() ?: throw NullPointerException("Node.js returned an empty result")
      if (result.error != null) {
        var exceptionMsg = result.error.msg
        if ("use_password" == result.error.type) {
          exceptionMsg = context.getString(R.string.opening_password_encrypted_msg_not_implemented_yet)
        }
        throw Exception(exceptionMsg)
      }

      return result
    }

    /**
     * Remove the file which not downloaded fully.
     *
     * @param attachmentFile The file which will be removed.
     */
    private fun removeNotCompletedAtt(attachmentFile: File?) {
      if (attachmentFile != null && attachmentFile.exists()) {
        if (!attachmentFile.delete()) {
          LogsUtil.d(TAG, "Cannot delete a file: $attachmentFile")
        } else {
          LogsUtil.d(TAG, "Canceled attachment \"$attachmentFile\" was deleted")
        }
      }
    }

    private fun updateProgress(currentPercentage: Int, timeLeft: Long) {
      if (!Thread.currentThread().isInterrupted) {
        listener?.onProgress(att, currentPercentage, timeLeft)
      }
    }

    /**
     * Create the local file where we will write an input stream from the IMAP server.
     *
     * @return A new created file.
     */
    private fun prepareAttFile(): File {
      return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), att.name)
    }

    companion object {
      private const val MIN_UPDATE_PROGRESS_INTERVAL = 500
      private const val DEFAULT_BUFFER_SIZE = 1024 * 16
    }
  }

  companion object {
    const val ACTION_START_DOWNLOAD_ATTACHMENT = BuildConfig.APPLICATION_ID + ".ACTION_START_DOWNLOAD_ATTACHMENT"
    const val ACTION_CANCEL_DOWNLOAD_ATTACHMENT = BuildConfig.APPLICATION_ID + ".ACTION_CANCEL_DOWNLOAD_ATTACHMENT"
    const val ACTION_RETRY_DOWNLOAD_ATTACHMENT = BuildConfig.APPLICATION_ID + ".ACTION_RETRY_DOWNLOAD_ATTACHMENT"

    val EXTRA_KEY_ATTACHMENT_INFO =
        GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_ATTACHMENT_INFO", AttachmentDownloadManagerService::class.java)

    private val TAG = AttachmentDownloadManagerService::class.java.simpleName

    @JvmStatic
    fun newIntent(context: Context, attInfo: AttachmentInfo): Intent {
      val intent = Intent(context, AttachmentDownloadManagerService::class.java)
      intent.action = ACTION_START_DOWNLOAD_ATTACHMENT
      intent.putExtra(EXTRA_KEY_ATTACHMENT_INFO, attInfo)
      return intent
    }
  }
}
