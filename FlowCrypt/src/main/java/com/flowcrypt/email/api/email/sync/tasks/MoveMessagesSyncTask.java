/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import android.os.Messenger;

import com.flowcrypt.email.api.email.LocalFolder;
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
 * This task does job of moving messages.
 *
 * @author DenBond7
 * Date: 28.06.2017
 * Time: 15:20
 * E-mail: DenBond7@gmail.com
 */

public class MoveMessagesSyncTask extends BaseSyncTask {
  private LocalFolder sourceLocalFolderName;
  private LocalFolder destinationLocalFolderName;
  private long[] uids;

  /**
   * The base constructor.
   *
   * @param ownerKey          The name of the reply to {@link Messenger}.
   * @param requestCode       The unique request code for the reply to {@link Messenger}.
   * @param sourceLocalFolder      A local implementation of the remote folder which is the source.
   * @param destinationLocalFolder A local implementation of the remote folder which is the destination.
   * @param uids              The {@link com.sun.mail.imap.protocol.UID} of the moving
   */
  public MoveMessagesSyncTask(String ownerKey, int requestCode, LocalFolder sourceLocalFolder,
                              LocalFolder destinationLocalFolder, long[] uids) {
    super(ownerKey, requestCode);
    this.sourceLocalFolderName = sourceLocalFolder;
    this.destinationLocalFolderName = destinationLocalFolder;
    this.uids = uids;
  }

  @Override
  public void runIMAPAction(AccountDao account, Session session, Store store, SyncListener listener) throws Exception {
    IMAPFolder srcFolder = (IMAPFolder) store.getFolder(sourceLocalFolderName.getFullName());
    IMAPFolder destFolder = (IMAPFolder) store.getFolder(destinationLocalFolderName.getFullName());

    if (srcFolder == null || !srcFolder.exists()) {
      throw new IllegalArgumentException("The invalid source folder: " + "\"" + sourceLocalFolderName + "\"");
    }

    srcFolder.open(Folder.READ_WRITE);

    boolean isSingleMoving = uids.length == 1;

    Message[] msgs = srcFolder.getMessagesByUID(uids);
    msgs = trimNulls(msgs);

    if (msgs != null && msgs.length > 0) {
      if (destFolder == null || !destFolder.exists()) {
        throw new IllegalArgumentException("The invalid destination folder: " + "\"" + destFolder + "\"");
      }

      destFolder.open(Folder.READ_WRITE);
      srcFolder.moveMessages(msgs, destFolder);
      if (isSingleMoving) {
        listener.onMessageMoved(account, srcFolder, destFolder, msgs[0], ownerKey, requestCode);
      } else {
        listener.onMessagesMoved(account, srcFolder, destFolder, msgs, ownerKey, requestCode);
      }

      destFolder.close(false);
    } else {
      if (isSingleMoving) {
        listener.onMessagesMoved(account, srcFolder, destFolder, null, ownerKey, requestCode);
      } else {
        listener.onMessagesMoved(account, srcFolder, destFolder, new Message[]{}, ownerKey, requestCode);
      }
    }

    srcFolder.close(false);
  }

  /**
   * Remove all null objects from the array.
   *
   * @param messages The input messages array.
   * @return The array of non-null messages.
   */
  private Message[] trimNulls(Message[] messages) {
    if (messages != null) {
      List<Message> list = new ArrayList<>();

      for (Message message : messages) {
        if (message != null) {
          list.add(message);
        }
      }

      return list.toArray(new Message[0]);
    } else return null;
  }
}
