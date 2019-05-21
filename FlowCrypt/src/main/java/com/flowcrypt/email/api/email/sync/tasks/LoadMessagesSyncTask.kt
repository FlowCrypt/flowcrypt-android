/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import com.flowcrypt.email.api.email.model.LocalFolder;
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.sun.mail.imap.IMAPFolder;

import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;

/**
 * This task does job to load messages.
 *
 * @author DenBond7
 * Date: 20.06.2017
 * Time: 15:06
 * E-mail: DenBond7@gmail.com
 */

public class LoadMessagesSyncTask extends BaseSyncTask {
  private LocalFolder localFolder;
  private int start;
  private int end;

  public LoadMessagesSyncTask(String ownerKey, int requestCode,
                              LocalFolder localFolder, int start, int end) {
    super(ownerKey, requestCode);
    this.localFolder = localFolder;
    this.start = start;
    this.end = end;
  }

  @Override
  public void runIMAPAction(AccountDao account, Session session, Store store, SyncListener listener) throws Exception {
    IMAPFolder imapFolder = (IMAPFolder) store.getFolder(localFolder.getFullName());
    imapFolder.open(Folder.READ_ONLY);

    int msgsCount = imapFolder.getMessageCount();

    if (listener != null) {
      if (this.end < 1 || this.end > msgsCount || this.start < 1) {
        listener.onMsgsReceived(account, localFolder, imapFolder, new Message[]{}, ownerKey, requestCode);
      } else {
        Message[] messages;

        if (this.end == this.start) {
          messages = new Message[]{imapFolder.getMessage(end)};
        } else {
          messages = imapFolder.getMessages(start, end);
        }

        FetchProfile fetchProfile = new FetchProfile();
        fetchProfile.add(FetchProfile.Item.ENVELOPE);
        fetchProfile.add(FetchProfile.Item.FLAGS);
        fetchProfile.add(FetchProfile.Item.CONTENT_INFO);
        fetchProfile.add(UIDFolder.FetchProfileItem.UID);

        imapFolder.fetch(messages, fetchProfile);

        listener.onMsgsReceived(account, localFolder, imapFolder, messages, ownerKey, requestCode);
      }
    }

    imapFolder.close(false);
  }
}
