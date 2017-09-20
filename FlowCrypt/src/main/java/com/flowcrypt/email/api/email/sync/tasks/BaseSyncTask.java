/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import android.accounts.Account;
import android.content.Context;
import android.support.annotation.NonNull;

import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.gmail.GmailConstants;
import com.flowcrypt.email.api.email.model.AuthCredentials;
import com.flowcrypt.email.api.email.sync.SyncErrorTypes;
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.sun.mail.util.MailConnectException;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;

/**
 * The base realization of {@link SyncTask}.
 *
 * @author DenBond7
 *         Date: 23.06.2017
 *         Time: 15:59
 *         E-mail: DenBond7@gmail.com
 */

abstract class BaseSyncTask implements SyncTask {
    String ownerKey;
    int requestCode;

    /**
     * The base constructor.
     *
     * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
     * @param requestCode The unique request code for the reply to {@link android.os.Messenger}.
     */
    BaseSyncTask(String ownerKey, int requestCode) {
        this.ownerKey = ownerKey;
        this.requestCode = requestCode;
    }

    @Override
    public boolean isUseSMTP() {
        return false;
    }

    @Override
    public void runIMAPAction(AccountDao accountDao, Store store, SyncListener syncListener) throws Exception {

    }

    @Override
    public void runSMTPAction(AccountDao accountDao, Session session, SyncListener syncListener) throws Exception {
    }

    @Override
    public void handleException(AccountDao accountDao, Exception e, SyncListener syncListener) {
        if (syncListener != null) {
            int errorType;

            if (e instanceof MailConnectException) {
                errorType = SyncErrorTypes.CONNECTION_TO_STORE_IS_LOST;
            } else {
                errorType = SyncErrorTypes.TASK_RUNNING_ERROR;
            }

            syncListener.onError(accountDao, errorType, e, ownerKey, requestCode);
        }
    }

    public String getOwnerKey() {
        return ownerKey;
    }

    public int getRequestCode() {
        return requestCode;
    }

    @NonNull
    protected Transport prepareTransportForSmtp(Context context, Session session, AccountDao accountDao) throws
            MessagingException, IOException, GoogleAuthException {
        Transport transport = session.getTransport(JavaEmailConstants.PROTOCOL_SMTP);

        if (accountDao != null) {
            switch (accountDao.getAccountType()) {
                case AccountDao.ACCOUNT_TYPE_GOOGLE:
                    if (accountDao.getAccount() != null) {
                        transport.connect(GmailConstants.GMAIL_SMTP_SERVER, GmailConstants.GMAIL_SMTP_PORT,
                                accountDao.getEmail(), getTokenForGmailAccount(context, accountDao.getAccount()));
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

    /**
     * Get a valid OAuth2 token for some {@link Account}. Must be called on the non-UI thread.
     *
     * @return A new valid OAuth2 token;
     * @throws IOException         Signaling a transient error (typically network related). It is left to clients to
     *                             implement a backoff/abandonment strategy appropriate to their latency requirements.
     * @throws GoogleAuthException Signaling an unrecoverable authentication error. These errors will typically
     *                             result from client errors (e.g. providing an invalid scope).
     */
    protected String getTokenForGmailAccount(Context context, @NonNull Account account) throws IOException,
            GoogleAuthException {
        return GoogleAuthUtil.getToken(context, account, JavaEmailConstants.OAUTH2
                + GmailConstants.SCOPE_MAIL_GOOGLE_COM);
    }
}
