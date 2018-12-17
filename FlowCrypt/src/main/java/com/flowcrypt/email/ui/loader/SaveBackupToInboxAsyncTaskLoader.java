/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;

import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.api.email.protocol.SmtpProtocolUtil;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.js.core.Js;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.SecurityStorageConnector;
import com.flowcrypt.email.util.exception.ExceptionUtil;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;

import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;

/**
 * This loader can be used for saving a backup of private keys of some account.
 *
 * @author Denis Bondarenko
 * Date: 06.08.2018
 * Time: 17:28
 * E-mail: DenBond7@gmail.com
 */
public class SaveBackupToInboxAsyncTaskLoader extends AsyncTaskLoader<LoaderResult> {
  private final AccountDao account;
  private boolean isActionStarted;
  private LoaderResult data;

  public SaveBackupToInboxAsyncTaskLoader(Context context, AccountDao account) {
    super(context);
    this.account = account;
  }

  @Override
  public void onStartLoading() {
    if (data != null) {
      deliverResult(data);
    } else {
      if (!isActionStarted) {
        forceLoad();
      }
    }
  }

  @Override
  public LoaderResult loadInBackground() {
    isActionStarted = true;
    try {
      Js js = new Js(getContext(), new SecurityStorageConnector(getContext()));
      Session sess = OpenStoreHelper.getAccountSess(getContext(), account);
      Transport transport = SmtpProtocolUtil.prepareSmtpTransport(getContext(), sess, account);
      Message msg = EmailUtil.genMsgWithAllPrivateKeys(getContext(), account, sess, js);
      transport.sendMessage(msg, msg.getAllRecipients());
      return new LoaderResult(true, null);
    } catch (Exception e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
      return new LoaderResult(null, e);
    }
  }

  @Override
  public void deliverResult(@Nullable LoaderResult data) {
    this.data = data;
    super.deliverResult(data);
  }
}
