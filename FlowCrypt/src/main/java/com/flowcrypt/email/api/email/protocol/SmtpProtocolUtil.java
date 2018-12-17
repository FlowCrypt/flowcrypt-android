/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.protocol;

import android.content.Context;

import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.gmail.GmailConstants;
import com.flowcrypt.email.api.email.model.AuthCredentials;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.google.android.gms.auth.GoogleAuthException;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;

import androidx.annotation.NonNull;

/**
 * This class describes methods for a work with SMTP protocol.
 *
 * @author Denis Bondarenko
 * Date: 15.01.2018
 * Time: 13:12
 * E-mail: DenBond7@gmail.com
 */

public class SmtpProtocolUtil {

  /**
   * Prepare a {@link Transport} for SMTP protocol.
   *
   * @param context Interface to global information about an application environment.
   * @param session The {@link Session} object.
   * @param account The account information which will be used of connection.
   * @return Generated {@link Transport}
   * @throws MessagingException
   * @throws IOException
   * @throws GoogleAuthException
   */
  @NonNull
  public static Transport prepareSmtpTransport(Context context, Session session, AccountDao account) throws
      MessagingException, IOException, GoogleAuthException {
    Transport transport = session.getTransport(JavaEmailConstants.PROTOCOL_SMTP);

    if (account != null) {
      switch (account.getAccountType()) {
        case AccountDao.ACCOUNT_TYPE_GOOGLE:
          if (account.getAccount() != null) {
            String userName = account.getEmail();
            String password = EmailUtil.getGmailAccountToken(context, account.getAccount());
            transport.connect(GmailConstants.GMAIL_SMTP_SERVER, GmailConstants.GMAIL_SMTP_PORT, userName, password);
          } else throw new NullPointerException("The Account can't be a null when we try to receiving a token!");
          break;

        default:
          AuthCredentials authCreds = account.getAuthCreds();
          if (authCreds != null) {
            String userName;
            String password;

            if (authCreds.hasCustomSignInForSmtp()) {
              userName = authCreds.getSmtpSigInUsername();
              password = authCreds.getSmtpSignInPassword();
            } else {
              userName = authCreds.getUsername();
              password = authCreds.getPassword();
            }

            transport.connect(authCreds.getSmtpServer(), authCreds.getSmtpPort(), userName, password);
          } else throw new NullPointerException("The AuthCredentials can't be a null!");
          break;
      }
    } else throw new NullPointerException("The AccountDao can't be a null!");
    return transport;
  }
}
