/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import android.content.Context;
import android.os.Messenger;
import android.util.LongSparseArray;

import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.LocalFolder;
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource;
import com.sun.mail.imap.IMAPFolder;

import java.util.List;

import javax.mail.Folder;
import javax.mail.Session;
import javax.mail.Store;

/**
 * This task identifies encrypted messages and updates information about messages in the local database.
 *
 * @author Denis Bondarenko
 * Date: 02.06.2018
 * Time: 14:30
 * E-mail: DenBond7@gmail.com
 */
public class CheckIsLoadedMessagesEncryptedSyncTask extends BaseSyncTask {
  private LocalFolder localFolder;

  /**
   * The base constructor.
   *
   * @param ownerKey    The name of the reply to {@link Messenger}.
   * @param requestCode The unique request code for the reply to {@link Messenger}.
   * @param localFolder The local implementation of the remote folder
   */
  public CheckIsLoadedMessagesEncryptedSyncTask(String ownerKey, int requestCode,
                                                LocalFolder localFolder) {
    super(ownerKey, requestCode);
    this.localFolder = localFolder;
  }

  @Override
  public void runIMAPAction(AccountDao account, Session session, Store store, SyncListener listener) throws Exception {
    super.runIMAPAction(account, session, store, listener);

    Context context = listener.getContext();
    String folder = localFolder.getFolderAlias();

    if (localFolder == null) {
      return;
    }

    MessageDaoSource msgDaoSource = new MessageDaoSource();

    List<Long> uidList = msgDaoSource.getNotCheckedUIDs(context, account.getEmail(), folder);

    if (uidList == null || uidList.isEmpty()) {
      return;
    }

    IMAPFolder imapFolder = (IMAPFolder) store.getFolder(localFolder.getFullName());
    imapFolder.open(Folder.READ_ONLY);

    LongSparseArray<Boolean> booleanLongSparseArray = EmailUtil.getMessagesEncryptionState(imapFolder, uidList);

    if (booleanLongSparseArray.size() > 0) {
      msgDaoSource.updateMessagesEncryptionStateByUID(context, account.getEmail(), folder, booleanLongSparseArray);
    }

    listener.onIdentificationToEncryptionCompleted(account, localFolder, imapFolder, ownerKey, requestCode);

    imapFolder.close(false);
  }
}
