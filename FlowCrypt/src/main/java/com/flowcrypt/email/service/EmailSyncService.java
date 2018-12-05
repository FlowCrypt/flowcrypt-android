/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.OperationApplicationException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.util.LongSparseArray;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.FoldersManager;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.LocalFolder;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.protocol.ImapProtocolUtil;
import com.flowcrypt.email.api.email.sync.EmailSyncManager;
import com.flowcrypt.email.api.email.sync.SyncErrorTypes;
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.database.dao.source.imap.AttachmentDaoSource;
import com.flowcrypt.email.database.dao.source.imap.ImapLabelsDaoSource;
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource;
import com.flowcrypt.email.model.EmailAndNamePair;
import com.flowcrypt.email.ui.activity.SearchMessagesActivity;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.google.android.gms.common.util.CollectionUtils;
import com.sun.mail.imap.IMAPFolder;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.FolderClosedException;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.StoreClosedException;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.InternetHeaders;

import androidx.annotation.NonNull;

/**
 * This the email synchronization service. This class is responsible for the logic of
 * synchronization work. Using this service we can asynchronous download information and send
 * data to the IMAP server.
 *
 * @author DenBond7
 * Date: 14.06.2017
 * Time: 12:18
 * E-mail: DenBond7@gmail.com
 */
public class EmailSyncService extends BaseService implements SyncListener {
  public static final String ACTION_SWITCH_ACCOUNT = "ACTION_SWITCH_ACCOUNT";
  public static final String ACTION_BEGIN_SYNC = "ACTION_BEGIN_SYNC";

  public static final int REPLY_RESULT_CODE_ACTION_OK = 0;
  public static final int REPLY_RESULT_CODE_ACTION_ERROR_MESSAGE_NOT_FOUND = 1;
  public static final int REPLY_RESULT_CODE_ACTION_ERROR_BACKUP_NOT_SENT = 2;
  public static final int REPLY_RESULT_CODE_ACTION_ERROR_MESSAGE_WAS_NOT_SENT = 3;
  public static final int REPLY_RESULT_CODE_ACTION_ERROR_MESSAGE_NOT_EXISTS = 4;
  public static final int REPLY_RESULT_CODE_NEED_UPDATE = 2;

  public static final int MESSAGE_ADD_REPLY_MESSENGER = 1;
  public static final int MESSAGE_REMOVE_REPLY_MESSENGER = 2;
  public static final int MESSAGE_UPDATE_LABELS = 3;
  public static final int MESSAGE_LOAD_MESSAGES = 4;
  public static final int MESSAGE_LOAD_NEXT_MESSAGES = 5;
  public static final int MESSAGE_REFRESH_MESSAGES = 6;
  public static final int MESSAGE_LOAD_MESSAGE_DETAILS = 7;
  public static final int MESSAGE_MOVE_MESSAGE = 8;
  public static final int MESSAGE_LOAD_PRIVATE_KEYS = 9;
  public static final int MESSAGE_SEND_MESSAGE_WITH_BACKUP = 10;
  public static final int MESSAGE_SEARCH_MESSAGES = 11;
  public static final int MESSAGE_CANCEL_ALL_TASKS = 12;

  private static final String TAG = EmailSyncService.class.getSimpleName();
  /**
   * This {@link Messenger} is responsible for the receive intents from other client and
   * handles them.
   */
  private Messenger messenger;

  private Map<String, Messenger> replyToMessengers;

  private EmailSyncManager emailSyncManager;

  private boolean isServiceStarted;
  private BroadcastReceiver connectionBroadcastReceiver;
  private MessagesNotificationManager notificationManager;

  public EmailSyncService() {
    this.replyToMessengers = new HashMap<>();
  }

  /**
   * This method can bu used to start {@link EmailSyncService}.
   *
   * @param context Interface to global information about an application environment.
   */
  public static void startEmailSyncService(Context context) {
    Intent startEmailServiceIntent = new Intent(context, EmailSyncService.class);
    startEmailServiceIntent.setAction(ACTION_BEGIN_SYNC);
    context.startService(startEmailServiceIntent);
  }

  /**
   * This method can bu used to start {@link EmailSyncService} with the action {@link #ACTION_SWITCH_ACCOUNT}.
   *
   * @param context Interface to global information about an application environment.
   */
  public static void switchAccount(Context context) {
    Intent startEmailServiceIntent = new Intent(context, EmailSyncService.class);
    startEmailServiceIntent.setAction(ACTION_SWITCH_ACCOUNT);
    context.startService(startEmailServiceIntent);
  }


  @Override
  public void onCreate() {
    super.onCreate();
    Log.d(TAG, "onCreate");

    this.notificationManager = new MessagesNotificationManager(this);

    emailSyncManager = new EmailSyncManager(new AccountDaoSource().getActiveAccountInformation(this));
    emailSyncManager.setSyncListener(this);

    messenger = new Messenger(new IncomingHandler(emailSyncManager, replyToMessengers));

    connectionBroadcastReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        handleConnectivityAction(context, intent);
      }
    };

    registerReceiver(connectionBroadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.d(TAG, "onStartCommand |intent =" + intent + "|flags = " + flags + "|startId = " + startId);
    isServiceStarted = true;

    if (intent != null && intent.getAction() != null) {
      switch (intent.getAction()) {
        case ACTION_SWITCH_ACCOUNT:
          emailSyncManager.switchAccount(new AccountDaoSource().getActiveAccountInformation(this));
          break;

        default:
          emailSyncManager.beginSync(false);
          break;
      }
    } else {
      emailSyncManager.beginSync(false);
    }

    return super.onStartCommand(intent, flags, startId);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "onDestroy");

    if (emailSyncManager != null) {
      emailSyncManager.stopSync();
    }

    unregisterReceiver(connectionBroadcastReceiver);
  }

  @Override
  public boolean onUnbind(Intent intent) {
    Log.d(TAG, "onUnbind:" + intent);
    return super.onUnbind(intent);
  }

  @Override
  public void onRebind(Intent intent) {
    super.onRebind(intent);
    Log.d(TAG, "onRebind:" + intent);
  }

  @Override
  public IBinder onBind(Intent intent) {
    Log.d(TAG, "onBind:" + intent);

    if (!isServiceStarted) {
      EmailSyncService.startEmailSyncService(getContext());
    }
    return messenger.getBinder();
  }

  @Override
  public Context getContext() {
    return this.getApplicationContext();
  }

  @Override
  public void onMessageWithBackupToKeyOwnerSent(AccountDao account, String ownerKey, int requestCode, boolean isSent) {
    try {
      if (isSent) {
        sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_OK);
      } else {
        sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_ERROR_BACKUP_NOT_SENT);
      }
    } catch (RemoteException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
      onError(account, SyncErrorTypes.UNKNOWN_ERROR, e, ownerKey, requestCode);
    }
  }

  @Override
  public void onPrivateKeysFound(AccountDao account, List<String> keys, String ownerKey, int requestCode) {
    try {
      sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_OK, keys);
    } catch (RemoteException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
    }
  }

  @Override
  public void onMessageSent(AccountDao account, String ownerKey, int requestCode, boolean isSent) {
    try {
      if (isSent) {
        sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_OK);
      } else {
        sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_ERROR_MESSAGE_WAS_NOT_SENT);
      }
    } catch (RemoteException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
      onError(account, SyncErrorTypes.UNKNOWN_ERROR, e, ownerKey, requestCode);
    }
  }

  @Override
  public void onMessagesMoved(AccountDao account, IMAPFolder srcFolder, IMAPFolder destFolder,
                              javax.mail.Message[] msgs, String ownerKey, int requestCode) {
    //Todo-denbond7 Not implemented yet.
  }

  @Override
  public void onMessageMoved(AccountDao account, IMAPFolder srcFolder, IMAPFolder destFolder,
                             javax.mail.Message msg, String ownerKey, int requestCode) {
    try {
      if (msg != null) {
        sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_OK);
      } else {
        sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_ERROR_MESSAGE_NOT_EXISTS);
      }
    } catch (RemoteException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
      onError(account, SyncErrorTypes.UNKNOWN_ERROR, e, ownerKey, requestCode);
    }
  }

  @Override
  public void onMessageDetailsReceived(AccountDao account, LocalFolder localFolder,
                                       IMAPFolder remoteFolder, long uid, javax.mail.Message msg,
                                       String rawMsgWithoutAtts, String ownerKey, int requestCode) {
    try {
      MessageDaoSource msgDaoSource = new MessageDaoSource();
      msgDaoSource.updateMessageRawText(this, account.getEmail(), localFolder.getFolderAlias(), uid, rawMsgWithoutAtts);

      if (TextUtils.isEmpty(rawMsgWithoutAtts)) {
        sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_ERROR_MESSAGE_NOT_FOUND);
      } else {
        updateAttachmentTable(account, localFolder, remoteFolder, msg);
        sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_OK);
      }
    } catch (RemoteException | MessagingException | IOException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
      onError(account, SyncErrorTypes.UNKNOWN_ERROR, e, ownerKey, requestCode);
    }
  }

  @Override
  public void onMessagesReceived(AccountDao account, LocalFolder localFolder,
                                 IMAPFolder remoteFolder, javax.mail.Message[] msgs, String ownerKey, int requestCode) {
    Log.d(TAG, "onMessagesReceived: imapFolder = " + remoteFolder.getFullName() + " message " +
        "count: " + msgs.length);
    try {
      String email = account.getEmail();
      String folderAlias = localFolder.getFolderAlias();

      boolean isEncryptedModeEnabled = new AccountDaoSource().isEncryptedModeEnabled(this, email);

      MessageDaoSource messageDaoSource = new MessageDaoSource();
      messageDaoSource.addRows(this, email, folderAlias, remoteFolder, msgs, false, isEncryptedModeEnabled);

      if (!isEncryptedModeEnabled) {
        emailSyncManager.identifyEncryptedMessages(ownerKey, R.id.syns_identify_encrypted_messages, localFolder);
      }

      if (msgs.length > 0) {
        sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_NEED_UPDATE, localFolder);
      } else {
        sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_OK, localFolder);
      }

      updateLocalContactsIfNeeded(remoteFolder, msgs);
    } catch (MessagingException | RemoteException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
      onError(account, SyncErrorTypes.UNKNOWN_ERROR, e, ownerKey, requestCode);
    }
  }

  @Override
  public void onNewMessagesReceived(AccountDao account, LocalFolder localFolder,
                                    IMAPFolder remoteFolder, javax.mail.Message[] newMsgs,
                                    LongSparseArray<Boolean> msgsEncryptionStates, String ownerKey, int requestCode) {
    Log.d(TAG, "onMessagesReceived:message count: " + newMsgs.length);
    try {
      String email = account.getEmail();
      String folderAlias = localFolder.getFolderAlias();

      MessageDaoSource msgDaoSource = new MessageDaoSource();
      FoldersManager.FolderType folderType = FoldersManager.getFolderTypeForImapFolder(localFolder);
      boolean isNew = !GeneralUtil.isAppForegrounded() && folderType == FoldersManager.FolderType.INBOX;
      msgDaoSource.addRows(this, email, folderAlias, remoteFolder, newMsgs, msgsEncryptionStates, isNew, false);

      if (newMsgs.length > 0) {
        sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_NEED_UPDATE);
      } else {
        sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_OK);
      }

      if (!GeneralUtil.isAppForegrounded()) {
        List<GeneralMessageDetails> detailsList = msgDaoSource.getNewMessages(this, email, folderAlias);
        List<Integer> uidListOfUnseenMessages = msgDaoSource.getUIDOfUnseenMessages(this, email, folderAlias);
        notificationManager.notify(this, account, localFolder, detailsList, uidListOfUnseenMessages, false);
      }
    } catch (MessagingException | RemoteException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
      onError(account, SyncErrorTypes.UNKNOWN_ERROR, e, ownerKey, requestCode);
    }
  }

  @Override
  public void onSearchMessagesReceived(AccountDao account, LocalFolder localFolder, IMAPFolder remoteFolder,
                                       javax.mail.Message[] msgs, String ownerKey, int requestCode) {
    Log.d(TAG, "onSearchMessagesReceived: message count: " + msgs.length);
    String email = account.getEmail();
    try {
      boolean isEncryptedModeEnabled = new AccountDaoSource().isEncryptedModeEnabled(this, email);

      MessageDaoSource msgDaoSource = new MessageDaoSource();
      String searchLabel = SearchMessagesActivity.SEARCH_FOLDER_NAME;
      msgDaoSource.addRows(this, email, searchLabel, remoteFolder, msgs, false, isEncryptedModeEnabled);

      if (!isEncryptedModeEnabled) {
        emailSyncManager.identifyEncryptedMessages(ownerKey, R.id.syns_identify_encrypted_messages, localFolder);
      }

      if (msgs.length > 0) {
        sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_NEED_UPDATE);
      } else {
        sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_OK);
      }

      updateLocalContactsIfNeeded(remoteFolder, msgs);
    } catch (MessagingException | RemoteException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
      onError(account, SyncErrorTypes.UNKNOWN_ERROR, e, ownerKey, requestCode);
    }
  }

  @Override
  public void onRefreshMessagesReceived(AccountDao account, LocalFolder localFolder,
                                        IMAPFolder remoteFolder, javax.mail.Message[] newMsgs,
                                        javax.mail.Message[] updatedMsgs, String key, int requestCode) {
    Log.d(TAG, "onRefreshMessagesReceived: imapFolder = " + remoteFolder.getFullName() + " newMessages " +
        "count: " + newMsgs.length + ", updateMessages count = " + updatedMsgs.length);
    String email = account.getEmail();
    String folderAlias = localFolder.getFolderAlias();

    try {
      MessageDaoSource msgsDaoSource = new MessageDaoSource();

      Map<Long, String> mapOfUIDAndMsgFlags = msgsDaoSource.getMapOfUIDAndMessageFlags(this, email, folderAlias);
      Collection<Long> msgsUIDs = new HashSet<>(mapOfUIDAndMsgFlags.keySet());
      Collection<Long> deleteCandidatesUIDs = EmailUtil.genDeleteCandidates(msgsUIDs, remoteFolder, updatedMsgs);

      msgsDaoSource.deleteMessagesByUID(this, email, folderAlias, deleteCandidatesUIDs);

      FoldersManager.FolderType folderType = FoldersManager.getFolderTypeForImapFolder(localFolder);
      if (!GeneralUtil.isAppForegrounded() && folderType == FoldersManager.FolderType.INBOX) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          for (long uid : deleteCandidatesUIDs) {
            notificationManager.cancel(this, (int) uid);
          }
        } else {
          List<GeneralMessageDetails> detailsList = msgsDaoSource.getNewMessages(this, email, folderAlias);
          List<Integer> uidListOfUnseenMsgs = msgsDaoSource.getUIDOfUnseenMessages(this, email, folderAlias);
          notificationManager.notify(this, account, localFolder, detailsList, uidListOfUnseenMsgs, false);
        }
      }

      javax.mail.Message[] newCandidates = EmailUtil.genNewCandidates(msgsUIDs, remoteFolder, newMsgs);

      boolean isEncryptedModeEnabled = new AccountDaoSource().isEncryptedModeEnabled(this, email);
      boolean isNew = !GeneralUtil.isAppForegrounded() && folderType == FoldersManager.FolderType.INBOX;

      msgsDaoSource.addRows(this, email, folderAlias, remoteFolder, newCandidates, isNew, isEncryptedModeEnabled);

      if (!isEncryptedModeEnabled) {
        emailSyncManager.identifyEncryptedMessages(key, R.id.syns_identify_encrypted_messages, localFolder);
      }

      javax.mail.Message[] msgs = EmailUtil.genUpdateCandidates(mapOfUIDAndMsgFlags, remoteFolder, updatedMsgs);
      msgsDaoSource.updateMessagesByUID(this, email, folderAlias, remoteFolder, msgs);

      if (newMsgs.length > 0 || updatedMsgs.length > 0) {
        sendReply(key, requestCode, REPLY_RESULT_CODE_NEED_UPDATE);
      } else {
        sendReply(key, requestCode, REPLY_RESULT_CODE_ACTION_OK);
      }

      updateLocalContactsIfNeeded(remoteFolder, newCandidates);
    } catch (RemoteException | MessagingException | OperationApplicationException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
      if (e instanceof StoreClosedException || e instanceof FolderClosedException) {
        onError(account, SyncErrorTypes.ACTION_FAILED_SHOW_TOAST, e, key, requestCode);
      }
    }
  }

  @Override
  public void onFolderInfoReceived(AccountDao account, Folder[] folders, String key, int requestCode) {
    Log.d(TAG, "onFolderInfoReceived:" + Arrays.toString(folders));
    String email = account.getEmail();

    FoldersManager foldersManager = new FoldersManager();
    for (Folder folder : folders) {
      try {
        IMAPFolder imapFolder = (IMAPFolder) folder;
        foldersManager.addFolder(imapFolder, folder.getName());
      } catch (MessagingException e) {
        e.printStackTrace();
        ExceptionUtil.handleError(e);
      }
    }

    LocalFolder localFolder = new LocalFolder(JavaEmailConstants.FOLDER_OUTBOX, JavaEmailConstants.FOLDER_OUTBOX, 0,
        new String[]{JavaEmailConstants.FOLDER_FLAG_HAS_NO_CHILDREN}, false);

    foldersManager.addFolder(localFolder);

    ImapLabelsDaoSource imapLabelsDaoSource = new ImapLabelsDaoSource();
    List<LocalFolder> currentFoldersList = imapLabelsDaoSource.getFolders(this, email);
    if (currentFoldersList.isEmpty()) {
      imapLabelsDaoSource.addRows(this, email, foldersManager.getAllFolders());
    } else {
      try {
        imapLabelsDaoSource.updateLabels(this, email, currentFoldersList, foldersManager.getAllFolders());
      } catch (Exception e) {
        e.printStackTrace();
        ExceptionUtil.handleError(e);
      }
    }

    try {
      sendReply(key, requestCode, REPLY_RESULT_CODE_ACTION_OK);
    } catch (RemoteException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
    }
  }

  @Override
  public void onError(AccountDao account, int errorType, Exception e, String key, int requestCode) {
    Log.e(TAG, "onError: errorType" + errorType + "| e =" + e);
    try {
      if (replyToMessengers.containsKey(key)) {
        Messenger messenger = replyToMessengers.get(key);
        messenger.send(Message.obtain(null, REPLY_ERROR, requestCode, errorType, e));
        ExceptionUtil.handleError(e);
      }
    } catch (RemoteException remoteException) {
      remoteException.printStackTrace();
    }
  }

  @Override
  public void onActionProgress(AccountDao account, String ownerKey, int requestCode, int resultCode) {
    Log.d(TAG, "onActionProgress: account" + account + "| ownerKey =" + ownerKey + "| requestCode =" + requestCode);
    try {
      if (replyToMessengers.containsKey(ownerKey)) {
        Messenger messenger = replyToMessengers.get(ownerKey);
        messenger.send(Message.obtain(null, REPLY_ACTION_PROGRESS, requestCode, resultCode, account));
      }
    } catch (RemoteException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
    }
  }

  @Override
  public void onMessageChanged(AccountDao account, LocalFolder localFolder, IMAPFolder remoteFolder,
                               javax.mail.Message msg, String ownerKey, int requestCode) {
    String email = account.getEmail();
    String folderAlias = localFolder.getFolderAlias();
    FoldersManager.FolderType folderType = FoldersManager.getFolderTypeForImapFolder(localFolder);

    if (!GeneralUtil.isAppForegrounded() && folderType == FoldersManager.FolderType.INBOX) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        try {
          if (msg.getFlags().contains(Flags.Flag.SEEN)) {
            notificationManager.cancel(this, (int) remoteFolder.getUID(msg));
          }
        } catch (MessagingException e) {
          e.printStackTrace();
        }
      } else {
        MessageDaoSource msgDaoSource = new MessageDaoSource();
        List<GeneralMessageDetails> detailsList = msgDaoSource.getNewMessages(this, email, folderAlias);
        List<Integer> uidListOfUnseenMsgs = msgDaoSource.getUIDOfUnseenMessages(this, email, folderAlias);
        notificationManager.notify(this, account, localFolder, detailsList, uidListOfUnseenMsgs, true);
      }
    }
  }

  @Override
  public void onIdentificationToEncryptionCompleted(AccountDao account, LocalFolder localFolder,
                                                    IMAPFolder remoteFolder, String ownerKey, int requestCode) {
    String email = account.getEmail();
    String folderAlias = localFolder.getFolderAlias();
    FoldersManager.FolderType folderType = FoldersManager.getFolderTypeForImapFolder(localFolder);

    if (folderType == FoldersManager.FolderType.INBOX && !GeneralUtil.isAppForegrounded()) {
      MessageDaoSource msgDaoSource = new MessageDaoSource();

      List<GeneralMessageDetails> detailsList = msgDaoSource.getNewMessages(this, email, folderAlias);
      List<Integer> uidListOfUnseenMsgs = msgDaoSource.getUIDOfUnseenMessages(this, email, folderAlias);

      notificationManager.notify(this, account, localFolder, detailsList, uidListOfUnseenMsgs, false);
    }
  }

  protected void handleConnectivityAction(Context context, Intent intent) {
    if (ConnectivityManager.CONNECTIVITY_ACTION.equalsIgnoreCase(intent.getAction())) {
      ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context
          .CONNECTIVITY_SERVICE);

      if (connectivityManager != null) {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (GeneralUtil.isInternetConnectionAvailable(this)) {
          Log.d(TAG, "networkInfo = " + networkInfo);
          if (emailSyncManager != null) {
            emailSyncManager.beginSync(false);
          }
        }
      }
    }
  }

  /**
   * @param account     The object which contains information about an email account.
   * @param localFolder The local reflection of the remote localFolder.
   * @param imapFolder  The localFolder where the new messages exist.
   * @param msg         The new messages.
   * @throws MessagingException This exception meybe happen when we try to call {@code
   *                            {@link IMAPFolder#getUID(javax.mail.Message)}}
   */
  private void updateAttachmentTable(AccountDao account, LocalFolder localFolder,
                                     IMAPFolder imapFolder, javax.mail.Message msg)
      throws MessagingException, IOException {
    String email = account.getEmail();
    String folderAlias = localFolder.getFolderAlias();

    AttachmentDaoSource attachmentDaoSource = new AttachmentDaoSource();
    ArrayList<ContentValues> contentValuesList = new ArrayList<>();

    ArrayList<AttachmentInfo> attachmentInfoList = getAttachmentsInfoFromPart(imapFolder, msg.getMessageNumber(), msg);

    if (!attachmentInfoList.isEmpty()) {
      for (AttachmentInfo att : attachmentInfoList) {
        ContentValues cV = AttachmentDaoSource.prepareContentValues(email, folderAlias, imapFolder.getUID(msg), att);
        contentValuesList.add(cV);
      }
    }

    attachmentDaoSource.addRows(this, contentValuesList.toArray(new ContentValues[0]));
  }

  /**
   * Find attachments in the {@link Part}.
   *
   * @param imapFolder The {@link IMAPFolder} which contains the parent message;
   * @param msgNumber  This number will be used for fetching {@link Part} details;
   * @param part       The parent part.
   * @return The list of created {@link AttachmentInfo}
   * @throws MessagingException
   * @throws IOException
   */
  @NonNull
  private ArrayList<AttachmentInfo> getAttachmentsInfoFromPart(IMAPFolder imapFolder, int msgNumber, Part part)
      throws MessagingException, IOException {
    ArrayList<AttachmentInfo> atts = new ArrayList<>();

    if (part.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
      Multipart multiPart = (Multipart) part.getContent();
      int numberOfParts = multiPart.getCount();
      String[] headers;
      for (int partCount = 0; partCount < numberOfParts; partCount++) {
        BodyPart bodyPart = multiPart.getBodyPart(partCount);
        if (bodyPart.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
          ArrayList<AttachmentInfo> partAtts = getAttachmentsInfoFromPart(imapFolder, msgNumber, bodyPart);
          if (!CollectionUtils.isEmpty(partAtts)) {
            atts.addAll(partAtts);
          }
        } else if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
          InputStream inputStream = ImapProtocolUtil.getHeaderStream(imapFolder, msgNumber, partCount + 1);

          if (inputStream != null) {
            InternetHeaders internetHeaders = new InternetHeaders(inputStream);
            headers = internetHeaders.getHeader(JavaEmailConstants.HEADER_CONTENT_ID);

            if (headers == null) {
              //try to receive custom Gmail attachments header X-Attachment-Id
              headers = internetHeaders.getHeader(JavaEmailConstants.HEADER_X_ATTACHMENT_ID);
            }

            if (headers != null && headers.length > 0 && !TextUtils.isEmpty(bodyPart.getFileName())) {
              AttachmentInfo attachmentInfo = new AttachmentInfo();
              attachmentInfo.setName(bodyPart.getFileName());
              attachmentInfo.setEncodedSize(bodyPart.getSize());
              attachmentInfo.setType(new ContentType(bodyPart.getContentType()).getBaseType());
              attachmentInfo.setId(headers[0]);
              atts.add(attachmentInfo);
            }
          }
        }
      }
    }

    return atts;
  }

  /**
   * Send a reply to the called component.
   *
   * @param key         The key which identify the reply to {@link Messenger}
   * @param requestCode The unique request code for the reply to {@link android.os.Messenger}.
   * @param resultCode  The result code of the some action. Can take the following values:
   *                    <ul>
   *                    <li>{@link EmailSyncService#REPLY_RESULT_CODE_ACTION_OK}</li>
   *                    <li>{@link EmailSyncService#REPLY_RESULT_CODE_NEED_UPDATE}</li>
   *                    </ul>
   *                    and different errors.
   * @throws RemoteException
   */
  private void sendReply(String key, int requestCode, int resultCode) throws RemoteException {
    sendReply(key, requestCode, resultCode, null);
  }

  /**
   * Send a reply to the called component.
   *
   * @param key         The key which identify the reply to {@link Messenger}
   * @param requestCode The unique request code for the reply to {@link android.os.Messenger}.
   * @param resultCode  The result code of the some action. Can take the following values:
   *                    <ul>
   *                    <li>{@link EmailSyncService#REPLY_RESULT_CODE_ACTION_OK}</li>
   *                    <li>{@link EmailSyncService#REPLY_RESULT_CODE_NEED_UPDATE}</li>
   *                    </ul>
   *                    and different errors.
   * @param obj         The object which will be send to the request {@link Messenger}.
   * @throws RemoteException
   */
  private void sendReply(String key, int requestCode, int resultCode, Object obj) throws RemoteException {
    if (replyToMessengers.containsKey(key)) {
      Messenger messenger = replyToMessengers.get(key);
      messenger.send(Message.obtain(null, REPLY_OK, requestCode, resultCode, obj));
    }
  }

  /**
   * Update information about contacts in the local database if current messages from the
   * Sent folder.
   *
   * @param imapFolder The folder where messages exist.
   * @param messages   The received messages.
   */
  private void updateLocalContactsIfNeeded(IMAPFolder imapFolder, javax.mail.Message[] messages) {
    try {
      boolean isSentFolder = Arrays.asList(imapFolder.getAttributes()).contains("\\Sent");

      if (isSentFolder) {
        ArrayList<EmailAndNamePair> emailAndNamePairs = new ArrayList<>();
        for (javax.mail.Message message : messages) {
          emailAndNamePairs.addAll(getEmailAndNamePairs(message));
        }

        EmailAndNameUpdaterService.enqueueWork(this, emailAndNamePairs);
      }
    } catch (MessagingException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
    }
  }

  /**
   * Generate a list of {@link EmailAndNamePair} objects from the input message.
   * This information will be retrieved from "to" and "cc" headers.
   *
   * @param msg The input {@link javax.mail.Message}.
   * @return <tt>{@link List}</tt> of EmailAndNamePair objects, which contains information
   * about
   * emails and names.
   * @throws MessagingException when retrieve information about recipients.
   */
  private List<EmailAndNamePair> getEmailAndNamePairs(javax.mail.Message msg) throws MessagingException {
    List<EmailAndNamePair> pairs = new ArrayList<>();

    Address[] addressesTo = msg.getRecipients(javax.mail.Message.RecipientType.TO);
    if (addressesTo != null) {
      for (Address address : addressesTo) {
        InternetAddress internetAddress = (InternetAddress) address;
        pairs.add(new EmailAndNamePair(internetAddress.getAddress(), internetAddress.getPersonal()));
      }
    }

    Address[] addressesCC = msg.getRecipients(javax.mail.Message.RecipientType.CC);
    if (addressesCC != null) {
      for (Address address : addressesCC) {
        InternetAddress internetAddress = (InternetAddress) address;
        pairs.add(new EmailAndNamePair(internetAddress.getAddress(), internetAddress.getPersonal()));
      }
    }

    return pairs;
  }

  /**
   * The incoming handler realization. This handler will be used to communicate with current
   * service and other Android components.
   */
  private static class IncomingHandler extends Handler {
    private final WeakReference<EmailSyncManager> gmailSynsManagerWeakReference;
    private final WeakReference<Map<String, Messenger>> replyToMessengersWeakReference;

    IncomingHandler(EmailSyncManager emailSyncManager, Map<String, Messenger> replyToMessengersWeakReference) {
      this.gmailSynsManagerWeakReference = new WeakReference<>(emailSyncManager);
      this.replyToMessengersWeakReference = new WeakReference<>(replyToMessengersWeakReference);
    }

    @Override
    public void handleMessage(Message message) {
      if (gmailSynsManagerWeakReference.get() != null) {
        EmailSyncManager emailSyncManager = gmailSynsManagerWeakReference.get();
        Action action = null;
        String ownerKey = null;
        int requestCode = -1;

        if (message.obj instanceof Action) {
          action = (Action) message.obj;
          ownerKey = action.getOwnerKey();
          requestCode = action.getRequestCode();
        }

        switch (message.what) {
          case MESSAGE_ADD_REPLY_MESSENGER:
            Map<String, Messenger> replyToMessengersForAdd = replyToMessengersWeakReference.get();

            if (replyToMessengersForAdd != null && action != null) {
              replyToMessengersForAdd.put(ownerKey, message.replyTo);
            }
            break;

          case MESSAGE_REMOVE_REPLY_MESSENGER:
            Map<String, Messenger> replyToMessengersForRemove = replyToMessengersWeakReference.get();

            if (replyToMessengersForRemove != null && action != null) {
              replyToMessengersForRemove.remove(ownerKey);
            }
            break;

          case MESSAGE_UPDATE_LABELS:
            if (emailSyncManager != null && action != null) {
              emailSyncManager.updateLabels(ownerKey, requestCode, message.arg1 == 1);
            }
            break;

          case MESSAGE_LOAD_MESSAGES:
            if (emailSyncManager != null && action != null) {
              LocalFolder localFolder = (LocalFolder) action.getObject();
              emailSyncManager.loadMessages(ownerKey, requestCode, localFolder, message.arg1, message.arg2);
            }
            break;

          case MESSAGE_LOAD_NEXT_MESSAGES:
            if (emailSyncManager != null && action != null) {
              LocalFolder localFolder = (LocalFolder) action.getObject();
              emailSyncManager.loadNextMessages(ownerKey, requestCode, localFolder, message.arg1);
            }
            break;

          case MESSAGE_REFRESH_MESSAGES:
            if (emailSyncManager != null && action != null) {
              LocalFolder refreshLocalFolder = (LocalFolder) action.getObject();
              emailSyncManager.refreshMessages(ownerKey, requestCode, refreshLocalFolder, true);
            }
            break;

          case MESSAGE_LOAD_MESSAGE_DETAILS:
            if (emailSyncManager != null && action != null) {
              LocalFolder localFolder = (LocalFolder) action.getObject();
              emailSyncManager.loadMessageDetails(ownerKey, requestCode, localFolder, message.arg1);
            }
            break;

          case MESSAGE_MOVE_MESSAGE:
            if (emailSyncManager != null && action != null) {
              LocalFolder[] localFolders = (LocalFolder[]) action.getObject();

              String emailDomain = emailSyncManager.getAccountDao().getAccountType();

              if (localFolders == null || localFolders.length != 2) {
                throw new IllegalArgumentException(emailDomain + "| Cannot move the message. Folders " +
                    "are null.");
              }

              if (localFolders[0] == null) {
                throw new IllegalArgumentException(emailDomain + "| Cannot move the message. The " +
                    "source folder is null.");
              }

              if (localFolders[1] == null) {
                throw new IllegalArgumentException(emailDomain + "| Cannot move the message. The " +
                    "destination folder is null.");
              }

              emailSyncManager.moveMessage(ownerKey, requestCode, localFolders[0], localFolders[1], message.arg1);
            }
            break;

          case MESSAGE_LOAD_PRIVATE_KEYS:
            if (emailSyncManager != null && action != null) {
              emailSyncManager.loadPrivateKeys(ownerKey, requestCode);
            }
            break;

          case MESSAGE_SEND_MESSAGE_WITH_BACKUP:
            if (emailSyncManager != null && action != null) {
              emailSyncManager.sendMessageWithBackup(ownerKey, requestCode);
            }
            break;

          case MESSAGE_SEARCH_MESSAGES:
            if (emailSyncManager != null && action != null) {
              LocalFolder localFolderWhereWeDoSearch = (LocalFolder) action.getObject();
              emailSyncManager.searchMessages(ownerKey, requestCode, localFolderWhereWeDoSearch, message.arg1);
            }
            break;

          case MESSAGE_CANCEL_ALL_TASKS:
            if (emailSyncManager != null && action != null) {
              emailSyncManager.cancelAllSyncTasks();
            }
            break;

          default:
            super.handleMessage(message);
        }
      }
    }
  }
}
