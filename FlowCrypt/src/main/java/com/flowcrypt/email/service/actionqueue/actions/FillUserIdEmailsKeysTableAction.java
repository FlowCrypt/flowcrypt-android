/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.actionqueue.actions;

import android.content.Context;
import android.os.Parcel;
import android.util.Pair;

import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor;
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails;
import com.flowcrypt.email.database.dao.source.UserIdEmailsKeysDaoSource;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.js.PgpKeyInfo;
import com.flowcrypt.email.security.SecurityStorageConnector;
import com.google.android.gms.common.util.CollectionUtils;

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
  public static final Creator<FillUserIdEmailsKeysTableAction> CREATOR =
      new Creator<FillUserIdEmailsKeysTableAction>() {
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
    SecurityStorageConnector connector = new SecurityStorageConnector(context);

    List<Pair<String, String>> pairs = new ArrayList<>();

    PgpKeyInfo[] pgpKeyInfoArray = connector.getAllPgpPrivateKeys();
    for (PgpKeyInfo pgpKeyInfo : pgpKeyInfoArray) {
      List<NodeKeyDetails> nodeKeyDetailsList = NodeCallsExecutor.parseKeys(pgpKeyInfo.getPrivate());

      if (!CollectionUtils.isEmpty(nodeKeyDetailsList)) {
        for (NodeKeyDetails nodeKeyDetails : nodeKeyDetailsList) {
          List<PgpContact> pgpContacts = nodeKeyDetails.getPgpContacts();
          if (!CollectionUtils.isEmpty(pgpContacts)) {
            for (PgpContact pgpContact : pgpContacts) {
              if (pgpContact != null) {
                pairs.add(Pair.create(nodeKeyDetails.getLongId(), pgpContact.getEmail()));
              }
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
