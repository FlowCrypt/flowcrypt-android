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
 * This task does job to load messages.
 *
 * @author DenBond7
 *         Date: 20.06.2017
 *         Time: 15:06
 *         E-mail: DenBond7@gmail.com
 */

public class LoadMessagesSyncTask implements SyncTask {
    private static final int COUNT_OF_LOADED_EMAILS_BY_STEP = 10;
    private String folderName;
    private int start;
    private int end;
    private boolean isSingleLoad;

    public LoadMessagesSyncTask(String folderName, int start, int end) {
        this.folderName = folderName;
        this.start = start;
        this.end = end;
    }

    public LoadMessagesSyncTask(String folderName, int end, boolean isSingleLoad) {
        this(folderName, -1, end);
        this.isSingleLoad = isSingleLoad;
    }

    @Override
    public void run(GmailSSLStore gmailSSLStore, SyncListener syncListener) throws Exception {
        IMAPFolder imapFolder = (IMAPFolder) gmailSSLStore.getFolder(folderName);
        imapFolder.open(Folder.READ_ONLY);

        int messagesCount = imapFolder.getMessageCount();

        if (this.end < 1 || this.end > messagesCount) {
            this.end = messagesCount;
        }

        if (this.start == -1) {
            this.start = this.end - COUNT_OF_LOADED_EMAILS_BY_STEP;
        }

        if (this.start < 1) {
            this.start = 1;
        }

        if (syncListener != null) {
            Message[] messages;

            if (this.isSingleLoad) {
                messages = new Message[]{imapFolder.getMessage(end)};
            } else {
                messages = imapFolder.getMessages(start, end);
            }

            FetchProfile fetchProfile = new FetchProfile();
            fetchProfile.add(FetchProfile.Item.ENVELOPE);
            fetchProfile.add(FetchProfile.Item.FLAGS);
            fetchProfile.add(UIDFolder.FetchProfileItem.UID);
            imapFolder.fetch(messages, fetchProfile);

            syncListener.onMessageReceived(imapFolder, messages);
        }
    }
}
