/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.actionqueue.actions;

import android.content.Context;
import android.os.Parcel;
import android.text.TextUtils;

import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.api.email.protocol.SmtpProtocolUtil;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.js.PgpKeyInfo;
import com.flowcrypt.email.security.SecurityStorageConnector;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;

/**
 * This action describes a task which backups a private key to INBOX.
 *
 * @author Denis Bondarenko
 *         Date: 29.01.2018
 *         Time: 16:58
 *         E-mail: DenBond7@gmail.com
 */

public class BackupPrivateKeyToInboxAction extends Action {
    public static final Creator<BackupPrivateKeyToInboxAction> CREATOR = new Creator<BackupPrivateKeyToInboxAction>() {
        @Override
        public BackupPrivateKeyToInboxAction createFromParcel(Parcel source) {
            return new BackupPrivateKeyToInboxAction(source);
        }

        @Override
        public BackupPrivateKeyToInboxAction[] newArray(int size) {
            return new BackupPrivateKeyToInboxAction[size];
        }
    };

    private String privateKeyLongId;

    public BackupPrivateKeyToInboxAction(String email, String privateKeyLongId) {
        super(email, ActionType.BACKUP_PRIVATE_KEY_TO_INBOX);
        this.privateKeyLongId = privateKeyLongId;
    }


    protected BackupPrivateKeyToInboxAction(Parcel in) {
        super(in);
        this.privateKeyLongId = in.readString();
    }

    @Override
    public boolean run(Context context) {
        AccountDao accountDao = new AccountDaoSource().getAccountInformation(context, email);
        SecurityStorageConnector securityStorageConnector = new SecurityStorageConnector(context);
        PgpKeyInfo pgpKeyInfo = securityStorageConnector.getPgpPrivateKey(privateKeyLongId);
        if (accountDao != null && pgpKeyInfo != null && !TextUtils.isEmpty(pgpKeyInfo.getPrivate())) {
            try {
                Session session = OpenStoreHelper.getSessionForAccountDao(context, accountDao);
                Transport transport = SmtpProtocolUtil.prepareTransportForSmtp(context, session, accountDao);
                Message message = EmailUtil.generateMessageWithPrivateKeysBackup(context, accountDao.getEmail(),
                        session, EmailUtil.generateAttachmentBodyPartWithPrivateKey(
                                accountDao.getEmail(), pgpKeyInfo.getPrivate(), -1));
                transport.sendMessage(message, message.getAllRecipients());
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.privateKeyLongId);
    }
}
