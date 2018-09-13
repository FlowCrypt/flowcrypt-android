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
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.api.email.FoldersManager;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.gmail.GmailApiHelper;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.api.email.protocol.SmtpProtocolUtil;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.database.dao.source.imap.AttachmentDaoSource;
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource;
import com.flowcrypt.email.util.FileAndDirectoryUtils;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.common.util.CollectionUtils;
import com.google.api.client.util.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.sun.mail.imap.IMAPFolder;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * @author Denis Bondarenko
 *         Date: 11.09.2018
 *         Time: 18:43
 *         E-mail: DenBond7@gmail.com
 */
public class MessagesSenderJobService extends JobService {

    private static final String TAG = MessagesSenderJobService.class.getSimpleName();

    public static void schedule(Context context) {
        JobInfo.Builder jobInfoBuilder = new JobInfo.Builder(JobIdManager.JOB_TYPE_SEND_MESSAGES,
                new ComponentName(context, MessagesSenderJobService.class))
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
        new SendMessagesAsyncTask(this).execute(jobParameters);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(TAG, "onStopJob");
        jobFinished(jobParameters, true);
        return false;
    }

    /**
     * This is an implementation of {@link AsyncTask} which sends the outgoing messages.
     */
    private static class SendMessagesAsyncTask extends AsyncTask<JobParameters, Boolean, JobParameters> {
        private final WeakReference<MessagesSenderJobService> messagesSenderJobServiceWeakReference;

        private Session session;
        private Store store;
        private boolean isFailed;

        SendMessagesAsyncTask(MessagesSenderJobService messagesSenderJobService) {
            this.messagesSenderJobServiceWeakReference = new WeakReference<>(messagesSenderJobService);
        }

        @Override
        protected JobParameters doInBackground(JobParameters... params) {
            Log.d(TAG, "doInBackground");
            try {
                if (messagesSenderJobServiceWeakReference.get() != null) {
                    Context context = messagesSenderJobServiceWeakReference.get().getApplicationContext();
                    AccountDao accountDao = new AccountDaoSource().getActiveAccountInformation(context);
                    MessageDaoSource messageDaoSource = new MessageDaoSource();

                    File attachmentsCacheDirectory = new File(context.getCacheDir(), Constants.ATTACHMENTS_CACHE_DIR);

                    if (accountDao != null) {
                        List<GeneralMessageDetails> generalMessageDetailsList = messageDaoSource.getMessages
                                (context, accountDao.getEmail(), JavaEmailConstants.FOLDER_OUTBOX);

                        if (!CollectionUtils.isEmpty(generalMessageDetailsList)) {
                            session = OpenStoreHelper.getSessionForAccountDao(context, accountDao);
                            store = OpenStoreHelper.openAndConnectToStore(context, accountDao, session);

                            while (!CollectionUtils.isEmpty(generalMessageDetailsList = messageDaoSource.getMessages
                                    (context, accountDao.getEmail(), JavaEmailConstants.FOLDER_OUTBOX))) {
                                GeneralMessageDetails generalMessageDetails = generalMessageDetailsList.get(0);
                                AttachmentDaoSource attachmentDaoSource = new AttachmentDaoSource();
                                List<AttachmentInfo> attachmentInfoList =
                                        attachmentDaoSource.getAttachmentInfoList(context, accountDao.getEmail(),
                                                JavaEmailConstants.FOLDER_OUTBOX, generalMessageDetails.getUid());

                                boolean isMessageSent = sendMessage(context, accountDao, generalMessageDetails,
                                        attachmentInfoList);

                                if (!isMessageSent) {
                                    continue;
                                }

                                messageDaoSource.deleteMessagesByUID(context, accountDao.getEmail(),
                                        JavaEmailConstants.FOLDER_OUTBOX, Collections.singletonList((long)
                                                generalMessageDetails.getUid()));

                                if (!CollectionUtils.isEmpty(attachmentInfoList)) {
                                    AttachmentInfo attachmentInfo = attachmentInfoList.get(0);
                                    attachmentDaoSource.deleteAttachments(context, accountDao.getEmail(),
                                            JavaEmailConstants.FOLDER_OUTBOX, generalMessageDetails.getUid());

                                    Uri uri = attachmentInfo.getUri();
                                    List<String> segments = uri.getPathSegments();
                                    int size = segments.size();
                                    if (size <= 1) {
                                        continue;
                                    }

                                    String attachmentFolderName = segments.get(size - 2);

                                    if (!TextUtils.isEmpty(attachmentFolderName)) {
                                        FileAndDirectoryUtils.deleteDirectory(new File(attachmentsCacheDirectory,
                                                attachmentFolderName));
                                    }
                                }
                            }

                            if (store != null) {
                                store.close();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                publishProgress(true);
            }

            publishProgress(false);
            return params[0];
        }

        @Override
        protected void onPostExecute(JobParameters jobParameters) {
            Log.d(TAG, "onPostExecute");
            try {
                if (messagesSenderJobServiceWeakReference.get() != null) {
                    messagesSenderJobServiceWeakReference.get().jobFinished(jobParameters, isFailed);
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

        private boolean sendMessage(Context context, AccountDao accountDao,
                                    GeneralMessageDetails generalMessageDetails,
                                    List<AttachmentInfo> attachmentInfoList)
                throws IOException, MessagingException, GoogleAuthException {
            MimeMessage mimeMessage = createMimeMessage(context, session, generalMessageDetails, attachmentInfoList);

            switch (accountDao.getAccountType()) {
                case AccountDao.ACCOUNT_TYPE_GOOGLE:
                    if (accountDao.getEmail().equalsIgnoreCase(generalMessageDetails.getFrom()[0].getAddress())) {
                        Transport transport = SmtpProtocolUtil.prepareTransportForSmtp(context, session, accountDao);
                        transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
                    } else {
                        Gmail gmailApiService = GmailApiHelper.generateGmailApiService(context, accountDao);
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        mimeMessage.writeTo(byteArrayOutputStream);

                        String threadId = null;
                        String replyMessageId = mimeMessage.getHeader(JavaEmailConstants.HEADER_IN_REPLY_TO, null);

                        if (!TextUtils.isEmpty(replyMessageId)) {
                            threadId = getGmailMessageThreadID(gmailApiService, replyMessageId);
                        }

                        com.google.api.services.gmail.model.Message sentMessage
                                = new com.google.api.services.gmail.model.Message();
                        sentMessage.setRaw(Base64.encodeBase64URLSafeString(byteArrayOutputStream.toByteArray()));

                        if (!TextUtils.isEmpty(threadId)) {
                            sentMessage.setThreadId(threadId);
                        }

                        sentMessage = gmailApiService
                                .users()
                                .messages()
                                .send(GmailApiHelper.DEFAULT_USER_ID, sentMessage)
                                .execute();

                        if (sentMessage.getId() == null) {
                            return false;
                        }
                    }

                    //Gmail automatically save a copy of the sent message.
                    break;

                default:
                    Transport transport = SmtpProtocolUtil.prepareTransportForSmtp(context, session, accountDao);
                    transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
                    saveCopyOfSentMessage(accountDao, store, context, mimeMessage);
            }
            return true;
        }

        /**
         * Create {@link MimeMessage} from the given {@link GeneralMessageDetails}.
         *
         * @param session Will be used to create {@link MimeMessage}
         * @param context Interface to global information about an application environment.
         * @throws IOException
         * @throws MessagingException
         */
        @NonNull
        private MimeMessage createMimeMessage(Context context, Session session,
                                              GeneralMessageDetails generalMessageDetails,
                                              List<AttachmentInfo> attachmentInfoList)
                throws IOException, MessagingException {
            MimeMessage mimeMessage = new MimeMessage(session, IOUtils.toInputStream(generalMessageDetails
                    .getRawMessageWithoutAttachments(), StandardCharsets.UTF_8));

            if (mimeMessage.getContent() instanceof MimeMultipart && !CollectionUtils.isEmpty(attachmentInfoList)) {
                MimeMultipart mimeMultipart = (MimeMultipart) mimeMessage.getContent();

                for (AttachmentInfo attachmentInfo : attachmentInfoList) {
                    BodyPart attachmentBodyPart = generateBodyPartWithAttachment(context, attachmentInfo);
                    mimeMultipart.addBodyPart(attachmentBodyPart);
                }

                mimeMessage.setContent(mimeMultipart);
                mimeMessage.saveChanges();
            }

            return mimeMessage;
        }

        /**
         * Generate a {@link BodyPart} with an attachment.
         *
         * @param context        Interface to global information about an application environment.
         * @param attachmentInfo The {@link AttachmentInfo} object, which contains general information about the
         *                       attachment.
         * @return Generated {@link MimeBodyPart} with the attachment.
         * @throws MessagingException
         */
        @NonNull
        private BodyPart generateBodyPartWithAttachment(Context context, AttachmentInfo attachmentInfo)
                throws MessagingException {
            MimeBodyPart attachmentsBodyPart = new MimeBodyPart();
            attachmentsBodyPart.setDataHandler(new DataHandler(new AttachmentInfoDataSource(context, attachmentInfo)));
            attachmentsBodyPart.setFileName(attachmentInfo.getName());
            attachmentsBodyPart.setContentID(attachmentInfo.getId());

            return attachmentsBodyPart;
        }

        /**
         * Retrieve a Gmail message thread id.
         *
         * @param service          A {@link Gmail} reference.
         * @param rfc822msgidValue An rfc822 Message-Id value of the input message.
         * @return The input message thread id.
         * @throws IOException
         */
        private String getGmailMessageThreadID(Gmail service, String rfc822msgidValue) throws IOException {
            ListMessagesResponse response = service.users().messages().list(GmailApiHelper.DEFAULT_USER_ID).setQ(
                    "rfc822msgid:" + rfc822msgidValue).execute();

            if (response.getMessages() != null && response.getMessages().size() == 1) {
                return response.getMessages().get(0).getThreadId();
            }

            return null;
        }

        /**
         * Save a copy of the sent message to the account SENT folder.
         *
         * @param accountDao  The object which contains information about an email account.
         * @param store       The connected and opened {@link Store} object.
         * @param context     Interface to global information about an application environment.
         * @param mimeMessage The original {@link MimeMessage} which will be saved to the SENT folder.
         * @throws MessagingException Errors can be happened when we try to save a copy of sent message.
         */
        private void saveCopyOfSentMessage(AccountDao accountDao, Store store, Context context,
                                           MimeMessage mimeMessage) throws MessagingException {
            FoldersManager foldersManager = FoldersManager.fromDatabase(context, accountDao.getEmail());
            com.flowcrypt.email.api.email.Folder sentFolder = foldersManager.getFolderSent();

            if (sentFolder != null) {
                IMAPFolder sentImapFolder = (IMAPFolder) store.getFolder(sentFolder.getServerFullFolderName());

                if (sentImapFolder == null || !sentImapFolder.exists()) {
                    throw new IllegalArgumentException("The SENT folder doesn't exists. Can't create a copy of " +
                            "the sent message!");
                }

                sentImapFolder.open(Folder.READ_WRITE);
                mimeMessage.setFlag(Flags.Flag.SEEN, true);
                sentImapFolder.appendMessages(new Message[]{mimeMessage});
                sentImapFolder.close(false);
            } else {
                throw new IllegalArgumentException("The SENT folder is not defined");
            }
        }
    }

    /**
     * The {@link DataSource} realization for a file which received from {@link Uri}
     */
    private static class AttachmentInfoDataSource implements DataSource {
        private AttachmentInfo attachmentInfo;
        private Context context;

        AttachmentInfoDataSource(Context context, AttachmentInfo attachmentInfo) {
            this.attachmentInfo = attachmentInfo;
            this.context = context;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            InputStream inputStream = attachmentInfo.getUri() == null ? (attachmentInfo.getRawData() != null ?
                    IOUtils.toInputStream(attachmentInfo.getRawData(), StandardCharsets.UTF_8) : null) :
                    context.getContentResolver().openInputStream(attachmentInfo.getUri());

            return inputStream == null ? null : new BufferedInputStream(inputStream);
        }

        @Override
        public OutputStream getOutputStream() {
            return null;
        }

        /**
         * If a content type is unknown we return "application/octet-stream".
         * http://www.rfc-editor.org/rfc/rfc2046.txt (section 4.5.1.  Octet-Stream Subtype)
         */
        @Override
        public String getContentType() {
            return TextUtils.isEmpty(attachmentInfo.getType()) ? "application/octet-stream" : attachmentInfo.getType();
        }

        @Override
        public String getName() {
            return attachmentInfo.getName();
        }
    }
}
