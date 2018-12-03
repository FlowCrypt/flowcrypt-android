/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import android.content.Context;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.database.dao.source.imap.ImapLabelsDaoSource;
import com.sun.mail.imap.IMAPFolder;

import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;

/**
 * This task loads the older messages via some step.
 *
 * @author DenBond7
 * Date: 23.06.2017
 * Time: 11:26
 * E-mail: DenBond7@gmail.com
 */

public class LoadMessagesToCacheSyncTask extends BaseSyncTask {
  private static final String TAG = LoadMessagesToCacheSyncTask.class.getSimpleName();
  private com.flowcrypt.email.api.email.Folder localFolder;
  private int countOfAlreadyLoadedMessages;

  public LoadMessagesToCacheSyncTask(String ownerKey, int requestCode,
                                     com.flowcrypt.email.api.email.Folder localFolder, int countOfAlreadyLoadedMsgs) {
    super(ownerKey, requestCode);
    this.localFolder = localFolder;
    this.countOfAlreadyLoadedMessages = countOfAlreadyLoadedMsgs;
  }

  @Override
  public void runIMAPAction(AccountDao account, Session session, Store store, SyncListener listener) throws Exception {
    if (listener != null) {
      IMAPFolder imapFolder = (IMAPFolder) store.getFolder(localFolder.getFullName());
      listener.onActionProgress(account, ownerKey, requestCode, R.id.progress_id_opening_store);
      imapFolder.open(Folder.READ_ONLY);

      if (countOfAlreadyLoadedMessages < 0) {
        countOfAlreadyLoadedMessages = 0;
      }

      Context context = listener.getContext();
      boolean isEncryptedModeEnabled = new AccountDaoSource().isEncryptedModeEnabled(context, account.getEmail());
      Message[] foundMsgs = new Message[0];
      int msgsCount;

      if (isEncryptedModeEnabled) {
        foundMsgs = imapFolder.search(EmailUtil.genEncryptedMessagesSearchTerm(account));
        msgsCount = foundMsgs.length;
      } else {
        msgsCount = imapFolder.getMessageCount();
      }

      int end = msgsCount - countOfAlreadyLoadedMessages;
      int start = end - JavaEmailConstants.COUNT_OF_LOADED_EMAILS_BY_STEP + 1;
      String folderName = imapFolder.getFullName();
      new ImapLabelsDaoSource().updateLabelMessagesCount(context, account.getEmail(), folderName, msgsCount);

      listener.onActionProgress(account, ownerKey, requestCode, R.id.progress_id_getting_list_of_emails);
      if (end < 1) {
        listener.onMessagesReceived(account, localFolder, imapFolder, new Message[]{}, ownerKey, requestCode);
      } else {
        if (start < 1) {
          start = 1;
        }

        Message[] msgs;
        if (isEncryptedModeEnabled) {
          msgs = new Message[end - start + 1];
          System.arraycopy(foundMsgs, start - 1, msgs, 0, end - start + 1);
        } else {
          msgs = imapFolder.getMessages(start, end);
        }

        FetchProfile fetchProfile = new FetchProfile();
        fetchProfile.add(FetchProfile.Item.ENVELOPE);
        fetchProfile.add(FetchProfile.Item.FLAGS);
        fetchProfile.add(FetchProfile.Item.CONTENT_INFO);
        fetchProfile.add(UIDFolder.FetchProfileItem.UID);
        imapFolder.fetch(msgs, fetchProfile);

        listener.onMessagesReceived(account, localFolder, imapFolder, msgs, ownerKey, requestCode);
      }
      imapFolder.close(false);
    }
  }
}
