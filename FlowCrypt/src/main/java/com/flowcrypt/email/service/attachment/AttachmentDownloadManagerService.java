/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
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
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.database.dao.source.imap.ImapLabelsDaoSource;
import com.flowcrypt.email.util.GeneralUtil;
import com.sun.mail.gimap.GmailFolder;
import com.sun.mail.gimap.GmailSSLStore;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;

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
 *         Date: 16.08.2017
 *         Time: 10:29
 *         E-mail: DenBond7@gmail.com
 */

public class AttachmentDownloadManagerService extends Service {
    public static final String ACTION_START_DOWNLOAD_ATTACHMENT = BuildConfig.APPLICATION_ID + "" +
            ".ACTION_START_DOWNLOAD_ATTACHMENT";
    public static final String ACTION_CANCEL_DOWNLOAD_ATTACHMENT = BuildConfig.APPLICATION_ID + "" +
            ".ACTION_CANCEL_DOWNLOAD_ATTACHMENT";
    public static final String ACTION_RETRY_DOWNLOAD_ATTACHMENT = BuildConfig.APPLICATION_ID + "" +
            ".ACTION_RETRY_DOWNLOAD_ATTACHMENT";

    public static final String EXTRA_KEY_START_ID = GeneralUtil.generateUniqueExtraKey
            ("EXTRA_KEY_START_ID", AttachmentDownloadManagerService.class);
    public static final String EXTRA_KEY_ATTACHMENT_INFO = GeneralUtil.generateUniqueExtraKey
            ("EXTRA_KEY_ATTACHMENT_INFO", AttachmentDownloadManagerService.class);

    private static final String TAG = AttachmentDownloadManagerService.class.getSimpleName();

    private volatile Looper serviceWorkerLooper;
    private volatile AttachmentDownloadManagerService.ServiceWorkerHandler serviceWorkerHandler;
    private volatile int callCounter;

    private Messenger replyMessenger;
    private AttachmentNotificationManager attachmentNotificationManager;

    public AttachmentDownloadManagerService() {
        this.replyMessenger = new Messenger(new AttachmentDownloadManagerService.ReplyHandler(this));
    }

    public static Intent newAttachmentDownloadIntent(Context context, AttachmentInfo attachmentInfo) {
        Intent intent = new Intent(context, AttachmentDownloadManagerService.class);
        intent.setAction(ACTION_START_DOWNLOAD_ATTACHMENT);
        intent.putExtra(EXTRA_KEY_ATTACHMENT_INFO, attachmentInfo);
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
            AttachmentInfo attachmentInfo = intent.getParcelableExtra(EXTRA_KEY_ATTACHMENT_INFO);
            switch (intent.getAction()) {
                case ACTION_CANCEL_DOWNLOAD_ATTACHMENT:
                    int startIdOfCanceledAttachment = intent.getIntExtra(EXTRA_KEY_START_ID, -1);
                    cancelDownloadAttachment(startIdOfCanceledAttachment, attachmentInfo);
                    break;

                case ACTION_RETRY_DOWNLOAD_ATTACHMENT:
                case ACTION_START_DOWNLOAD_ATTACHMENT:
                    if (attachmentInfo != null) {
                        addDownloadTaskToQueue(getApplicationContext(), startId, attachmentInfo);
                    }
                    break;
            }
        }

        if (callCounter == 0) {
            stopSelf();
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

    /**
     * Try to stop current service if all started works done.
     *
     * @return true if service stopped, otherwise false.
     */
    private boolean tryToStopService() {
        boolean isServiceStopped = false;

        if (callCounter != 0) {
            callCounter--;
        }

        if (callCounter == 0) {
            stopSelf();
            isServiceStopped = true;
        }

        Log.d(TAG, "tryToStopService| callCounter = " + callCounter + "| isServiceStopped = " + isServiceStopped);
        return isServiceStopped;
    }

    private void releaseResources() {
        Message message = serviceWorkerHandler.obtainMessage();
        message.what = AttachmentDownloadManagerService.ServiceWorkerHandler.MESSAGE_RELEASE_RESOURCES;
        serviceWorkerHandler.sendMessage(message);
    }

    private void cancelDownloadAttachment(int startIdOfCanceledAttachment, AttachmentInfo attachmentInfo) {
        tryToStopService();
        attachmentNotificationManager.loadCanceledByUser(startIdOfCanceledAttachment);
        Message message = serviceWorkerHandler.obtainMessage();
        message.what = AttachmentDownloadManagerService.ServiceWorkerHandler.MESSAGE_CANCEL_DOWNLOAD;
        message.obj = attachmentInfo;
        message.arg1 = startIdOfCanceledAttachment;
        serviceWorkerHandler.sendMessage(message);
    }

    private void addDownloadTaskToQueue(Context context, int startId, AttachmentInfo attachmentInfo) {
        callCounter++;
        Message message = serviceWorkerHandler.obtainMessage();
        message.what = AttachmentDownloadManagerService.ServiceWorkerHandler.MESSAGE_START_DOWNLOAD;
        message.arg1 = startId;
        message.obj = new DownloadAttachmentTaskRequest(context, startId, attachmentInfo);
        serviceWorkerHandler.sendMessage(message);
    }

    private interface OnDownloadAttachmentListener {
        void onAttachmentSuccessDownloaded(int startId, AttachmentInfo attachmentInfo, Uri uri);

        void onAttachmentDownloadFiled(int startId, AttachmentInfo attachmentInfo, Exception e);

        void onProgress(int startId, AttachmentInfo attachmentInfo, int progressInPercentage, long timeLeft);
    }

    /**
     * The incoming handler realization. This handler will be used to communicate with current
     * service and the worker thread.
     */
    private static class ReplyHandler extends Handler {
        static final int MESSAGE_EXCEPTION_HAPPENED = 1;
        static final int MESSAGE_DOWNLOAD_TASK_FOR_ATTACHMENT_ALREADY_EXISTS = 2;
        static final int MESSAGE_ATTACHMENT_DOWNLOAD = 3;
        static final int MESSAGE_DOWNLOAD_STARTED = 4;
        static final int MESSAGE_ATTACHMENT_ADDED_TO_QUEUE = 5;
        static final int MESSAGE_PROGRESS = 6;
        static final int MESSAGE_RELEASE_RESOURCES = 8;

        private final WeakReference<AttachmentDownloadManagerService>
                checkClipboardToFindPrivateKeyServiceWeakReference;

        ReplyHandler(AttachmentDownloadManagerService checkClipboardToFindPrivateKeyService) {
            this.checkClipboardToFindPrivateKeyServiceWeakReference = new WeakReference<>
                    (checkClipboardToFindPrivateKeyService);
        }

        @Override
        public void handleMessage(Message message) {
            if (checkClipboardToFindPrivateKeyServiceWeakReference.get() != null) {
                AttachmentDownloadManagerService attachmentDownloadManagerService =
                        checkClipboardToFindPrivateKeyServiceWeakReference.get();

                DownloadAttachmentTaskResult downloadAttachmentTaskResult =
                        (DownloadAttachmentTaskResult) message.obj;
                int startId = downloadAttachmentTaskResult.getStartId();
                AttachmentInfo attachmentInfo = downloadAttachmentTaskResult.getAttachmentInfo();
                Uri uri = downloadAttachmentTaskResult.getUri();
                switch (message.what) {
                    case MESSAGE_EXCEPTION_HAPPENED:
                        attachmentDownloadManagerService.tryToStopService();
                        attachmentDownloadManagerService.attachmentNotificationManager.errorHappened
                                (attachmentDownloadManagerService, startId, attachmentInfo,
                                        downloadAttachmentTaskResult.getException());
                        break;

                    case MESSAGE_DOWNLOAD_TASK_FOR_ATTACHMENT_ALREADY_EXISTS:
                        attachmentDownloadManagerService.tryToStopService();
                        Toast.makeText(attachmentDownloadManagerService,
                                attachmentDownloadManagerService.getString(R.string.template_attachment_already_loading,
                                        attachmentInfo.getName()), Toast.LENGTH_SHORT).show();
                        break;

                    case MESSAGE_ATTACHMENT_DOWNLOAD:
                        attachmentDownloadManagerService.tryToStopService();
                        attachmentDownloadManagerService.attachmentNotificationManager.downloadComplete
                                (attachmentDownloadManagerService, startId, attachmentInfo, uri);
                        break;

                    case MESSAGE_ATTACHMENT_ADDED_TO_QUEUE:
                        attachmentDownloadManagerService.attachmentNotificationManager.attachmentAddedToLoadQueue
                                (attachmentDownloadManagerService, startId, attachmentInfo);
                        break;

                    case MESSAGE_PROGRESS:
                        attachmentDownloadManagerService.attachmentNotificationManager.updateLoadingProgress
                                (attachmentDownloadManagerService, startId, attachmentInfo,
                                        downloadAttachmentTaskResult.getProgressInPercentage(),
                                        downloadAttachmentTaskResult.getTimeLeft());
                        break;

                    case MESSAGE_RELEASE_RESOURCES:
                        attachmentDownloadManagerService.serviceWorkerLooper.quit();
                        break;
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
        /**
         * Maximum number of simultaneous downloads
         */
        private static final int QUEUE_SIZE = 3;
        private final Messenger replyMessenger;
        private ExecutorService executorService;
        private HashMap<String, AttachmentInfo> stringAttachmentInfoHashMap;
        private SparseArray<Future<?>> futureSparseArray;

        ServiceWorkerHandler(Looper looper, Messenger replyMessenger) {
            super(looper);
            this.replyMessenger = replyMessenger;
            this.executorService = Executors.newFixedThreadPool(QUEUE_SIZE);
            this.stringAttachmentInfoHashMap = new HashMap<>();
            this.futureSparseArray = new SparseArray<>();
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_START_DOWNLOAD:
                    DownloadAttachmentTaskRequest downloadAttachmentTaskRequest
                            = (DownloadAttachmentTaskRequest) msg.obj;

                    AttachmentInfo attachmentInfo = downloadAttachmentTaskRequest.getAttachmentInfo();
                    Context context = downloadAttachmentTaskRequest.getContext();
                    int startId = downloadAttachmentTaskRequest.getStartId();

                    try {
                        if (stringAttachmentInfoHashMap.get(attachmentInfo.getId()) == null) {
                            stringAttachmentInfoHashMap.put(attachmentInfo.getId(), attachmentInfo);
                            AttachmentDownloadRunnable attachmentDownloadRunnable = new AttachmentDownloadRunnable
                                    (context.getApplicationContext(), startId, attachmentInfo);
                            attachmentDownloadRunnable.setOnDownloadAttachmentListener(this);
                            futureSparseArray.put(startId, executorService.submit(attachmentDownloadRunnable));
                            replyMessenger.send(Message.obtain(null, ReplyHandler.MESSAGE_ATTACHMENT_ADDED_TO_QUEUE,
                                    new DownloadAttachmentTaskResult.Builder().setStartId(startId).setAttachmentInfo
                                            (attachmentInfo).setException(null).setUri(null)
                                            .build()));
                        } else {
                            notifyAboutAttachmentDownloadTaskAlreadyExists(startId, attachmentInfo);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        try {
                            replyMessenger.send(Message.obtain(null, ReplyHandler.MESSAGE_EXCEPTION_HAPPENED,
                                    new DownloadAttachmentTaskResult.Builder().setStartId(startId).setAttachmentInfo
                                            (attachmentInfo).setException(e).build()));
                        } catch (RemoteException e1) {
                            e1.printStackTrace();
                        }
                    }
                    break;

                case MESSAGE_CANCEL_DOWNLOAD:
                    Future future = futureSparseArray.get(msg.arg1);
                    if (future != null) {
                        future.cancel(true);
                    }

                    AttachmentInfo canceledAttachmentInfo = (AttachmentInfo) msg.obj;
                    stringAttachmentInfoHashMap.remove(canceledAttachmentInfo.getId());
                    futureSparseArray.remove(msg.arg1);
                    break;

                case MESSAGE_RELEASE_RESOURCES:
                    if (executorService != null) {
                        executorService.shutdown();
                    }

                    try {
                        DownloadAttachmentTaskResult downloadAttachmentTaskResult
                                = new DownloadAttachmentTaskResult.Builder().build();
                        replyMessenger.send(Message.obtain(null, ReplyHandler.MESSAGE_RELEASE_RESOURCES,
                                downloadAttachmentTaskResult));
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }

        @Override
        public void onAttachmentDownloadFiled(int startId, AttachmentInfo attachmentInfo, Exception e) {
            stringAttachmentInfoHashMap.remove(attachmentInfo.getId());
            try {
                DownloadAttachmentTaskResult downloadAttachmentTaskResult = new DownloadAttachmentTaskResult.Builder()
                        .setStartId(startId).setAttachmentInfo(attachmentInfo).setException(e).build();
                replyMessenger.send(Message.obtain(null, ReplyHandler.MESSAGE_EXCEPTION_HAPPENED,
                        downloadAttachmentTaskResult));
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }
        }

        @Override
        public void onProgress(int startId, AttachmentInfo attachmentInfo, int progressInPercentage, long timeLeft) {
            try {
                DownloadAttachmentTaskResult downloadAttachmentTaskResult = new DownloadAttachmentTaskResult.Builder()
                        .setStartId(startId).setAttachmentInfo(attachmentInfo)
                        .setProgressInPercentage(progressInPercentage).setTimeLeft(timeLeft).build();
                replyMessenger.send(Message.obtain(null, ReplyHandler.MESSAGE_PROGRESS,
                        downloadAttachmentTaskResult));
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }
        }

        @Override
        public void onAttachmentSuccessDownloaded(int startId, AttachmentInfo attachmentInfo, Uri uri) {
            stringAttachmentInfoHashMap.remove(attachmentInfo.getId());
            try {
                DownloadAttachmentTaskResult downloadAttachmentTaskResult = new DownloadAttachmentTaskResult.Builder()
                        .setStartId(startId).setAttachmentInfo(attachmentInfo).setUri(uri).build();
                replyMessenger.send(Message.obtain(null, ReplyHandler.MESSAGE_ATTACHMENT_DOWNLOAD,
                        downloadAttachmentTaskResult));
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }
        }

        private void notifyAboutAttachmentDownloadTaskAlreadyExists(int startId, AttachmentInfo attachmentInfo) {
            try {
                DownloadAttachmentTaskResult downloadAttachmentTaskResult = new DownloadAttachmentTaskResult.Builder()
                        .setStartId(startId).setAttachmentInfo(attachmentInfo).build();
                replyMessenger.send(Message.obtain(null, ReplyHandler
                                .MESSAGE_DOWNLOAD_TASK_FOR_ATTACHMENT_ALREADY_EXISTS,
                        downloadAttachmentTaskResult));
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }
        }
    }

    private static class AttachmentDownloadRunnable implements Runnable {
        private static final int MIN_UPDATE_PROGRESS_INTERVAL = 500;
        private static final int DEFAULT_BUFFER_SIZE = 1024 * 16;
        private AttachmentInfo attachmentInfo;
        private Context context;
        private int startId;
        private OnDownloadAttachmentListener onDownloadAttachmentListener;

        public AttachmentDownloadRunnable(Context context, int startId, AttachmentInfo attachmentInfo) {
            this.context = context;
            this.startId = startId;
            this.attachmentInfo = attachmentInfo;
        }

        @Override
        public void run() {
            if (BuildConfig.DEBUG) {
                Thread.currentThread().setName(AttachmentDownloadRunnable.class.getSimpleName() + "|" + startId
                        + "|" + attachmentInfo.getName());
            }

            File attachmentFile = prepareAttachmentFile();

            try {
                //TODO-DenBond7 ISSUE_75 Need fix after use different providers
                GmailSSLStore gmailSSLStore = OpenStoreHelper.openAndConnectToGimapsStore(context,
                        OpenStoreHelper.getAttachmentGmailSession(), null);
                GmailFolder gmailFolder = (GmailFolder) gmailSSLStore.getFolder(new ImapLabelsDaoSource()
                        .getFolderByAlias(context, attachmentInfo.getEmail(),
                                attachmentInfo.getFolder()).getServerFullFolderName());
                gmailFolder.open(Folder.READ_ONLY);

                javax.mail.Message message = gmailFolder.getMessageByUID(attachmentInfo.getUid());
                Part attachment = getAttachmentPart(message, attachmentInfo.getId());

                if (attachment != null) {
                    InputStream input = attachment.getInputStream();
                    OutputStream output = FileUtils.openOutputStream(attachmentFile);

                    try {
                        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                        double count = 0;
                        double size = attachmentInfo.getEncodedSize();
                        int numberOfReadBytes;
                        int lastPercentage = 0;
                        int currentPercentage = 0;
                        long startTime, elapsedTime;
                        long lastUpdateTime = startTime = System.currentTimeMillis();
                        updateProgress(currentPercentage, 0);
                        while (IOUtils.EOF != (numberOfReadBytes = input.read(buffer))) {
                            if (!Thread.currentThread().isInterrupted()) {
                                output.write(buffer, 0, numberOfReadBytes);
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

                        if (!Thread.currentThread().isInterrupted()) {
                            updateProgress(100, 0);
                        }

                        output.close();
                    } finally {
                        IOUtils.closeQuietly(output);
                        if (Thread.currentThread().isInterrupted()) {
                            removeNotCompleteDownloadFile(attachmentFile);
                        }
                    }

                    if (!Thread.currentThread().isInterrupted()) {
                        if (onDownloadAttachmentListener != null) {
                            onDownloadAttachmentListener.onAttachmentSuccessDownloaded(startId, attachmentInfo,
                                    FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider",
                                            attachmentFile));
                        }
                    }
                } else throw new IOException("The attachment does not exists on an IMAP server.");

                gmailFolder.close(false);
                gmailSSLStore.close();
            } catch (Exception e) {
                e.printStackTrace();
                removeNotCompleteDownloadFile(attachmentFile);
                if (onDownloadAttachmentListener != null) {
                    onDownloadAttachmentListener.onAttachmentDownloadFiled(startId, attachmentInfo, e);
                }
            }
        }

        public void setOnDownloadAttachmentListener(OnDownloadAttachmentListener onDownloadAttachmentListener) {
            this.onDownloadAttachmentListener = onDownloadAttachmentListener;
        }

        /**
         * Remove the file which not downloaded fully.
         *
         * @param attachmentFile The file which will be removed.
         */
        private void removeNotCompleteDownloadFile(File attachmentFile) {
            if (!attachmentFile.delete()) {
                Log.d(TAG, "Cannot delete file: " + attachmentFile);
            } else {
                Log.d(TAG, "Canceled attachment \"" + attachmentFile + "\" was deleted");
            }
        }

        private void updateProgress(int currentPercentage, long timeLeft) {
            if (onDownloadAttachmentListener != null) {
                onDownloadAttachmentListener.onProgress(startId, attachmentInfo, currentPercentage, timeLeft);
            }
        }

        /**
         * Create the local file where we will write an input stream from the IMAP server.
         *
         * @return A new created file.
         */
        private File prepareAttachmentFile() {
            return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    attachmentInfo.getName());
        }

        /**
         * Get {@link Part} which has an attachment with such attachment id.
         *
         * @param part The parent part.
         * @return {@link Part} which has attachment or null if message doesn't have such attachment.
         * @throws MessagingException
         * @throws IOException
         */
        private Part getAttachmentPart(Part part, String attachmentId) throws MessagingException, IOException {
            if (part != null && part.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
                Multipart multiPart = (Multipart) part.getContent();
                int numberOfParts = multiPart.getCount();
                String[] headers;
                for (int partCount = 0; partCount < numberOfParts; partCount++) {
                    BodyPart bodyPart = multiPart.getBodyPart(partCount);
                    if (bodyPart.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
                        Part innerPart = getAttachmentPart(bodyPart, attachmentId);
                        if (innerPart != null) {
                            return innerPart;
                        }
                    } else if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())
                            && (headers = bodyPart.getHeader(JavaEmailConstants.HEADER_X_ATTACHMENT_ID)) != null
                            && headers.length > 0
                            && attachmentId.equals(headers[0])) {
                        return bodyPart;
                    }
                }
                return null;
            } else {
                return null;
            }
        }
    }
}
