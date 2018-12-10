/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
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
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.js.MessageBlock;
import com.flowcrypt.email.js.core.Js;
import com.flowcrypt.email.model.KeyDetails;
import com.google.android.gms.auth.GoogleAuthException;
import com.sun.mail.imap.IMAPFolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
      ArrayList<KeyDetails> keyDetailsList = new ArrayList<>();
      List<String> keys = new ArrayList<>();

      Js js = new Js(context, null);

      switch (account.getAccountType()) {
        case AccountDao.ACCOUNT_TYPE_GOOGLE:
          keyDetailsList.addAll(EmailUtil.getPrivateKeyBackupsViaGmailAPI(context, account, session, js));
          break;

        default:
          keyDetailsList.addAll(getPrivateKeyBackupsUsingJavaMailAPI(context, account, session, js));
          break;
      }

      for (KeyDetails keyDetails : keyDetailsList) {
        keys.add(keyDetails.getValue());
      }

      listener.onPrivateKeysFound(account, keys, ownerKey, requestCode);
    }
  }

  /**
   * Get a list of {@link KeyDetails} using the standard <b>JavaMail API</b>
   *
   * @param session A {@link Session} object.
   * @param js      An instance of {@link Js}
   * @return A list of {@link KeyDetails}
   * @throws MessagingException
   * @throws IOException
   * @throws GoogleAuthException
   */
  private Collection<? extends KeyDetails> getPrivateKeyBackupsUsingJavaMailAPI(Context context,
                                                                                AccountDao account,
                                                                                Session session, Js js)
      throws MessagingException, IOException, GoogleAuthException {
    ArrayList<KeyDetails> keyDetailsList = new ArrayList<>();
    Store store = null;
    try {
      store = OpenStoreHelper.openStore(context, account, session);
      Folder[] folders = store.getDefaultFolder().list("*");

      for (Folder folder : folders) {
        if (!EmailUtil.containsNoSelectAttribute((IMAPFolder) folder)) {
          folder.open(Folder.READ_ONLY);

          Message[] foundMsgs = folder.search(SearchBackupsUtil.genSearchTerms(account.getEmail()));
          for (Message message : foundMsgs) {
            String backup = EmailUtil.getKeyFromMimeMsg(message);

            if (TextUtils.isEmpty(backup)) {
              continue;
            }

            MessageBlock[] msgBlocks = js.crypto_armor_detect_blocks(backup);
            keyDetailsList = getDetails(msgBlocks);
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

  private ArrayList<KeyDetails> getDetails(MessageBlock[] msgBlocks) {
    ArrayList<KeyDetails> keyDetailsList = new ArrayList<>();
    for (MessageBlock messageBlock : msgBlocks) {
      if (MessageBlock.TYPE_PGP_PRIVATE_KEY.equalsIgnoreCase(messageBlock.getType())) {
        String content = messageBlock.getContent();
        boolean isContentEmpty = TextUtils.isEmpty(content);
        if (!isContentEmpty && !EmailUtil.containsKey(keyDetailsList, content)) {
          keyDetailsList.add(new KeyDetails(messageBlock.getContent(), KeyDetails.Type.EMAIL));
        }
      }
    }

    return keyDetailsList;
  }
}
