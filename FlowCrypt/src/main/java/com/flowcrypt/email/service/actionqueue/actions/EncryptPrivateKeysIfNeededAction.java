/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.actionqueue.actions;

import android.content.ContentProviderResult;
import android.content.Context;
import android.os.Parcel;
import android.text.TextUtils;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor;
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails;
import com.flowcrypt.email.api.retrofit.response.node.EncryptKeyResult;
import com.flowcrypt.email.broadcastreceivers.UpdateStorageConnectorBroadcastReceiver;
import com.flowcrypt.email.database.dao.KeysDao;
import com.flowcrypt.email.database.dao.source.KeysDaoSource;
import com.flowcrypt.email.model.PgpKeyInfo;
import com.flowcrypt.email.security.KeyStoreCryptoManager;
import com.flowcrypt.email.security.KeysStorageImpl;
import com.flowcrypt.email.util.SharedPreferencesHelper;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.google.android.gms.common.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.preference.PreferenceManager;

/**
 * This {@link Action} checks all available private keys are they encrypted. If not we will try to encrypt a key and
 * save to the local database.
 *
 * @author Denis Bondarenko
 * Date: 2/25/19
 * Time: 4:03 PM
 * E-mail: DenBond7@gmail.com
 */
public class EncryptPrivateKeysIfNeededAction extends Action {
  public static final Creator<EncryptPrivateKeysIfNeededAction> CREATOR =
      new Creator<EncryptPrivateKeysIfNeededAction>() {
        @Override
        public EncryptPrivateKeysIfNeededAction createFromParcel(Parcel source) {
          return new EncryptPrivateKeysIfNeededAction(source);
        }

        @Override
        public EncryptPrivateKeysIfNeededAction[] newArray(int size) {
          return new EncryptPrivateKeysIfNeededAction[size];
        }
      };

  public EncryptPrivateKeysIfNeededAction(String email) {
    super(email, ActionType.ENCRYPT_PRIVATE_KEYS);
  }

  protected EncryptPrivateKeysIfNeededAction(Parcel in) {
    super(in);
  }

  @Override
  public void run(Context context) throws Exception {
    super.run(context);

    KeysStorageImpl keysStore = KeysStorageImpl.getInstance(context);
    List<PgpKeyInfo> pgpKeyInfoList = keysStore.getAllPgpPrivateKeys();

    if (CollectionUtils.isEmpty(pgpKeyInfoList)) {
      return;
    }

    KeyStoreCryptoManager keyStoreCryptoManager = KeyStoreCryptoManager.getInstance(context);
    List<KeysDao> keysDaoList = new ArrayList<>();

    for (PgpKeyInfo pgpKeyInfo : pgpKeyInfoList) {
      String passphrase = keysStore.getPassphrase(pgpKeyInfo.getLongid());

      if (TextUtils.isEmpty(passphrase)) {
        continue;
      }

      List<NodeKeyDetails> keyDetailsList = NodeCallsExecutor.parseKeys(pgpKeyInfo.getPrivate());
      if (CollectionUtils.isEmpty(keyDetailsList) || keyDetailsList.size() != 1) {
        ExceptionUtil.handleError(new IllegalArgumentException("An error occurred during the key parsing| 1: " +
            (CollectionUtils.isEmpty(keyDetailsList) ? "Empty results" : "Size = " + keyDetailsList.size())));
        continue;
      }

      NodeKeyDetails nodeKeyDetails = keyDetailsList.get(0);

      if (!nodeKeyDetails.isDecrypted()) {
        continue;
      }

      EncryptKeyResult encryptResult = NodeCallsExecutor.encryptKey(nodeKeyDetails.getPrivateKey(), passphrase);

      if (TextUtils.isEmpty(encryptResult.getEncryptedKey())) {
        ExceptionUtil.handleError(new IllegalArgumentException("An error occurred during the key encryption"));
        continue;
      }

      List<NodeKeyDetails> modifiedKeyDetailsList = NodeCallsExecutor.parseKeys(encryptResult.getEncryptedKey());
      if (CollectionUtils.isEmpty(modifiedKeyDetailsList) || modifiedKeyDetailsList.size() != 1) {
        ExceptionUtil.handleError(new IllegalArgumentException("An error occurred during the key parsing| 2"));
        continue;
      }

      keysDaoList.add(KeysDao.generateKeysDao(keyStoreCryptoManager, modifiedKeyDetailsList.get(0), passphrase));
    }

    if (keysDaoList.size() > 0) {
      ContentProviderResult[] contentProviderResults = new KeysDaoSource().updateKeys(context, keysDaoList);

      if (contentProviderResults == null || contentProviderResults.length == 0) {
        throw new IllegalArgumentException("An error occurred during saving changes");
      }

      for (ContentProviderResult contentProviderResult : contentProviderResults) {
        if (contentProviderResult.count < 1) {
          throw new IllegalArgumentException("An error occurred when we tried update " + contentProviderResult.uri);
        }
      }

      context.sendBroadcast(UpdateStorageConnectorBroadcastReceiver.newIntent(context));
    }

    SharedPreferencesHelper.setBoolean(PreferenceManager
        .getDefaultSharedPreferences(context), Constants.PREFERENCES_KEY_IS_CHECK_KEYS_NEEDED, false);
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
