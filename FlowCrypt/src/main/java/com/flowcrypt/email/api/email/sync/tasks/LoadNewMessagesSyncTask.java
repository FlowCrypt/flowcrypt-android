/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import com.flowcrypt.email.api.email.sync.SyncListener;
import com.sun.mail.gimap.GmailSSLStore;
import com.sun.mail.imap.IMAPFolder;

import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.UIDFolder;

/**
 * This task does job to load all new messages which not exist in the cache but exist on the server.
 *
 * @author DenBond7
 *         Date: 22.06.2017
 *         Time: 17:12
 *         E-mail: DenBond7@gmail.com
 */

public class LoadNewMessagesSyncTask implements SyncTask {
    private String folderName;
    private int lastUID;

    public LoadNewMessagesSyncTask(String folderName, int lastUID) {
        this.folderName = folderName;
        this.lastUID = lastUID;
    }

    @Override
    public void run(GmailSSLStore gmailSSLStore, SyncListener syncListener) throws Exception {
        IMAPFolder imapFolder = (IMAPFolder) gmailSSLStore.getFolder(folderName);
        imapFolder.open(Folder.READ_ONLY);

        long nextUID = imapFolder.getUIDNext();

        if (syncListener != null) {
            if (lastUID < nextUID - 1) {
                Message[] messages = imapFolder.getMessagesByUID(lastUID + 1, nextUID - 1);

                if (messages.length > 0) {
                    FetchProfile fetchProfile = new FetchProfile();
                    fetchProfile.add(FetchProfile.Item.ENVELOPE);
                    fetchProfile.add(FetchProfile.Item.FLAGS);
                    fetchProfile.add(UIDFolder.FetchProfileItem.UID);
                    imapFolder.fetch(messages, fetchProfile);
                }

                syncListener.onMessageReceived(imapFolder, messages);
            } else {
                syncListener.onMessageReceived(imapFolder, new Message[]{});
            }
        }

        imapFolder.close(false);
    }
}
