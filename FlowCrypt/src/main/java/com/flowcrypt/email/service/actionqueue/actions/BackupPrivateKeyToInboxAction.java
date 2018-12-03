/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
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
 * Date: 29.01.2018
 * Time: 16:58
 * E-mail: DenBond7@gmail.com
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
  public void run(Context context) throws Exception {
    AccountDao account = new AccountDaoSource().getAccountInformation(context, email);
    SecurityStorageConnector securityStorageConnector = new SecurityStorageConnector(context);
    PgpKeyInfo pgpKeyInfo = securityStorageConnector.getPgpPrivateKey(privateKeyLongId);
    if (account != null && pgpKeyInfo != null && !TextUtils.isEmpty(pgpKeyInfo.getPrivate())) {
      Session session = OpenStoreHelper.getSessionForAccountDao(context, account);
      Transport transport = SmtpProtocolUtil.prepareTransportForSmtp(context, session, account);
      Message message = EmailUtil.genMessageWithPrivateKeys(context, account, session,
          EmailUtil.genBodyPartWithPrivateKey(account, pgpKeyInfo.getPrivate()));
      transport.sendMessage(message, message.getAllRecipients());
    }
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
