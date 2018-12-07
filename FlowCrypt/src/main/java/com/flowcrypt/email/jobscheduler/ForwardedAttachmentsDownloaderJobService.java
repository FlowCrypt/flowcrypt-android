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
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.js.core.Js;
import com.flowcrypt.email.security.SecurityStorageConnector;
import com.flowcrypt.email.security.SecurityUtils;
import com.flowcrypt.email.util.FileAndDirectoryUtils;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.google.android.gms.common.util.CollectionUtils;
import com.sun.mail.imap.IMAPFolder;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
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

import androidx.core.content.FileProvider;

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
  private static class DownloadForwardedAttachmentsAsyncTask extends AsyncTask<JobParameters, Boolean, JobParameters> {
    private final WeakReference<ForwardedAttachmentsDownloaderJobService> weakReference;

    private Session sess;
    private Store store;
    private boolean isFailed;
    private File attCacheDir;
    private File fwdAttsCacheDir;
    private Js js;

    DownloadForwardedAttachmentsAsyncTask(ForwardedAttachmentsDownloaderJobService jobService) {
      this.weakReference = new WeakReference<>(jobService);
    }

    @Override
    protected JobParameters doInBackground(JobParameters... params) {
      Log.d(TAG, "doInBackground");
      try {
        if (weakReference.get() != null) {
          Context context = weakReference.get().getApplicationContext();

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

          js = new Js(context, new SecurityStorageConnector(context));

          AccountDao account = new AccountDaoSource().getActiveAccountInformation(context);
          MessageDaoSource msgDaoSource = new MessageDaoSource();

          if (account != null) {
            List<GeneralMessageDetails> newMsgs = msgDaoSource.getOutboxMsgs(context, account.getEmail(),
                MessageState.NEW_FORWARDED);

            if (!CollectionUtils.isEmpty(newMsgs)) {
              sess = OpenStoreHelper.getSessionForAccountDao(context, account);
              store = OpenStoreHelper.openAndConnectToStore(context, account, sess);
              downloadForwardedAttachments(context, js, account, msgDaoSource);
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

    private void downloadForwardedAttachments(Context context, Js js, AccountDao account, MessageDaoSource daoSource) {
      List<GeneralMessageDetails> detailsList;
      AttachmentDaoSource attDaoSource = new AttachmentDaoSource();

      while (!CollectionUtils.isEmpty(detailsList = daoSource.getOutboxMsgs(context, account.getEmail(),
          MessageState.NEW_FORWARDED))) {
        GeneralMessageDetails details = detailsList.get(0);
        String detEmail = details.getEmail();
        String detLabel = details.getLabel();
        File msgAttsDir = new File(attCacheDir, details.getAttachmentsDir());
        try {
          String[] pubKeys = null;
          if (details.isEncrypted()) {
            PgpContact[] pgpContacts = EmailUtil.getAllRecipients(context, details);
            String senderEmail = EmailUtil.getFirstAddressString(details.getFrom());
            pubKeys = SecurityUtils.getRecipientsPubKeys(context, js, pgpContacts, account, senderEmail);
          }

          List<AttachmentInfo> atts = attDaoSource.getAttachmentInfoList(context, account.getEmail(),
              JavaEmailConstants.FOLDER_OUTBOX, details.getUid());

          if (CollectionUtils.isEmpty(atts)) {
            daoSource.updateMsgState(context, detEmail, detLabel, details.getUid(), MessageState.QUEUED);
            continue;
          }

          IMAPFolder folder = null;
          Message fwdMsg = null;

          MessageState msgState = MessageState.QUEUED;

          for (AttachmentInfo att : atts) {
            if (att.isForwarded() && att.getUri() == null) {
              FileAndDirectoryUtils.cleanDirectory(fwdAttsCacheDir);

              if (folder == null) {
                String folderName = new ImapLabelsDaoSource().getFolderByAlias(context, att.getEmail(),
                    att.getFwdFolder()).getFullName();
                folder = (IMAPFolder) store.getFolder(folderName);
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
              Part part = ImapProtocolUtil.getAttachmentPartById(folder, msgNumber, fwdMsg, att.getId());

              File tempFile = new File(fwdAttsCacheDir, UUID.randomUUID().toString());
              File attFile = new File(msgAttsDir, att.getName());

              if (part != null) {
                InputStream inputStream = part.getInputStream();
                if (inputStream != null) {
                  if (details.isEncrypted()) {
                    byte[] originalBytes = IOUtils.toByteArray(inputStream);
                    String fileName = FilenameUtils.removeExtension(att.getName());
                    byte[] encryptedBytes = this.js.crypto_message_encrypt(pubKeys, originalBytes, fileName);
                    FileUtils.writeByteArrayToFile(tempFile, encryptedBytes);
                  } else {
                    FileUtils.copyInputStreamToFile(inputStream, tempFile);
                  }

                  if (msgAttsDir.exists()) {
                    FileUtils.moveFile(tempFile, attFile);
                    att.setUri(FileProvider.getUriForFile(context, Constants.FILE_PROVIDER_AUTHORITY, attFile));
                  } else {
                    FileAndDirectoryUtils.cleanDirectory(fwdAttsCacheDir);
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

          int updateResult = daoSource.updateMsgState(context, detEmail, detLabel, details.getUid(), msgState);
          if (updateResult > 0) {
            MessagesSenderJobService.schedule(context);
          }
        } catch (Exception e) {
          e.printStackTrace();
          ExceptionUtil.handleError(e);

          if (!GeneralUtil.isInternetConnectionAvailable(context)) {
            publishProgress(true);
            break;
          }
        }
      }
    }
  }
}
