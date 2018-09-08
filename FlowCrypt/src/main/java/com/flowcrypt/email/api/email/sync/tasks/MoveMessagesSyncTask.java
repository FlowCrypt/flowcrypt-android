/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
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
    private com.flowcrypt.email.api.email.Folder sourceFolderName;
    private com.flowcrypt.email.api.email.Folder destinationFolderName;
    private long[] uids;

    /**
     * The base constructor.
     *
     * @param ownerKey          The name of the reply to {@link Messenger}.
     * @param requestCode       The unique request code for the reply to {@link Messenger}.
     * @param sourceFolder      A local implementation of the remote folder which is the source.
     * @param destinationFolder A local implementation of the remote folder which is the destination.
     * @param uids              The {@link com.sun.mail.imap.protocol.UID} of the moving
     */
    public MoveMessagesSyncTask(String ownerKey, int requestCode, com.flowcrypt.email.api.email.Folder sourceFolder,
                                com.flowcrypt.email.api.email.Folder destinationFolder, long[] uids) {
        super(ownerKey, requestCode);
        this.sourceFolderName = sourceFolder;
        this.destinationFolderName = destinationFolder;
        this.uids = uids;
    }

    @Override
    public void runIMAPAction(AccountDao accountDao, Session session, Store store, SyncListener syncListener) throws
            Exception {
        IMAPFolder sourceImapFolder =
                (IMAPFolder) store.getFolder(sourceFolderName.getServerFullFolderName());
        IMAPFolder destinationImapFolder =
                (IMAPFolder) store.getFolder(destinationFolderName.getServerFullFolderName());

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
