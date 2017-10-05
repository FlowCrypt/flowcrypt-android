/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
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
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.database.dao.source.imap.AttachmentDaoSource;
import com.flowcrypt.email.database.dao.source.imap.ImapLabelsDaoSource;
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource;
import com.flowcrypt.email.model.EmailAndNamePair;
import com.sun.mail.imap.IMAPFolder;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
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
public class EmailSyncService extends Service implements SyncListener {
    public static final String ACTION_SWITCH_ACCOUNT = "ACTION_SWITCH_ACCOUNT";
    public static final String ACTION_BEGIN_SYNC = "ACTION_BEGIN_SYNC";

    public static final int REPLY_RESULT_CODE_ACTION_OK = 0;
    public static final int REPLY_RESULT_CODE_ACTION_ERROR = 1;
    public static final int REPLY_RESULT_CODE_NEED_UPDATE = 2;

    public static final int REPLY_OK = 0;
    public static final int REPLY_ERROR = 1;

    public static final int MESSAGE_ADD_REPLY_MESSENGER = 1;
    public static final int MESSAGE_REMOVE_REPLY_MESSENGER = 2;
    public static final int MESSAGE_UPDATE_LABELS = 3;
    public static final int MESSAGE_LOAD_MESSAGES = 4;
    public static final int MESSAGE_LOAD_NEXT_MESSAGES = 5;
    public static final int MESSAGE_LOAD_NEW_MESSAGES_MANUALLY = 6;
    public static final int MESSAGE_LOAD_MESSAGE_DETAILS = 7;
    public static final int MESSAGE_MOVE_MESSAGE = 8;
    public static final int MESSAGE_SEND_MESSAGE = 9;
    public static final int MESSAGE_LOAD_PRIVATE_KEYS = 10;
    public static final int MESSAGE_GET_ACTIVE_ACCOUNT = 11;
    public static final int MESSAGE_SEND_MESSAGE_WITH_BACKUP = 12;

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

        switch (intent.getAction()) {
            case ACTION_SWITCH_ACCOUNT:
                emailSyncManager.setAccount(new AccountDaoSource().getActiveAccountInformation(this));
                emailSyncManager.beginSync(true);
                break;

            default:
                emailSyncManager.beginSync(false);
                break;
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
                sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_ERROR);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPrivateKeyFound(AccountDao accountDao, List<String> keys, String ownerKey, int requestCode) {
        try {
            sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_OK, keys);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onEncryptedMessageSent(AccountDao accountDao, String ownerKey, int requestCode, boolean isSent) {
        try {
            if (isSent) {
                sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_OK);
            } else {
                sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_ERROR);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessagesMoved(AccountDao accountDao, IMAPFolder sourceImapFolder, IMAPFolder destinationImapFolder,
                                javax.mail.Message[] messages, String ownerKey, int requestCode) {
        try {
            if (messages != null && messages.length > 0) {
                sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_OK);
            } else {
                sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_ERROR);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageDetailsReceived(AccountDao accountDao, IMAPFolder imapFolder, long uid,
                                         String rawMessageWithOutAttachments, String ownerKey, int requestCode) {
        try {
            MessageDaoSource messageDaoSource = new MessageDaoSource();
            com.flowcrypt.email.api.email.Folder folder = FoldersManager.generateFolder(imapFolder,
                    imapFolder.getName());

            messageDaoSource.updateMessageRawText(getApplicationContext(),
                    accountDao.getEmail(),
                    folder.getFolderAlias(),
                    uid,
                    rawMessageWithOutAttachments);

            if (TextUtils.isEmpty(rawMessageWithOutAttachments)) {
                sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_ERROR);
            } else {
                sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_OK);
            }
        } catch (MessagingException | RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessagesReceived(AccountDao accountDao, IMAPFolder imapFolder, javax.mail.Message[] messages,
                                   String key, int requestCode) {
        Log.d(TAG, "onMessagesReceived: imapFolder = " + imapFolder.getFullName() + " message " +
                "count: " + messages.length);
        try {
            com.flowcrypt.email.api.email.Folder folder = FoldersManager.generateFolder(imapFolder,
                    imapFolder.getName());

            MessageDaoSource messageDaoSource = new MessageDaoSource();
            messageDaoSource.addRows(getApplicationContext(),
                    accountDao.getEmail(),
                    folder.getFolderAlias(),
                    imapFolder,
                    messages);

            if (messages.length > 0) {
                sendReply(key, requestCode, REPLY_RESULT_CODE_NEED_UPDATE);
            } else {
                sendReply(key, requestCode, REPLY_RESULT_CODE_ACTION_OK);
            }

            updateLocalContactsIfMessagesFromSentFolder(accountDao, imapFolder, messages);
            updateAttachmentTable(accountDao, folder, imapFolder, messages);

        } catch (MessagingException | RemoteException | IOException e) {
            e.printStackTrace();
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
            }
        }

        ImapLabelsDaoSource imapLabelsDaoSource = new ImapLabelsDaoSource();
        imapLabelsDaoSource.deleteFolders(getApplicationContext(), accountDao.getEmail());
        imapLabelsDaoSource.addRows(getApplicationContext(), accountDao.getEmail(), foldersManager.getAllFolders());

        try {
            sendReply(key, requestCode, REPLY_RESULT_CODE_ACTION_OK);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(AccountDao accountDao, int errorType, Exception e, String key, int requestCode) {
        Log.e(TAG, "onError: errorType" + errorType + "| e =" + e);
        try {
            if (replyToMessengers.containsKey(key)) {
                Messenger messenger = replyToMessengers.get(key);
                messenger.send(Message.obtain(null, REPLY_ERROR, requestCode, errorType, e));
            }
        } catch (RemoteException remoteException) {
            remoteException.printStackTrace();
        }
    }

    /**
     * @param accountDao The object which contains information about an email account.
     * @param folder     The local reflection of the remote folder.
     * @param imapFolder The folder where the new messages exist.
     * @param messages   The new messages.
     * @throws MessagingException This exception meybe happen when we try to call {@code
     *                            {@link IMAPFolder#getUID(javax.mail.Message)}}
     */
    private void updateAttachmentTable(AccountDao accountDao, com.flowcrypt.email.api.email.Folder folder,
                                       IMAPFolder imapFolder, javax.mail.Message[] messages)
            throws MessagingException {
        AttachmentDaoSource attachmentDaoSource = new AttachmentDaoSource();
        ArrayList<ContentValues> contentValuesList = new ArrayList<>();

        for (javax.mail.Message message : messages) {
            ArrayList<AttachmentInfo> attachmentInfoList = null;

            try {
                attachmentInfoList = getAttachmentsInfoFromPart(accountDao, imapFolder, message.getMessageNumber(),
                        message);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (attachmentInfoList != null && !attachmentInfoList.isEmpty()) {
                for (AttachmentInfo attachmentInfo : attachmentInfoList) {
                    contentValuesList.add(AttachmentDaoSource.prepareContentValues(accountDao.getEmail(),
                            folder.getFolderAlias(), imapFolder.getUID(message), attachmentInfo));
                }
            }
        }

        attachmentDaoSource.addRows(getApplicationContext(), contentValuesList.toArray(new ContentValues[0]));
    }

    /**
     * Find attachments in the {@link Part}.
     *
     * @param accountDao    The object which contains information about an email account.
     * @param imapFolder    The {@link IMAPFolder} which contains the parent message;
     * @param messageNumber This number will be used for fetching {@link Part} details;
     * @param part          The parent part.
     * @return The list of created {@link AttachmentInfo}
     * @throws MessagingException
     * @throws IOException
     */
    @NonNull
    private ArrayList<AttachmentInfo> getAttachmentsInfoFromPart(AccountDao accountDao, IMAPFolder imapFolder, int
            messageNumber, Part part)
            throws MessagingException, IOException {
        ArrayList<AttachmentInfo> attachmentInfoList = new ArrayList<>();

        if (part.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
            Multipart multiPart = (Multipart) part.getContent();
            int numberOfParts = multiPart.getCount();
            String[] headers;
            for (int partCount = 0; partCount < numberOfParts; partCount++) {
                BodyPart bodyPart = multiPart.getBodyPart(partCount);
                if (bodyPart.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
                    ArrayList<AttachmentInfo> attachmentInfoLists = getAttachmentsInfoFromPart(accountDao, imapFolder,
                            messageNumber, bodyPart);
                    if (!attachmentInfoLists.isEmpty()) {
                        attachmentInfoList.addAll(attachmentInfoLists);
                    }
                } else if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                    InputStream inputStream = ImapProtocolUtil.getHeaderStream(accountDao, imapFolder, messageNumber,
                            partCount + 1);

                    if (inputStream == null) {
                        throw new MessagingException("Failed to fetch headers");
                    }

                    InternetHeaders internetHeaders = new InternetHeaders(inputStream);
                    headers = internetHeaders.getHeader(JavaEmailConstants.HEADER_CONTENT_ID);

                    if (headers == null) {
                        //try to receive custom Gmail attachments header X-Attachment-Id
                        headers = internetHeaders.getHeader(JavaEmailConstants.HEADER_X_ATTACHMENT_ID);
                    }

                    if (headers != null && headers.length > 0) {
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
     *                    <li>{@link EmailSyncService#REPLY_RESULT_CODE_ACTION_ERROR}</li>
     *                    <li>{@link EmailSyncService#REPLY_RESULT_CODE_NEED_UPDATE}</li>
     *                    </ul>
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
     *                    <li>{@link EmailSyncService#REPLY_RESULT_CODE_ACTION_ERROR}</li>
     *                    <li>{@link EmailSyncService#REPLY_RESULT_CODE_NEED_UPDATE}</li>
     *                    </ul>
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
     * @param accountDao The object which contains information about an email account.
     * @param imapFolder The folder where messages exist.
     * @param messages   The received messages.
     */
    private void updateLocalContactsIfMessagesFromSentFolder(AccountDao accountDao, IMAPFolder imapFolder,
                                                             javax.mail.Message[] messages) {
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
        }
    }

    /**
     * Generate a list of {@link EmailAndNamePair} objects from the input message.
     * This information will be retrieved from "to" and "cc" headers.
     *
     * @param message The input {@link Message}.
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
                            emailSyncManager.updateLabels(action.getOwnerKey(), action.requestCode);
                        }
                        break;

                    case MESSAGE_LOAD_MESSAGES:
                        if (emailSyncManager != null && action != null) {
                            com.flowcrypt.email.api.email.Folder folder =
                                    (com.flowcrypt.email.api.email.Folder) action.getObject();
                            emailSyncManager.loadMessages(action.getOwnerKey(), action.getRequestCode(),
                                    folder.getServerFullFolderName(), message.arg1, message.arg2);
                        }
                        break;

                    case MESSAGE_LOAD_NEXT_MESSAGES:
                        if (emailSyncManager != null && action != null) {
                            com.flowcrypt.email.api.email.Folder folderOfMessages =
                                    (com.flowcrypt.email.api.email.Folder) action.getObject();

                            emailSyncManager.loadNextMessages(action.getOwnerKey(),
                                    action.getRequestCode(), folderOfMessages.getServerFullFolderName(), message.arg1);
                        }
                        break;

                    case MESSAGE_LOAD_NEW_MESSAGES_MANUALLY:
                        if (emailSyncManager != null && action != null) {
                            com.flowcrypt.email.api.email.Folder refreshFolder =
                                    (com.flowcrypt.email.api.email.Folder) action.getObject();

                            emailSyncManager.loadNewMessagesManually(action.getOwnerKey(),
                                    action.getRequestCode(), refreshFolder.getServerFullFolderName(), message.arg1);
                        }
                        break;

                    case MESSAGE_LOAD_MESSAGE_DETAILS:
                        if (emailSyncManager != null && action != null) {
                            com.flowcrypt.email.api.email.Folder messageFolder =
                                    (com.flowcrypt.email.api.email.Folder) action.getObject();

                            emailSyncManager.loadMessageDetails(action.getOwnerKey(),
                                    action.getRequestCode(), messageFolder.getServerFullFolderName(), message.arg1);
                        }
                        break;

                    case MESSAGE_MOVE_MESSAGE:
                        if (emailSyncManager != null && action != null) {
                            com.flowcrypt.email.api.email.Folder[] folders = (com.flowcrypt.email
                                    .api.email.Folder[]) action.getObject();

                            emailSyncManager.moveMessage(action.getOwnerKey(), action.getRequestCode(),
                                    folders[0].getServerFullFolderName(), folders[1].getServerFullFolderName(),
                                    message.arg1);
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
                            String searchTermString = (String) action.getObject();

                            emailSyncManager.loadPrivateKeys(action.getOwnerKey(), action.getRequestCode(),
                                    searchTermString);
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

                    default:
                        super.handleMessage(message);
                }
            }
        }
    }

    /**
     * This class can be used to create a new action for {@link EmailSyncService}
     */
    public static class Action {
        private String ownerKey;
        private int requestCode;
        private Object object;

        /**
         * The constructor.
         *
         * @param ownerKey    The name of reply to {@link Messenger}
         * @param requestCode The unique request code which identify some action
         * @param object      The object which will be passed to {@link EmailSyncService}.
         */
        public Action(String ownerKey, int requestCode, Object object) {
            this.ownerKey = ownerKey;
            this.requestCode = requestCode;
            this.object = object;
        }

        @Override
        public String toString() {
            return "Action{" +
                    "ownerKey='" + ownerKey + '\'' +
                    ", requestCode=" + requestCode +
                    ", object=" + object +
                    '}';
        }

        public String getOwnerKey() {
            return ownerKey;
        }

        public int getRequestCode() {
            return requestCode;
        }

        public Object getObject() {
            return object;
        }
    }
}
