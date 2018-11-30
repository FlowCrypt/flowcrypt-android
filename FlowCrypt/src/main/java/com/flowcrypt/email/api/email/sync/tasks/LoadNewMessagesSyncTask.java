/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import android.util.LongSparseArray;

import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.sun.mail.imap.IMAPFolder;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;

/**
 * This task loads new messages.
 *
 * @author DenBond7
 * Date: 18.07.2018
 * Time: 14:27
 * E-mail: DenBond7@gmail.com
 */

public class LoadNewMessagesSyncTask extends CheckIsLoadedMessagesEncryptedSyncTask {
  private com.flowcrypt.email.api.email.Folder folder;
  private int[] messageIds;

  public LoadNewMessagesSyncTask(String ownerKey, int requestCode,
                                 com.flowcrypt.email.api.email.Folder folder, Message[] messages) {
    super(ownerKey, requestCode, folder);
    this.folder = folder;

    if (messages != null) {
      messageIds = new int[messages.length];
      for (int i = 0; i < messages.length; i++) {
        messageIds[i] = messages[i].getMessageNumber();
      }
    }
  }

  @Override
  public void runIMAPAction(AccountDao account, Session session, Store store, SyncListener listener)
      throws Exception {
    IMAPFolder imapFolder = (IMAPFolder) store.getFolder(folder.getServerFullFolderName());
    imapFolder.open(Folder.READ_ONLY);

    if (listener != null && messageIds != null) {
      Message[] messages = imapFolder.getMessages(messageIds);
      EmailUtil.fetchMessagesInfo(imapFolder, messages);

      List<Long> uidList = new ArrayList<>();

      for (Message message : messages) {
        uidList.add(imapFolder.getUID(message));
      }

      if (uidList.isEmpty()) {
        listener.onNewMessagesReceived(account, folder, imapFolder, messages, new LongSparseArray
            <Boolean>(), ownerKey, requestCode);
        return;
      }

      listener.onNewMessagesReceived(account, folder, imapFolder, messages,
          EmailUtil.getInfoAreMessagesEncrypted(imapFolder, uidList), ownerKey, requestCode);
    }

    imapFolder.close(false);
  }
}
