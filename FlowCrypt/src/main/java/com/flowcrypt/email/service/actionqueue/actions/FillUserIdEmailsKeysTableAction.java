/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.actionqueue.actions;

import android.content.Context;
import android.os.Parcel;
import android.util.Pair;

import com.flowcrypt.email.database.dao.source.UserIdEmailsKeysDaoSource;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.js.PgpKeyInfo;
import com.flowcrypt.email.js.core.Js;
import com.flowcrypt.email.security.SecurityStorageConnector;

import java.util.ArrayList;
import java.util.List;

/**
 * This action describes a task which fills the table
 * {@link UserIdEmailsKeysDaoSource#TABLE_NAME_USER_ID_EMAILS_AND_KEYS}.
 *
 * @author Denis Bondarenko
 * Date: 31.07.2018
 * Time: 10:25
 * E-mail: DenBond7@gmail.com
 */
public class FillUserIdEmailsKeysTableAction extends Action {
  public static final Creator<FillUserIdEmailsKeysTableAction> CREATOR = new
      Creator<FillUserIdEmailsKeysTableAction>() {
        @Override
        public FillUserIdEmailsKeysTableAction createFromParcel(Parcel source) {
          return new FillUserIdEmailsKeysTableAction(source);
        }

        @Override
        public FillUserIdEmailsKeysTableAction[] newArray(int size) {
          return new FillUserIdEmailsKeysTableAction[size];
        }
      };

  public FillUserIdEmailsKeysTableAction() {
    super(USER_SYSTEM, ActionType.FILL_USER_ID_EMAILS_KEYS_TABLE);
  }


  protected FillUserIdEmailsKeysTableAction(Parcel in) {
    super(in);
  }

  @Override
  public void run(Context context) throws Exception {
    SecurityStorageConnector securityStorageConnector = new SecurityStorageConnector(context);
    Js js = new Js(context, securityStorageConnector);
    List<Pair<String, String>> pairs = new ArrayList<>();

    PgpKeyInfo[] pgpKeyInfoArray = securityStorageConnector.getAllPgpPrivateKeys();
    for (PgpKeyInfo pgpKeyInfo : pgpKeyInfoArray) {
      PgpKey pgpKey = js.crypto_key_read(pgpKeyInfo.getPrivate());
      if (pgpKey != null) {
        PgpContact[] pgpContacts = pgpKey.getUserIds();
        if (pgpContacts != null) {
          for (PgpContact pgpContact : pgpContacts) {
            if (pgpContact != null) {
              pairs.add(Pair.create(pgpKey.getLongid(), pgpContact.getEmail()));
            }
          }
        }
      }
    }

    UserIdEmailsKeysDaoSource userIdEmailsKeysDaoSource = new UserIdEmailsKeysDaoSource();

    for (Pair<String, String> pair : pairs) {
      userIdEmailsKeysDaoSource.addRow(context, pair.first, pair.second);
    }
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
  }
}
