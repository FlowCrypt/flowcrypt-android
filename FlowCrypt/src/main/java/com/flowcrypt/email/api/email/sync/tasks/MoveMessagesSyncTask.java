/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import android.os.Messenger;

import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.sun.mail.imap.IMAPFolder;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;

/**
 * This task does job of moving messages.
 *
 * @author DenBond7
 *         Date: 28.06.2017
 *         Time: 15:20
 *         E-mail: DenBond7@gmail.com
 */

public class MoveMessagesSyncTask extends BaseSyncTask {
    private String sourceFolderName;
    private String destinationFolderName;
    private long[] uids;

    /**
     * The base constructor.
     *
     * @param ownerKey              The name of the reply to {@link Messenger}.
     * @param requestCode           The unique request code for the reply to {@link Messenger}.
     * @param sourceFolderName      The source folder name where a message exists.
     * @param destinationFolderName The new destination folder name where a message will be move.
     * @param uids                  The {@link com.sun.mail.imap.protocol.UID} of the moving
     *                              messages.
     */
    public MoveMessagesSyncTask(String ownerKey, int requestCode, String sourceFolderName,
                                String destinationFolderName, long[] uids) {
        super(ownerKey, requestCode);
        this.sourceFolderName = sourceFolderName;
        this.destinationFolderName = destinationFolderName;
        this.uids = uids;
    }

    @Override
    public void runIMAPAction(AccountDao accountDao, Session session, Store store, SyncListener syncListener) throws
            Exception {
        IMAPFolder sourceImapFolder = (IMAPFolder) store.getFolder(sourceFolderName);
        IMAPFolder destinationImapFolder = (IMAPFolder) store.getFolder
                (destinationFolderName);

        if (sourceImapFolder == null || !sourceImapFolder.exists()) {
            throw new IllegalArgumentException("The invalid source " +
                    "folder: " + "\"" + sourceFolderName + "\"");
        }

        sourceImapFolder.open(Folder.READ_WRITE);

        boolean isSingleMoving = uids.length == 1;

        Message[] messages = sourceImapFolder.getMessagesByUID(uids);

        messages = trimNulls(messages);

        if (messages != null && messages.length > 0) {
            if (destinationImapFolder == null || !destinationImapFolder.exists()) {
                throw new IllegalArgumentException("The invalid " +
                        "destination folder: " + "\"" + destinationImapFolder + "\"");
            }

            destinationImapFolder.open(Folder.READ_WRITE);
            sourceImapFolder.moveMessages(messages, destinationImapFolder);
            if (isSingleMoving) {
                syncListener.onMessageMoved(accountDao, sourceImapFolder, destinationImapFolder, messages[0],
                        ownerKey, requestCode);
            } else {
                syncListener.onMessagesMoved(accountDao, sourceImapFolder, destinationImapFolder, messages,
                        ownerKey, requestCode);
            }

            destinationImapFolder.close(false);
        } else {
            if (isSingleMoving) {
                syncListener.onMessagesMoved(accountDao, sourceImapFolder, destinationImapFolder, null,
                        ownerKey, requestCode);
            } else {
                syncListener.onMessagesMoved(accountDao, sourceImapFolder, destinationImapFolder, new Message[]{},
                        ownerKey, requestCode);
            }
        }

        sourceImapFolder.close(false);
    }

    /**
     * Remove all null objects from the array.
     *
     * @param messages The input messages array.
     * @return The array of non-null messages.
     */
    private Message[] trimNulls(Message[] messages) {
        if (messages != null) {
            List<Message> list = new ArrayList<>();

            for (Message message : messages) {
                if (message != null) {
                    list.add(message);
                }
            }

            return list.toArray(new Message[0]);
        } else return null;
    }
}
