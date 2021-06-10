/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.attachment

import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.provider.MediaStore
import android.text.TextUtils
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.protocol.ImapProtocolUtil
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.kotlin.toHex
import com.flowcrypt.email.jetpack.viewmodel.AccountViewModel
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.security.pgp.PgpDecrypt
import com.flowcrypt.email.util.FileAndDirectoryUtils
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.ManualHandledException
import com.google.android.gms.common.util.CollectionUtils
import com.sun.mail.imap.IMAPFolder
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.lang.ref.WeakReference
import java.util.HashMap
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import javax.mail.Folder

/**
 * This service can be used to download email attachments.
 * To start loading an attachment just run service via the intent
 * [AttachmentDownloadManagerService.newIntent]
 *
 * This service provides the following:
 *
 *  * Can download simultaneously 3 attachments. Other attachments will be added to the queue.
 *  * All loading attachments will visible in the status bar
 *  * A user can stop loading an attachment at any time.
 *
 *
 * @author Denis Bondarenko
 * Date: 16.08.2017
 * Time: 10:29
 * E-mail: DenBond7@gmail.com
 */
class AttachmentDownloadManagerService : Service() {

  @Volatile
  private lateinit var looper: Looper

  @Volatile
  private lateinit var workerHandler: ServiceWorkerHandler

  private val replyMessenger: Messenger = Messenger(ReplyHandler(this))
  private lateinit var attsNotificationManager: AttachmentNotificationManager

  override fun onCreate() {
    super.onCreate()
    LogsUtil.d(TAG, "onCreate")

    val handlerThread = HandlerThread(TAG)
    handlerThread.start()

    looper = handlerThread.looper
    workerHandler = ServiceWorkerHandler(looper, replyMessenger)
    attsNotificationManager = AttachmentNotificationManager(applicationContext)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    LogsUtil.d(TAG, "onStartCommand |intent =$intent|flags = $flags|startId = $startId")
    if (intent != null && !TextUtils.isEmpty(intent.action)) {
      val attInfo = intent.getParcelableExtra<AttachmentInfo>(EXTRA_KEY_ATTACHMENT_INFO)
      when (intent.action) {
        ACTION_CANCEL_DOWNLOAD_ATTACHMENT -> attInfo?.let { cancelDownloadAtt(it) }

        ACTION_RETRY_DOWNLOAD_ATTACHMENT, ACTION_START_DOWNLOAD_ATTACHMENT -> attInfo?.let {
          addDownloadTaskToQueue(applicationContext, it)
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
  //todo-denbond7 need to fix deprecation
  private class ReplyHandler(
    attDownloadManagerService: AttachmentDownloadManagerService
  ) : Handler() {

    private val weakRef: WeakReference<AttachmentDownloadManagerService> =
      WeakReference(attDownloadManagerService)

    override fun handleMessage(message: Message) {
      if (weakRef.get() != null) {
        val attDownloadManagerService = weakRef.get()
        val notificationManager = attDownloadManagerService?.attsNotificationManager

        val (attInfo, exception, uri, progressInPercentage, timeLeft, isLast)
            = message.obj as DownloadAttachmentTaskResult

        when (message.what) {
          MESSAGE_EXCEPTION_HAPPENED -> notificationManager?.errorHappened(
            attDownloadManagerService, attInfo!!,
            exception!!
          )

          MESSAGE_TASK_ALREADY_EXISTS -> {
            val msg = attDownloadManagerService?.getString(
              R.string.template_attachment_already_loading,
              attInfo!!.name
            )
            Toast.makeText(attDownloadManagerService, msg, Toast.LENGTH_SHORT).show()
          }

          MESSAGE_ATTACHMENT_DOWNLOAD -> {
            notificationManager?.downloadCompleted(attDownloadManagerService, attInfo!!, uri!!)
            LogsUtil.d(TAG, attInfo?.getSafeName() + " is downloaded")
          }

          MESSAGE_ATTACHMENT_ADDED_TO_QUEUE ->
            notificationManager?.attachmentAddedToLoadQueue(attDownloadManagerService, attInfo!!)

          MESSAGE_PROGRESS -> notificationManager?.updateLoadingProgress(
            attDownloadManagerService, attInfo!!,
            progressInPercentage, timeLeft
          )

          MESSAGE_RELEASE_RESOURCES -> attDownloadManagerService?.looper!!.quit()

          MESSAGE_DOWNLOAD_CANCELED -> {
            notificationManager?.loadingCanceledByUser(attInfo!!)
            LogsUtil.d(TAG, attInfo?.getSafeName() + " was canceled")
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
      internal const val MESSAGE_ATTACHMENT_ADDED_TO_QUEUE = 4
      internal const val MESSAGE_PROGRESS = 5
      internal const val MESSAGE_RELEASE_RESOURCES = 6
      internal const val MESSAGE_DOWNLOAD_CANCELED = 7
      internal const val MESSAGE_STOP_SERVICE = 8
    }
  }

  /**
   * This handler will be used by the instance of [HandlerThread] to receive message from
   * the UI thread.
   */
  private class ServiceWorkerHandler(looper: Looper, private val messenger: Messenger) :
    Handler(looper), OnDownloadAttachmentListener {
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
              messenger.send(
                Message.obtain(
                  null,
                  ReplyHandler.MESSAGE_ATTACHMENT_ADDED_TO_QUEUE,
                  result
                )
              )
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
        val result = DownloadAttachmentTaskResult(
          attInfo, progressInPercentage = progressInPercentage,
          timeLeft = timeLeft
        )
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

  private class AttDownloadRunnable(
    private val context: Context,
    private val att: AttachmentInfo
  ) : Runnable {
    private var listener: OnDownloadAttachmentListener? = null
    private var attTempFile: File = File.createTempFile("tmp", null, context.externalCacheDir)

    override fun run() {
      if (GeneralUtil.isDebugBuild()) {
        Thread.currentThread().name =
          AttDownloadRunnable::class.java.simpleName + "|" + att.getSafeName()
      }
      val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)

      try {
        if (att.uri != null) {
          val inputStream = context.contentResolver.openInputStream(att.uri!!)
          if (inputStream != null) {
            FileUtils.copyInputStreamToFile(inputStream, attTempFile)
            attTempFile = decryptFileIfNeeded(context, attTempFile)
            if (!Thread.currentThread().isInterrupted) {
              val uri = storeFileToSharedFolder(context, attTempFile)
              listener?.onAttDownloaded(att, uri)
            }

            return
          }
        }

        val email = att.email ?: return
        val account = AccountViewModel.getAccountEntityWithDecryptedInfo(
          roomDatabase.accountDao().getAccount(email)
        )

        if (account == null) {
          listener?.onCanceled(this.att)
          return
        }

        if (account.useAPI) {
          when (account.accountType) {
            AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
              val msg = GmailApiHelper.loadMsgFullInfo(context, account, att.uid.toHex())
              val attPart = GmailApiHelper.getAttPartByPath(msg.payload, neededPath = att.path)
                ?: throw ManualHandledException(context.getString(R.string.attachment_not_found))

              GmailApiHelper.getAttInputStream(
                context,
                account,
                att.uid.toHex(),
                attPart.body.attachmentId
              ).use { inputStream ->
                handleAttachmentInputStream(inputStream)
              }
            }

            else -> throw ManualHandledException("Unsupported provider")
          }
        } else {
          val session = OpenStoreHelper.getAttsSess(context, account)
          OpenStoreHelper.openStore(context, account, session).use { store ->
            val label = roomDatabase.labelDao().getLabel(email, account.accountType, att.folder!!)
              ?: if (roomDatabase.accountDao().getAccount(email) == null) {
                listener?.onCanceled(this.att)
                store.close()
                return
              } else throw ManualHandledException("Folder \"" + att.folder + "\" not found in the local cache")

            store.getFolder(label.name).use { folder ->
              val remoteFolder = (folder as IMAPFolder).apply { open(Folder.READ_ONLY) }
              val msg = remoteFolder.getMessageByUID(att.uid)
                ?: throw ManualHandledException(context.getString(R.string.no_message_with_this_attachment))

              ImapProtocolUtil.getAttPartByPath(
                msg, neededPath = this.att.path
              )?.inputStream?.let { inputStream ->
                handleAttachmentInputStream(inputStream)
              } ?: throw ManualHandledException(context.getString(R.string.attachment_not_found))
            }
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
        listener?.onError(att, e)
      } finally {
        deleteTempFile(attTempFile)
      }
    }

    private fun handleAttachmentInputStream(inputStream: InputStream) {
      downloadFile(attTempFile, inputStream)

      if (Thread.currentThread().isInterrupted) {
        listener?.onCanceled(this.att)
      } else {
        attTempFile = decryptFileIfNeeded(context, attTempFile)
        if (Thread.currentThread().isInterrupted) {
          listener?.onCanceled(this.att)
        } else {
          val uri = storeFileToSharedFolder(context, attTempFile)
          listener?.onAttDownloaded(this.att, uri)
        }
      }
    }

    private fun storeFileToSharedFolder(context: Context, attFile: File): Uri {
      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        storeFileUsingScopedStorage(context, attFile)
      } else {
        storeLegacy(attFile, context)
      }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun storeFileUsingScopedStorage(context: Context, attFile: File): Uri {
      val resolver = context.contentResolver
      val fileExtension = FilenameUtils.getExtension(att.name).toLowerCase(Locale.getDefault())
      val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)

      val contentValues = ContentValues().apply {
        put(MediaStore.DownloadColumns.DISPLAY_NAME, att.getSafeName())
        put(MediaStore.DownloadColumns.SIZE, attFile.length())
        put(MediaStore.DownloadColumns.MIME_TYPE, mimeType)
        put(MediaStore.Downloads.IS_PENDING, 1)
      }

      val imageUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

      requireNotNull(imageUri)

      //we should check maybe a file is already exist. Then we will use the file name from the system
      val cursor =
        resolver.query(imageUri, arrayOf(MediaStore.DownloadColumns.DISPLAY_NAME), null, null, null)
      cursor?.let {
        if (it.moveToFirst()) {
          val nameIndex = it.getColumnIndex(MediaStore.DownloadColumns.DISPLAY_NAME)
          if (nameIndex != -1) {
            val nameFromSystem = it.getString(nameIndex)
            if (nameFromSystem != att.getSafeName()) {
              att.name = nameFromSystem
            }
          }
        }
      }
      cursor?.close()

      val srcInputStream = attFile.inputStream()
      val destOutputStream = resolver.openOutputStream(imageUri)
        ?: throw IllegalArgumentException("provided URI could not be opened")
      srcInputStream.use { srcStream ->
        destOutputStream.use { outStream -> srcStream.copyTo(outStream) }
      }

      //notify the system that the file is ready
      resolver.update(imageUri, ContentValues().apply {
        put(MediaStore.Downloads.IS_PENDING, 0)
      }, null, null)

      return imageUri
    }

    /**
     * We use this method to support saving files on Android 9 and less which uses an old approach.
     */
    @Suppress("DEPRECATION")
    private fun storeLegacy(attFile: File, context: Context): Uri {
      val fileName = att.getSafeName()
      val fileDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
      var sharedFile = File(fileDir, fileName)
      sharedFile = if (sharedFile.exists()) {
        FileAndDirectoryUtils.createFileWithIncreasedIndex(fileDir, fileName)
      } else {
        sharedFile
      }

      att.name = sharedFile.name
      val srcInputStream = attFile.inputStream()
      val destOutputStream = FileUtils.openOutputStream(sharedFile)
      srcInputStream.use { srcStream ->
        destOutputStream.use { outStream -> srcStream.copyTo(outStream) }
      }
      return FileProvider.getUriForFile(context, Constants.FILE_PROVIDER_AUTHORITY, sharedFile)
    }

    fun setListener(listener: OnDownloadAttachmentListener) {
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
          var elapsedTime: Long
          val startTime: Long = System.currentTimeMillis()
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
              val isUpdateNeeded =
                System.currentTimeMillis() - lastUpdateTime >= MIN_UPDATE_PROGRESS_INTERVAL
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
     * Do decryption of the downloaded file if needed.
     *
     * @param context Interface to global information about an application environment;
     * @param file    The downloaded file which can be encrypted.
     * @return The decrypted or the original file.
     */
    private fun decryptFileIfNeeded(context: Context, file: File): File {
      if (!file.exists()) {
        throw NullPointerException("Error. The file is missing")
      }

      if (!SecurityUtils.isEncryptedData(att.name)) {
        return file
      }

      FileInputStream(file).use { inputStream ->
        val decryptedFile = File.createTempFile("tmp", null, context.externalCacheDir)
        val pgpSecretKeyRings = KeysStorageImpl.getInstance(context).getPGPSecretKeyRings()
        val pgpSecretKeyRingCollection = PGPSecretKeyRingCollection(pgpSecretKeyRings)
        val protector = KeysStorageImpl.getInstance(context).getSecretKeyRingProtector()

        try {
          val result = PgpDecrypt.decrypt(
            srcInputStream = inputStream,
            destOutputStream = decryptedFile.outputStream(),
            pgpSecretKeyRingCollection = pgpSecretKeyRingCollection,
            protector = protector
          )

          att.name = FilenameUtils.getBaseName(att.name)
          result.fileInfo?.fileName?.let { fileName ->
            if (att.name == null) {
              att.name = fileName
            }
          }

          return decryptedFile
        } catch (e: Exception) {
          deleteTempFile(decryptedFile)
          throw e
        } finally {
          deleteTempFile(file)
        }
      }
    }

    /**
     * Remove the file which not downloaded fully.
     *
     * @param attachmentFile The file which will be removed.
     */
    private fun deleteTempFile(attachmentFile: File?) {
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

    companion object {
      private const val MIN_UPDATE_PROGRESS_INTERVAL = 500
      private const val DEFAULT_BUFFER_SIZE = 1024 * 16
    }
  }

  companion object {
    const val ACTION_START_DOWNLOAD_ATTACHMENT =
      BuildConfig.APPLICATION_ID + ".ACTION_START_DOWNLOAD_ATTACHMENT"
    const val ACTION_CANCEL_DOWNLOAD_ATTACHMENT =
      BuildConfig.APPLICATION_ID + ".ACTION_CANCEL_DOWNLOAD_ATTACHMENT"
    const val ACTION_RETRY_DOWNLOAD_ATTACHMENT =
      BuildConfig.APPLICATION_ID + ".ACTION_RETRY_DOWNLOAD_ATTACHMENT"

    val EXTRA_KEY_ATTACHMENT_INFO =
      GeneralUtil.generateUniqueExtraKey(
        "EXTRA_KEY_ATTACHMENT_INFO",
        AttachmentDownloadManagerService::class.java
      )

    private val TAG = AttachmentDownloadManagerService::class.java.simpleName

    fun newIntent(context: Context?, attInfo: AttachmentInfo?): Intent? {
      if (context == null || attInfo == null) {
        return null
      }

      val intent = Intent(context, AttachmentDownloadManagerService::class.java)
      intent.action = ACTION_START_DOWNLOAD_ATTACHMENT
      intent.putExtra(EXTRA_KEY_ATTACHMENT_INFO, attInfo)
      return intent
    }
  }
}
