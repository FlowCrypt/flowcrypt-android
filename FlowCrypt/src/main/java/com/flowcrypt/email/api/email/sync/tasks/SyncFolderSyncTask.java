/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource;
import com.sun.mail.imap.IMAPFolder;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;

/**
 * This task does syncing a local folder with a remote. (Server -> client)
 *
 * @author Denis Bondarenko
 * Date: 25.07.2018
 * Time: 14:19
 * E-mail: DenBond7@gmail.com
 */
public class SyncFolderSyncTask extends BaseSyncTask {
    private com.flowcrypt.email.api.email.Folder localFolder;

    public SyncFolderSyncTask(String ownerKey, int requestCode, Folder localFolder) {
        super(ownerKey, requestCode);
        this.localFolder = localFolder;
    }

    @Override
    public void runIMAPAction(AccountDao accountDao, Session session, Store store, SyncListener syncListener) throws
            Exception {
        IMAPFolder imapFolder = (IMAPFolder) store.getFolder(localFolder.getServerFullFolderName());
        imapFolder.open(javax.mail.Folder.READ_ONLY);

        MessageDaoSource messageDaoSource = new MessageDaoSource();

        long nextUID = imapFolder.getUIDNext();
        if (syncListener != null) {
            int cachedUID = messageDaoSource.getLastUIDOfMessageInLabel(syncListener.getContext(), accountDao
                            .getEmail(),
                    localFolder.getFolderAlias());
            int countOfLoadedMessages = messageDaoSource.getCountOfMessagesForLabel(syncListener.getContext(),
                    accountDao.getEmail(), localFolder.getFolderAlias());

            Message[] newMessages = new Message[0];

            if (cachedUID > 1 && cachedUID < nextUID - 1) {
                newMessages = EmailUtil.fetchMessagesInfo(imapFolder, imapFolder.getMessagesByUID(cachedUID + 1,
                        nextUID - 1));
            }

            Message[] updatedMessages = EmailUtil.getUpdatedMessages(imapFolder, countOfLoadedMessages,
                    newMessages.length);

            syncListener.onRefreshMessagesReceived(accountDao, localFolder, imapFolder, newMessages,
                    updatedMessages, ownerKey, requestCode);

            if (newMessages.length > 0) {
                List<Long> uidList = new ArrayList<>();

                for (Message message : newMessages) {
                    uidList.add(imapFolder.getUID(message));
                }

                syncListener.onNewMessagesReceived(accountDao, localFolder, imapFolder, newMessages,
                        EmailUtil.getInfoAreMessagesEncrypted(imapFolder, uidList), ownerKey, requestCode);
            }
        }

        imapFolder.close(false);
    }
}
