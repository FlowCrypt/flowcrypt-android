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
import android.os.AsyncTask;
import android.util.Log;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.api.email.protocol.SmtpProtocolUtil;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource;
import com.flowcrypt.email.util.FileAndDirectoryUtils;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.google.android.gms.common.util.CollectionUtils;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

/**
 * @author Denis Bondarenko
 * Date: 11.09.2018
 * Time: 18:43
 * E-mail: DenBond7@gmail.com
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
                    Log.d(TAG, "A job has already scheduled! Skip to schedule a new job.");
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
        new SendMessagesJobTask(this).execute(jobParameters);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(TAG, "onStopJob");
        jobFinished(jobParameters, true);
        return false;
    }

    /**
     *
     */
    private static class SendMessagesJobTask extends AsyncTask<JobParameters, Boolean, JobParameters> {
        private final WeakReference<MessagesSenderJobService> messagesSenderJobServiceWeakReference;

        private Session session;
        private Store store;
        private boolean isFailed;

        SendMessagesJobTask(MessagesSenderJobService messagesSenderJobService) {
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
                                MimeMessage mimeMessage = new MimeMessage(session,
                                        IOUtils.toInputStream(generalMessageDetails.getRawMessageWithoutAttachments()
                                                , StandardCharsets.UTF_8));

                                Transport transport =
                                        SmtpProtocolUtil.prepareTransportForSmtp(context, session, accountDao);
                                transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());

                                messageDaoSource.deleteMessagesByUID(context, accountDao.getEmail(),
                                        JavaEmailConstants.FOLDER_OUTBOX, Collections.singletonList((long)
                                                generalMessageDetails.getUid()));

                                FileAndDirectoryUtils.deleteDirectory(new File(attachmentsCacheDirectory,
                                        String.valueOf(generalMessageDetails.getSentDateInMillisecond())));
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
    }
}
