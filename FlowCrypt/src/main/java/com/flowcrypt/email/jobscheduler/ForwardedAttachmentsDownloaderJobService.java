/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jobscheduler;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.protocol.ImapProtocolUtil;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.database.MessageState;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.database.dao.source.imap.AttachmentDaoSource;
import com.flowcrypt.email.database.dao.source.imap.ImapLabelsDaoSource;
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.security.SecurityStorageConnector;
import com.flowcrypt.email.security.SecurityUtils;
import com.flowcrypt.email.util.FileAndDirectoryUtils;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.google.android.gms.common.util.CollectionUtils;
import com.sun.mail.imap.IMAPFolder;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.UUID;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;

/**
 * This realization of {@link JobService} downloads the attachments for forwarding purposes.
 *
 * @author Denis Bondarenko
 *         Date: 09.10.2018
 *         Time: 11:48
 *         E-mail: DenBond7@gmail.com
 */
public class ForwardedAttachmentsDownloaderJobService extends JobService {

    private static final String TAG = ForwardedAttachmentsDownloaderJobService.class.getSimpleName();

    public static void schedule(Context context) {
        JobInfo.Builder jobInfoBuilder = new JobInfo.Builder(JobIdManager.JOB_TYPE_DOWNLOAD_FORWARDED_ATTACHMENTS,
                new ComponentName(context, ForwardedAttachmentsDownloaderJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true);

        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler != null) {

            for (JobInfo jobInfo : scheduler.getAllPendingJobs()) {
                if (jobInfo.getId() == JobIdManager.JOB_TYPE_SEND_MESSAGES) {
                    //skip schedule a new job if we already have another one
                    Log.d(TAG, "A job has already scheduled! Skip scheduling a new job.");
                    return;
                }
            }

            int result = scheduler.schedule(jobInfoBuilder.build());
            if (result == JobScheduler.RESULT_SUCCESS) {
                Log.d(TAG, "A job has scheduled successfully");
            } else {
                String errorMessage = "Error. Can't schedule a job";
                Log.e(TAG, errorMessage);
                ExceptionUtil.handleError(new IllegalStateException(errorMessage));
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG, "onStartJob");
        new DownloadForwardedAttachmentsAsyncTask(this).execute(jobParameters);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(TAG, "onStopJob");
        jobFinished(jobParameters, true);
        return false;
    }

    /**
     * This is an implementation of {@link AsyncTask} which downloads the forwarded attachments.
     */
    private static class DownloadForwardedAttachmentsAsyncTask extends AsyncTask<JobParameters,
            Boolean, JobParameters> {
        private final WeakReference<ForwardedAttachmentsDownloaderJobService> jobServiceWeakReference;

        private Session session;
        private Store store;
        private boolean isFailed;
        private File attachmentCacheDir;
        private File forwardedAttachmentCacheDir;
        private Js js;

        DownloadForwardedAttachmentsAsyncTask(ForwardedAttachmentsDownloaderJobService
                                                      forwardedAttachmentsDownloaderJobService) {
            this.jobServiceWeakReference = new WeakReference<>(forwardedAttachmentsDownloaderJobService);
        }

        @Override
        protected JobParameters doInBackground(JobParameters... params) {
            Log.d(TAG, "doInBackground");
            try {
                if (jobServiceWeakReference.get() != null) {
                    Context context = jobServiceWeakReference.get().getApplicationContext();

                    attachmentCacheDir = new File(context.getCacheDir(), Constants.ATTACHMENTS_CACHE_DIR);

                    if (!attachmentCacheDir.exists()) {
                        if (!attachmentCacheDir.mkdirs()) {
                            throw new IllegalStateException("Create cache directory " + attachmentCacheDir.getName() +
                                    " filed!");
                        }
                    }

                    forwardedAttachmentCacheDir = new File(attachmentCacheDir, Constants
                            .FORWARDED_ATTACHMENTS_CACHE_DIR);

                    if (!forwardedAttachmentCacheDir.exists()) {
                        if (!forwardedAttachmentCacheDir.mkdirs()) {
                            throw new IllegalStateException("Create cache directory " + forwardedAttachmentCacheDir
                                    .getName() + " filed!");
                        }
                    }

                    js = new Js(context, new SecurityStorageConnector(context));

                    AccountDao accountDao = new AccountDaoSource().getActiveAccountInformation(context);
                    MessageDaoSource messageDaoSource = new MessageDaoSource();

                    if (accountDao != null) {
                        List<GeneralMessageDetails> listOfNewMessages = messageDaoSource.getOutboxMessages
                                (context, accountDao.getEmail(), MessageState.NEW_FORWARDED);

                        if (!CollectionUtils.isEmpty(listOfNewMessages)) {
                            session = OpenStoreHelper.getSessionForAccountDao(context, accountDao);
                            store = OpenStoreHelper.openAndConnectToStore(context, accountDao, session);
                            downloadForwardedAttachments(context, js, accountDao, messageDaoSource);
                        }

                        if (store != null && store.isConnected()) {
                            store.close();
                        }
                    }
                }

                publishProgress(false);
            } catch (Exception e) {
                e.printStackTrace();
                ExceptionUtil.handleError(e);
                publishProgress(true);
            }

            return params[0];
        }

        @Override
        protected void onPostExecute(JobParameters jobParameters) {
            Log.d(TAG, "onPostExecute");
            try {
                if (jobServiceWeakReference.get() != null) {
                    jobServiceWeakReference.get().jobFinished(jobParameters, isFailed);
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onProgressUpdate(Boolean... values) {
            super.onProgressUpdate(values);
            isFailed = values[0];
        }

        private void downloadForwardedAttachments(Context context, Js js, AccountDao accountDao,
                                                  MessageDaoSource messageDaoSource) {
            List<GeneralMessageDetails> generalMessageDetailsList;
            AttachmentDaoSource attachmentDaoSource = new AttachmentDaoSource();

            while (!CollectionUtils.isEmpty(generalMessageDetailsList = messageDaoSource
                    .getOutboxMessages(context, accountDao.getEmail(), MessageState.NEW_FORWARDED))) {
                GeneralMessageDetails generalMessageDetails = generalMessageDetailsList.get(0);
                File messageAttachmentsDir =
                        new File(attachmentCacheDir, generalMessageDetails.getAttachmentsDirectory());
                try {
                    String[] pubKeys = generalMessageDetails.isEncrypted() ? SecurityUtils.getRecipientsPubKeys(context,
                            js, EmailUtil.getAllRecipients(context, generalMessageDetails), accountDao,
                            EmailUtil.getFirstAddressString(generalMessageDetails.getFrom())) : null;

                    List<AttachmentInfo> attachmentInfoList =
                            attachmentDaoSource.getAttachmentInfoList(context, accountDao.getEmail(),
                                    JavaEmailConstants.FOLDER_OUTBOX, generalMessageDetails.getUid());

                    if (CollectionUtils.isEmpty(attachmentInfoList)) {
                        messageDaoSource.updateMessageState(context,
                                generalMessageDetails.getEmail(), generalMessageDetails.getLabel(),
                                generalMessageDetails.getUid(), MessageState.QUEUED);
                        continue;
                    }

                    IMAPFolder folderOfForwardedMessage = null;
                    Message forwardedMessage = null;

                    MessageState messageState = MessageState.QUEUED;

                    for (AttachmentInfo attachmentInfo : attachmentInfoList) {
                        if (attachmentInfo.isForwarded() && attachmentInfo.getUri() == null) {
                            FileAndDirectoryUtils.cleanDirectory(forwardedAttachmentCacheDir);

                            if (folderOfForwardedMessage == null) {
                                folderOfForwardedMessage = (IMAPFolder) store.getFolder(new ImapLabelsDaoSource()
                                        .getFolderByAlias(context, attachmentInfo.getEmail(),
                                                attachmentInfo.getForwardedFolder()).getServerFullFolderName());
                                folderOfForwardedMessage.open(Folder.READ_ONLY);
                            }

                            if (forwardedMessage == null) {
                                forwardedMessage = folderOfForwardedMessage
                                        .getMessageByUID(attachmentInfo.getForwardedUid());
                            }

                            if (forwardedMessage == null) {
                                messageState = MessageState.CACHE_ERROR;
                                break;
                            }

                            Part partWithForwardedAttachment = ImapProtocolUtil.getAttachmentPartById
                                    (folderOfForwardedMessage, forwardedMessage.getMessageNumber(), forwardedMessage,
                                            attachmentInfo.getId());

                            File tempFile = new File(forwardedAttachmentCacheDir, UUID.randomUUID().toString());
                            File attachmentFile = new File(messageAttachmentsDir, attachmentInfo.getName());

                            if (partWithForwardedAttachment != null) {
                                InputStream inputStream = partWithForwardedAttachment.getInputStream();
                                if (inputStream != null) {
                                    if (generalMessageDetails.isEncrypted()) {
                                        byte[] encryptedBytes = this.js.crypto_message_encrypt(pubKeys, IOUtils
                                                .toByteArray(inputStream), attachmentInfo.getName());
                                        FileUtils.writeByteArrayToFile(tempFile, encryptedBytes);
                                    } else {
                                        FileUtils.copyInputStreamToFile(inputStream, tempFile);
                                    }

                                    if (messageAttachmentsDir.exists()) {
                                        FileUtils.moveFile(tempFile, attachmentFile);
                                        attachmentInfo.setUri(FileProvider.getUriForFile(context,
                                                Constants.FILE_PROVIDER_AUTHORITY, attachmentFile));
                                    } else {
                                        FileAndDirectoryUtils.cleanDirectory(forwardedAttachmentCacheDir);
                                        //It means the user has already deleted the current message. We don't need
                                        // to download other attachments.
                                        break;
                                    }
                                } else {
                                    messageState = MessageState.CACHE_ERROR;
                                    break;
                                }
                            } else {
                                messageState = MessageState.CACHE_ERROR;
                                break;
                            }
                        }

                        if (attachmentInfo.getUri() != null) {
                            ContentValues contentValues = new ContentValues();
                            contentValues.put(AttachmentDaoSource.COL_FILE_URI, attachmentInfo.getUri().toString());

                            attachmentDaoSource.update(context, attachmentInfo.getEmail(), attachmentInfo.getFolder()
                                    , attachmentInfo.getUid(), attachmentInfo.getId(), contentValues);
                        }
                    }

                    if (messageDaoSource.updateMessageState(context, generalMessageDetails.getEmail(),
                            generalMessageDetails.getLabel(), generalMessageDetails.getUid(), messageState) > 0) {
                        MessagesSenderJobService.schedule(context);
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    if (!GeneralUtil.isInternetConnectionAvailable(context)) {
                        publishProgress(true);
                        break;
                    }
                }
            }
        }
    }
}
