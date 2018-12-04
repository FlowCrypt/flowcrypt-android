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
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.api.email.FoldersManager;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.gmail.GmailApiHelper;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.api.email.protocol.SmtpProtocolUtil;
import com.flowcrypt.email.database.MessageState;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.database.dao.source.imap.AttachmentDaoSource;
import com.flowcrypt.email.database.dao.source.imap.ImapLabelsDaoSource;
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource;
import com.flowcrypt.email.util.FileAndDirectoryUtils;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.common.util.CollectionUtils;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.util.MailConnectException;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
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
import javax.net.ssl.SSLException;

import androidx.annotation.NonNull;

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
          Log.d(TAG, "A job has already scheduled! Skip scheduling a new job.");
          return;
        }
      }

      int result = scheduler.schedule(jobInfoBuilder.build());
      if (result == JobScheduler.RESULT_SUCCESS) {
        Log.d(TAG, "A job has scheduled successfully");
      } else {
        String errorMsg = "Error. Can't schedule a job";
        Log.e(TAG, errorMsg);
        ExceptionUtil.handleError(new IllegalStateException(errorMsg));
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
    private final WeakReference<MessagesSenderJobService> weakReference;

    private Session sess;
    private Store store;
    private boolean isFailed;

    SendMessagesAsyncTask(MessagesSenderJobService jobService) {
      this.weakReference = new WeakReference<>(jobService);
    }

    @Override
    protected JobParameters doInBackground(JobParameters... params) {
      Log.d(TAG, "doInBackground");
      try {
        if (weakReference.get() != null) {
          Context context = weakReference.get().getApplicationContext();
          AccountDao account = new AccountDaoSource().getActiveAccountInformation(context);
          MessageDaoSource msgDaoSource = new MessageDaoSource();
          ImapLabelsDaoSource imapLabelsDaoSource = new ImapLabelsDaoSource();

          File attsCacheDir = new File(context.getCacheDir(), Constants.ATTACHMENTS_CACHE_DIR);

          if (account != null) {
            msgDaoSource.resetMessagesWithSendingState(context, account.getEmail());

            List<GeneralMessageDetails> queuedMsgs = msgDaoSource.getOutboxMessages
                (context, account.getEmail(), MessageState.QUEUED);

            List<GeneralMessageDetails> sentButNotSavedMsgs = msgDaoSource.getOutboxMessages
                (context, account.getEmail(), MessageState.SENT_WITHOUT_LOCAL_COPY);

            if (!CollectionUtils.isEmpty(queuedMsgs) || !CollectionUtils.isEmpty(sentButNotSavedMsgs)) {
              sess = OpenStoreHelper.getSessionForAccountDao(context, account);
              store = OpenStoreHelper.openAndConnectToStore(context, account, sess);
            }

            if (!CollectionUtils.isEmpty(queuedMsgs)) {
              sendQueuedMessages(context, account, msgDaoSource, imapLabelsDaoSource, attsCacheDir);
            }

            if (!CollectionUtils.isEmpty(sentButNotSavedMsgs)) {
              saveCopyOfAlreadySentMessages(context, account, msgDaoSource, attsCacheDir);
            }

            if (store != null && store.isConnected()) {
              store.close();
            }
          }
        }

        publishProgress(false);
      } catch (Exception e) {
        e.printStackTrace();
        publishProgress(true);
      }

      return params[0];
    }

    @Override
    protected void onPostExecute(JobParameters jobParameters) {
      Log.d(TAG, "onPostExecute");
      try {
        if (weakReference.get() != null) {
          weakReference.get().jobFinished(jobParameters, isFailed);
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

    private void sendQueuedMessages(Context context, AccountDao account, MessageDaoSource msgDaoSource,
                                    ImapLabelsDaoSource imapLabelsDaoSource, File attsCacheDir)
        throws InterruptedException {
      List<GeneralMessageDetails> list;
      int lastMsgUID = 0;
      String email = account.getEmail();
      while (!CollectionUtils.isEmpty(list = msgDaoSource.getOutboxMessages(context, email, MessageState.QUEUED))) {
        Iterator<GeneralMessageDetails> iterator = list.iterator();
        GeneralMessageDetails msgDetails = null;

        while (iterator.hasNext()) {
          GeneralMessageDetails tempMsgDetails = iterator.next();
          if (tempMsgDetails.getUid() > lastMsgUID) {
            msgDetails = tempMsgDetails;
            break;
          }
        }

        if (msgDetails == null) {
          msgDetails = list.get(0);
        }

        lastMsgUID = msgDetails.getUid();
        int msgUid = msgDetails.getUid();
        String msgEmail = msgDetails.getEmail();
        String msgLabel = msgDetails.getLabel();

        try {
          msgDaoSource.resetMessagesWithSendingState(context, email);
          msgDaoSource.updateMessageState(context, msgEmail, msgLabel, msgUid, MessageState.SENDING);
          Thread.sleep(2000);

          AttachmentDaoSource attsDaoSource = new AttachmentDaoSource();
          List<AttachmentInfo> attInfoList = attsDaoSource.getAttachmentInfoList(context, email,
              JavaEmailConstants.FOLDER_OUTBOX, msgUid);

          boolean isMessageSent = sendMessage(context, account, msgDaoSource, msgDetails, attInfoList);

          if (!isMessageSent) {
            continue;
          }

          msgDetails = msgDaoSource.getMessage(context, email, JavaEmailConstants.FOLDER_OUTBOX, msgUid);

          if (msgDetails.getMsgState() == MessageState.SENT) {
            msgDaoSource.deleteMessage(context, email, JavaEmailConstants.FOLDER_OUTBOX, msgUid);

            if (!CollectionUtils.isEmpty(attInfoList)) {
              deleteMessageAttachments(context, account, attsCacheDir, msgDetails, attsDaoSource);
            }

            int msgsCount = msgDaoSource.getOutboxMessages(context, msgEmail).size();
            imapLabelsDaoSource.updateLabelMessagesCount(context, email, JavaEmailConstants.FOLDER_OUTBOX, msgsCount);
          }
        } catch (Exception e) {
          e.printStackTrace();
          ExceptionUtil.handleError(e);

          if (!GeneralUtil.isInternetConnectionAvailable(context)) {
            if (msgDetails.getMsgState() != MessageState.SENT) {
              msgDaoSource.updateMessageState(context, msgEmail, msgLabel, msgUid, MessageState.QUEUED);
            }

            publishProgress(true);

            break;
          } else {
            MessageState newMsgState = MessageState.ERROR_SENDING_FAILED;

            if (e instanceof MailConnectException) {
              newMsgState = MessageState.QUEUED;
            }

            if (e instanceof MessagingException) {
              if (e.getCause() != null) {
                if (e.getCause() instanceof SSLException || e.getCause() instanceof SocketException) {
                  newMsgState = MessageState.QUEUED;
                }
              }
            }

            if (e.getCause() != null) {
              if (e.getCause() instanceof FileNotFoundException) {
                newMsgState = MessageState.ERROR_CACHE_PROBLEM;
              }
            }

            msgDaoSource.updateMessageState(context, msgEmail, msgLabel, msgUid, newMsgState);
          }

          Thread.sleep(5000);
        }
      }
    }

    private void saveCopyOfAlreadySentMessages(Context context, AccountDao account, MessageDaoSource msgDaoSource,
                                               File attsCacheDir) {
      List<GeneralMessageDetails> list;
      String email = account.getEmail();
      while (!CollectionUtils.isEmpty(list = msgDaoSource.getOutboxMessages(context, email,
          MessageState.SENT_WITHOUT_LOCAL_COPY))) {
        GeneralMessageDetails details = list.get(0);
        try {
          AttachmentDaoSource attDaoSource = new AttachmentDaoSource();
          List<AttachmentInfo> atts = attDaoSource.getAttachmentInfoList(context, email,
              JavaEmailConstants.FOLDER_OUTBOX, details.getUid());

          MimeMessage mimeMsg = createMimeMessage(context, sess, details, atts);
          boolean isMsgSaved = saveCopyOfSentMessage(account, store, context, mimeMsg);

          if (!isMsgSaved) {
            continue;
          }

          msgDaoSource.deleteMessage(context, email, JavaEmailConstants.FOLDER_OUTBOX, details.getUid());

          if (!CollectionUtils.isEmpty(atts)) {
            deleteMessageAttachments(context, account, attsCacheDir, details, attDaoSource);
          }
        } catch (Exception e) {
          e.printStackTrace();
          ExceptionUtil.handleError(e);

          if (!GeneralUtil.isInternetConnectionAvailable(context)) {
            msgDaoSource.updateMessageState(context, details.getEmail(), details.getLabel(), details.getUid(),
                MessageState.SENT_WITHOUT_LOCAL_COPY);
            publishProgress(true);
            break;
          }

          if (e.getCause() != null) {
            if (e.getCause() instanceof FileNotFoundException) {
              msgDaoSource.deleteMessage(context, email, JavaEmailConstants.FOLDER_OUTBOX, details.getUid());
            } else {
              msgDaoSource.updateMessageState(context, details.getEmail(), details.getLabel(),
                  details.getUid(), MessageState.SENT_WITHOUT_LOCAL_COPY);
            }
          } else {
            msgDaoSource.deleteMessage(context, email, JavaEmailConstants.FOLDER_OUTBOX, details.getUid());
          }
        }
      }
    }

    private void deleteMessageAttachments(Context context, AccountDao account, File attsCacheDir,
                                          GeneralMessageDetails details, AttachmentDaoSource attDaoSource)
        throws IOException {
      attDaoSource.deleteAttachments(context, account.getEmail(), JavaEmailConstants.FOLDER_OUTBOX, details.getUid());

      if (!TextUtils.isEmpty(details.getAttachmentsDir())) {
        FileAndDirectoryUtils.deleteDirectory(new File(attsCacheDir, details.getAttachmentsDir()));
      }
    }

    private boolean sendMessage(Context context, AccountDao account, MessageDaoSource msgDaoSource,
                                GeneralMessageDetails details, List<AttachmentInfo> atts)
        throws IOException, MessagingException, GoogleAuthException {
      MimeMessage mimeMsg = createMimeMessage(context, sess, details, atts);
      String detEmail = details.getEmail();
      String detLabel = details.getLabel();

      switch (account.getAccountType()) {
        case AccountDao.ACCOUNT_TYPE_GOOGLE:
          if (account.getEmail().equalsIgnoreCase(details.getFrom()[0].getAddress())) {
            Transport transport = SmtpProtocolUtil.prepareTransportForSmtp(context, sess, account);
            transport.sendMessage(mimeMsg, mimeMsg.getAllRecipients());
          } else {
            Gmail gmail = GmailApiHelper.generateGmailApiService(context, account);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            mimeMsg.writeTo(outputStream);

            String threadId = null;
            String replyMsgId = mimeMsg.getHeader(JavaEmailConstants.HEADER_IN_REPLY_TO, null);

            if (!TextUtils.isEmpty(replyMsgId)) {
              threadId = getGmailMessageThreadID(gmail, replyMsgId);
            }

            com.google.api.services.gmail.model.Message sentMessage = new com.google.api.services.gmail.model.Message();
            sentMessage.setRaw(Base64.encodeToString(outputStream.toByteArray(), Base64.URL_SAFE));

            if (!TextUtils.isEmpty(threadId)) {
              sentMessage.setThreadId(threadId);
            }

            sentMessage = gmail
                .users()
                .messages()
                .send(GmailApiHelper.DEFAULT_USER_ID, sentMessage)
                .execute();

            if (sentMessage.getId() == null) {
              return false;
            }
          }

          msgDaoSource.updateMessageState(context, detEmail, detLabel, details.getUid(), MessageState.SENT);

          //Gmail automatically save a copy of the sent message.
          break;

        case AccountDao.ACCOUNT_TYPE_OUTLOOK:
          Transport outlookTransport = SmtpProtocolUtil.prepareTransportForSmtp(context, sess, account);
          outlookTransport.sendMessage(mimeMsg, mimeMsg.getAllRecipients());

          msgDaoSource.updateMessageState(context, detEmail, detLabel, details.getUid(), MessageState.SENT);
          break;

        default:
          Transport defaultTransport = SmtpProtocolUtil.prepareTransportForSmtp(context, sess, account);
          defaultTransport.sendMessage(mimeMsg, mimeMsg.getAllRecipients());

          msgDaoSource.updateMessageState(context, detEmail, detLabel, details.getUid(),
              MessageState.SENT_WITHOUT_LOCAL_COPY);

          if (saveCopyOfSentMessage(account, store, context, mimeMsg)) {
            msgDaoSource.updateMessageState(context, detEmail, detLabel, details.getUid(), MessageState.SENT);
          }
      }

      return true;
    }

    /**
     * Create {@link MimeMessage} from the given {@link GeneralMessageDetails}.
     *
     * @param sess    Will be used to create {@link MimeMessage}
     * @param context Interface to global information about an application environment.
     * @throws IOException
     * @throws MessagingException
     */
    @NonNull
    private MimeMessage createMimeMessage(Context context, Session sess, GeneralMessageDetails details,
                                          List<AttachmentInfo> atts)
        throws IOException, MessagingException {
      InputStream stream = IOUtils.toInputStream(details.getRawMessageWithoutAttachments(), StandardCharsets.UTF_8);
      MimeMessage mimeMsg = new MimeMessage(sess, stream);

      if (mimeMsg.getContent() instanceof MimeMultipart && !CollectionUtils.isEmpty(atts)) {
        MimeMultipart mimeMultipart = (MimeMultipart) mimeMsg.getContent();

        for (AttachmentInfo att : atts) {
          BodyPart attBodyPart = genBodyPartWithAttachment(context, att);
          mimeMultipart.addBodyPart(attBodyPart);
        }

        mimeMsg.setContent(mimeMultipart);
        mimeMsg.saveChanges();
      }

      return mimeMsg;
    }

    /**
     * Generate a {@link BodyPart} with an attachment.
     *
     * @param context Interface to global information about an application environment.
     * @param att     The {@link AttachmentInfo} object, which contains general information about the
     *                attachment.
     * @return Generated {@link MimeBodyPart} with the attachment.
     * @throws MessagingException
     */
    @NonNull
    private BodyPart genBodyPartWithAttachment(Context context, AttachmentInfo att) throws MessagingException {
      MimeBodyPart attBodyPart = new MimeBodyPart();
      attBodyPart.setDataHandler(new DataHandler(new AttachmentInfoDataSource(context, att)));
      attBodyPart.setFileName(att.getName());
      attBodyPart.setContentID(att.getId());

      return attBodyPart;
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
      ListMessagesResponse response = service
          .users()
          .messages()
          .list(GmailApiHelper.DEFAULT_USER_ID)
          .setQ("rfc822msgid:" + rfc822msgidValue)
          .execute();

      if (response.getMessages() != null && response.getMessages().size() == 1) {
        return response.getMessages().get(0).getThreadId();
      }

      return null;
    }

    /**
     * Save a copy of the sent message to the account SENT folder.
     *
     * @param account The object which contains information about an email account.
     * @param store   The connected and opened {@link Store} object.
     * @param context Interface to global information about an application environment.
     * @param mimeMsg The original {@link MimeMessage} which will be saved to the SENT folder.
     */
    private boolean saveCopyOfSentMessage(AccountDao account, Store store, Context context, MimeMessage mimeMsg) {
      FoldersManager foldersManager = FoldersManager.fromDatabase(context, account.getEmail());
      com.flowcrypt.email.api.email.Folder sentFolder = foldersManager.getFolderSent();

      try {
        if (sentFolder != null) {
          IMAPFolder sentRemoteFolder = (IMAPFolder) store.getFolder(sentFolder.getFullName());

          if (sentRemoteFolder == null || !sentRemoteFolder.exists()) {
            throw new IllegalArgumentException("The SENT folder doesn't exists. Can't create a " +
                "copy of the sent message!");
          }

          sentRemoteFolder.open(Folder.READ_WRITE);
          mimeMsg.setFlag(Flags.Flag.SEEN, true);
          sentRemoteFolder.appendMessages(new Message[]{mimeMsg});
          sentRemoteFolder.close(false);
          return true;
        } else {
          throw new IllegalArgumentException("The SENT folder is not defined");
        }
      } catch (MessagingException e) {
        e.printStackTrace();
      }

      return false;
    }
  }

  /**
   * The {@link DataSource} realization for a file which received from {@link Uri}
   */
  private static class AttachmentInfoDataSource implements DataSource {
    private AttachmentInfo att;
    private Context context;

    AttachmentInfoDataSource(Context context, AttachmentInfo att) {
      this.att = att;
      this.context = context;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      InputStream inputStream;
      if (att.getUri() == null) {
        if (att.getRawData() != null) {
          inputStream = IOUtils.toInputStream(att.getRawData(), StandardCharsets.UTF_8);
        } else {
          inputStream = null;
        }
      } else {
        inputStream = context.getContentResolver().openInputStream(att.getUri());
      }

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
      return TextUtils.isEmpty(att.getType()) ? "application/octet-stream" : att.getType();
    }

    @Override
    public String getName() {
      return att.getName();
    }
  }
}
