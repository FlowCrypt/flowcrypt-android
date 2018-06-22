/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync;

import android.content.Context;

import com.flowcrypt.email.database.dao.source.AccountDao;
import com.sun.mail.imap.IMAPFolder;

import java.util.List;

import javax.mail.Folder;
import javax.mail.Message;

/**
 * This class can be used for communication with {@link EmailSyncManager}
 *
 * @author DenBond7
 *         Date: 19.06.2017
 *         Time: 13:35
 *         E-mail: DenBond7@gmail.com
 */

public interface SyncListener {

    /**
     * Get the service context.
     *
     * @return The service context
     */
    Context getContext();

    /**
     * This method called when a message with a backup to the key owner was sent.
     *
     * @param accountDao  The {@link AccountDao} object which contains information about an email account.
     * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
     * @param requestCode The unique request code for the reply to
     *                    {@link android.os.Messenger}.
     * @param isSent      true if the message was sent, false otherwise.
     */
    void onMessageWithBackupToKeyOwnerSent(AccountDao accountDao, String ownerKey, int requestCode, boolean isSent);

    /**
     * This method called when the private keys found.
     *
     * @param accountDao  The {@link AccountDao} object which contains information about an email account.
     * @param keys        The private keys list.
     * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
     * @param requestCode The unique request code for the reply to
     *                    {@link android.os.Messenger}.
     */
    void onPrivateKeyFound(AccountDao accountDao, List<String> keys, String ownerKey, int requestCode);

    /**
     * This method called when a message was sent.
     *
     * @param accountDao  The {@link AccountDao} object which contains information about an email account.
     * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
     * @param requestCode The unique request code for the reply to
     *                    {@link android.os.Messenger}.
     * @param isSent      true if the message was sent, false otherwise.
     */
    void onMessageSent(AccountDao accountDao, String ownerKey, int requestCode, boolean isSent);

    /**
     * This method called when a new messages received from the some folder.
     *
     * @param accountDao            The {@link AccountDao} object which contains information about an email account.
     * @param sourceImapFolder      The source folder where the messages exist.
     * @param destinationImapFolder The destination folder where the messages were moved.
     * @param messages              The moved messages.
     * @param ownerKey              The name of the reply to {@link android.os.Messenger}.
     * @param requestCode           The unique request code for the reply to
     *                              {@link android.os.Messenger}.
     */
    void onMessagesMoved(AccountDao accountDao, IMAPFolder sourceImapFolder, IMAPFolder destinationImapFolder,
                         Message[] messages, String ownerKey, int requestCode);

    /**
     * This method called when a new messages received from the some folder.
     *
     * @param accountDao            The {@link AccountDao} object which contains information about an email account.
     * @param sourceImapFolder      The source folder where the messages exist.
     * @param destinationImapFolder The destination folder where the messages were moved.
     * @param message               The moved message.
     * @param ownerKey              The name of the reply to {@link android.os.Messenger}.
     * @param requestCode           The unique request code for the reply to
     *                              {@link android.os.Messenger}.
     */
    void onMessageMoved(AccountDao accountDao, IMAPFolder sourceImapFolder, IMAPFolder destinationImapFolder,
                        Message message, String ownerKey, int requestCode);

    /**
     * This method called when a new messages received from the some folder.
     *
     * @param accountDao                   The {@link AccountDao} object which contains information about an email
     *                                     account;
     * @param localFolder                  The local implementation of the remote folder;
     * @param imapFolder                   The folder where the new messages exist;
     * @param uid                          The UID of the message;
     * @param message                      The received message;
     * @param rawMessageWithOutAttachments The raw message without attachments;
     * @param ownerKey                     The name of the reply to {@link android.os.Messenger};
     * @param requestCode                  The unique request code for the reply to
     *                                     {@link android.os.Messenger}.
     */
    void onMessageDetailsReceived(AccountDao accountDao, com.flowcrypt.email.api.email.Folder localFolder,
                                  IMAPFolder imapFolder, long uid, Message message, String rawMessageWithOutAttachments,
                                  String ownerKey, int requestCode);

    /**
     * This method called when a new messages received from the some folder.
     *
     * @param accountDao   The {@link AccountDao} object which contains information about an
     *                     email account.
     * @param localFolder  The local implementation of the remote folder
     * @param remoteFolder The remote folder where the new messages exist.
     * @param messages     The new messages.
     * @param ownerKey     The name of the reply to {@link android.os.Messenger}.
     * @param requestCode  The unique request code for the reply to
     *                     {@link android.os.Messenger}.
     */
    void onMessagesReceived(AccountDao accountDao, com.flowcrypt.email.api.email.Folder localFolder,
                            IMAPFolder remoteFolder, Message[] messages, String ownerKey, int requestCode);

    /**
     * This method called when a new messages received from the some folder.
     *
     * @param accountDao   The {@link AccountDao} object which contains information about an
     *                     email account.
     * @param localFolder  The local implementation of the remote folder.
     * @param remoteFolder The folder where the messages exist.
     * @param messages     The new messages.
     * @param ownerKey     The name of the reply to {@link android.os.Messenger}.
     * @param requestCode  The unique request code for the reply to
     *                     {@link android.os.Messenger}.
     */
    void onSearchMessagesReceived(AccountDao accountDao, com.flowcrypt.email.api.email.Folder localFolder,
                                  IMAPFolder remoteFolder, Message[] messages, String ownerKey, int requestCode);

    /**
     * This method called when received information about messages which already exist in the local database.
     *
     * @param accountDao     The {@link AccountDao} object which contains information about an email account.
     * @param localFolder    The local implementation of the remote folder.
     * @param remoteFolder   The folder where the messages exist.
     * @param newMessages    The refreshed messages.
     * @param updateMessages The messages which will must be updated.
     * @param ownerKey       The name of the reply to {@link android.os.Messenger}.
     * @param requestCode    The unique request code for the reply to {@link android.os.Messenger}.
     */
    void onRefreshMessagesReceived(AccountDao accountDao, com.flowcrypt.email.api.email.Folder localFolder,
                                   IMAPFolder remoteFolder, Message[] newMessages, Message[] updateMessages,
                                   String ownerKey, int requestCode);

    /**
     * This method called when new folders list received.
     *
     * @param accountDao  The {@link AccountDao} object which contains information about an email account.
     * @param folders     The new folders list.
     * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
     * @param requestCode The unique request code for the reply to {@link android.os.Messenger}.
     */
    void onFolderInfoReceived(AccountDao accountDao, Folder[] folders, String ownerKey, int requestCode);

    /**
     * Handle an error of synchronization.
     *
     * @param accountDao  The {@link AccountDao} object which contains information about an email account.
     * @param errorType   The error type code.
     * @param e           The exception that occurred during synchronization.
     * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
     * @param requestCode The unique request code for the reply to {@link android.os.Messenger}.
     */
    void onError(AccountDao accountDao, int errorType, Exception e, String ownerKey, int requestCode);

    /**
     * This method can be used for debugging. Using this method we can identify a progress of some operation.
     *
     * @param accountDao  The {@link AccountDao} object which contains information about an email account.
     * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
     * @param requestCode The unique request code for the reply to {@link android.os.Messenger}.
     * @param resultCode  The unique result code for the reply which identifies the progress of some request.
     */
    void onActionProgress(AccountDao accountDao, String ownerKey, int requestCode, int resultCode);

    /**
     * This method called when received information about new messages in some folder.
     *
     * @param accountDao   The {@link AccountDao} object which contains information about an email account.
     * @param localFolder  The local implementation of the remote folder.
     * @param remoteFolder The folder where the new  messages exist.
     * @param newMessages  The new messages.
     * @param ownerKey     The name of the reply to {@link android.os.Messenger}.
     * @param requestCode  The unique request code for the reply to {@link android.os.Messenger}.
     */
    void onNewMessagesReceived(AccountDao accountDao, com.flowcrypt.email.api.email.Folder localFolder, IMAPFolder
            remoteFolder, Message[] newMessages, String ownerKey, int requestCode);
}
