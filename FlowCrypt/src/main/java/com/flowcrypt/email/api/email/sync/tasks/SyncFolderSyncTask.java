/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import android.content.Context;
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
  public void runIMAPAction(AccountDao account, Session session, Store store, SyncListener listener) throws Exception {
    if (listener != null) {
      Context context = listener.getContext();
      String folderName = localFolder.getFolderAlias();
      boolean isEncryptedModeEnabled = new AccountDaoSource().isEncryptedModeEnabled(context, account.getEmail());

      IMAPFolder folder = (IMAPFolder) store.getFolder(localFolder.getServerFullFolderName());
      folder.open(javax.mail.Folder.READ_ONLY);

      MessageDaoSource messageDaoSource = new MessageDaoSource();

      long nextUID = folder.getUIDNext();
      int newestCachedUID = messageDaoSource.getLastUIDOfMessageInLabel(context, account.getEmail(), folderName);
      int countOfLoadedMessages = messageDaoSource.getCountOfMessagesForLabel(context, account.getEmail(), folderName);

      Message[] newMsgs = new Message[0];

      if (newestCachedUID > 1 && newestCachedUID < nextUID - 1) {
        if (isEncryptedModeEnabled) {
          Message[] foundMsgs = folder.search(EmailUtil.generateSearchTermForEncryptedMessages(account));

          FetchProfile fetchProfile = new FetchProfile();
          fetchProfile.add(UIDFolder.FetchProfileItem.UID);

          folder.fetch(foundMsgs, fetchProfile);

          List<Message> newMsgsList = new ArrayList<>();

          for (Message msg : foundMsgs) {
            if (folder.getUID(msg) > newestCachedUID) {
              newMsgsList.add(msg);
            }
          }

          newMsgs = EmailUtil.fetchMessagesInfo(folder, newMsgsList.toArray(new Message[0]));
        } else {
          Message[] tempMsgs = folder.getMessagesByUID(newestCachedUID + 1, nextUID - 1);
          newMsgs = EmailUtil.fetchMessagesInfo(folder, tempMsgs);
        }
      }

      Message[] updatedMsgs;
      if (isEncryptedModeEnabled) {
        int oldestCachedUID = messageDaoSource.getOldestUIDOfMessageInLabel(context, account.getEmail(), folderName);
        updatedMsgs = EmailUtil.getUpdatedMessagesByUID(folder, oldestCachedUID, newestCachedUID);
      } else {
        updatedMsgs = EmailUtil.getUpdatedMessages(folder, countOfLoadedMessages, newMsgs.length);
      }

      listener.onRefreshMessagesReceived(account, localFolder, folder, newMsgs, updatedMsgs, ownerKey, requestCode);

      if (newMsgs.length > 0) {
        LongSparseArray<Boolean> array = EmailUtil.getMessagesEncryptionInfo(isEncryptedModeEnabled, folder, newMsgs);
        listener.onNewMessagesReceived(account, localFolder, folder, newMsgs, array, ownerKey, requestCode);
      }

      folder.close(false);
    }
  }
}
