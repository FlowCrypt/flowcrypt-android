/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import android.content.Context;
import android.util.LongSparseArray;

import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.model.LocalFolder;
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
 * Date: 22.06.2018
 * Time: 15:50
 * E-mail: DenBond7@gmail.com
 */
public class CheckNewMessagesSyncTask extends CheckIsLoadedMessagesEncryptedSyncTask {
  protected LocalFolder localFolder;

  public CheckNewMessagesSyncTask(String ownerKey, int requestCode, LocalFolder localFolder) {
    super(ownerKey, requestCode, localFolder);
    this.localFolder = localFolder;
  }

  @Override
  public void runIMAPAction(AccountDao account, Session session, Store store, SyncListener listener) throws Exception {
    if (listener != null) {
      Context context = listener.getContext();
      String email = account.getEmail();
      String folderAlias = localFolder.getFolderAlias();
      boolean isEncryptedModeEnabled = new AccountDaoSource().isEncryptedModeEnabled(context, email);

      IMAPFolder folder = (IMAPFolder) store.getFolder(localFolder.getFullName());
      folder.open(Folder.READ_ONLY);

      long nextUID = folder.getUIDNext();
      int newestCachedUID = new MessageDaoSource().getLastUIDOfMsgInLabel(context, email, folderAlias);
      Message[] newMsgs = new Message[0];

      if (newestCachedUID < nextUID - 1) {
        if (isEncryptedModeEnabled) {
          Message[] foundMsgs = folder.search(EmailUtil.genEncryptedMsgsSearchTerm(account));

          FetchProfile fetchProfile = new FetchProfile();
          fetchProfile.add(UIDFolder.FetchProfileItem.UID);

          folder.fetch(foundMsgs, fetchProfile);

          List<Message> newMsgsList = new ArrayList<>();

          for (Message msg : foundMsgs) {
            if (folder.getUID(msg) > newestCachedUID) {
              newMsgsList.add(msg);
            }
          }

          newMsgs = EmailUtil.fetchMsgs(folder, newMsgsList.toArray(new Message[0]));
        } else {
          newMsgs = EmailUtil.fetchMsgs(folder, folder.getMessagesByUID(newestCachedUID + 1, nextUID - 1));
        }
      }

      LongSparseArray<Boolean> array = EmailUtil.getMsgsEncryptionInfo(isEncryptedModeEnabled, folder, newMsgs);
      listener.onNewMsgsReceived(account, localFolder, folder, newMsgs, array, ownerKey, requestCode);
      folder.close(false);
    }
  }
}
