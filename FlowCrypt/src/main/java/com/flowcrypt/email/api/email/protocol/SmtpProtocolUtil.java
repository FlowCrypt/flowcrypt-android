/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.protocol;

import android.content.Context;
import android.support.annotation.NonNull;

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

/**
 * This class describes methods for a work with SMTP protocol.
 *
 * @author Denis Bondarenko
 *         Date: 15.01.2018
 *         Time: 13:12
 *         E-mail: DenBond7@gmail.com
 */

public class SmtpProtocolUtil {

    /**
     * Prepare a {@link Transport} for SMTP protocol.
     *
     * @param context    Interface to global information about an application environment.
     * @param session    The {@link Session} object.
     * @param accountDao The account information which will be used of connection.
     * @return Generated {@link Transport}
     * @throws MessagingException
     * @throws IOException
     * @throws GoogleAuthException
     */
    @NonNull
    public static Transport prepareTransportForSmtp(Context context, Session session, AccountDao accountDao) throws
            MessagingException, IOException, GoogleAuthException {
        Transport transport = session.getTransport(JavaEmailConstants.PROTOCOL_SMTP);

        if (accountDao != null) {
            switch (accountDao.getAccountType()) {
                case AccountDao.ACCOUNT_TYPE_GOOGLE:
                    if (accountDao.getAccount() != null) {
                        transport.connect(GmailConstants.GMAIL_SMTP_SERVER, GmailConstants.GMAIL_SMTP_PORT,
                                accountDao.getEmail(),
                                EmailUtil.getTokenForGmailAccount(context, accountDao.getAccount()));
                    } else throw new NullPointerException("The Account can't be a null when we try to receiving a " +
                            "token!");
                    break;

                default:
                    AuthCredentials authCredentials = accountDao.getAuthCredentials();
                    if (authCredentials != null) {
                        String userName;
                        String password;

                        if (authCredentials.isUseCustomSignInForSmtp()) {
                            userName = authCredentials.getSmtpSigInUsername();
                            password = authCredentials.getSmtpSignInPassword();
                        } else {
                            userName = authCredentials.getUsername();
                            password = authCredentials.getPassword();
                        }

                        transport.connect(authCredentials.getSmtpServer(), authCredentials.getSmtpPort(),
                                userName, password);
                    } else throw new NullPointerException("The AuthCredentials can't be a null!");
                    break;
            }
        } else throw new NullPointerException("The AccountDao can't be a null!");
        return transport;
    }
}
