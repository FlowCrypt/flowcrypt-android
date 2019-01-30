/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import android.content.Context;

import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.LocalFolder;
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
 * This task does a job of loading all new messages which not exist in the cache but exist on the server and updates
 * existing messages in the local database.
 *
 * @author DenBond7
 * Date: 22.06.2017
 * Time: 17:12
 * E-mail: DenBond7@gmail.com
 */

public class RefreshMessagesSyncTask extends CheckNewMessagesSyncTask {
  public RefreshMessagesSyncTask(String ownerKey, int requestCode, LocalFolder localFolder) {
    super(ownerKey, requestCode, localFolder);
  }

  @Override
  public void runIMAPAction(AccountDao account, Session session, Store store, SyncListener listener) throws Exception {
    if (listener != null) {
      Context context = listener.getContext();
      String folderName = localFolder.getFolderAlias();
      IMAPFolder imapFolder = (IMAPFolder) store.getFolder(localFolder.getFullName());
      imapFolder.open(Folder.READ_ONLY);

      long nextUID = imapFolder.getUIDNext();

      Message[] newMsgs = new Message[0];
      MessageDaoSource messageDaoSource = new MessageDaoSource();

      int newestCachedUID = messageDaoSource.getLastUIDOfMsgInLabel(context, account.getEmail(), folderName);
      int countOfLoadedMsgs = messageDaoSource.getLabelMsgsCount(context, account.getEmail(), folderName);
      boolean isEncryptedModeEnabled = new AccountDaoSource().isEncryptedModeEnabled(context, account.getEmail());

      if (newestCachedUID > 1 && newestCachedUID < nextUID - 1) {
        if (isEncryptedModeEnabled) {
          Message[] foundMsgs = imapFolder.search(EmailUtil.genEncryptedMsgsSearchTerm(account));

          FetchProfile fetchProfile = new FetchProfile();
          fetchProfile.add(UIDFolder.FetchProfileItem.UID);

          imapFolder.fetch(foundMsgs, fetchProfile);

          List<Message> newMsgsList = new ArrayList<>();

          for (Message message : foundMsgs) {
            if (imapFolder.getUID(message) > newestCachedUID) {
              newMsgsList.add(message);
            }
          }

          newMsgs = EmailUtil.fetchMsgs(imapFolder, newMsgsList.toArray(new Message[0]));
        } else {
          Message[] msgs = imapFolder.getMessagesByUID(newestCachedUID + 1, nextUID - 1);
          newMsgs = EmailUtil.fetchMsgs(imapFolder, msgs);
        }
      }

      Message[] updatedMsgs;
      if (isEncryptedModeEnabled) {
        int oldestCachedUID = messageDaoSource.getOldestUIDOfMsgInLabel(context, account.getEmail(), folderName);
        updatedMsgs = EmailUtil.getUpdatedMsgsByUID(imapFolder, oldestCachedUID, newestCachedUID);
      } else {
        int countOfNewMsgs = newMsgs != null ? newMsgs.length : 0;
        updatedMsgs = EmailUtil.getUpdatedMsgs(imapFolder, countOfLoadedMsgs, countOfNewMsgs);
      }

      listener.onRefreshMsgsReceived(account, localFolder, imapFolder, newMsgs, updatedMsgs, ownerKey, requestCode);

      imapFolder.close(false);
    }
  }
}
