/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import android.content.Context;
import android.support.annotation.NonNull;

import com.flowcrypt.email.api.email.protocol.SmtpProtocolUtil;
import com.flowcrypt.email.api.email.sync.SyncErrorTypes;
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.google.android.gms.auth.GoogleAuthException;
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
    public void runSMTPAction(AccountDao accountDao, Session session, Store store, SyncListener syncListener)
            throws Exception {
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

    @Override
    public String getOwnerKey() {
        return ownerKey;
    }

    @Override
    public int getRequestCode() {
        return requestCode;
    }

    @Override
    public void runIMAPAction(AccountDao accountDao, Session session, Store store, SyncListener syncListener)
            throws Exception {

    }

    @NonNull
    protected Transport prepareTransportForSmtp(Context context, Session session, AccountDao accountDao) throws
            MessagingException, IOException, GoogleAuthException {
        return SmtpProtocolUtil.prepareTransportForSmtp(context, session, accountDao);
    }
}
