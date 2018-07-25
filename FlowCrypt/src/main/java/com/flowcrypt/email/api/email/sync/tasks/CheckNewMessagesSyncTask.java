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

import java.util.ArrayList;
import java.util.List;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;

/**
 * This task does a job of loading all new messages which not exist in the cache but exist on the server.
 *
 * @author Denis Bondarenko
 *         Date: 22.06.2018
 *         Time: 15:50
 *         E-mail: DenBond7@gmail.com
 */
public class CheckNewMessagesSyncTask extends CheckIsLoadedMessagesEncryptedSyncTask {
    protected com.flowcrypt.email.api.email.Folder localFolder;

    public CheckNewMessagesSyncTask(String ownerKey, int requestCode,
                                    com.flowcrypt.email.api.email.Folder localFolder) {
        super(ownerKey, requestCode, localFolder);
        this.localFolder = localFolder;
    }

    @Override
    public void runIMAPAction(AccountDao accountDao, Session session, Store store, SyncListener syncListener) throws
            Exception {
        IMAPFolder imapFolder = (IMAPFolder) store.getFolder(localFolder.getServerFullFolderName());
        imapFolder.open(Folder.READ_ONLY);

        long nextUID = imapFolder.getUIDNext();

        if (syncListener != null) {
            Message[] messages = new Message[0];

            int lastCachedUID = new MessageDaoSource().getLastUIDOfMessageInLabel(syncListener.getContext(), accountDao
                    .getEmail(), localFolder.getFolderAlias());

            if (lastCachedUID < nextUID - 1) {
                messages = EmailUtil.fetchMessagesInfo(imapFolder,
                        imapFolder.getMessagesByUID(lastCachedUID + 1, nextUID - 1));
            }

            List<Long> uidList = new ArrayList<>();

            for (Message message : messages) {
                uidList.add(imapFolder.getUID(message));
            }

            syncListener.onNewMessagesReceived(accountDao, localFolder, imapFolder, messages,
                    EmailUtil.getInfoAreMessagesEncrypted(imapFolder, uidList), ownerKey, requestCode);
        }

        imapFolder.close(false);
    }
}
