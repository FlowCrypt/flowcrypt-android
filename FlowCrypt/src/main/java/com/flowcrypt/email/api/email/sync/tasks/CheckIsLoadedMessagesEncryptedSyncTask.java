/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import android.os.Messenger;
import android.util.LongSparseArray;

import com.flowcrypt.email.api.email.EmailUtil;
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
  private com.flowcrypt.email.api.email.Folder localFolder;

  /**
   * The base constructor.
   *
   * @param ownerKey    The name of the reply to {@link Messenger}.
   * @param requestCode The unique request code for the reply to {@link Messenger}.
   * @param localFolder The local implementation of the remote folder
   */
  public CheckIsLoadedMessagesEncryptedSyncTask(String ownerKey, int requestCode,
                                                com.flowcrypt.email.api.email.Folder localFolder) {
    super(ownerKey, requestCode);
    this.localFolder = localFolder;
  }

  @Override
  public void runIMAPAction(AccountDao accountDao, Session session, Store store, SyncListener syncListener)
      throws Exception {
    super.runIMAPAction(accountDao, session, store, syncListener);

    if (localFolder == null) {
      return;
    }

    MessageDaoSource messageDaoSource = new MessageDaoSource();

    List<Long> uidList = messageDaoSource.getUIDsOfMessagesWhichWereNotCheckedToEncryption(syncListener
        .getContext(), accountDao.getEmail(), localFolder.getFolderAlias());

    if (uidList == null || uidList.isEmpty()) {
      return;
    }

    IMAPFolder imapFolder = (IMAPFolder) store.getFolder(localFolder.getServerFullFolderName());
    imapFolder.open(Folder.READ_ONLY);

    LongSparseArray<Boolean> booleanLongSparseArray = EmailUtil.getInfoAreMessagesEncrypted(imapFolder, uidList);

    if (booleanLongSparseArray.size() > 0) {
      messageDaoSource.updateMessagesEncryptionStateByUID(syncListener.getContext(), accountDao.getEmail(),
          localFolder.getFolderAlias(), booleanLongSparseArray);
    }

    syncListener.onIdentificationToEncryptionCompleted(accountDao, localFolder, imapFolder, ownerKey, requestCode);

    imapFolder.close(false);
  }
}
