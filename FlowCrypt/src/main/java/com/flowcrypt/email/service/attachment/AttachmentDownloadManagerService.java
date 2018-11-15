/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.attachment;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.api.email.protocol.ImapProtocolUtil;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.database.dao.source.imap.ImapLabelsDaoSource;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.PgpDecrypted;
import com.flowcrypt.email.security.SecurityStorageConnector;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.google.android.gms.common.util.CollectionUtils;
import com.sun.mail.imap.IMAPFolder;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.mail.Folder;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

/**
 * This service will be use to download email attachments. To start load an attachment just run service via the intent
 * {@link AttachmentDownloadManagerService#newAttachmentDownloadIntent(Context, AttachmentInfo)}
 * <p>
 * This service can do:
 * <p>
 * <ul>
 * <li>Can download simultaneously 3 attachments. The other attachments will be added to the queue.</li>
 * <li>All loading attachments will visible in the status bar</li>
 * <li>The user can stop loading an attachment at any time.</li>
 * </ul>
 *
 * @author Denis Bondarenko
 * Date: 16.08.2017
 * Time: 10:29
 * E-mail: DenBond7@gmail.com
 */

public class AttachmentDownloadManagerService extends Service {
  public static final String ACTION_START_DOWNLOAD_ATTACHMENT = BuildConfig.APPLICATION_ID + "" +
      ".ACTION_START_DOWNLOAD_ATTACHMENT";
  public static final String ACTION_CANCEL_DOWNLOAD_ATTACHMENT = BuildConfig.APPLICATION_ID + "" +
      ".ACTION_CANCEL_DOWNLOAD_ATTACHMENT";
  public static final String ACTION_RETRY_DOWNLOAD_ATTACHMENT = BuildConfig.APPLICATION_ID + "" +
      ".ACTION_RETRY_DOWNLOAD_ATTACHMENT";

  public static final String EXTRA_KEY_ATTACHMENT_INFO = GeneralUtil.generateUniqueExtraKey
      ("EXTRA_KEY_ATTACHMENT_INFO", AttachmentDownloadManagerService.class);

  private static final String TAG = AttachmentDownloadManagerService.class.getSimpleName();

  private volatile Looper serviceWorkerLooper;
  private volatile AttachmentDownloadManagerService.ServiceWorkerHandler serviceWorkerHandler;

  private Messenger replyMessenger;
  private AttachmentNotificationManager attachmentNotificationManager;

  public AttachmentDownloadManagerService() {
    this.replyMessenger = new Messenger(new AttachmentDownloadManagerService.ReplyHandler(this));
  }

  public static Intent newAttachmentDownloadIntent(Context context, AttachmentInfo attInfo) {
    Intent intent = new Intent(context, AttachmentDownloadManagerService.class);
    intent.setAction(ACTION_START_DOWNLOAD_ATTACHMENT);
    intent.putExtra(EXTRA_KEY_ATTACHMENT_INFO, attInfo);
    return intent;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    Log.d(TAG, "onCreate");

    HandlerThread handlerThread = new HandlerThread(TAG);
    handlerThread.start();

    serviceWorkerLooper = handlerThread.getLooper();
    serviceWorkerHandler =
        new AttachmentDownloadManagerService.ServiceWorkerHandler(serviceWorkerLooper, replyMessenger);
    attachmentNotificationManager = new AttachmentNotificationManager(getApplicationContext());
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.d(TAG, "onStartCommand |intent =" + intent + "|flags = " + flags + "|startId = " + startId);
    if (intent != null && !TextUtils.isEmpty(intent.getAction())) {
      AttachmentInfo attInfo = intent.getParcelableExtra(EXTRA_KEY_ATTACHMENT_INFO);
      switch (intent.getAction()) {
        case ACTION_CANCEL_DOWNLOAD_ATTACHMENT:
          cancelDownloadAttachment(attInfo);
          break;

        case ACTION_RETRY_DOWNLOAD_ATTACHMENT:
        case ACTION_START_DOWNLOAD_ATTACHMENT:
          if (attInfo != null) {
            addDownloadTaskToQueue(getApplicationContext(), attInfo);
          }
          break;

        default:
          checkAndStopIfNeeded();
          break;
      }
    }

    return super.onStartCommand(intent, flags, startId);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "onDestroy");
    releaseResources();
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  private void stopService() {
    stopSelf();
    Log.d(TAG, "Trying to stop the service");
  }

  private void releaseResources() {
    Message message = serviceWorkerHandler.obtainMessage();
    message.what = AttachmentDownloadManagerService.ServiceWorkerHandler.MESSAGE_RELEASE_RESOURCES;
    serviceWorkerHandler.sendMessage(message);
  }

  private void cancelDownloadAttachment(AttachmentInfo attInfo) {
    attachmentNotificationManager.loadCanceledByUser(attInfo);
    Message message = serviceWorkerHandler.obtainMessage();
    message.what = AttachmentDownloadManagerService.ServiceWorkerHandler.MESSAGE_CANCEL_DOWNLOAD;
    message.obj = attInfo;
    serviceWorkerHandler.sendMessage(message);
  }

  private void addDownloadTaskToQueue(Context context, AttachmentInfo attInfo) {
    Message message = serviceWorkerHandler.obtainMessage();
    message.what = AttachmentDownloadManagerService.ServiceWorkerHandler.MESSAGE_START_DOWNLOAD;
    message.obj = new DownloadAttachmentTaskRequest(context, attInfo);
    serviceWorkerHandler.sendMessage(message);
  }

  private void checkAndStopIfNeeded() {
    Message message = serviceWorkerHandler.obtainMessage();
    message.what = AttachmentDownloadManagerService.ServiceWorkerHandler.MESSAGE_CHECK_AND_STOP_IF_NEEDED;
    serviceWorkerHandler.sendMessage(message);
  }

  private interface OnDownloadAttachmentListener {
    void onAttachmentDownloaded(AttachmentInfo attInfo, Uri uri);

    void onCanceled(AttachmentInfo attInfo);

    void onError(AttachmentInfo attInfo, Exception e);

    void onProgress(AttachmentInfo attInfo, int progressInPercentage, long timeLeft);
  }

  /**
   * The incoming handler realization. This handler will be used to communicate with current
   * service and the worker thread.
   */
  private static class ReplyHandler extends Handler {
    static final int MESSAGE_EXCEPTION_HAPPENED = 1;
    static final int MESSAGE_TASK_ALREADY_EXISTS = 2;
    static final int MESSAGE_ATTACHMENT_DOWNLOAD = 3;
    static final int MESSAGE_DOWNLOAD_STARTED = 4;
    static final int MESSAGE_ATTACHMENT_ADDED_TO_QUEUE = 5;
    static final int MESSAGE_PROGRESS = 6;
    static final int MESSAGE_RELEASE_RESOURCES = 8;
    static final int MESSAGE_DOWNLOAD_CANCELED = 9;
    static final int MESSAGE_STOP_SERVICE = 10;

    private final WeakReference<AttachmentDownloadManagerService> weakReference;

    ReplyHandler(AttachmentDownloadManagerService attDownloadManagerService) {
      this.weakReference = new WeakReference<>(attDownloadManagerService);
    }

    @Override
    public void handleMessage(Message message) {
      if (weakReference.get() != null) {
        AttachmentDownloadManagerService attDownloadManagerService = weakReference.get();

        DownloadAttachmentTaskResult downloadAttachmentTaskResult = (DownloadAttachmentTaskResult) message.obj;
        AttachmentInfo attInfo = downloadAttachmentTaskResult.getAttachmentInfo();
        Uri uri = downloadAttachmentTaskResult.getUri();

        switch (message.what) {
          case MESSAGE_EXCEPTION_HAPPENED:
            attDownloadManagerService.attachmentNotificationManager.errorHappened
                (attDownloadManagerService, attInfo, downloadAttachmentTaskResult.getException());
            break;

          case MESSAGE_TASK_ALREADY_EXISTS:
            Toast.makeText(attDownloadManagerService,
                attDownloadManagerService.getString(R.string.template_attachment_already_loading,
                    attInfo.getName()), Toast.LENGTH_SHORT).show();
            break;

          case MESSAGE_ATTACHMENT_DOWNLOAD:
            attDownloadManagerService.attachmentNotificationManager.downloadComplete
                (attDownloadManagerService, attInfo, uri);
            Log.d(TAG, attInfo.getName() + " is downloaded");
            break;

          case MESSAGE_ATTACHMENT_ADDED_TO_QUEUE:
            attDownloadManagerService.attachmentNotificationManager.attachmentAddedToLoadQueue
                (attDownloadManagerService, attInfo);
            break;

          case MESSAGE_PROGRESS:
            attDownloadManagerService.attachmentNotificationManager.updateLoadingProgress
                (attDownloadManagerService, attInfo, downloadAttachmentTaskResult.getProgressInPercentage(),
                    downloadAttachmentTaskResult.getTimeLeft());
            break;

          case MESSAGE_RELEASE_RESOURCES:
            attDownloadManagerService.serviceWorkerLooper.quit();
            break;

          case MESSAGE_DOWNLOAD_CANCELED:
            attDownloadManagerService.attachmentNotificationManager.loadCanceledByUser(attInfo);
            Log.d(TAG, attInfo.getName() + " was canceled");
            break;

          case MESSAGE_STOP_SERVICE:
            attDownloadManagerService.stopService();
            break;
        }

        if (downloadAttachmentTaskResult.isLast()) {
          attDownloadManagerService.stopService();
        }
      }
    }
  }

  /**
   * This handler will be used by the instance of {@link HandlerThread} to receive message from
   * the UI thread.
   */
  private static class ServiceWorkerHandler extends Handler implements OnDownloadAttachmentListener {
    static final int MESSAGE_START_DOWNLOAD = 1;
    static final int MESSAGE_CANCEL_DOWNLOAD = 2;
    static final int MESSAGE_RELEASE_RESOURCES = 3;
    static final int MESSAGE_CHECK_AND_STOP_IF_NEEDED = 4;
    /**
     * Maximum number of simultaneous downloads
     */
    private static final int QUEUE_SIZE = 3;
    private final Messenger replyMessenger;
    private ExecutorService executorService;
    private HashMap<String, AttachmentInfo> stringAttachmentInfoHashMap;
    private HashMap<String, Future<?>> futureMap;

    ServiceWorkerHandler(Looper looper, Messenger replyMessenger) {
      super(looper);
      this.replyMessenger = replyMessenger;
      this.executorService = Executors.newFixedThreadPool(QUEUE_SIZE);
      this.stringAttachmentInfoHashMap = new HashMap<>();
      this.futureMap = new HashMap<>();
    }

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MESSAGE_START_DOWNLOAD:
          DownloadAttachmentTaskRequest downloadAttachmentTaskRequest = (DownloadAttachmentTaskRequest) msg.obj;

          AttachmentInfo attInfo = downloadAttachmentTaskRequest.getAttInfo();
          Context context = downloadAttachmentTaskRequest.getContext();

          try {
            if (stringAttachmentInfoHashMap.get(attInfo.getId()) == null) {
              stringAttachmentInfoHashMap.put(attInfo.getId(), attInfo);
              AttDownloadRunnable attDownloadRunnable = new AttDownloadRunnable(context, attInfo);
              attDownloadRunnable.setOnDownloadAttListener(this);
              futureMap.put(attInfo.getUniqueStringId(), executorService.submit(attDownloadRunnable));
              replyMessenger.send(Message.obtain(null, ReplyHandler.MESSAGE_ATTACHMENT_ADDED_TO_QUEUE,
                  new DownloadAttachmentTaskResult.Builder().setAttachmentInfo(attInfo).setException(null).setUri(null)
                      .build()));
            } else {
              notifyTaskAlreadyExists(attInfo);
            }
          } catch (Exception e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
            try {
              replyMessenger.send(Message.obtain(null, ReplyHandler.MESSAGE_EXCEPTION_HAPPENED,
                  new DownloadAttachmentTaskResult.Builder().setAttachmentInfo(attInfo).setException(e).build()));
            } catch (RemoteException remoteException) {
              remoteException.printStackTrace();
            }
          }
          break;

        case MESSAGE_CANCEL_DOWNLOAD:
          AttachmentInfo canceledAttInfo = (AttachmentInfo) msg.obj;
          Future future = futureMap.get(canceledAttInfo.getUniqueStringId());
          if (future != null) {
            future.cancel(true);
          }

          stringAttachmentInfoHashMap.remove(canceledAttInfo.getId());
          break;

        case MESSAGE_RELEASE_RESOURCES:
          if (executorService != null) {
            executorService.shutdown();
          }

          try {
            replyMessenger.send(Message.obtain(null, ReplyHandler.MESSAGE_RELEASE_RESOURCES,
                new DownloadAttachmentTaskResult.Builder().build()));
          } catch (RemoteException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
          }
          break;

        case MESSAGE_CHECK_AND_STOP_IF_NEEDED:
          if (CollectionUtils.isEmpty(futureMap.values())) {
            try {
              replyMessenger.send(Message.obtain(null, ReplyHandler.MESSAGE_STOP_SERVICE,
                  new DownloadAttachmentTaskResult.Builder().build()));
            } catch (RemoteException e) {
              e.printStackTrace();
              ExceptionUtil.handleError(e);
            }
          }
          break;
      }
    }

    @Override
    public void onError(AttachmentInfo attInfo, Exception e) {
      stringAttachmentInfoHashMap.remove(attInfo.getId());
      futureMap.remove(attInfo.getUniqueStringId());
      try {
        replyMessenger.send(Message.obtain(null, ReplyHandler.MESSAGE_EXCEPTION_HAPPENED,
            new DownloadAttachmentTaskResult.Builder().setAttachmentInfo(attInfo).setException(e)
                .setLast(isItLastWorkingTask()).build()));
      } catch (RemoteException remoteException) {
        remoteException.printStackTrace();
      }
    }

    @Override
    public void onProgress(AttachmentInfo attInfo, int progressInPercentage, long timeLeft) {
      try {
        replyMessenger.send(Message.obtain(null, ReplyHandler.MESSAGE_PROGRESS, new DownloadAttachmentTaskResult
            .Builder().setAttachmentInfo(attInfo).setProgressInPercentage(progressInPercentage)
            .setTimeLeft(timeLeft).build()));
      } catch (RemoteException remoteException) {
        remoteException.printStackTrace();
      }
    }

    @Override
    public void onAttachmentDownloaded(AttachmentInfo attInfo, Uri uri) {
      stringAttachmentInfoHashMap.remove(attInfo.getId());
      futureMap.remove(attInfo.getUniqueStringId());
      try {
        replyMessenger.send(Message.obtain(null, ReplyHandler.MESSAGE_ATTACHMENT_DOWNLOAD,
            new DownloadAttachmentTaskResult.Builder().setAttachmentInfo(attInfo).setUri(uri)
                .setLast(isItLastWorkingTask()).build()));
      } catch (RemoteException remoteException) {
        remoteException.printStackTrace();
      }
    }

    @Override
    public void onCanceled(AttachmentInfo attInfo) {
      stringAttachmentInfoHashMap.remove(attInfo.getId());
      futureMap.remove(attInfo.getUniqueStringId());
      try {
        replyMessenger.send(Message.obtain(null, ReplyHandler.MESSAGE_DOWNLOAD_CANCELED,
            new DownloadAttachmentTaskResult.Builder().setAttachmentInfo(attInfo)
                .setLast(isItLastWorkingTask()).build()));
      } catch (RemoteException remoteException) {
        remoteException.printStackTrace();
      }
    }

    private void notifyTaskAlreadyExists(AttachmentInfo attInfo) {
      try {
        replyMessenger.send(Message.obtain(null, ReplyHandler.MESSAGE_TASK_ALREADY_EXISTS,
            new DownloadAttachmentTaskResult.Builder().setAttachmentInfo(attInfo)
                .setLast(isItLastWorkingTask()).build()));
      } catch (RemoteException e) {
        e.printStackTrace();
      }
    }

    private boolean isItLastWorkingTask() {
      return CollectionUtils.isEmpty(futureMap.values());
    }
  }

  private static class AttDownloadRunnable implements Runnable {
    private static final int MIN_UPDATE_PROGRESS_INTERVAL = 500;
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 16;
    private AttachmentInfo attInfo;
    private Context context;
    private OnDownloadAttachmentListener onDownloadAttListener;

    AttDownloadRunnable(Context context, AttachmentInfo attInfo) {
      this.context = context;
      this.attInfo = attInfo;
    }

    @Override
    public void run() {
      if (BuildConfig.DEBUG) {
        Thread.currentThread().setName(AttDownloadRunnable.class.getSimpleName() + "|" + attInfo.getName());
      }

      File attachmentFile = prepareAttFile();
      AccountDao accountDao = new AccountDaoSource().getAccountInformation(context, attInfo.getEmail());

      try {
        checkMaxDecryptedFileSize();

        if (attInfo.getUri() != null) {
          InputStream inputStream = context.getContentResolver().openInputStream(attInfo.getUri());
          if (inputStream != null) {
            FileUtils.copyInputStreamToFile(inputStream, attachmentFile);
            attachmentFile = decryptFileIfNeed(context, attachmentFile);
            attInfo.setName(attachmentFile.getName());

            if (!Thread.currentThread().isInterrupted()) {
              if (onDownloadAttListener != null) {
                onDownloadAttListener.onAttachmentDownloaded(attInfo, FileProvider.getUriForFile(context,
                    Constants.FILE_PROVIDER_AUTHORITY, attachmentFile));
              }
            }

            return;
          }
        }

        Session session = OpenStoreHelper.getAttachmentSession(context, accountDao);
        Store store = OpenStoreHelper.openAndConnectToStore(context, accountDao, session);

        com.flowcrypt.email.api.email.Folder folder = new ImapLabelsDaoSource()
            .getFolderByAlias(context, attInfo.getEmail(), attInfo.getFolder());

        if (folder == null) {
          throw new IllegalArgumentException("Folder " + attInfo.getFolder() + " doesn't found in " +
              "the local cache");
        }

        IMAPFolder imapFolder = (IMAPFolder) store.getFolder(folder.getServerFullFolderName());
        imapFolder.open(Folder.READ_ONLY);

        javax.mail.Message message = imapFolder.getMessageByUID(attInfo.getUid());
        Part attachment = ImapProtocolUtil.getAttachmentPartById(imapFolder, message.getMessageNumber(),
            message, attInfo.getId());

        if (attachment != null) {
          InputStream inputStream = attachment.getInputStream();

          try (OutputStream outputStream = FileUtils.openOutputStream(attachmentFile)) {
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            double count = 0;
            double size = attInfo.getEncodedSize();
            int numberOfReadBytes;
            int lastPercentage = 0;
            int currentPercentage = 0;
            long startTime, elapsedTime;
            long lastUpdateTime = startTime = System.currentTimeMillis();
            updateProgress(currentPercentage, 0);
            while (IOUtils.EOF != (numberOfReadBytes = inputStream.read(buffer))) {
              if (!Thread.currentThread().isInterrupted()) {
                outputStream.write(buffer, 0, numberOfReadBytes);
                count += numberOfReadBytes;
                currentPercentage = (int) ((count / size) * 100f);
                if (currentPercentage - lastPercentage >= 1
                    && System.currentTimeMillis() - lastUpdateTime >=
                    MIN_UPDATE_PROGRESS_INTERVAL) {
                  lastPercentage = currentPercentage;
                  lastUpdateTime = System.currentTimeMillis();
                  elapsedTime = lastUpdateTime - startTime;
                  long allTimeForDownloading = (long) (elapsedTime * size / count);
                  updateProgress(currentPercentage, allTimeForDownloading - elapsedTime);
                }
              } else {
                break;
              }
            }

            updateProgress(100, 0);
          } finally {
            if (Thread.currentThread().isInterrupted()) {
              removeNotCompleteDownloadFile(attachmentFile);
              onDownloadAttListener.onCanceled(attInfo);
            }
          }

          attachmentFile = decryptFileIfNeed(context, attachmentFile);
          attInfo.setName(attachmentFile.getName());

          if (!Thread.currentThread().isInterrupted()) {
            if (onDownloadAttListener != null) {
              onDownloadAttListener.onAttachmentDownloaded(attInfo, FileProvider.getUriForFile(context,
                  Constants.FILE_PROVIDER_AUTHORITY, attachmentFile));
            }
          }
        } else throw new IOException("The attachment does not exist on an IMAP server.");

        imapFolder.close(false);
        store.close();
      } catch (Exception e) {
        e.printStackTrace();
        ExceptionUtil.handleError(e);
        removeNotCompleteDownloadFile(attachmentFile);
        if (onDownloadAttListener != null) {
          onDownloadAttListener.onError(attInfo, e);
        }
      }
    }

    void setOnDownloadAttListener(OnDownloadAttachmentListener onDownloadAttListener) {
      this.onDownloadAttListener = onDownloadAttListener;
    }

    /**
     * Check is decrypted file has size not more than
     * {@link Constants#MAX_ATTACHMENT_SIZE_WHICH_CAN_BE_DECRYPTED}. If the file greater then
     * {@link Constants#MAX_ATTACHMENT_SIZE_WHICH_CAN_BE_DECRYPTED} we throw an exception. This is only for files
     * with the "pgp" extension.
     */
    private void checkMaxDecryptedFileSize() {
      if ("pgp".equalsIgnoreCase(FilenameUtils.getExtension(attInfo.getName()))) {
        if (attInfo.getEncodedSize() > Constants.MAX_ATTACHMENT_SIZE_WHICH_CAN_BE_DECRYPTED) {
          throw new IllegalArgumentException(context.getString(R.string
                  .template_warning_max_attachments_size_for_decryption,
              FileUtils.byteCountToDisplaySize(Constants
                  .MAX_ATTACHMENT_SIZE_WHICH_CAN_BE_DECRYPTED)));
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
    private File decryptFileIfNeed(Context context, File file) throws IOException {
      if (file == null) {
        return null;
      }

      if (!"pgp".equalsIgnoreCase(FilenameUtils.getExtension(file.getName()))) {
        return file;
      }

      try (InputStream inputStream = new FileInputStream(file)) {
        PgpDecrypted pgpDecrypted = new Js(context, new SecurityStorageConnector(context))
            .crypto_message_decrypt(IOUtils.toByteArray(inputStream));
        byte[] decryptedBytes = pgpDecrypted.getBytes();

        File decryptedFile = new File(file.getParent(),
            file.getName().substring(0, file.getName().lastIndexOf(".")));

        boolean isInnerExceptionHappened = false;

        try (OutputStream outputStream = FileUtils.openOutputStream(decryptedFile)) {
          IOUtils.write(decryptedBytes, outputStream);
          return decryptedFile;
        } catch (IOException e) {
          if (!decryptedFile.delete()) {
            Log.d(TAG, "Cannot delete file: " + file);
          }

          isInnerExceptionHappened = true;
          throw e;
        } finally {
          if (!isInnerExceptionHappened) {
            if (!file.delete()) {
              Log.d(TAG, "Cannot delete file: " + file);
            }
          }
        }
      }
    }

    /**
     * Remove the file which not downloaded fully.
     *
     * @param attachmentFile The file which will be removed.
     */
    private void removeNotCompleteDownloadFile(File attachmentFile) {
      if (attachmentFile != null && attachmentFile.exists()) {
        if (!attachmentFile.delete()) {
          Log.d(TAG, "Cannot delete a file: " + attachmentFile);
        } else {
          Log.d(TAG, "Canceled attachment \"" + attachmentFile + "\" was deleted");
        }
      }
    }

    private void updateProgress(int currentPercentage, long timeLeft) {
      if (onDownloadAttListener != null && !Thread.currentThread().isInterrupted()) {
        onDownloadAttListener.onProgress(attInfo, currentPercentage, timeLeft);
      }
    }

    /**
     * Create the local file where we will write an input stream from the IMAP server.
     *
     * @return A new created file.
     */
    private File prepareAttFile() {
      return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
          attInfo.getName());
    }
  }
}
