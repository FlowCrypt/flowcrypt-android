/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.sun.mail.imap.IMAPFolder;

import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;

/**
 * This task does job to load all new messages which not exist in the cache but exist on the server.
 *
 * @author DenBond7
 *         Date: 22.06.2017
 *         Time: 17:12
 *         E-mail: DenBond7@gmail.com
 */

public class RefreshMessagesSyncTask extends BaseSyncTask {
    private com.flowcrypt.email.api.email.Folder localFolder;
    private int lastUID;
    private int countOfLoadedMessages;

    public RefreshMessagesSyncTask(String ownerKey, int requestCode, com.flowcrypt.email.api.email.Folder localFolder,
                                   int lastUID, int countOfLoadedMessages) {
        super(ownerKey, requestCode);
        this.localFolder = localFolder;
        this.lastUID = lastUID;
        this.countOfLoadedMessages = countOfLoadedMessages;
    }

    @Override
    public void runIMAPAction(AccountDao accountDao, Session session, Store store, SyncListener syncListener) throws
            Exception {
        IMAPFolder imapFolder = (IMAPFolder) store.getFolder(localFolder.getServerFullFolderName());
        imapFolder.open(Folder.READ_ONLY);

        long nextUID = imapFolder.getUIDNext();

        if (syncListener != null) {
            Message[] newMessages = new Message[0];

            if (lastUID < nextUID - 1) {
                newMessages = getNewMessages(imapFolder, nextUID);
            }
            int countOfNewMessages = newMessages != null ? newMessages.length : 0;
            Message[] updatedMessages = getUpdatedMessages(imapFolder, countOfLoadedMessages, countOfNewMessages);

            syncListener.onRefreshMessagesReceived(accountDao, localFolder, imapFolder, newMessages,
                    updatedMessages, ownerKey, requestCode);
        }

        imapFolder.close(false);
    }

    /**
     * Load new messages using the next UID for fetching.
     *
     * @param imapFolder The folder which contains messages.
     * @param nextUID    The next UID.
     * @return New messages from a server which not exist in a local database.
     * @throws MessagingException for other failures.
     */
    private Message[] getNewMessages(IMAPFolder imapFolder, long nextUID) throws MessagingException {
        Message[] messages = imapFolder.getMessagesByUID(lastUID + 1, nextUID - 1);

        if (messages.length > 0) {
            FetchProfile fetchProfile = new FetchProfile();
            fetchProfile.add(FetchProfile.Item.ENVELOPE);
            fetchProfile.add(FetchProfile.Item.FLAGS);
            fetchProfile.add(FetchProfile.Item.CONTENT_INFO);
            fetchProfile.add(UIDFolder.FetchProfileItem.UID);

            imapFolder.fetch(messages, fetchProfile);
        }
        return messages;
    }

    /**
     * Get updated information about messages in the local database.
     *
     * @param imapFolder            The folder which contains messages.
     * @param countOfLoadedMessages The count of already loaded messages.
     * @param countOfNewMessages    The count of new messages (offset value).
     * @return A list of messages which already exist in the local database.
     * @throws MessagingException for other failures.
     */
    private Message[] getUpdatedMessages(IMAPFolder imapFolder, int countOfLoadedMessages, int countOfNewMessages)
            throws MessagingException {
        int end = imapFolder.getMessageCount() - countOfNewMessages;
        int start = end - countOfLoadedMessages + 1;

        if (end < 1) {
            return new Message[]{};
        } else {
            if (start < 1) {
                start = 1;
            }

            Message[] messages = imapFolder.getMessages(start, end);

            if (messages.length > 0) {
                FetchProfile fetchProfile = new FetchProfile();
                fetchProfile.add(FetchProfile.Item.FLAGS);
                fetchProfile.add(UIDFolder.FetchProfileItem.UID);
                imapFolder.fetch(messages, fetchProfile);
            }
            return messages;
        }
    }
}
