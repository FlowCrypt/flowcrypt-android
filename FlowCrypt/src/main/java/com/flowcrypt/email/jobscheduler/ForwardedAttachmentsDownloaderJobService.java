/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
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
import android.util.Log;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.protocol.ImapProtocolUtil;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.api.retrofit.node.NodeRetrofitHelper;
import com.flowcrypt.email.api.retrofit.node.NodeService;
import com.flowcrypt.email.api.retrofit.request.node.EncryptFileRequest;
import com.flowcrypt.email.api.retrofit.response.node.EncryptedFileResult;
import com.flowcrypt.email.database.MessageState;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.database.dao.source.imap.AttachmentDaoSource;
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource;
import com.flowcrypt.email.security.SecurityUtils;
import com.flowcrypt.email.util.FileAndDirectoryUtils;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.LogsUtil;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.google.android.gms.common.util.CollectionUtils;
import com.sun.mail.imap.IMAPFolder;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.UUID;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;

import androidx.core.content.FileProvider;
import retrofit2.Response;

/**
 * This realization of {@link JobService} downloads the attachments for forwarding purposes.
 *
 * @author Denis Bondarenko
 * Date: 09.10.2018
 * Time: 11:48
 * E-mail: DenBond7@gmail.com
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
          LogsUtil.d(TAG, "A job has already scheduled! Skip scheduling a new job.");
          return;
        }
      }

      int result = scheduler.schedule(jobInfoBuilder.build());
      if (result == JobScheduler.RESULT_SUCCESS) {
        LogsUtil.d(TAG, "A job has scheduled successfully");
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
    LogsUtil.d(TAG, "onCreate");
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    LogsUtil.d(TAG, "onDestroy");
  }

  @Override
  public boolean onStartJob(JobParameters jobParameters) {
    LogsUtil.d(TAG, "onStartJob");
    new DownloadForwardedAttachmentsAsyncTask(this).execute(jobParameters);
    return true;
  }

  @Override
  public boolean onStopJob(JobParameters jobParameters) {
    LogsUtil.d(TAG, "onStopJob");
    jobFinished(jobParameters, true);
    return false;
  }

  /**
   * This is an implementation of {@link AsyncTask} which downloads the forwarded attachments.
   */
  private static class DownloadForwardedAttachmentsAsyncTask extends AsyncTask<JobParameters, Boolean, JobParameters> {
    private final WeakReference<ForwardedAttachmentsDownloaderJobService> weakRef;

    private Session sess;
    private Store store;
    private boolean isFailed;
    private File attCacheDir;
    private File fwdAttsCacheDir;

    DownloadForwardedAttachmentsAsyncTask(ForwardedAttachmentsDownloaderJobService jobService) {
      this.weakRef = new WeakReference<>(jobService);
    }

    @Override
    protected JobParameters doInBackground(JobParameters... params) {
      LogsUtil.d(TAG, "doInBackground");
      try {
        if (weakRef.get() != null) {
          Context context = weakRef.get().getApplicationContext();

          attCacheDir = new File(context.getCacheDir(), Constants.ATTACHMENTS_CACHE_DIR);

          if (!attCacheDir.exists()) {
            if (!attCacheDir.mkdirs()) {
              throw new IllegalStateException("Create cache directory " + attCacheDir.getName() + " filed!");
            }
          }

          fwdAttsCacheDir = new File(attCacheDir, Constants.FORWARDED_ATTACHMENTS_CACHE_DIR);

          if (!fwdAttsCacheDir.exists()) {
            if (!fwdAttsCacheDir.mkdirs()) {
              throw new IllegalStateException("Create cache directory " + fwdAttsCacheDir.getName() + " filed!");
            }
          }

          AccountDao account = new AccountDaoSource().getActiveAccountInformation(context);
          MessageDaoSource msgDaoSource = new MessageDaoSource();

          if (account != null) {
            List<GeneralMessageDetails> newMsgs = msgDaoSource.getOutboxMsgs(context, account.getEmail(),
                MessageState.NEW_FORWARDED);

            if (!CollectionUtils.isEmpty(newMsgs)) {
              sess = OpenStoreHelper.getAccountSess(context, account);
              store = OpenStoreHelper.openStore(context, account, sess);
              downloadForwardedAtts(context, account, msgDaoSource);
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
      LogsUtil.d(TAG, "onPostExecute");
      try {
        if (weakRef.get() != null) {
          weakRef.get().jobFinished(jobParameters, isFailed);
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

    private void downloadForwardedAtts(Context context, AccountDao account, MessageDaoSource daoSource) {
      AttachmentDaoSource attDaoSource = new AttachmentDaoSource();

      while (true) {
        List<GeneralMessageDetails> detailsList = daoSource.getOutboxMsgs(context, account.getEmail(),
            MessageState.NEW_FORWARDED);

        if (CollectionUtils.isEmpty(detailsList)) {
          break;
        }

        GeneralMessageDetails details = detailsList.get(0);
        String detEmail = details.getEmail();
        String detLabel = details.getLabel();
        File msgAttsDir = new File(attCacheDir, details.getAttsDir());
        try {
          List<String> pubKeys = null;
          if (details.isEncrypted()) {
            String senderEmail = EmailUtil.getFirstAddressString(details.getFrom());
            pubKeys = SecurityUtils.getRecipientsPubKeys(context, details.getAllRecipients(), account, senderEmail);
          }

          List<AttachmentInfo> atts = attDaoSource.getAttInfoList(context, account.getEmail(),
              JavaEmailConstants.FOLDER_OUTBOX, details.getUid());

          if (CollectionUtils.isEmpty(atts)) {
            daoSource.updateMsgState(context, detEmail, detLabel, details.getUid(), MessageState.QUEUED);
            continue;
          }

          MessageState msgState = getNewMsgState(context, attDaoSource, details, msgAttsDir, pubKeys, atts);

          int updateResult = daoSource.updateMsgState(context, detEmail, detLabel, details.getUid(), msgState);
          if (updateResult > 0) {
            MessagesSenderJobService.schedule(context);
          }
        } catch (Exception e) {
          e.printStackTrace();
          ExceptionUtil.handleError(e);

          if (!GeneralUtil.isConnected(context)) {
            publishProgress(true);
            break;
          }
        }
      }
    }

    private MessageState getNewMsgState(Context context, AttachmentDaoSource attDaoSource,
                                        GeneralMessageDetails details, File msgAttsDir, List<String> pubKeys,
                                        List<AttachmentInfo> atts) throws IOException, MessagingException {
      IMAPFolder folder = null;
      Message fwdMsg = null;

      MessageState msgState = MessageState.QUEUED;

      for (AttachmentInfo att : atts) {
        if (!att.isForwarded()) {
          continue;
        }

        File attFile = new File(msgAttsDir, att.getName());
        boolean exists = attFile.exists();

        if (exists) {
          att.setUri(FileProvider.getUriForFile(context, Constants.FILE_PROVIDER_AUTHORITY, attFile));
        } else if (att.getUri() == null) {
          FileAndDirectoryUtils.cleanDir(fwdAttsCacheDir);

          if (folder == null) {
            folder = (IMAPFolder) store.getFolder(att.getFwdFolder());
            folder.open(Folder.READ_ONLY);
          }

          if (fwdMsg == null) {
            fwdMsg = folder.getMessageByUID(att.getFwdUid());
          }

          if (fwdMsg == null) {
            msgState = MessageState.ERROR_ORIGINAL_MESSAGE_MISSING;
            break;
          }

          int msgNumber = fwdMsg.getMessageNumber();
          Part part = ImapProtocolUtil.getAttPartById(folder, msgNumber, fwdMsg, att.getId());

          File tempFile = new File(fwdAttsCacheDir, UUID.randomUUID().toString());

          if (part != null) {
            InputStream inputStream = part.getInputStream();
            if (inputStream != null) {
              downloadFile(details, pubKeys, att, tempFile, inputStream);

              if (msgAttsDir.exists()) {
                FileUtils.moveFile(tempFile, attFile);
                att.setUri(FileProvider.getUriForFile(context, Constants.FILE_PROVIDER_AUTHORITY, attFile));
              } else {
                FileAndDirectoryUtils.cleanDir(fwdAttsCacheDir);
                //It means the user has already deleted the current message. We don't need
                // to download other attachments.
                break;
              }
            } else {
              msgState = MessageState.ERROR_ORIGINAL_ATTACHMENT_NOT_FOUND;
              break;
            }
          } else {
            msgState = MessageState.ERROR_ORIGINAL_ATTACHMENT_NOT_FOUND;
            break;
          }
        }

        if (att.getUri() != null) {
          ContentValues contentValues = new ContentValues();
          contentValues.put(AttachmentDaoSource.COL_FILE_URI, att.getUri().toString());
          attDaoSource.update(context, att.getEmail(), att.getFolder(), att.getUid(), att.getId(), contentValues);
        }
      }
      return msgState;
    }

    private void downloadFile(GeneralMessageDetails details, List<String> pubKeys, AttachmentInfo att,
                              File tempFile, InputStream inputStream) throws IOException {
      if (details.isEncrypted()) {
        byte[] originalBytes = IOUtils.toByteArray(inputStream);
        String fileName = FilenameUtils.removeExtension(att.getName());
        NodeService nodeService = NodeRetrofitHelper.getInstance().getRetrofit().create(NodeService.class);
        EncryptFileRequest request = new EncryptFileRequest(originalBytes, fileName, pubKeys);

        Response<EncryptedFileResult> response = nodeService.encryptFile(request).execute();
        EncryptedFileResult encryptedFileResult = response.body();

        if (encryptedFileResult == null) {
          ExceptionUtil.handleError(new NullPointerException("encryptedFileResult == null"));
          FileUtils.writeByteArrayToFile(tempFile, new byte[]{});
          return;
        }

        if (encryptedFileResult.getError() != null) {
          ExceptionUtil.handleError(new Exception(encryptedFileResult.getError().getMsg()));
          FileUtils.writeByteArrayToFile(tempFile, new byte[]{});
          return;
        }

        byte[] encryptedBytes = encryptedFileResult.getEncryptedBytes();
        FileUtils.writeByteArrayToFile(tempFile, encryptedBytes);
      } else {
        FileUtils.copyInputStreamToFile(inputStream, tempFile);
      }
    }
  }
}
