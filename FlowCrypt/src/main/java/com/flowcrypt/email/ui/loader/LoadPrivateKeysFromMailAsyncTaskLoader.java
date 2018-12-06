/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;
import android.text.TextUtils;

import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.SearchBackupsUtil;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.js.MessageBlock;
import com.flowcrypt.email.js.core.Js;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.SecurityStorageConnector;
import com.flowcrypt.email.util.exception.ExceptionUtil;
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
    ArrayList<KeyDetails> privateKeyDetailsList = new ArrayList<>();

    try {
      Js js = new Js(getContext(), new SecurityStorageConnector(getContext()));

      Session session = OpenStoreHelper.getSessionForAccountDao(getContext(), account);

      switch (account.getAccountType()) {
        case AccountDao.ACCOUNT_TYPE_GOOGLE:
          privateKeyDetailsList.addAll(EmailUtil.getPrivateKeyBackupsViaGmailAPI(getContext(), account, session, js));
          break;

        default:
          privateKeyDetailsList.addAll(getPrivateKeyBackupsUsingJavaMailAPI(session, js));
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
   * Get a list of {@link KeyDetails} using the standard <b>JavaMail API</b>
   *
   * @param session A {@link Session} object.
   * @param js      An instance of {@link Js}
   * @return A list of {@link KeyDetails}
   * @throws MessagingException
   * @throws IOException
   * @throws GoogleAuthException
   */
  private Collection<? extends KeyDetails> getPrivateKeyBackupsUsingJavaMailAPI(Session session, Js js)
      throws MessagingException, IOException, GoogleAuthException {
    ArrayList<KeyDetails> details = new ArrayList<>();
    Store store = null;
    try {
      store = OpenStoreHelper.openAndConnectToStore(getContext(), account, session);
      Folder[] folders = store.getDefaultFolder().list("*");

      for (Folder folder : folders) {
        boolean containsNoSelectAttr = EmailUtil.containsNoSelectAttribute((IMAPFolder) folder);
        if (!isLoadInBackgroundCanceled() && !isLoaderReset && !containsNoSelectAttr) {
          folder.open(Folder.READ_ONLY);

          Message[] foundMsgs = folder.search(SearchBackupsUtil.genSearchTerms(account.getEmail()));

          for (Message message : foundMsgs) {
            String backup = EmailUtil.getKeyFromMimeMessage(message);

            if (TextUtils.isEmpty(backup)) {
              continue;
            }

            MessageBlock[] messageBlocks = js.crypto_armor_detect_blocks(backup);

            for (MessageBlock messageBlock : messageBlocks) {
              if (MessageBlock.TYPE_PGP_PRIVATE_KEY.equalsIgnoreCase(messageBlock.getType())) {
                boolean isExist = EmailUtil.containsKey(details, messageBlock.getContent());
                if (!TextUtils.isEmpty(messageBlock.getContent()) && !isExist) {
                  details.add(new KeyDetails(messageBlock.getContent(), KeyDetails.Type.EMAIL));
                }
              }
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
