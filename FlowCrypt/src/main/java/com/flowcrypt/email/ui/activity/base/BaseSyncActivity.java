/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.LocalFolder;
import com.flowcrypt.email.service.BaseService;
import com.flowcrypt.email.service.EmailSyncService;
import com.flowcrypt.email.util.exception.ExceptionUtil;

import androidx.annotation.Nullable;

/**
 * This class describes a bind to the email sync service logic.
 *
 * @author DenBond7
 * Date: 16.06.2017
 * Time: 11:30
 * E-mail: DenBond7@gmail.com
 */

public abstract class BaseSyncActivity extends BaseActivity {
  // Messengers for communicating with the service.
  protected Messenger syncMessenger;
  protected Messenger syncReplyMessenger;

  /**
   * Flag indicating whether we have called bind on the {@link EmailSyncService}.
   */
  protected boolean isSyncServiceBound;

  private ServiceConnection syncConn = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      Log.d(tag, "Activity connected to " + name.getClassName());
      syncMessenger = new Messenger(service);
      isSyncServiceBound = true;

      registerReplyMessenger(EmailSyncService.MESSAGE_ADD_REPLY_MESSENGER, syncMessenger, syncReplyMessenger);
      onSyncServiceConnected();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
      Log.d(tag, "Activity disconnected from " + name.getClassName());
      syncMessenger = null;
      isSyncServiceBound = false;
    }
  };

  public BaseSyncActivity() {
    super();
    syncReplyMessenger = new Messenger(new ReplyHandler(this));
  }

  /**
   * Check is a sync enable.
   *
   * @return true - if sync enable, false - otherwise.
   */
  public abstract boolean isSyncEnabled();

  public abstract void onSyncServiceConnected();

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (isSyncEnabled()) {
      bindService(EmailSyncService.class, syncConn);
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (isSyncEnabled() && isSyncServiceBound) {
      if (syncMessenger != null) {
        unregisterReplyMessenger(EmailSyncService.MESSAGE_REMOVE_REPLY_MESSENGER, syncMessenger, syncReplyMessenger);
      }

      unbindService(EmailSyncService.class, syncConn);
      isSyncServiceBound = false;
    }
  }

  /**
   * Send a message with a backup to the key owner.
   *
   * @param requestCode The unique request code for identify the current action.
   */
  public void sendMsgWithPrivateKeyBackup(int requestCode) {
    if (checkServiceBound(isSyncServiceBound)) return;

    BaseService.Action action = new BaseService.Action(getReplyMessengerName(), requestCode, null);

    Message msg = Message.obtain(null, EmailSyncService.MESSAGE_SEND_MESSAGE_WITH_BACKUP, action);
    msg.replyTo = syncReplyMessenger;
    try {
      syncMessenger.send(msg);
    } catch (RemoteException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
    }
  }

  /**
   * Load the user private keys.
   *
   * @param requestCode The unique request code for identify the current action.
   */
  public void loadPrivateKeys(int requestCode) {
    if (checkServiceBound(isSyncServiceBound)) return;
    try {
      BaseService.Action action = new BaseService.Action(getReplyMessengerName(), requestCode, null);

      Message msg = Message.obtain(null, EmailSyncService.MESSAGE_LOAD_PRIVATE_KEYS, action);
      msg.replyTo = syncReplyMessenger;

      syncMessenger.send(msg);
    } catch (RemoteException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
    }
  }

  /**
   * Load messages from some localFolder in some range.
   *
   * @param requestCode The unique request code for identify the current action.
   * @param localFolder {@link LocalFolder} object.
   * @param start       The position of the start.
   * @param end         The position of the end.
   */
  public void loadMsgs(int requestCode, LocalFolder localFolder, int start, int end) {
    if (checkServiceBound(isSyncServiceBound)) return;

    BaseService.Action action = new BaseService.Action(getReplyMessengerName(), requestCode, localFolder);

    Message msg = Message.obtain(null, EmailSyncService.MESSAGE_LOAD_MESSAGES, start, end, action);
    msg.replyTo = syncReplyMessenger;
    try {
      syncMessenger.send(msg);
    } catch (RemoteException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
    }
  }

  /**
   * Start a job to load message to cache.
   *
   * @param requestCode            The unique request code for identify the current action.
   * @param localFolder            {@link LocalFolder} object.
   * @param alreadyLoadedMsgsCount The count of already loaded messages in the localFolder.
   */
  public void loadNextMsgs(int requestCode, LocalFolder localFolder, int alreadyLoadedMsgsCount) {
    if (checkServiceBound(isSyncServiceBound)) return;

    onProgressReplyReceived(requestCode, R.id.progress_id_start_of_loading_new_messages, null);

    BaseService.Action action = new BaseService.Action(getReplyMessengerName(), requestCode, localFolder);

    Message msg = Message.obtain(null, EmailSyncService.MESSAGE_LOAD_NEXT_MESSAGES, alreadyLoadedMsgsCount, 0, action);
    msg.replyTo = syncReplyMessenger;
    try {
      syncMessenger.send(msg);
    } catch (RemoteException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
    }
  }

  /**
   * Start a job to load searched messages to the cache.
   *
   * @param requestCode            The unique request code for identify the current action.
   * @param localFolder            {@link LocalFolder} object which contains the search query.
   * @param alreadyLoadedMsgsCount The count of already loaded messages in the localFolder.
   */
  public void searchNextMsgs(int requestCode, LocalFolder localFolder, int alreadyLoadedMsgsCount) {
    if (checkServiceBound(isSyncServiceBound)) return;

    BaseService.Action action = new BaseService.Action(getReplyMessengerName(), requestCode, localFolder);

    Message msg = Message.obtain(null, EmailSyncService.MESSAGE_SEARCH_MESSAGES, alreadyLoadedMsgsCount, 0, action);
    msg.replyTo = syncReplyMessenger;
    try {
      syncMessenger.send(msg);
    } catch (RemoteException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
    }
  }

  /**
   * Run update a folders list.
   *
   * @param requestCode    The unique request code for identify the current action.
   * @param isInBackground if true we will run this task using the passive queue, else we will use the active queue.
   */
  public void updateLabels(int requestCode, boolean isInBackground) {
    if (checkServiceBound(isSyncServiceBound)) return;

    BaseService.Action action = new BaseService.Action(getReplyMessengerName(), requestCode, null);

    Message msg = Message.obtain(null, EmailSyncService.MESSAGE_UPDATE_LABELS, isInBackground ? 1 : 0, 0, action);
    msg.replyTo = syncReplyMessenger;
    try {
      syncMessenger.send(msg);
    } catch (RemoteException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
    }
  }

  /**
   * Load the last messages which not exist in the database.
   *
   * @param requestCode        The unique request code for identify the current action.
   * @param currentLocalFolder {@link LocalFolder} object.
   */
  public void refreshMsgs(int requestCode, LocalFolder currentLocalFolder) {
    if (checkServiceBound(isSyncServiceBound)) return;

    BaseService.Action action = new BaseService.Action(getReplyMessengerName(), requestCode, currentLocalFolder);

    Message msg = Message.obtain(null, EmailSyncService.MESSAGE_REFRESH_MESSAGES, action);
    msg.replyTo = syncReplyMessenger;
    try {
      syncMessenger.send(msg);
    } catch (RemoteException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
    }
  }

  /**
   * Start a job to load message details.
   *
   * @param requestCode The unique request code for identify the current action.
   * @param localFolder {@link LocalFolder} object.
   * @param uid         The {@link com.sun.mail.imap.protocol.UID} of {@link javax.mail.Message ).
   */
  public void loadMsgDetails(int requestCode, LocalFolder localFolder, int uid) {
    if (checkServiceBound(isSyncServiceBound)) return;

    BaseService.Action action = new BaseService.Action(getReplyMessengerName(), requestCode, localFolder);

    Message msg = Message.obtain(null, EmailSyncService.MESSAGE_LOAD_MESSAGE_DETAILS, uid, 0, action);
    msg.replyTo = syncReplyMessenger;
    try {
      syncMessenger.send(msg);
    } catch (RemoteException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
    }
  }

  /**
   * Move the message to an another folder.
   *
   * @param requestCode            The unique request code for identify the current action.
   * @param sourcesLocalFolder     The message {@link LocalFolder} object.
   * @param destinationLocalFolder The new destionation {@link LocalFolder} object.
   * @param uid                    The {@link com.sun.mail.imap.protocol.UID} of {@link javax.mail
   *                               .Message ).
   */
  public void moveMsg(int requestCode, LocalFolder sourcesLocalFolder,
                      LocalFolder destinationLocalFolder, int uid) {
    if (checkServiceBound(isSyncServiceBound)) return;

    LocalFolder[] localFolders = new LocalFolder[]{sourcesLocalFolder, destinationLocalFolder};
    BaseService.Action action = new BaseService.Action(getReplyMessengerName(), requestCode, localFolders);

    Message msg = Message.obtain(null, EmailSyncService.MESSAGE_MOVE_MESSAGE, uid, 0, action);
    msg.replyTo = syncReplyMessenger;
    try {
      syncMessenger.send(msg);
    } catch (RemoteException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
    }
  }

  /**
   * Cancel all sync tasks which are waiting for executing.
   *
   * @param requestCode The unique request code for identify the current action.
   */
  public void cancelAllSyncTasks(int requestCode) {
    if (checkServiceBound(isSyncServiceBound)) return;

    BaseService.Action action = new BaseService.Action(getReplyMessengerName(), requestCode, null);

    Message msg = Message.obtain(null, EmailSyncService.MESSAGE_CANCEL_ALL_TASKS, action);
    msg.replyTo = syncReplyMessenger;
    try {
      syncMessenger.send(msg);
    } catch (RemoteException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
    }
  }

  public boolean isSyncServiceConnected() {
    return isSyncServiceBound;
  }
}
