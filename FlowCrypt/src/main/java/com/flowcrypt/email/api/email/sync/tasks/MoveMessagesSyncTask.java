/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import android.os.Messenger;

import com.flowcrypt.email.api.email.sync.SyncListener;
import com.sun.mail.gimap.GmailSSLStore;
import com.sun.mail.imap.IMAPFolder;

import javax.mail.Folder;
import javax.mail.Message;

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
    public void run(GmailSSLStore gmailSSLStore, SyncListener syncListener) throws Exception {
        IMAPFolder sourceImapFolder = (IMAPFolder) gmailSSLStore.getFolder(sourceFolderName);
        IMAPFolder destinationImapFolder = (IMAPFolder) gmailSSLStore.getFolder
                (destinationFolderName);

        if (sourceImapFolder == null || !sourceImapFolder.exists()) {
            throw new IllegalArgumentException("The invalid source " +
                    "folder: " + "\"" + sourceFolderName + "\"");
        }

        sourceImapFolder.open(Folder.READ_WRITE);

        Message[] messages = sourceImapFolder.getMessagesByUID(uids);

        if (messages != null && messages.length > 0) {
            if (destinationImapFolder == null || !destinationImapFolder.exists()) {
                throw new IllegalArgumentException("The invalid " +
                        "destination folder: " + "\"" + destinationImapFolder + "\"");
            }

            destinationImapFolder.open(Folder.READ_WRITE);
            sourceImapFolder.moveMessages(messages, destinationImapFolder);
            syncListener.onMessagesMoved(sourceImapFolder, destinationImapFolder, messages,
                    ownerKey, requestCode);

            destinationImapFolder.close(false);
        } else {
            syncListener.onMessagesMoved(sourceImapFolder, destinationImapFolder, new Message[]{},
                    ownerKey, requestCode);
        }

        sourceImapFolder.close(false);
    }
}
