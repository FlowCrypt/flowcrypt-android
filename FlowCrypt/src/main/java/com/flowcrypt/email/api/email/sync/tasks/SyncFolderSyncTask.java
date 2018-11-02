/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import android.util.LongSparseArray;

import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource;
import com.sun.mail.imap.IMAPFolder;

import java.util.ArrayList;
import java.util.List;

import javax.mail.FetchProfile;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;

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

    if (syncListener != null) {
      boolean isShowOnlyEncryptedMessages = new AccountDaoSource().isShowOnlyEncryptedMessages(
          syncListener.getContext(), accountDao.getEmail());

      IMAPFolder imapFolder = (IMAPFolder) store.getFolder(localFolder.getServerFullFolderName());
      imapFolder.open(javax.mail.Folder.READ_ONLY);

      MessageDaoSource messageDaoSource = new MessageDaoSource();

      long nextUID = imapFolder.getUIDNext();
      int newestCachedUID = messageDaoSource.getLastUIDOfMessageInLabel(syncListener.getContext(),
          accountDao.getEmail(), localFolder.getFolderAlias());
      int countOfLoadedMessages = messageDaoSource.getCountOfMessagesForLabel(syncListener.getContext(),
          accountDao.getEmail(), localFolder.getFolderAlias());

      Message[] newMessages = new Message[0];

      if (newestCachedUID > 1 && newestCachedUID < nextUID - 1) {
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

      Message[] updatedMessages;
      if (isShowOnlyEncryptedMessages) {
        int oldestCachedUID = messageDaoSource.getOldestUIDOfMessageInLabel(syncListener.getContext(),
            accountDao.getEmail(), localFolder.getFolderAlias());
        updatedMessages = EmailUtil.getUpdatedMessagesByUID(imapFolder, oldestCachedUID, newestCachedUID);
      } else {
        updatedMessages = EmailUtil.getUpdatedMessages(imapFolder, countOfLoadedMessages, newMessages.length);
      }

      syncListener.onRefreshMessagesReceived(accountDao, localFolder, imapFolder, newMessages,
          updatedMessages, ownerKey, requestCode);

      if (newMessages.length > 0) {
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
      }

      imapFolder.close(false);
    }
  }
}
