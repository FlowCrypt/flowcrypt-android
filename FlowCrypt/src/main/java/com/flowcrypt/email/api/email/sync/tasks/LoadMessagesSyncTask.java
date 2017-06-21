/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import com.flowcrypt.email.api.email.sync.SyncListener;
import com.sun.mail.gimap.GmailSSLStore;
import com.sun.mail.imap.IMAPFolder;

import javax.mail.Folder;
import javax.mail.Message;

/**
 * This task does job to load messages.
 *
 * @author DenBond7
 *         Date: 20.06.2017
 *         Time: 15:06
 *         E-mail: DenBond7@gmail.com
 */

public class LoadMessagesSyncTask implements SyncTask {
    private String folderName;
    private int from;
    private int to;

    public LoadMessagesSyncTask(String folderName, int from, int to) {
        this.folderName = folderName;
        this.from = from;
        this.to = to;
    }

    @Override
    public void run(GmailSSLStore gmailSSLStore, SyncListener syncListener) throws Exception {
        IMAPFolder imapFolder = (IMAPFolder) gmailSSLStore.getFolder(folderName);
        imapFolder.open(Folder.READ_ONLY);

        if (syncListener != null) {
            Message[] messages;

            if (from == to) {
                messages = new Message[]{imapFolder.getMessage(from)};
            } else {
                messages = imapFolder.getMessages(from, to);
            }

            for (Message message : messages) {
                imapFolder.getUID(message);
            }

            syncListener.onMessageReceived(imapFolder, messages);
        }
    }
}
