/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
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
     *                                     account.
     * @param imapFolder                   The folder where the new messages exist.
     * @param uid                          The UID of the message.
     * @param rawMessageWithOutAttachments The raw message without attachments.
     * @param ownerKey                     The name of the reply to {@link android.os.Messenger}.
     * @param requestCode                  The unique request code for the reply to
     *                                     {@link android.os.Messenger}.
     */
    void onMessageDetailsReceived(AccountDao accountDao, IMAPFolder imapFolder, long uid, String
            rawMessageWithOutAttachments, String ownerKey, int requestCode);

    /**
     * This method called when a new messages received from the some folder.
     *
     * @param accountDao  The {@link AccountDao} object which contains information about an email account.
     * @param folder      The folder where the new messages exist.
     * @param messages    The new messages.
     * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
     * @param requestCode The unique request code for the reply to {@link android.os.Messenger}.
     */
    void onMessagesReceived(AccountDao accountDao, IMAPFolder folder, Message[] messages, String ownerKey, int
            requestCode);

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
}
