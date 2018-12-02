/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import android.os.Messenger;

import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.security.SecurityStorageConnector;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;

/**
 * This task send a message with backup to the key owner.
 *
 * @author DenBond7
 * Date: 05.07.2017
 * Time: 14:08
 * E-mail: DenBond7@gmail.com
 */

public class SendMessageWithBackupToKeyOwnerSynsTask extends BaseSyncTask {
  /**
   * The base constructor.
   *
   * @param ownerKey    The name of the reply to {@link Messenger}.
   * @param requestCode The unique request code for the reply to {@link Messenger}.
   */
  public SendMessageWithBackupToKeyOwnerSynsTask(String ownerKey, int requestCode) {
    super(ownerKey, requestCode);
  }

  @Override
  public boolean isSMTPRequired() {
    return true;
  }

  @Override
  public void runSMTPAction(AccountDao account, Session session, Store store, SyncListener listener) throws Exception {
    super.runSMTPAction(account, session, store, listener);

    if (listener != null && account != null) {
      Transport transport = prepareTransportForSmtp(listener.getContext(), session, account);
      Js js = new Js(listener.getContext(), new SecurityStorageConnector(listener.getContext()));
      Message message = EmailUtil.generateMessageWithAllPrivateKeysBackups(listener.getContext(), account, session, js);
      transport.sendMessage(message, message.getAllRecipients());

      listener.onMessageWithBackupToKeyOwnerSent(account, ownerKey, requestCode, true);
    }
  }
}
