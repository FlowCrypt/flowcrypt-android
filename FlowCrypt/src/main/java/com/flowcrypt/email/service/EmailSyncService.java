/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service;

import android.accounts.Account;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.api.email.FoldersManager;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.gmail.GmailConstants;
import com.flowcrypt.email.api.email.sync.GmailSynsManager;
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.database.dao.source.imap.ImapLabelsDaoSource;
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.sun.mail.imap.IMAPFolder;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.mail.Folder;
import javax.mail.MessagingException;

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
    public static final int REPLY_RESULT_CODE_OK = 1;
    public static final int REPLY_RESULT_CODE_ERROR = 0;
    public static final int REPLY_RESULT_CODE_NEED_UPDATE = 2;

    public static final int REPLY_WHAT = 0;

    public static final int MESSAGE_ADD_REPLY_MESSENGER = 1;
    public static final int MESSAGE_REMOVE_REPLY_MESSENGER = 2;
    public static final int MESSAGE_UPDATE_LABELS = 3;
    public static final int MESSAGE_LOAD_MESSAGES = 4;
    public static final int MESSAGE_LOAD_NEXT_MESSAGES = 5;
    public static final int MESSAGE_LOAD_NEW_MESSAGES_MANUALLY = 6;
    public static final int MESSAGE_LOAD_MESSAGE_DETAILS = 7;

    public static final String EXTRA_KEY_GMAIL_ACCOUNT = BuildConfig.APPLICATION_ID
            + ".EXTRA_KEY_GMAIL_ACCOUNT";
    private static final String TAG = EmailSyncService.class.getSimpleName();
    /**
     * This {@link Messenger} is responsible for the receive intents from other client and
     * handles them.
     */
    private Messenger messenger;

    private Map<String, Messenger> replyToMessengers;

    private GmailSynsManager gmailSynsManager;

    /**
     * The current {@link Account} for what we do synchronization.
     */
    private Account account;

    public EmailSyncService() {
        this.replyToMessengers = new HashMap<>();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        gmailSynsManager = GmailSynsManager.getInstance();
        gmailSynsManager.setSyncListener(this);

        messenger = new Messenger(new IncomingHandler(gmailSynsManager, replyToMessengers));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand |intent =" + intent + "|flags = " + flags + "|startId = " +
                startId);
        if (intent != null) {
            account = intent.getParcelableExtra(EXTRA_KEY_GMAIL_ACCOUNT);
            if (account != null) {
                gmailSynsManager.beginSync(false);
            } else {
                //todo-denbond7 handle this error;
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
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
        return messenger.getBinder();
    }

    @Override
    public void onMessageDetailsReceived(IMAPFolder imapFolder, javax.mail.Message message, String
            rawMessageWithOutAttachments, String ownerKey, int requestCode) {
        try {
            MessageDaoSource messageDaoSource = new MessageDaoSource();
            com.flowcrypt.email.api.email.Folder folder = FoldersManager.generateFolder(imapFolder,
                    imapFolder.getName());

            messageDaoSource.updateMessageRawText(getApplicationContext(),
                    account.name,
                    folder.getFolderAlias(),
                    imapFolder.getUID(message),
                    rawMessageWithOutAttachments);

            if (TextUtils.isEmpty(rawMessageWithOutAttachments)) {
                sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ERROR);
            } else {
                sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_OK);
            }
        } catch (MessagingException | RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessagesReceived(IMAPFolder imapFolder, javax.mail.Message[] messages, String
            key, int requestCode) {
        Log.d(TAG, "onMessagesReceived: imapFolder = " + imapFolder.getFullName() + " message " +
                "count: " + messages.length);
        try {
            com.flowcrypt.email.api.email.Folder folder = FoldersManager.generateFolder(imapFolder,
                    imapFolder.getName());

            MessageDaoSource messageDaoSource = new MessageDaoSource();
            messageDaoSource.addRows(getApplicationContext(),
                    account.name,
                    folder.getFolderAlias(),
                    imapFolder,
                    messages);

            if (messages.length > 0) {
                sendReply(key, requestCode, REPLY_RESULT_CODE_NEED_UPDATE);
            } else {
                sendReply(key, requestCode, REPLY_RESULT_CODE_OK);
            }

        } catch (MessagingException | RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getValidToken() throws IOException, GoogleAuthException {
        return GoogleAuthUtil.getToken(this, account,
                JavaEmailConstants.OAUTH2 + GmailConstants.SCOPE_MAIL_GOOGLE_COM);
    }

    @Override
    public String getEmail() {
        return account.name;
    }

    @Override
    public void onFolderInfoReceived(Folder[] folders, String key, int requestCode) {
        Log.d(TAG, "onFolderInfoReceived:" + Arrays.toString(folders));
        ImapLabelsDaoSource imapLabelsDaoSource = new ImapLabelsDaoSource();
        imapLabelsDaoSource.deleteFolders(getApplicationContext(), account.name);

        FoldersManager foldersManager = new FoldersManager();
        for (Folder folder : folders) {
            try {
                IMAPFolder imapFolder = (IMAPFolder) folder;
                foldersManager.addFolder(imapFolder, folder.getName());
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }

        //// TODO-denbond7: 23.06.2017 Better use insert per one transaction
        for (com.flowcrypt.email.api.email.Folder folder : foldersManager.getAllFolders()) {
            imapLabelsDaoSource.addRow(getApplicationContext(), account.name, folder);
        }

        try {
            sendReply(key, requestCode, REPLY_RESULT_CODE_OK);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(int errorType, Exception e, String key, int requestCode) {
        Log.e(TAG, "onError: errorType" + errorType + "| e =" + e);
        try {
            sendReply(key, requestCode, REPLY_RESULT_CODE_ERROR);
        } catch (RemoteException remoteException) {
            remoteException.printStackTrace();
        }
    }

    /**
     * Send a reply to the called component.
     *
     * @param key         The key which identify the reply to {@link Messenger}
     * @param requestCode The unique request code for the reply to {@link android.os.Messenger}.
     * @param resultCode  The result code of the some action. Can take the following values:
     *                    <ul>
     *                    <li>{@link EmailSyncService#REPLY_RESULT_CODE_OK}</li>
     *                    <li>{@link EmailSyncService#REPLY_RESULT_CODE_ERROR}</li>
     *                    <li>{@link EmailSyncService#REPLY_RESULT_CODE_NEED_UPDATE}</li>
     *                    </ul>
     * @throws RemoteException
     */
    private void sendReply(String key, int requestCode, int resultCode) throws RemoteException {
        if (replyToMessengers.containsKey(key)) {
            Messenger messenger = replyToMessengers.get(key);
            messenger.send(Message.obtain(null, REPLY_WHAT, requestCode, resultCode));
        }
    }

    /**
     * The incoming handler realization. This handler will be used to communicate with current
     * service and other Android components.
     */
    private static class IncomingHandler extends Handler {
        private final WeakReference<GmailSynsManager> gmailSynsManagerWeakReference;
        private final WeakReference<Map<String, Messenger>> replyToMessengersWeakReference;

        IncomingHandler(GmailSynsManager gmailSynsManager, Map<String, Messenger>
                replyToMessengersWeakReference) {
            this.gmailSynsManagerWeakReference = new WeakReference<>(gmailSynsManager);
            this.replyToMessengersWeakReference = new WeakReference<>
                    (replyToMessengersWeakReference);
        }

        @Override
        public void handleMessage(Message message) {
            if (gmailSynsManagerWeakReference.get() != null) {
                GmailSynsManager gmailSynsManager = gmailSynsManagerWeakReference.get();
                Action action = null;

                if (message.obj instanceof Action) {
                    action = (Action) message.obj;
                }

                switch (message.what) {
                    case MESSAGE_ADD_REPLY_MESSENGER:
                        Map<String, Messenger> replyToMessengersForAdd
                                = replyToMessengersWeakReference.get();

                        if (replyToMessengersForAdd != null && action != null) {
                            replyToMessengersForAdd.put(action.getOwnerKey(), message.replyTo);
                        }
                        break;

                    case MESSAGE_REMOVE_REPLY_MESSENGER:
                        Map<String, Messenger> replyToMessengersForRemove
                                = replyToMessengersWeakReference.get();

                        if (replyToMessengersForRemove != null && action != null) {
                            replyToMessengersForRemove.remove(action.getOwnerKey());
                        }
                        break;

                    case MESSAGE_UPDATE_LABELS:
                        if (gmailSynsManager != null && action != null) {
                            gmailSynsManager.updateLabels(action.getOwnerKey(), action.requestCode);
                        }
                        break;

                    case MESSAGE_LOAD_MESSAGES:
                        if (gmailSynsManager != null && action != null) {
                            com.flowcrypt.email.api.email.Folder folder = (com.flowcrypt.email.api
                                    .email.Folder) action.getObject();
                            gmailSynsManager.loadMessages(action.getOwnerKey(),
                                    action.getRequestCode(), folder.getServerFullFolderName(),
                                    message.arg1, message.arg2);
                        }
                        break;

                    case MESSAGE_LOAD_NEXT_MESSAGES:
                        if (gmailSynsManager != null && action != null) {
                            com.flowcrypt.email.api.email.Folder folderOfMessages =
                                    (com.flowcrypt.email.api.email.Folder) action.getObject();

                            gmailSynsManager.loadNextMessages(action.getOwnerKey(),
                                    action.getRequestCode(),
                                    folderOfMessages.getServerFullFolderName(), message.arg1);
                        }
                        break;

                    case MESSAGE_LOAD_NEW_MESSAGES_MANUALLY:
                        if (gmailSynsManager != null && action != null) {
                            com.flowcrypt.email.api.email.Folder refreshFolder =
                                    (com.flowcrypt.email.api.email.Folder) action.getObject();

                            gmailSynsManager.loadNewMessagesManually(action.getOwnerKey(),
                                    action.getRequestCode(),
                                    refreshFolder.getServerFullFolderName(), message.arg1);
                        }
                        break;

                    case MESSAGE_LOAD_MESSAGE_DETAILS:
                        if (gmailSynsManager != null && action != null) {
                            com.flowcrypt.email.api.email.Folder messageFolder =
                                    (com.flowcrypt.email.api.email.Folder) action.getObject();

                            gmailSynsManager.loadMessageDetails(action.getOwnerKey(),
                                    action.getRequestCode(),
                                    messageFolder.getServerFullFolderName(), message.arg1);
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
