/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource;
import com.sun.mail.imap.IMAPFolder;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;

/**
 * This task does a job of loading all new messages which not exist in the cache but exist on the server and updates
 * existing messages in the local database.
 *
 * @author DenBond7
 *         Date: 22.06.2017
 *         Time: 17:12
 *         E-mail: DenBond7@gmail.com
 */

public class RefreshMessagesSyncTask extends CheckNewMessagesSyncTask {
    public RefreshMessagesSyncTask(String ownerKey, int requestCode, com.flowcrypt.email.api.email.Folder localFolder) {
        super(ownerKey, requestCode, localFolder);
    }

    @Override
    public void runIMAPAction(AccountDao accountDao, Session session, Store store, SyncListener syncListener) throws
            Exception {
        IMAPFolder imapFolder = (IMAPFolder) store.getFolder(localFolder.getServerFullFolderName());
        imapFolder.open(Folder.READ_ONLY);

        long nextUID = imapFolder.getUIDNext();

        if (syncListener != null) {
            Message[] newMessages = new Message[0];

            MessageDaoSource messageDaoSource = new MessageDaoSource();

            int cachedUID = messageDaoSource.getLastUIDOfMessageInLabel(syncListener.getContext(), accountDao
                    .getEmail(), localFolder.getFolderAlias());

            int countOfLoadedMessages = messageDaoSource.getCountOfMessagesForLabel(syncListener.getContext(),
                    accountDao.getEmail(),
                    localFolder.getFolderAlias());

            if (cachedUID > 1 && cachedUID < nextUID - 1) {
                newMessages = EmailUtil.fetchMessagesInfo(imapFolder,
                        imapFolder.getMessagesByUID(cachedUID + 1, nextUID - 1));
            }

            int countOfNewMessages = newMessages != null ? newMessages.length : 0;
            Message[] updatedMessages = EmailUtil.getUpdatedMessages(imapFolder, countOfLoadedMessages,
                    countOfNewMessages);

            syncListener.onRefreshMessagesReceived(accountDao, localFolder, imapFolder, newMessages,
                    updatedMessages, ownerKey, requestCode);
        }

        imapFolder.close(false);
    }
}
