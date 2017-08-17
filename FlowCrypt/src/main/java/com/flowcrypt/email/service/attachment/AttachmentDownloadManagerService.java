/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.attachment;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.gmail.GmailConstants;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.util.GeneralUtil;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.sun.mail.gimap.GmailFolder;
import com.sun.mail.gimap.GmailSSLStore;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    public static final String EXTRA_KEY_ATTACHMENT_INFO = GeneralUtil.generateUniqueExtraKey
            ("EXTRA_KEY_ATTACHMENT_INFO", AttachmentDownloadManagerService.class);
    private static final String TAG = AttachmentDownloadManagerService.class.getSimpleName();

    private volatile Looper serviceWorkerLooper;
    private volatile AttachmentDownloadManagerService.ServiceWorkerHandler serviceWorkerHandler;

    private Messenger replyMessenger;

    public AttachmentDownloadManagerService() {
        this.replyMessenger = new Messenger(new AttachmentDownloadManagerService.ReplyHandler(this));
    }

    public static Intent newAttachmentDownloadIntent(Context context, AttachmentInfo attachmentInfo) {
        Intent intent = new Intent(context, AttachmentDownloadManagerService.class);
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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            AttachmentInfo attachmentInfo = intent.getParcelableExtra(EXTRA_KEY_ATTACHMENT_INFO);
            if (attachmentInfo != null) {
                addDownloadTaskToQueue(getApplicationContext(), startId, attachmentInfo);
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        serviceWorkerLooper.quit();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void addDownloadTaskToQueue(Context context, int startId, AttachmentInfo attachmentInfo) {
        Message message = serviceWorkerHandler.obtainMessage();
        message.what = AttachmentDownloadManagerService.ServiceWorkerHandler.MESSAGE_START_DOWNLOAD;
        message.arg1 = startId;
        message.obj = new DownloadAttachmentTaskRequest(context, startId, attachmentInfo);
        serviceWorkerHandler.sendMessage(message);
    }

    private interface OnDownloadAttachmentListener {
        void onAttachmentSuccessDownloaded(int startId, AttachmentInfo attachmentInfo);

        void onAttachmentDownloadFiled(int startId, AttachmentInfo attachmentInfo, Exception e);

        void onAttachmentDownloadCanceled(int startId, AttachmentInfo attachmentInfo);
    }

    /**
     * The incoming handler realization. This handler will be used to communicate with current
     * service and the worker thread.
     */
    private static class ReplyHandler extends Handler {
        static final int MESSAGE_EXCEPTION_HAPPENED = 1;
        static final int MESSAGE_DOWNLOAD_TASK_FOR_ATTACHMENT_ALREADY_EXISTS = 2;
        static final int MESSAGE_ATTACHMENT_DOWNLOAD = 3;
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

                AttachmentInfo attachmentInfo = downloadAttachmentTaskResult.getAttachmentInfo();

                switch (message.what) {
                    case MESSAGE_EXCEPTION_HAPPENED:
                        Exception e = downloadAttachmentTaskResult.getException();
                        Toast.makeText(attachmentDownloadManagerService, attachmentInfo.getName() + " | " +
                                e.getMessage(), Toast.LENGTH_SHORT).show();
                        attachmentDownloadManagerService.stopSelf(downloadAttachmentTaskResult.getStartId());
                        break;

                    case MESSAGE_DOWNLOAD_TASK_FOR_ATTACHMENT_ALREADY_EXISTS:
                        Toast.makeText(attachmentDownloadManagerService, "Task for " + attachmentInfo.getName() + " " +
                                "already " + "exists", Toast.LENGTH_SHORT).show();
                        attachmentDownloadManagerService.stopSelf(downloadAttachmentTaskResult.getStartId());
                        break;

                    case MESSAGE_ATTACHMENT_DOWNLOAD:
                        Toast.makeText(attachmentDownloadManagerService, attachmentInfo.getName() + " downloaded"
                                , Toast.LENGTH_SHORT).show();
                        attachmentDownloadManagerService.stopSelf(downloadAttachmentTaskResult.getStartId());
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

        /**
         * Maximum number of simultaneous downloads
         */
        private static final int QUEUE_SIZE = 3;
        private final Messenger replyMessenger;
        private ExecutorService executorService;
        private HashMap<String, AttachmentInfo> stringAttachmentInfoHashMap;

        ServiceWorkerHandler(Looper looper, Messenger replyMessenger) {
            super(looper);
            this.replyMessenger = replyMessenger;
            this.executorService = Executors.newFixedThreadPool(QUEUE_SIZE);
            this.stringAttachmentInfoHashMap = new HashMap<>();
        }

        @Override
        public void handleMessage(Message msg) {
            DownloadAttachmentTaskRequest downloadAttachmentTaskRequest
                    = (DownloadAttachmentTaskRequest) msg.obj;

            AttachmentInfo attachmentInfo = downloadAttachmentTaskRequest.getAttachmentInfo();
            Context context = downloadAttachmentTaskRequest.getContext();
            int startId = downloadAttachmentTaskRequest.getStartId();

            switch (msg.what) {
                case MESSAGE_START_DOWNLOAD:
                    try {
                        if (stringAttachmentInfoHashMap.get(attachmentInfo.getId()) == null) {
                            stringAttachmentInfoHashMap.put(attachmentInfo.getId(), attachmentInfo);
                            String token = GoogleAuthUtil.getToken(context, attachmentInfo.getGoogleAccount(),
                                    JavaEmailConstants.OAUTH2 + GmailConstants.SCOPE_MAIL_GOOGLE_COM);

                            AttachmentDownloadRunnable attachmentDownloadRunnable = new AttachmentDownloadRunnable
                                    (token, startId, attachmentInfo);
                            attachmentDownloadRunnable.setOnDownloadAttachmentListener(this);
                            executorService.submit(attachmentDownloadRunnable);
                        } else {
                            notifyAboutAttachmentDownloadTaskAlreadyExists(startId, attachmentInfo);
                        }
                    } catch (IOException | GoogleAuthException e) {
                        e.printStackTrace();
                        try {
                            replyMessenger.send(Message.obtain(null, ReplyHandler.MESSAGE_EXCEPTION_HAPPENED,
                                    new DownloadAttachmentTaskResult(startId, attachmentInfo, e)));
                        } catch (RemoteException e1) {
                            e1.printStackTrace();
                        }
                    }
                    break;
            }
        }

        @Override
        public void onAttachmentSuccessDownloaded(int startId, AttachmentInfo attachmentInfo) {
            stringAttachmentInfoHashMap.remove(attachmentInfo.getId());
            try {
                DownloadAttachmentTaskResult downloadAttachmentTaskResult = new DownloadAttachmentTaskResult
                        (startId, attachmentInfo, null);
                replyMessenger.send(Message.obtain(null, ReplyHandler.MESSAGE_ATTACHMENT_DOWNLOAD,
                        downloadAttachmentTaskResult));
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }
        }

        @Override
        public void onAttachmentDownloadFiled(int startId, AttachmentInfo attachmentInfo, Exception e) {
            stringAttachmentInfoHashMap.remove(attachmentInfo.getId());
            try {
                DownloadAttachmentTaskResult downloadAttachmentTaskResult = new DownloadAttachmentTaskResult
                        (startId, attachmentInfo, e);
                replyMessenger.send(Message.obtain(null, ReplyHandler.MESSAGE_EXCEPTION_HAPPENED,
                        downloadAttachmentTaskResult));
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }
        }

        @Override
        public void onAttachmentDownloadCanceled(int startId, AttachmentInfo attachmentInfo) {
            stringAttachmentInfoHashMap.remove(attachmentInfo.getId());
        }

        private void notifyAboutAttachmentDownloadTaskAlreadyExists(int startId, AttachmentInfo attachmentInfo) {
            try {
                DownloadAttachmentTaskResult downloadAttachmentTaskResult = new DownloadAttachmentTaskResult
                        (startId, attachmentInfo, null);
                replyMessenger.send(Message.obtain(null, ReplyHandler
                                .MESSAGE_DOWNLOAD_TASK_FOR_ATTACHMENT_ALREADY_EXISTS,
                        downloadAttachmentTaskResult));
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }
        }
    }

    private static class AttachmentDownloadRunnable implements Runnable {
        private AttachmentInfo attachmentInfo;
        private String token;
        private int startId;
        private OnDownloadAttachmentListener onDownloadAttachmentListener;

        public AttachmentDownloadRunnable(String token, int startId, AttachmentInfo attachmentInfo) {
            this.token = token;
            this.startId = startId;
            this.attachmentInfo = attachmentInfo;
        }

        @Override
        public void run() {
            if (BuildConfig.DEBUG) {
                Thread.currentThread().setName(attachmentInfo.getName() + "|" + startId);
            }

            try {
                GmailSSLStore gmailSSLStore = OpenStoreHelper.openAndConnectToGimapsStore(token, attachmentInfo
                        .getEmail());
                GmailFolder gmailFolder = (GmailFolder) gmailSSLStore.getFolder(GmailConstants.FOLDER_NAME_INBOX);
                gmailFolder.open(Folder.READ_ONLY);

                javax.mail.Message message = gmailFolder.getMessageByUID(attachmentInfo.getUid());
                Part attachment = getAttachmentPart(message, attachmentInfo.getId());

                if (attachment != null) {
                    FileUtils.copyInputStreamToFile(attachment.getInputStream(),
                            new File("/sdcard/" + attachmentInfo.getName()));
                    if (onDownloadAttachmentListener != null) {
                        onDownloadAttachmentListener.onAttachmentSuccessDownloaded(startId, attachmentInfo);
                    }
                } else {
                    // TODO-DenBond7: 16.08.2017 Attachment does not exist.
                }

                gmailFolder.close(false);
                gmailSSLStore.close();
            } catch (Exception e) {
                e.printStackTrace();
                if (onDownloadAttachmentListener != null) {
                    onDownloadAttachmentListener.onAttachmentDownloadFiled(startId, attachmentInfo, e);
                }
            }
        }

        public void setOnDownloadAttachmentListener(OnDownloadAttachmentListener onDownloadAttachmentListener) {
            this.onDownloadAttachmentListener = onDownloadAttachmentListener;
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
            if (part.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
                Multipart multiPart = (Multipart) part.getContent();
                int numberOfParts = multiPart.getCount();
                for (int partCount = 0; partCount < numberOfParts; partCount++) {
                    BodyPart bodyPart = multiPart.getBodyPart(partCount);
                    if (bodyPart.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
                        Part innerPart = getAttachmentPart(bodyPart, attachmentId);
                        if (innerPart != null) {
                            return innerPart;
                        }
                    } else if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())
                            && attachmentId.equals(bodyPart.getHeader(JavaEmailConstants.HEADER_X_ATTACHMENT_ID)[0])) {
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
