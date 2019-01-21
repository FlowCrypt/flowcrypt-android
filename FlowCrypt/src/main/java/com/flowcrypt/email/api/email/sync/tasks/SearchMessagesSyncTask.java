/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import android.content.Context;
import android.os.Messenger;

import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.LocalFolder;
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.sun.mail.gimap.GmailRawSearchTerm;
import com.sun.mail.imap.IMAPFolder;

import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;
import javax.mail.search.AndTerm;
import javax.mail.search.SearchTerm;
import javax.mail.search.StringTerm;
import javax.mail.search.SubjectTerm;

import androidx.annotation.NonNull;

/**
 * This task finds messages on some localFolder.
 *
 * @author DenBond7
 * Date: 26.04.2018
 * Time: 14:20
 * E-mail: DenBond7@gmail.com
 */

public class SearchMessagesSyncTask extends BaseSyncTask {
  private LocalFolder localFolder;
  private int countOfAlreadyLoadedMsgs;

  /**
   * The base constructor.
   *
   * @param ownerKey    The name of the reply to {@link Messenger}.
   * @param requestCode The unique request code for the reply to {@link Messenger}.
   */
  public SearchMessagesSyncTask(String ownerKey, int requestCode, LocalFolder localFolder,
                                int countOfAlreadyLoadedMsgs) {
    super(ownerKey, requestCode);
    this.localFolder = localFolder;
    this.countOfAlreadyLoadedMsgs = countOfAlreadyLoadedMsgs;
  }

  @Override
  public void runIMAPAction(AccountDao account, Session session, Store store, SyncListener listener) throws Exception {
    super.runIMAPAction(account, session, store, listener);

    if (listener != null) {
      IMAPFolder imapFolder = (IMAPFolder) store.getFolder(localFolder.getFullName());
      imapFolder.open(Folder.READ_ONLY);

      if (countOfAlreadyLoadedMsgs < 0) {
        countOfAlreadyLoadedMsgs = 0;
      }

      Message[] foundMsgs = imapFolder.search(generateSearchTerm(listener.getContext(), account));

      int messagesCount = foundMsgs.length;
      int end = messagesCount - countOfAlreadyLoadedMsgs;
      int start = end - JavaEmailConstants.COUNT_OF_LOADED_EMAILS_BY_STEP + 1;

      if (end < 1) {
        listener.onSearchMsgsReceived(account, localFolder, imapFolder, new Message[]{}, ownerKey, requestCode);
      } else {
        if (start < 1) {
          start = 1;
        }

        Message[] bufferedMsgs = new Message[end - start + 1];
        System.arraycopy(foundMsgs, start - 1, bufferedMsgs, 0, end - start + 1);

        FetchProfile fetchProfile = new FetchProfile();
        fetchProfile.add(FetchProfile.Item.ENVELOPE);
        fetchProfile.add(FetchProfile.Item.FLAGS);
        fetchProfile.add(FetchProfile.Item.CONTENT_INFO);
        fetchProfile.add(UIDFolder.FetchProfileItem.UID);

        imapFolder.fetch(bufferedMsgs, fetchProfile);

        listener.onSearchMsgsReceived(account, localFolder, imapFolder, bufferedMsgs, ownerKey, requestCode);
      }

      imapFolder.close(false);
    }
  }

  /**
   * Generate a {@link SearchTerm} depend on an input {@link AccountDao}.
   *
   * @param context Interface to global information about an application environment.
   * @param account An input {@link AccountDao}
   * @return A generated {@link SearchTerm}.
   */
  @NonNull
  private SearchTerm generateSearchTerm(Context context, AccountDao account) {
    boolean isEncryptedModeEnabled = new AccountDaoSource().isEncryptedModeEnabled(context, account.getEmail());

    if (isEncryptedModeEnabled) {
      SearchTerm searchTerm = EmailUtil.genEncryptedMsgsSearchTerm(account);

      if (AccountDao.ACCOUNT_TYPE_GOOGLE.equalsIgnoreCase(account.getAccountType())) {
        StringTerm stringTerm = (StringTerm) searchTerm;
        return new GmailRawSearchTerm(localFolder.getSearchQuery() + " AND (" + stringTerm.getPattern() + ")");
      } else {
        return new AndTerm(searchTerm, new SubjectTerm(localFolder.getSearchQuery()));
      }
    } else {
      if (AccountDao.ACCOUNT_TYPE_GOOGLE.equalsIgnoreCase(account.getAccountType())) {
        return new GmailRawSearchTerm(localFolder.getSearchQuery());
      } else {
        return new SubjectTerm(localFolder.getSearchQuery());
      }
    }
  }
}
