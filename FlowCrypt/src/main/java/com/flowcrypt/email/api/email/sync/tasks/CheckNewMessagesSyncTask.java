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
 * This task does a job of loading all new messages which not exist in the cache but exist on the server.
 *
 * @author Denis Bondarenko
 * Date: 22.06.2018
 * Time: 15:50
 * E-mail: DenBond7@gmail.com
 */
public class CheckNewMessagesSyncTask extends BaseSyncTask {
    protected com.flowcrypt.email.api.email.Folder localFolder;
    protected int lastUID;

    public CheckNewMessagesSyncTask(String ownerKey, int requestCode, com.flowcrypt.email.api.email.Folder localFolder,
                                    int lastUID) {
        super(ownerKey, requestCode);
        this.localFolder = localFolder;
        this.lastUID = lastUID;
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

            syncListener.onNewMessagesReceived(accountDao, localFolder, imapFolder, newMessages, ownerKey, requestCode);
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
    protected Message[] getNewMessages(IMAPFolder imapFolder, long nextUID) throws MessagingException {
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
}
