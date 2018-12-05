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
import android.content.OperationApplicationException;
import android.os.AsyncTask;
import android.os.Build;
import android.os.RemoteException;
import android.util.Log;
import android.util.LongSparseArray;

import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.FoldersManager;
import com.flowcrypt.email.api.email.LocalFolder;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.api.email.sync.tasks.SyncFolderSyncTask;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource;
import com.flowcrypt.email.service.MessagesNotificationManager;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.sun.mail.imap.IMAPFolder;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

/**
 * This is an implementation of {@link JobService}. Here we are going to do syncing INBOX folder of an active account.
 *
 * @author Denis Bondarenko
 * Date: 20.06.2018
 * Time: 12:40
 * E-mail: DenBond7@gmail.com
 */
public class SyncJobService extends JobService implements SyncListener {
  private static final long INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(15);
  private static final String TAG = SyncJobService.class.getSimpleName();
  private MessagesNotificationManager messagesNotificationManager;

  public static void schedule(Context context) {
    ComponentName serviceName = new ComponentName(context, SyncJobService.class);
    JobInfo.Builder jobInfoBuilder = new JobInfo.Builder(JobIdManager.JOB_TYPE_SYNC, serviceName)
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .setPeriodic(INTERVAL_MILLIS)
        .setPersisted(true);

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      jobInfoBuilder.setRequiresBatteryNotLow(true);
    }

    JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    if (scheduler != null) {
      int result = scheduler.schedule(jobInfoBuilder.build());
      if (result == JobScheduler.RESULT_SUCCESS) {
        Log.d(TAG, "A job scheduled successfully");
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
    this.messagesNotificationManager = new MessagesNotificationManager(this);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "onDestroy");
  }

  @Override
  public boolean onStartJob(JobParameters jobParameters) {
    Log.d(TAG, "onStartJob");
    new CheckNewMessagesJobTask(this).execute(jobParameters);
    return true;
  }

  @Override
  public boolean onStopJob(JobParameters jobParameters) {
    Log.d(TAG, "onStopJob");
    jobFinished(jobParameters, true);
    return false;
  }

  @Override
  public Context getContext() {
    return getApplicationContext();
  }

  @Override
  public void onMessageWithBackupToKeyOwnerSent(AccountDao account, String ownerKey, int requestCode, boolean isSent) {

  }

  @Override
  public void onPrivateKeysFound(AccountDao account, List<String> keys, String ownerKey, int requestCode) {

  }

  @Override
  public void onMessageSent(AccountDao account, String ownerKey, int requestCode, boolean isSent) {

  }

  @Override
  public void onMessagesMoved(AccountDao account, IMAPFolder srcFolder, IMAPFolder destFolder,
                              Message[] msgs, String ownerKey, int requestCode) {

  }

  @Override
  public void onMessageMoved(AccountDao account, IMAPFolder srcFolder, IMAPFolder destFolder,
                             Message msg, String ownerKey, int requestCode) {

  }

  @Override
  public void onMessageDetailsReceived(AccountDao account, LocalFolder localFolder, IMAPFolder remoteFolder, long uid,
                                       Message msg, String rawMsgWithOutAtts, String ownerKey, int requestCode) {

  }

  @Override
  public void onMessagesReceived(AccountDao account, LocalFolder localFolder, IMAPFolder remoteFolder,
                                 Message[] msgs, String ownerKey, int requestCode) {

  }

  @Override
  public void onSearchMessagesReceived(AccountDao account, LocalFolder localFolder, IMAPFolder remoteFolder,
                                       Message[] msgs, String ownerKey, int requestCode) {

  }

  @Override
  public void onRefreshMessagesReceived(AccountDao account, LocalFolder localFolder,
                                        IMAPFolder remoteFolder, Message[] newMsgs,
                                        Message[] updateMsgs, String ownerKey, int requestCode) {
    try {
      MessageDaoSource msgDaoSource = new MessageDaoSource();

      Map<Long, String> mapOfUIDsAndMsgsFlags = msgDaoSource.getMapOfUIDAndMessageFlags
          (getApplicationContext(), account.getEmail(), localFolder.getFolderAlias());

      Collection<Long> uidSet = new HashSet<>(mapOfUIDsAndMsgsFlags.keySet());
      Collection<Long> deleteCandidatesUIDs = EmailUtil.genDeleteCandidates(uidSet, remoteFolder, updateMsgs);

      String folderAlias = localFolder.getFolderAlias();
      List<GeneralMessageDetails> generalMessageDetailsBeforeUpdate = msgDaoSource.getNewMessages
          (getApplicationContext(), account.getEmail(), folderAlias);

      msgDaoSource.deleteMessagesByUID(getApplicationContext(), account.getEmail(), localFolder.getFolderAlias(),
          deleteCandidatesUIDs);

      msgDaoSource.updateMessagesByUID(getApplicationContext(), account.getEmail(), localFolder.getFolderAlias(),
          remoteFolder, EmailUtil.genUpdateCandidates(mapOfUIDsAndMsgsFlags, remoteFolder, updateMsgs));

      List<GeneralMessageDetails> detailsAfterUpdate = msgDaoSource.getNewMessages(getApplicationContext(),
          account.getEmail(), folderAlias);

      List<GeneralMessageDetails> detailsDeleteCandidates = new LinkedList<>(generalMessageDetailsBeforeUpdate);
      detailsDeleteCandidates.removeAll(detailsAfterUpdate);

      boolean isInbox = FoldersManager.getFolderTypeForImapFolder(localFolder) == FoldersManager.FolderType.INBOX;
      if (!GeneralUtil.isAppForegrounded() && isInbox) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          for (GeneralMessageDetails details : detailsDeleteCandidates) {
            messagesNotificationManager.cancel(this, details.getUid());
          }
        } else {
          if (!detailsDeleteCandidates.isEmpty()) {
            List<Integer> unseenMsgs = msgDaoSource.getUIDOfUnseenMessages(this, account.getEmail(), folderAlias);
            messagesNotificationManager.notify(this, account, localFolder, detailsAfterUpdate, unseenMsgs, true);
          }
        }
      }
    } catch (RemoteException | MessagingException | OperationApplicationException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
    }
  }

  @Override
  public void onFolderInfoReceived(AccountDao account, javax.mail.Folder[] folders, String ownerKey, int requestCode) {

  }

  @Override
  public void onError(AccountDao account, int errorType, Exception e, String ownerKey, int requestCode) {

  }

  @Override
  public void onActionProgress(AccountDao account, String ownerKey, int requestCode, int resultCode) {

  }

  @Override
  public void onMessageChanged(AccountDao account, LocalFolder localFolder, IMAPFolder remoteFolder, Message msg,
                               String ownerKey, int requestCode) {

  }

  @Override
  public void onIdentificationToEncryptionCompleted(AccountDao account, LocalFolder localFolder,
                                                    IMAPFolder remoteFolder, String ownerKey, int requestCode) {

  }

  @Override
  public void onNewMessagesReceived(final AccountDao account, LocalFolder localFolder, IMAPFolder remoteFolder,
                                    Message[] newMsgs, LongSparseArray<Boolean> msgsEncryptionStates,
                                    String ownerKey, int requestCode) {
    try {
      Context context = getApplicationContext();
      boolean isEncryptedModeEnabled = new AccountDaoSource().isEncryptedModeEnabled(context, account.getEmail());

      MessageDaoSource msgDaoSource = new MessageDaoSource();

      Map<Long, String> mapOfUIDAndMsgFlags = msgDaoSource.getMapOfUIDAndMessageFlags
          (context, account.getEmail(), localFolder.getFolderAlias());

      Collection<Long> uids = new HashSet<>(mapOfUIDAndMsgFlags.keySet());

      javax.mail.Message[] newCandidates = EmailUtil.genNewCandidates(uids, remoteFolder, newMsgs);

      msgDaoSource.addRows(context, account.getEmail(), localFolder.getFolderAlias(), remoteFolder, newCandidates,
          msgsEncryptionStates, !GeneralUtil.isAppForegrounded(), isEncryptedModeEnabled);

      if (!GeneralUtil.isAppForegrounded()) {
        String folderAlias = localFolder.getFolderAlias();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && newCandidates.length == 0) {
          return;
        }

        List<GeneralMessageDetails> newMsgsList = msgDaoSource.getNewMessages(getApplicationContext(),
            account.getEmail(), folderAlias);
        List<Integer> unseenUIDs = msgDaoSource.getUIDOfUnseenMessages(this, account.getEmail(), folderAlias);

        messagesNotificationManager.notify(this, account, localFolder, newMsgsList, unseenUIDs, false);
      }
    } catch (MessagingException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
    }
  }

  /**
   * This is a worker. Here we will do sync in the background thread. If the sync will be failed we'll schedule it
   * again.
   */
  private static class CheckNewMessagesJobTask extends AsyncTask<JobParameters, Boolean, JobParameters> {
    private final WeakReference<SyncJobService> weakReference;

    private Session sess;
    private Store store;
    private boolean isFailed;

    CheckNewMessagesJobTask(SyncJobService syncJobService) {
      this.weakReference = new WeakReference<>(syncJobService);
    }

    @Override
    protected JobParameters doInBackground(JobParameters... params) {
      Log.d(TAG, "doInBackground");

      try {
        if (weakReference.get() != null) {
          Context context = weakReference.get().getApplicationContext();
          AccountDao account = new AccountDaoSource().getActiveAccountInformation(context);

          if (account != null) {
            FoldersManager foldersManager = FoldersManager.fromDatabase(context, account.getEmail());
            LocalFolder localFolder = foldersManager.findInboxFolder();

            if (localFolder != null) {
              sess = OpenStoreHelper.getSessionForAccountDao(context, account);
              store = OpenStoreHelper.openAndConnectToStore(context, account, sess);

              new SyncFolderSyncTask("", 0, localFolder).runIMAPAction(account, sess, store, weakReference.get());

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
  }
}
