/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;
import android.text.TextUtils;

import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.SearchBackupsUtil;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor;
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.model.results.LoaderResult;
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

import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;

/**
 * This loader finds and returns a user backup of private keys from the mail.
 *
 * @author DenBond7
 * Date: 30.04.2017.
 * Time: 22:28.
 * E-mail: DenBond7@gmail.com
 */
public class LoadPrivateKeysFromMailAsyncTaskLoader extends AsyncTaskLoader<LoaderResult> {

  /**
   * An user account.
   */
  private AccountDao account;
  private LoaderResult data;
  private boolean isActionStarted;
  private boolean isLoaderReset;

  public LoadPrivateKeysFromMailAsyncTaskLoader(Context context, AccountDao account) {
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
    ArrayList<NodeKeyDetails> privateKeyDetailsList = new ArrayList<>();

    try {
      Session session = OpenStoreHelper.getAccountSess(getContext(), account);

      switch (account.getAccountType()) {
        case AccountDao.ACCOUNT_TYPE_GOOGLE:
          privateKeyDetailsList.addAll(EmailUtil.getPrivateKeyBackupsViaGmailAPI(getContext(), account, session));
          break;

        default:
          privateKeyDetailsList.addAll(getPrivateKeyBackupsUsingJavaMailAPI(session));
          break;
      }
      return new LoaderResult(privateKeyDetailsList, null);
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

  @Override
  protected void onReset() {
    super.onReset();
    this.isLoaderReset = true;
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
  private Collection<? extends NodeKeyDetails> getPrivateKeyBackupsUsingJavaMailAPI(Session session)
      throws MessagingException, IOException, GoogleAuthException {
    ArrayList<NodeKeyDetails> details = new ArrayList<>();
    Store store = null;
    try {
      store = OpenStoreHelper.openStore(getContext(), account, session);
      Folder[] folders = store.getDefaultFolder().list("*");

      for (Folder folder : folders) {
        boolean containsNoSelectAttr = EmailUtil.containsNoSelectAttr((IMAPFolder) folder);
        if (!isLoadInBackgroundCanceled() && !isLoaderReset && !containsNoSelectAttr) {
          folder.open(Folder.READ_ONLY);

          Message[] foundMsgs = folder.search(SearchBackupsUtil.genSearchTerms(account.getEmail()));

          for (Message message : foundMsgs) {
            String backup = EmailUtil.getKeyFromMimeMsg(message);

            if (TextUtils.isEmpty(backup)) {
              continue;
            }

            try {
              details.addAll(NodeCallsExecutor.parseKeys(backup));
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
    return details;
  }
}
