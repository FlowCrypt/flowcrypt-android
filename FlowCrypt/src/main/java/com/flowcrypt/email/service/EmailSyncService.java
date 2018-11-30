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
import com.flowcrypt.email.api.email.model.AttachmentInfo;
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
  private MessagesNotificationManager messagesNotificationManager;

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

    this.messagesNotificationManager = new MessagesNotificationManager(this);

    emailSyncManager = new EmailSyncManager(new AccountDaoSource().getActiveAccountInformation(this));
    emailSyncManager.setSyncListener(this);

    messenger = new Messenger(new IncomingHandler(this, emailSyncManager, replyToMessengers));

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
  public void onMessageWithBackupToKeyOwnerSent(AccountDao account, String ownerKey, int requestCode,
                                                boolean isSent) {
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
  public void onPrivateKeyFound(AccountDao account, List<String> keys, String ownerKey, int requestCode) {
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
  public void onMessagesMoved(AccountDao account, IMAPFolder sourceImapFolder, IMAPFolder destinationImapFolder,
                              javax.mail.Message[] messages, String ownerKey, int requestCode) {
    //Todo-denbond7 Not implemented yet.
  }

  @Override
  public void onMessageMoved(AccountDao account, IMAPFolder sourceImapFolder, IMAPFolder destinationImapFolder,
                             javax.mail.Message message, String ownerKey, int requestCode) {
    try {
      if (message != null) {
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
  public void onMessageDetailsReceived(AccountDao account, com.flowcrypt.email.api.email.Folder localFolder,
                                       IMAPFolder imapFolder, long uid, javax.mail.Message message, String
                                           rawMessageWithOutAttachments,
                                       String ownerKey, int requestCode) {
    try {
      MessageDaoSource messageDaoSource = new MessageDaoSource();

      messageDaoSource.updateMessageRawText(getApplicationContext(),
          account.getEmail(),
          localFolder.getFolderAlias(),
          uid,
          rawMessageWithOutAttachments);

      if (TextUtils.isEmpty(rawMessageWithOutAttachments)) {
        sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_ERROR_MESSAGE_NOT_FOUND);
      } else {
        updateAttachmentTable(account, localFolder, imapFolder, message);
        sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_OK);
      }
    } catch (RemoteException | MessagingException | IOException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
      onError(account, SyncErrorTypes.UNKNOWN_ERROR, e, ownerKey, requestCode);
    }
  }

  @Override
  public void onMessagesReceived(AccountDao account, com.flowcrypt.email.api.email.Folder localFolder,
                                 IMAPFolder remoteFolder, javax.mail.Message[] messages,
                                 String ownerKey, int requestCode) {
    Log.d(TAG, "onMessagesReceived: imapFolder = " + remoteFolder.getFullName() + " message " +
        "count: " + messages.length);
    try {
      boolean isShowOnlyEncryptedMessages =
          new AccountDaoSource().isShowOnlyEncryptedMessages(getApplicationContext(), account.getEmail());

      MessageDaoSource messageDaoSource = new MessageDaoSource();
      messageDaoSource.addRows(getApplicationContext(),
          account.getEmail(),
          localFolder.getFolderAlias(),
          remoteFolder,
          messages, false, isShowOnlyEncryptedMessages);

      if (!isShowOnlyEncryptedMessages) {
        emailSyncManager.identifyEncryptedMessages(ownerKey, R.id.syns_identify_encrypted_messages,
            localFolder);
      }

      if (messages.length > 0) {
        sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_NEED_UPDATE, localFolder);
      } else {
        sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_OK, localFolder);
      }

      updateLocalContactsIfMessagesFromSentFolder(remoteFolder, messages);
    } catch (MessagingException | RemoteException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
      onError(account, SyncErrorTypes.UNKNOWN_ERROR, e, ownerKey, requestCode);
    }
  }

  @Override
  public void onNewMessagesReceived(AccountDao account, com.flowcrypt.email.api.email.Folder localFolder,
                                    IMAPFolder remoteFolder, javax.mail.Message[] newMessages,
                                    LongSparseArray<Boolean> isMessageEncryptedInfo, String ownerKey, int
                                        requestCode) {
    Log.d(TAG, "onMessagesReceived:message count: " + newMessages.length);
    try {
      MessageDaoSource messageDaoSource = new MessageDaoSource();

      messageDaoSource.addRows(getApplicationContext(),
          account.getEmail(),
          localFolder.getFolderAlias(),
          remoteFolder,
          newMessages,
          isMessageEncryptedInfo,
          !GeneralUtil.isAppForegrounded() && FoldersManager.getFolderTypeForImapFolder(localFolder) ==
              FoldersManager.FolderType.INBOX, false);

      if (newMessages.length > 0) {
        sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_NEED_UPDATE);
      } else {
        sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_OK);
      }

      if (!GeneralUtil.isAppForegrounded()) {
        String folderAlias = localFolder.getFolderAlias();

        messagesNotificationManager.notify(this, account, localFolder,
            messageDaoSource.getNewMessages(getApplicationContext(), account.getEmail(), folderAlias),
            messageDaoSource.getUIDOfUnseenMessages(this, account.getEmail(), folderAlias), false);
      }
    } catch (MessagingException | RemoteException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
      onError(account, SyncErrorTypes.UNKNOWN_ERROR, e, ownerKey, requestCode);
    }
  }

  @Override
  public void onSearchMessagesReceived(AccountDao account, com.flowcrypt.email.api.email.Folder localFolder,
                                       IMAPFolder imapFolder, javax.mail.Message[] messages,
                                       String ownerKey, int requestCode) {
    Log.d(TAG, "onSearchMessagesReceived: message count: " + messages.length);
    try {
      boolean isShowOnlyEncryptedMessages =
          new AccountDaoSource().isShowOnlyEncryptedMessages(getApplicationContext(), account.getEmail());

      MessageDaoSource messageDaoSource = new MessageDaoSource();
      messageDaoSource.addRows(getApplicationContext(),
          account.getEmail(),
          SearchMessagesActivity.SEARCH_FOLDER_NAME,
          imapFolder,
          messages, false, isShowOnlyEncryptedMessages);

      if (!isShowOnlyEncryptedMessages) {
        emailSyncManager.identifyEncryptedMessages(ownerKey, R.id.syns_identify_encrypted_messages,
            localFolder);
      }

      if (messages.length > 0) {
        sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_NEED_UPDATE);
      } else {
        sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_OK);
      }

      updateLocalContactsIfMessagesFromSentFolder(imapFolder, messages);
    } catch (MessagingException | RemoteException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
      onError(account, SyncErrorTypes.UNKNOWN_ERROR, e, ownerKey, requestCode);
    }
  }

  @Override
  public void onRefreshMessagesReceived(AccountDao account, com.flowcrypt.email.api.email.Folder localFolder,
                                        IMAPFolder remoteFolder, javax.mail.Message[] newMessages,
                                        javax.mail.Message[] updateMessages,
                                        String key, int requestCode) {
    Log.d(TAG, "onRefreshMessagesReceived: imapFolder = " + remoteFolder.getFullName() + " newMessages " +
        "count: " + newMessages.length + ", updateMessages count = " + updateMessages.length);

    try {
      MessageDaoSource messageDaoSource = new MessageDaoSource();

      Map<Long, String> messagesUIDWithFlagsInLocalDatabase = messageDaoSource.getMapOfUIDAndMessagesFlags
          (getApplicationContext(), account.getEmail(), localFolder.getFolderAlias());

      Collection<Long> messagesUIDsInLocalDatabase = new HashSet<>(messagesUIDWithFlagsInLocalDatabase.keySet());

      Collection<Long> deleteCandidatesUIDList = EmailUtil.generateDeleteCandidates(messagesUIDsInLocalDatabase,
          remoteFolder, updateMessages);

      messageDaoSource.deleteMessagesByUID(getApplicationContext(),
          account.getEmail(), localFolder.getFolderAlias(), deleteCandidatesUIDList);

      if (!GeneralUtil.isAppForegrounded() &&
          FoldersManager.getFolderTypeForImapFolder(localFolder) == FoldersManager.FolderType.INBOX) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          for (long uid : deleteCandidatesUIDList) {
            messagesNotificationManager.cancel(this, (int) uid);
          }
        } else {
          String folderAlias = localFolder.getFolderAlias();

          messagesNotificationManager.notify(this, account, localFolder,
              messageDaoSource.getNewMessages(getApplicationContext(), account.getEmail(), folderAlias)
              , messageDaoSource.getUIDOfUnseenMessages(this, account.getEmail(), folderAlias), false);
        }
      }

      javax.mail.Message[] messagesNewCandidates = EmailUtil.generateNewCandidates(messagesUIDsInLocalDatabase,
          remoteFolder, newMessages);

      boolean isShowOnlyEncryptedMessages =
          new AccountDaoSource().isShowOnlyEncryptedMessages(getApplicationContext(), account.getEmail());

      messageDaoSource.addRows(getApplicationContext(),
          account.getEmail(),
          localFolder.getFolderAlias(),
          remoteFolder,
          messagesNewCandidates,
          !GeneralUtil.isAppForegrounded() && FoldersManager.getFolderTypeForImapFolder(localFolder) ==
              FoldersManager.FolderType.INBOX, isShowOnlyEncryptedMessages);

      if (!isShowOnlyEncryptedMessages) {
        emailSyncManager.identifyEncryptedMessages(key, R.id.syns_identify_encrypted_messages, localFolder);
      }

      messageDaoSource.updateMessagesByUID(getApplicationContext(),
          account.getEmail(),
          localFolder.getFolderAlias(),
          remoteFolder,
          EmailUtil.generateUpdateCandidates(messagesUIDWithFlagsInLocalDatabase, remoteFolder,
              updateMessages));

      if (newMessages.length > 0 || updateMessages.length > 0) {
        sendReply(key, requestCode, REPLY_RESULT_CODE_NEED_UPDATE);
      } else {
        sendReply(key, requestCode, REPLY_RESULT_CODE_ACTION_OK);
      }

      updateLocalContactsIfMessagesFromSentFolder(remoteFolder, messagesNewCandidates);
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

    foldersManager.addFolder(new com.flowcrypt.email.api.email.Folder(JavaEmailConstants.FOLDER_OUTBOX,
        JavaEmailConstants.FOLDER_OUTBOX, 0,
        new String[]{JavaEmailConstants.FOLDER_FLAG_HAS_NO_CHILDREN}, false));

    ImapLabelsDaoSource imapLabelsDaoSource = new ImapLabelsDaoSource();
    List<com.flowcrypt.email.api.email.Folder> currentFoldersList =
        imapLabelsDaoSource.getFolders(getApplicationContext(), account.getEmail());
    if (currentFoldersList.isEmpty()) {
      imapLabelsDaoSource.addRows(getApplicationContext(), account.getEmail(), foldersManager.getAllFolders());
    } else {
      try {
        imapLabelsDaoSource.updateLabels(getApplicationContext(), account.getEmail(), currentFoldersList,
            foldersManager.getAllFolders());
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
    Log.d(TAG, "onActionProgress: account" + account + "| ownerKey =" + ownerKey + "| requestCode =" +
        requestCode);
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
  public void onMessageChanged(AccountDao account, com.flowcrypt.email.api.email.Folder localFolder, IMAPFolder
      remoteFolder, javax.mail.Message message, String ownerKey, int requestCode) {
    if (!GeneralUtil.isAppForegrounded() &&
        FoldersManager.getFolderTypeForImapFolder(localFolder) == FoldersManager.FolderType.INBOX) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        try {
          if (message.getFlags().contains(Flags.Flag.SEEN)) {
            messagesNotificationManager.cancel(this, (int) remoteFolder.getUID(message));
          }
        } catch (MessagingException e) {
          e.printStackTrace();
        }
      } else {
        String folderAlias = localFolder.getFolderAlias();
        MessageDaoSource messageDaoSource = new MessageDaoSource();

        messagesNotificationManager.notify(this, account, localFolder,
            messageDaoSource.getNewMessages(getApplicationContext(), account.getEmail(), folderAlias),
            messageDaoSource.getUIDOfUnseenMessages(this, account.getEmail(), folderAlias), true);
      }
    }
  }

  @Override
  public void onIdentificationToEncryptionCompleted(AccountDao account, com.flowcrypt.email.api.email.Folder
      localFolder, IMAPFolder remoteFolder, String ownerKey, int requestCode) {
    if (FoldersManager.getFolderTypeForImapFolder(localFolder) == FoldersManager.FolderType.INBOX
        && !GeneralUtil.isAppForegrounded()) {
      String folderAlias = localFolder.getFolderAlias();
      MessageDaoSource messageDaoSource = new MessageDaoSource();

      messagesNotificationManager.notify(this, account, localFolder,
          messageDaoSource.getNewMessages(getApplicationContext(), account.getEmail(), folderAlias),
          messageDaoSource.getUIDOfUnseenMessages(this, account.getEmail(), folderAlias), false);
    }
  }

  protected void handleConnectivityAction(Context context, Intent intent) {
    if (ConnectivityManager.CONNECTIVITY_ACTION.equalsIgnoreCase(intent.getAction())) {
      ConnectivityManager connectivityManager =
          (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

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
   * @param account The object which contains information about an email account.
   * @param folder     The local reflection of the remote folder.
   * @param imapFolder The folder where the new messages exist.
   * @param message    The new messages.
   * @throws MessagingException This exception meybe happen when we try to call {@code
   *                            {@link IMAPFolder#getUID(javax.mail.Message)}}
   */
  private void updateAttachmentTable(AccountDao account, com.flowcrypt.email.api.email.Folder folder,
                                     IMAPFolder imapFolder, javax.mail.Message message)
      throws MessagingException, IOException {
    AttachmentDaoSource attachmentDaoSource = new AttachmentDaoSource();
    ArrayList<ContentValues> contentValuesList = new ArrayList<>();

    ArrayList<AttachmentInfo> attachmentInfoList = getAttachmentsInfoFromPart(imapFolder, message
        .getMessageNumber(), message);

    if (!attachmentInfoList.isEmpty()) {
      for (AttachmentInfo attachmentInfo : attachmentInfoList) {
        contentValuesList.add(AttachmentDaoSource.prepareContentValues(account.getEmail(),
            folder.getFolderAlias(), imapFolder.getUID(message), attachmentInfo));
      }
    }

    attachmentDaoSource.addRows(getApplicationContext(), contentValuesList.toArray(new ContentValues[0]));
  }

  /**
   * Find attachments in the {@link Part}.
   *
   * @param imapFolder    The {@link IMAPFolder} which contains the parent message;
   * @param messageNumber This number will be used for fetching {@link Part} details;
   * @param part          The parent part.
   * @return The list of created {@link AttachmentInfo}
   * @throws MessagingException
   * @throws IOException
   */
  @NonNull
  private ArrayList<AttachmentInfo> getAttachmentsInfoFromPart(IMAPFolder imapFolder, int messageNumber, Part part)
      throws MessagingException, IOException {
    ArrayList<AttachmentInfo> attachmentInfoList = new ArrayList<>();

    if (part.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
      Multipart multiPart = (Multipart) part.getContent();
      int numberOfParts = multiPart.getCount();
      String[] headers;
      for (int partCount = 0; partCount < numberOfParts; partCount++) {
        BodyPart bodyPart = multiPart.getBodyPart(partCount);
        if (bodyPart.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
          ArrayList<AttachmentInfo> attachmentInfoLists = getAttachmentsInfoFromPart(imapFolder,
              messageNumber, bodyPart);
          if (!attachmentInfoLists.isEmpty()) {
            attachmentInfoList.addAll(attachmentInfoLists);
          }
        } else if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
          InputStream inputStream = ImapProtocolUtil.getHeaderStream(imapFolder,
              messageNumber, partCount + 1);

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
              attachmentInfoList.add(attachmentInfo);
            }
          }
        }
      }
    }

    return attachmentInfoList;
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
  private void updateLocalContactsIfMessagesFromSentFolder(IMAPFolder imapFolder, javax.mail.Message[] messages) {
    try {
      boolean isSentFolder = Arrays.asList(imapFolder.getAttributes()).contains("\\Sent");

      if (isSentFolder) {
        ArrayList<EmailAndNamePair> emailAndNamePairs = new ArrayList<>();
        for (javax.mail.Message message : messages) {
          emailAndNamePairs.addAll(getEmailAndNamePairsFromMessage(message));
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
   * @param message The input {@link javax.mail.Message}.
   * @return <tt>{@link List}</tt> of EmailAndNamePair objects, which contains information
   * about
   * emails and names.
   * @throws MessagingException when retrieve information about recipients.
   */
  private List<EmailAndNamePair> getEmailAndNamePairsFromMessage(javax.mail.Message message) throws
      MessagingException {
    List<EmailAndNamePair> emailAndNamePairs = new ArrayList<>();

    Address[] addressesTo = message.getRecipients(javax.mail.Message.RecipientType.TO);
    if (addressesTo != null) {
      for (Address address : addressesTo) {
        InternetAddress internetAddress = (InternetAddress) address;
        emailAndNamePairs.add(new EmailAndNamePair(
            internetAddress.getAddress(),
            internetAddress.getPersonal()));
      }
    }

    Address[] addressesCC = message.getRecipients(javax.mail.Message.RecipientType.CC);
    if (addressesCC != null) {
      for (Address address : addressesCC) {
        InternetAddress internetAddress = (InternetAddress) address;
        emailAndNamePairs.add(new EmailAndNamePair(
            internetAddress.getAddress(),
            internetAddress.getPersonal()));
      }
    }

    return emailAndNamePairs;
  }

  /**
   * The incoming handler realization. This handler will be used to communicate with current
   * service and other Android components.
   */
  private static class IncomingHandler extends Handler {
    private final WeakReference<EmailSyncManager> gmailSynsManagerWeakReference;
    private final WeakReference<EmailSyncService> syncServiceWeakReference;
    private final WeakReference<Map<String, Messenger>> replyToMessengersWeakReference;

    IncomingHandler(EmailSyncService emailSyncService, EmailSyncManager emailSyncManager,
                    Map<String, Messenger> replyToMessengersWeakReference) {
      this.syncServiceWeakReference = new WeakReference<>(emailSyncService);
      this.gmailSynsManagerWeakReference = new WeakReference<>(emailSyncManager);
      this.replyToMessengersWeakReference = new WeakReference<>(replyToMessengersWeakReference);
    }

    @Override
    public void handleMessage(Message message) {
      if (gmailSynsManagerWeakReference.get() != null) {
        EmailSyncManager emailSyncManager = gmailSynsManagerWeakReference.get();
        Action action = null;

        if (message.obj instanceof Action) {
          action = (Action) message.obj;
        }

        switch (message.what) {
          case MESSAGE_ADD_REPLY_MESSENGER:
            Map<String, Messenger> replyToMessengersForAdd = replyToMessengersWeakReference.get();

            if (replyToMessengersForAdd != null && action != null) {
              replyToMessengersForAdd.put(action.getOwnerKey(), message.replyTo);
            }
            break;

          case MESSAGE_REMOVE_REPLY_MESSENGER:
            Map<String, Messenger> replyToMessengersForRemove = replyToMessengersWeakReference.get();

            if (replyToMessengersForRemove != null && action != null) {
              replyToMessengersForRemove.remove(action.getOwnerKey());
            }
            break;

          case MESSAGE_UPDATE_LABELS:
            if (emailSyncManager != null && action != null) {
              emailSyncManager.updateLabels(action.getOwnerKey(), action.getRequestCode(),
                  message.arg1 == 1);
            }
            break;

          case MESSAGE_LOAD_MESSAGES:
            if (emailSyncManager != null && action != null) {
              com.flowcrypt.email.api.email.Folder folder =
                  (com.flowcrypt.email.api.email.Folder) action.getObject();
              emailSyncManager.loadMessages(action.getOwnerKey(), action.getRequestCode(),
                  folder, message.arg1, message.arg2);
            }
            break;

          case MESSAGE_LOAD_NEXT_MESSAGES:
            if (emailSyncManager != null && action != null) {
              com.flowcrypt.email.api.email.Folder folder =
                  (com.flowcrypt.email.api.email.Folder) action.getObject();

              emailSyncManager.loadNextMessages(action.getOwnerKey(), action.getRequestCode(),
                  folder, message.arg1);
            }
            break;

          case MESSAGE_REFRESH_MESSAGES:
            if (emailSyncManager != null && action != null) {
              com.flowcrypt.email.api.email.Folder refreshFolder =
                  (com.flowcrypt.email.api.email.Folder) action.getObject();

              emailSyncManager.refreshMessages(action.getOwnerKey(),
                  action.getRequestCode(), refreshFolder, true);
            }
            break;

          case MESSAGE_LOAD_MESSAGE_DETAILS:
            if (emailSyncManager != null && action != null) {
              com.flowcrypt.email.api.email.Folder localFolder =
                  (com.flowcrypt.email.api.email.Folder) action.getObject();

              emailSyncManager.loadMessageDetails(action.getOwnerKey(),
                  action.getRequestCode(), localFolder, message.arg1);
            }
            break;

          case MESSAGE_MOVE_MESSAGE:
            if (emailSyncManager != null && action != null) {
              com.flowcrypt.email.api.email.Folder[] folders = (com.flowcrypt.email
                  .api.email.Folder[]) action.getObject();

              String emailDomain = emailSyncManager.getAccountDao().getAccountType();

              if (folders == null || folders.length != 2) {
                throw new IllegalArgumentException(emailDomain + "| Cannot move the message. Folders " +
                    "are null.");
              }

              if (folders[0] == null) {
                throw new IllegalArgumentException(emailDomain + "| Cannot move the message. The " +
                    "source folder is null.");
              }

              if (folders[1] == null) {
                throw new IllegalArgumentException(emailDomain + "| Cannot move the message. The " +
                    "destination folder is null.");
              }

              emailSyncManager.moveMessage(action.getOwnerKey(), action.getRequestCode(),
                  folders[0], folders[1], message.arg1);
            }
            break;

          case MESSAGE_LOAD_PRIVATE_KEYS:
            if (emailSyncManager != null && action != null) {
              emailSyncManager.loadPrivateKeys(action.getOwnerKey(), action.getRequestCode());
            }
            break;

          case MESSAGE_SEND_MESSAGE_WITH_BACKUP:
            if (emailSyncManager != null && action != null) {
              emailSyncManager.sendMessageWithBackup(action.getOwnerKey(), action.getRequestCode());
            }
            break;

          case MESSAGE_SEARCH_MESSAGES:
            if (emailSyncManager != null && action != null) {
              com.flowcrypt.email.api.email.Folder folderWhereWeDoSearch =
                  (com.flowcrypt.email.api.email.Folder) action.getObject();

              emailSyncManager.searchMessages(action.getOwnerKey(),
                  action.getRequestCode(), folderWhereWeDoSearch, message.arg1);
            }
            break;

          case MESSAGE_CANCEL_ALL_TASKS:
            if (emailSyncManager != null && action != null) {
              emailSyncManager.cancelAllSyncTask();
            }
            break;

          default:
            super.handleMessage(message);
        }
      }
    }
  }
}
