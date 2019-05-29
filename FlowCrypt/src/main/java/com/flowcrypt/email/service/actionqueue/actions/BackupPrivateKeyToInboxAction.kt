/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.actionqueue.actions;

import android.content.Context;
import android.os.Parcel;
import android.text.TextUtils;

import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.api.email.protocol.SmtpProtocolUtil;
import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor;
import com.flowcrypt.email.api.retrofit.response.node.EncryptKeyResult;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.model.PgpKeyInfo;
import com.flowcrypt.email.security.KeysStorageImpl;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeBodyPart;

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
    AccountDao account = new AccountDaoSource().getAccountInformation(context, getEmail());
    KeysStorageImpl keysStorage = KeysStorageImpl.getInstance(context);
    PgpKeyInfo pgpKeyInfo = keysStorage.getPgpPrivateKey(privateKeyLongId);
    if (account != null && pgpKeyInfo != null && !TextUtils.isEmpty(pgpKeyInfo.getPrivate())) {
      Session session = OpenStoreHelper.getAccountSess(context, account);
      Transport transport = SmtpProtocolUtil.prepareSmtpTransport(context, session, account);

      EncryptKeyResult encryptKeyResult = NodeCallsExecutor.encryptKey(pgpKeyInfo.getPrivate(),
          keysStorage.getPassphrase(privateKeyLongId));

      if (TextUtils.isEmpty(encryptKeyResult.getEncryptedKey())) {
        throw new IllegalStateException("An error occurred during encrypting some key");
      }

      MimeBodyPart mimeBodyPart = EmailUtil.genBodyPartWithPrivateKey(account, encryptKeyResult.getEncryptedKey());
      Message message = EmailUtil.genMsgWithPrivateKeys(context, account, session, mimeBodyPart);
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
