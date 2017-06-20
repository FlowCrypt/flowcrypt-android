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
import android.util.Log;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.api.email.FoldersManager;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.gmail.GmailConstants;
import com.flowcrypt.email.api.email.sync.GmailSynsManager;
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.database.dao.source.imap.ImapLabelsDaoSource;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.sun.mail.imap.IMAPFolder;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;

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
    public static final int MESSAGE_UPDATE_LABELS = 1;
    public static final String EXTRA_KEY_GMAIL_ACCOUNT = BuildConfig.APPLICATION_ID
            + ".EXTRA_KEY_GMAIL_ACCOUNT";

    private static final String TAG = EmailSyncService.class.getSimpleName();
    /**
     * This {@link Messenger} is responsible for the receive intents from other client and
     * handles them.
     */
    private Messenger messenger;

    private GmailSynsManager gmailSynsManager;

    /**
     * The current {@link Account} for what we do synchronization.
     */
    private Account account;

    public EmailSyncService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        gmailSynsManager = GmailSynsManager.getInstance();
        gmailSynsManager.setSyncListener(this);

        messenger = new Messenger(new IncomingHandler(gmailSynsManager));
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
    public void onMessageReceived(Folder folder, javax.mail.Message[] messages) {
        Log.d(TAG, "onMessageReceived: folder = " + folder.getFullName() + " message count: " +
                messages.length);
    }

    @Override
    public void onFolderInfoReceived(Folder[] folders) {
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

        for (com.flowcrypt.email.api.email.Folder folder : foldersManager.getAllFolders()) {
            imapLabelsDaoSource.addRow(getApplicationContext(), account.name, folder);
        }
    }

    @Override
    public void onError(int errorType, Exception e) {
        Log.e(TAG, "onError: errorType" + errorType + "| e =" + e);
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

    /**
     * The incoming handler realization. This handler will be used to communicate with current
     * service and other Android components.
     */
    private static class IncomingHandler extends Handler {
        private final WeakReference<GmailSynsManager> gmailSynsManagerWeakReference;

        IncomingHandler(GmailSynsManager gmailSynsManager) {
            this.gmailSynsManagerWeakReference = new WeakReference<>(gmailSynsManager);
        }

        @Override
        public void handleMessage(Message msg) {
            if (gmailSynsManagerWeakReference.get() != null) {
                GmailSynsManager gmailSynsManager = gmailSynsManagerWeakReference.get();
                switch (msg.what) {
                    case MESSAGE_UPDATE_LABELS:
                        if (gmailSynsManager != null) {
                            gmailSynsManager.updateLabels();
                        }
                        break;
                    default:
                        super.handleMessage(msg);
                }
            }
        }
    }
}
