/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import android.content.Context;
import android.os.Messenger;
import android.text.TextUtils;

import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.SearchBackupsUtil;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor;
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.flowcrypt.email.util.exception.NodeException;
import com.google.android.gms.auth.GoogleAuthException;
import com.sun.mail.imap.IMAPFolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

/**
 * This task load the private keys from the email INBOX folder.
 *
 * @author DenBond7
 * Date: 05.07.2017
 * Time: 10:27
 * E-mail: DenBond7@gmail.com
 */

public class LoadPrivateKeysFromEmailBackupSyncTask extends BaseSyncTask {
  /**
   * The base constructor.
   *
   * @param ownerKey    The name of the reply to {@link Messenger}.
   * @param requestCode The unique request code for the reply to {@link Messenger}.
   */
  public LoadPrivateKeysFromEmailBackupSyncTask(String ownerKey, int requestCode) {
    super(ownerKey, requestCode);
  }

  @Override
  public void runIMAPAction(AccountDao account, Session session, Store store, SyncListener listener) throws Exception {
    super.runIMAPAction(account, session, store, listener);

    if (listener != null) {
      Context context = listener.getContext();
      ArrayList<NodeKeyDetails> keyDetailsList = new ArrayList<>();

      switch (account.getAccountType()) {
        case AccountDao.ACCOUNT_TYPE_GOOGLE:
          keyDetailsList.addAll(EmailUtil.getPrivateKeyBackupsViaGmailAPI(context, account, session));
          break;

        default:
          keyDetailsList.addAll(getPrivateKeyBackupsUsingJavaMailAPI(context, account, session));
          break;
      }

      listener.onPrivateKeysFound(account, keyDetailsList, ownerKey, requestCode);
    }
  }

  /**
   * Get a list of {@link NodeKeyDetails} using the standard <b>JavaMail API</b>
   *
   * @param session A {@link Session} object.
   * @return A list of {@link NodeKeyDetails}
   * @throws MessagingException
   * @throws IOException
   * @throws GoogleAuthException
   */
  private Collection<? extends NodeKeyDetails> getPrivateKeyBackupsUsingJavaMailAPI(Context context, AccountDao account,
                                                                                    Session session)
      throws MessagingException, IOException, GoogleAuthException {
    ArrayList<NodeKeyDetails> keyDetailsList = new ArrayList<>();
    Store store = null;
    try {
      store = OpenStoreHelper.openStore(context, account, session);
      Folder[] folders = store.getDefaultFolder().list("*");

      for (Folder folder : folders) {
        if (!EmailUtil.containsNoSelectAttr((IMAPFolder) folder)) {
          folder.open(Folder.READ_ONLY);

          Message[] foundMsgs = folder.search(SearchBackupsUtil.genSearchTerms(account.getEmail()));
          for (Message message : foundMsgs) {
            String backup = EmailUtil.getKeyFromMimeMsg(message);

            if (TextUtils.isEmpty(backup)) {
              continue;
            }

            try {
              keyDetailsList.addAll(NodeCallsExecutor.parseKeys(backup));
            } catch (NodeException e) {
              e.printStackTrace();
              ExceptionUtil.handleError(e);
            }
          }

          folder.close(false);
        }
      }

      store.close();
    } catch (MessagingException | IOException | GoogleAuthException e) {
      e.printStackTrace();
      if (store != null) {
        store.close();
      }
      throw e;
    }
    return keyDetailsList;
  }
}
