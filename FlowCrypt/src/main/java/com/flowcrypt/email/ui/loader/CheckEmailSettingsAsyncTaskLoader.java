/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;

import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.model.AuthCredentials;
import com.flowcrypt.email.api.email.protocol.PropertiesHelper;
import com.flowcrypt.email.model.results.LoaderResult;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;

import androidx.loader.content.AsyncTaskLoader;

/**
 * This loader does job of valid {@link AuthCredentials}. If incoming {@link AuthCredentials} is valid then this
 * loader returns {@code true}, otherwise returns false.
 *
 * @author DenBond7
 * Date: 14.09.2017.
 * Time: 15:08.
 * E-mail: DenBond7@gmail.com
 */
public class CheckEmailSettingsAsyncTaskLoader extends AsyncTaskLoader<LoaderResult> {

  private final AuthCredentials authCreds;

  public CheckEmailSettingsAsyncTaskLoader(Context context, AuthCredentials authCreds) {
    super(context);
    this.authCreds = authCreds;
    onContentChanged();
  }

  @Override
  public void onStartLoading() {
    if (takeContentChanged()) {
      forceLoad();
    }
  }

  @Override
  public LoaderResult loadInBackground() {
    Session sess = Session.getInstance(PropertiesHelper.genProps(authCreds));
    sess.setDebug(EmailUtil.hasEnabledDebug(getContext()));

    try {
      testImapConn(sess);
    } catch (Exception e) {
      e.printStackTrace();
      Exception exception = new Exception("IMAP: " + e.getMessage(), e);
      return new LoaderResult(null, exception);
    }

    try {
      testSmtpConn(sess);
    } catch (Exception e) {
      e.printStackTrace();
      Exception exception = new Exception("SMTP: " + e.getMessage(), e);
      return new LoaderResult(null, exception);
    }

    return new LoaderResult(true, null);
  }

  @Override
  public void onStopLoading() {
    cancelLoad();
  }

  /**
   * Trying to connect to an IMAP server. If an exception will occur than that exception will be throw up.
   *
   * @param session The {@link Session} which will be used for the connection.
   * @throws MessagingException This operation can throw some exception.
   */
  private void testImapConn(Session session) throws MessagingException {
    Store store = session.getStore(JavaEmailConstants.PROTOCOL_IMAP);
    store.connect(authCreds.getImapServer(), authCreds.getImapPort(), authCreds.getUsername(), authCreds.getPassword());
    Folder folder = store.getFolder(JavaEmailConstants.FOLDER_INBOX);
    folder.open(Folder.READ_ONLY);
    folder.close(false);
    store.close();
  }

  /**
   * Trying to connect to the SMTP server. If an exception will occur than that exception will be throw up.
   *
   * @param session The {@link Session} which will be used for the connection.
   * @throws MessagingException This operation can throw some exception.
   */
  private void testSmtpConn(Session session) throws MessagingException {
    Transport transport = session.getTransport(JavaEmailConstants.PROTOCOL_SMTP);
    String username;
    String password;

    if (authCreds.hasCustomSignInForSmtp()) {
      username = authCreds.getSmtpSigInUsername();
      password = authCreds.getSmtpSignInPassword();
    } else {
      username = authCreds.getUsername();
      password = authCreds.getPassword();
    }

    transport.connect(authCreds.getSmtpServer(), authCreds.getSmtpPort(), username, password);
    transport.close();
  }
}
