/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import android.util.LongSparseArray;

import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource;
import com.sun.mail.imap.IMAPFolder;

import java.util.ArrayList;
import java.util.List;

import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;

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
    public void runIMAPAction(AccountDao accountDao, Session session, Store store, SyncListener syncListener)
            throws Exception {
        if (syncListener != null) {
            boolean isShowOnlyEncryptedMessages = new AccountDaoSource().isShowOnlyEncryptedMessages(
                    syncListener.getContext(), accountDao.getEmail());

            IMAPFolder imapFolder = (IMAPFolder) store.getFolder(localFolder.getServerFullFolderName());
            imapFolder.open(Folder.READ_ONLY);

            long nextUID = imapFolder.getUIDNext();

            Message[] newMessages = new Message[0];

            int newestCachedUID = new MessageDaoSource().getLastUIDOfMessageInLabel(syncListener.getContext(),
                    accountDao
                            .getEmail(), localFolder.getFolderAlias());

            if (newestCachedUID < nextUID - 1) {
                if (isShowOnlyEncryptedMessages) {
                    Message[] foundMessages =
                            imapFolder.search(EmailUtil.generateSearchTermForEncryptedMessages(accountDao));

                    FetchProfile fetchProfile = new FetchProfile();
                    fetchProfile.add(UIDFolder.FetchProfileItem.UID);

                    imapFolder.fetch(foundMessages, fetchProfile);

                    List<Message> newMessagesList = new ArrayList<>();

                    for (Message message : foundMessages) {
                        if (imapFolder.getUID(message) > newestCachedUID) {
                            newMessagesList.add(message);
                        }
                    }

                    newMessages = EmailUtil.fetchMessagesInfo(imapFolder, newMessagesList.toArray(new Message[0]));
                } else {
                    newMessages = EmailUtil.fetchMessagesInfo(imapFolder,
                            imapFolder.getMessagesByUID(newestCachedUID + 1, nextUID - 1));
                }
            }

            LongSparseArray<Boolean> booleanLongSparseArray = new LongSparseArray<>();
            if (isShowOnlyEncryptedMessages) {
                for (Message message : newMessages) {
                    booleanLongSparseArray.put(imapFolder.getUID(message), true);
                }
            } else {
                List<Long> uidList = new ArrayList<>();

                for (Message message : newMessages) {
                    uidList.add(imapFolder.getUID(message));
                }

                booleanLongSparseArray = EmailUtil.getInfoAreMessagesEncrypted(imapFolder, uidList);
            }

            syncListener.onNewMessagesReceived(accountDao, localFolder, imapFolder, newMessages,
                    booleanLongSparseArray, ownerKey, requestCode);

            imapFolder.close(false);
        }
    }
}
