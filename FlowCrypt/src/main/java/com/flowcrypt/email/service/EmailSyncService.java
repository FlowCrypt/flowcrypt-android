/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.flowcrypt.email.api.email.FoldersManager;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo;
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
import javax.mail.Folder;
import javax.mail.FolderClosedException;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.StoreClosedException;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.InternetHeaders;

/**
 * This the email synchronization service. This class is responsible for the logic of
 * synchronization work. Using this service we can asynchronous download information and send
 * data to the IMAP server.
 *
 * @author DenBond7
 *         Date: 14.06.2017
 *         Time: 12:18
 *         E-mail: DenBond7@gmail.com
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
    public static final int MESSAGE_SEND_MESSAGE = 9;
    public static final int MESSAGE_LOAD_PRIVATE_KEYS = 10;
    public static final int MESSAGE_GET_ACTIVE_ACCOUNT = 11;
    public static final int MESSAGE_SEND_MESSAGE_WITH_BACKUP = 12;
    public static final int MESSAGE_SEARCH_MESSAGES = 13;

    private static final String TAG = EmailSyncService.class.getSimpleName();
    /**
     * This {@link Messenger} is responsible for the receive intents from other client and
     * handles them.
     */
    private Messenger messenger;

    private Map<String, Messenger> replyToMessengers;

    private EmailSyncManager emailSyncManager;

    private boolean isServiceStarted;

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
        emailSyncManager = new EmailSyncManager(new AccountDaoSource().getActiveAccountInformation(this));
        emailSyncManager.setSyncListener(this);

        messenger = new Messenger(new IncomingHandler(this, emailSyncManager, replyToMessengers));
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
    public void onMessageWithBackupToKeyOwnerSent(AccountDao accountDao, String ownerKey, int requestCode,
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
            onError(accountDao, SyncErrorTypes.UNKNOWN_ERROR, e, ownerKey, requestCode);
        }
    }

    @Override
    public void onPrivateKeyFound(AccountDao accountDao, List<String> keys, String ownerKey, int requestCode) {
        try {
            sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_OK, keys);
        } catch (RemoteException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
        }
    }

    @Override
    public void onMessageSent(AccountDao accountDao, String ownerKey, int requestCode, boolean isSent) {
        try {
            if (isSent) {
                sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_OK);
            } else {
                sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_ERROR_MESSAGE_WAS_NOT_SENT);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
            onError(accountDao, SyncErrorTypes.UNKNOWN_ERROR, e, ownerKey, requestCode);
        }
    }

    @Override
    public void onMessagesMoved(AccountDao accountDao, IMAPFolder sourceImapFolder, IMAPFolder destinationImapFolder,
                                javax.mail.Message[] messages, String ownerKey, int requestCode) {
        //Todo-denbond7 Not implemented yet.
    }

    @Override
    public void onMessageMoved(AccountDao accountDao, IMAPFolder sourceImapFolder, IMAPFolder destinationImapFolder,
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
            onError(accountDao, SyncErrorTypes.UNKNOWN_ERROR, e, ownerKey, requestCode);
        }
    }

    @Override
    public void onMessageDetailsReceived(AccountDao accountDao, com.flowcrypt.email.api.email.Folder localFolder,
                                         IMAPFolder imapFolder, long uid, javax.mail.Message message, String
                                                 rawMessageWithOutAttachments,
                                         String ownerKey, int requestCode) {
        try {
            MessageDaoSource messageDaoSource = new MessageDaoSource();

            messageDaoSource.updateMessageRawText(getApplicationContext(),
                    accountDao.getEmail(),
                    localFolder.getFolderAlias(),
                    uid,
                    rawMessageWithOutAttachments);

            if (TextUtils.isEmpty(rawMessageWithOutAttachments)) {
                sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_ERROR_MESSAGE_NOT_FOUND);
            } else {
                sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_OK);
                updateAttachmentTable(accountDao, localFolder, imapFolder, message);
            }
        } catch (RemoteException | MessagingException | IOException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
            onError(accountDao, SyncErrorTypes.UNKNOWN_ERROR, e, ownerKey, requestCode);
        }
    }

    @Override
    public void onMessagesReceived(AccountDao accountDao, com.flowcrypt.email.api.email.Folder localFolder,
                                   IMAPFolder imapFolder, javax.mail.Message[] messages,
                                   String ownerKey, int requestCode) {
        Log.d(TAG, "onMessagesReceived: imapFolder = " + imapFolder.getFullName() + " message " +
                "count: " + messages.length);
        try {
            MessageDaoSource messageDaoSource = new MessageDaoSource();
            messageDaoSource.addRows(getApplicationContext(),
                    accountDao.getEmail(),
                    localFolder.getFolderAlias(),
                    imapFolder,
                    messages);

            if (messages.length > 0) {
                sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_NEED_UPDATE);
            } else {
                sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_OK);
            }

            updateLocalContactsIfMessagesFromSentFolder(imapFolder, messages);
        } catch (MessagingException | RemoteException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
            onError(accountDao, SyncErrorTypes.UNKNOWN_ERROR, e, ownerKey, requestCode);
        }
    }

    @Override
    public void onSearchMessagesReceived(AccountDao accountDao, com.flowcrypt.email.api.email.Folder folder,
                                         IMAPFolder imapFolder, javax.mail.Message[] messages,
                                         String ownerKey, int requestCode) {
        Log.d(TAG, "onSearchMessagesReceived: message count: " + messages.length);
        try {
            MessageDaoSource messageDaoSource = new MessageDaoSource();
            messageDaoSource.addRows(getApplicationContext(),
                    accountDao.getEmail(),
                    SearchMessagesActivity.SEARCH_FOLDER_NAME,
                    imapFolder,
                    messages);

            if (messages.length > 0) {
                sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_NEED_UPDATE);
            } else {
                sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_OK);
            }

            updateLocalContactsIfMessagesFromSentFolder(imapFolder, messages);
        } catch (MessagingException | RemoteException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
            onError(accountDao, SyncErrorTypes.UNKNOWN_ERROR, e, ownerKey, requestCode);
        }
    }

    @Override
    public void onRefreshMessagesReceived(AccountDao accountDao, IMAPFolder imapFolder,
                                          javax.mail.Message[] newMessages,
                                          javax.mail.Message[] updateMessages,
                                          String key, int requestCode) {
        Log.d(TAG, "onRefreshMessagesReceived: imapFolder = " + imapFolder.getFullName() + " newMessages " +
                "count: " + newMessages.length + ", updateMessages count = " + updateMessages.length);

        try {
            com.flowcrypt.email.api.email.Folder folder =
                    FoldersManager.generateFolder(imapFolder, imapFolder.getName());
            MessageDaoSource messageDaoSource = new MessageDaoSource();

            Map<Long, String> messagesUIDWithFlagsInLocalDatabase = messageDaoSource.getMapOfUIDAndMessagesFlags
                    (getApplicationContext(), accountDao.getEmail(), folder.getFolderAlias());

            Collection<Long> messagesUIDsInLocalDatabase = new HashSet<>(messagesUIDWithFlagsInLocalDatabase.keySet());

            messageDaoSource.deleteMessagesByUID(getApplicationContext(),
                    accountDao.getEmail(),
                    folder.getFolderAlias(),
                    generateDeleteCandidates(messagesUIDsInLocalDatabase, imapFolder, updateMessages));

            javax.mail.Message[] messagesNewCandidates = generateNewCandidates(messagesUIDsInLocalDatabase,
                    imapFolder, newMessages);

            messageDaoSource.addRows(getApplicationContext(),
                    accountDao.getEmail(),
                    folder.getFolderAlias(),
                    imapFolder,
                    messagesNewCandidates);

            messageDaoSource.updateMessagesByUID(getApplicationContext(),
                    accountDao.getEmail(),
                    folder.getFolderAlias(),
                    imapFolder,
                    generateUpdateCandidates(messagesUIDWithFlagsInLocalDatabase, imapFolder, updateMessages));

            if (newMessages.length > 0 || updateMessages.length > 0) {
                sendReply(key, requestCode, REPLY_RESULT_CODE_NEED_UPDATE);
            } else {
                sendReply(key, requestCode, REPLY_RESULT_CODE_ACTION_OK);
            }

            updateLocalContactsIfMessagesFromSentFolder(imapFolder, messagesNewCandidates);
        } catch (RemoteException | MessagingException | IOException | OperationApplicationException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
            if (e instanceof StoreClosedException || e instanceof FolderClosedException) {
                onError(accountDao, SyncErrorTypes.ACTION_FAILED_SHOW_TOAST, e, key, requestCode);
            }
        }
    }

    @Override
    public void onFolderInfoReceived(AccountDao accountDao, Folder[] folders, String key, int requestCode) {
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

        ImapLabelsDaoSource imapLabelsDaoSource = new ImapLabelsDaoSource();
        List<com.flowcrypt.email.api.email.Folder> currentFoldersList =
                imapLabelsDaoSource.getFolders(getApplicationContext(), accountDao.getEmail());
        if (currentFoldersList.isEmpty()) {
            imapLabelsDaoSource.addRows(getApplicationContext(), accountDao.getEmail(), foldersManager.getAllFolders());
        } else {
            try {
                imapLabelsDaoSource.updateLabels(getApplicationContext(), accountDao.getEmail(), currentFoldersList,
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
    public void onError(AccountDao accountDao, int errorType, Exception e, String key, int requestCode) {
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
    public void onActionProgress(AccountDao accountDao, String ownerKey, int requestCode, int resultCode) {
        Log.d(TAG, "onActionProgress: accountDao" + accountDao + "| ownerKey =" + ownerKey + "| requestCode =" +
                requestCode);
        try {
            if (replyToMessengers.containsKey(ownerKey)) {
                Messenger messenger = replyToMessengers.get(ownerKey);
                messenger.send(Message.obtain(null, REPLY_ACTION_PROGRESS, requestCode, resultCode, accountDao));
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
        }
    }

    /**
     * Generate an array of the messages which will be updated.
     *
     * @param messagesUIDWithFlagsInLocalDatabase The map of UID and flags of the local messages.
     * @param imapFolder                          The remote {@link IMAPFolder}.
     * @param messages                            The array of incoming messages.
     * @return
     */
    private javax.mail.Message[] generateUpdateCandidates(
            Map<Long, String> messagesUIDWithFlagsInLocalDatabase,
            IMAPFolder imapFolder, javax.mail.Message[] messages) {
        Collection<javax.mail.Message> updateCandidates = new ArrayList<>();
        try {
            for (javax.mail.Message message : messages) {
                String flags = messagesUIDWithFlagsInLocalDatabase.get(imapFolder.getUID(message));
                if (flags == null) {
                    flags = "";
                }

                if (!flags.equalsIgnoreCase(message.getFlags().toString())) {
                    updateCandidates.add(message);
                }
            }
        } catch (MessagingException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
        }
        return updateCandidates.toArray(new javax.mail.Message[0]);
    }

    /**
     * Generate an array of {@link javax.mail.Message} which contains candidates for insert.
     *
     * @param messagesUIDInLocalDatabase The list of UID of the local messages.
     * @param imapFolder                 The remote {@link IMAPFolder}.
     * @param messages                   The array of incoming messages.
     * @return The generated array.
     */
    private javax.mail.Message[] generateNewCandidates(Collection<Long> messagesUIDInLocalDatabase,
                                                       IMAPFolder imapFolder, javax.mail.Message[] messages) {
        List<javax.mail.Message> newCandidates = new ArrayList<>();
        try {
            for (javax.mail.Message message : messages) {
                if (!messagesUIDInLocalDatabase.contains(imapFolder.getUID(message))) {
                    newCandidates.add(message);
                }
            }
        } catch (MessagingException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
        }
        return newCandidates.toArray(new javax.mail.Message[0]);
    }

    /**
     * Generated a list of UID of the local messages which will be removed.
     *
     * @param messagesUIDInLocalDatabase The list of UID of the local messages.
     * @param imapFolder                 The remote {@link IMAPFolder}.
     * @param messages                   The array of incoming messages.
     * @return A list of UID of the local messages which will be removed.
     */
    private Collection<Long> generateDeleteCandidates(Collection<Long> messagesUIDInLocalDatabase,
                                                      IMAPFolder imapFolder, javax.mail.Message[] messages) {
        Collection<Long> uidListDeleteCandidates = new HashSet<>(messagesUIDInLocalDatabase);
        Collection<Long> uidList = new HashSet<>();
        try {
            for (javax.mail.Message message : messages) {
                uidList.add(imapFolder.getUID(message));
            }
        } catch (MessagingException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
        }

        uidListDeleteCandidates.removeAll(uidList);
        return uidListDeleteCandidates;
    }

    /**
     * @param accountDao The object which contains information about an email account.
     * @param folder     The local reflection of the remote folder.
     * @param imapFolder The folder where the new messages exist.
     * @param message    The new messages.
     * @throws MessagingException This exception meybe happen when we try to call {@code
     *                            {@link IMAPFolder#getUID(javax.mail.Message)}}
     */
    private void updateAttachmentTable(AccountDao accountDao, com.flowcrypt.email.api.email.Folder folder,
                                       IMAPFolder imapFolder, javax.mail.Message message)
            throws MessagingException, IOException {
        AttachmentDaoSource attachmentDaoSource = new AttachmentDaoSource();
        ArrayList<ContentValues> contentValuesList = new ArrayList<>();

        ArrayList<AttachmentInfo> attachmentInfoList = getAttachmentsInfoFromPart(imapFolder, message
                .getMessageNumber(), message);

        if (!attachmentInfoList.isEmpty()) {
            for (AttachmentInfo attachmentInfo : attachmentInfoList) {
                contentValuesList.add(AttachmentDaoSource.prepareContentValues(accountDao.getEmail(),
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
                            attachmentInfo.setType(new ContentType(bodyPart.getContentType()).getPrimaryType());
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

                startService(EmailAndNameUpdaterService.getStartIntent(this, emailAndNamePairs));
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
            this.replyToMessengersWeakReference = new WeakReference<>
                    (replyToMessengersWeakReference);
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
                            com.flowcrypt.email.api.email.Folder folderOfMessages =
                                    (com.flowcrypt.email.api.email.Folder) action.getObject();

                            emailSyncManager.loadNextMessages(action.getOwnerKey(),
                                    action.getRequestCode(), folderOfMessages, message.arg1);
                        }
                        break;

                    case MESSAGE_REFRESH_MESSAGES:
                        if (emailSyncManager != null && action != null) {
                            com.flowcrypt.email.api.email.Folder refreshFolder =
                                    (com.flowcrypt.email.api.email.Folder) action.getObject();

                            emailSyncManager.refreshMessages(action.getOwnerKey(),
                                    action.getRequestCode(), refreshFolder, message.arg1, message.arg2);
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

                    case MESSAGE_SEND_MESSAGE:
                        if (emailSyncManager != null && action != null) {
                            OutgoingMessageInfo outgoingMessageInfo = (OutgoingMessageInfo) action.getObject();

                            emailSyncManager.sendMessage(action.getOwnerKey(), action.getRequestCode(),
                                    outgoingMessageInfo);
                        }
                        break;

                    case MESSAGE_LOAD_PRIVATE_KEYS:
                        if (emailSyncManager != null && action != null) {
                            emailSyncManager.loadPrivateKeys(action.getOwnerKey(), action.getRequestCode());
                        }
                        break;

                    case MESSAGE_GET_ACTIVE_ACCOUNT:
                        EmailSyncService emailSyncService = syncServiceWeakReference.get();

                        if (emailSyncService != null && action != null) {
                            try {
                                emailSyncService.sendReply(action.getOwnerKey(), action.getRequestCode(),
                                        REPLY_RESULT_CODE_ACTION_OK, emailSyncManager.getAccountDao().getEmail());
                            } catch (RemoteException e) {
                                e.printStackTrace();
                                ExceptionUtil.handleError(e);
                            }
                        }
                        break;

                    case MESSAGE_SEND_MESSAGE_WITH_BACKUP:
                        if (emailSyncManager != null && action != null) {
                            String account = (String) action.getObject();

                            emailSyncManager.sendMessageWithBackup(action.getOwnerKey(), action.getRequestCode(),
                                    account);
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

                    default:
                        super.handleMessage(message);
                }
            }
        }
    }
}
