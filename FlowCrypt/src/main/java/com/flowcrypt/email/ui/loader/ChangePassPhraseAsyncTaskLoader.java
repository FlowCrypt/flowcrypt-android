/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.ContentProviderResult;
import android.content.Context;
import android.text.TextUtils;

import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor;
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails;
import com.flowcrypt.email.api.retrofit.response.node.DecryptKeyResult;
import com.flowcrypt.email.api.retrofit.response.node.EncryptKeyResult;
import com.flowcrypt.email.database.dao.KeysDao;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.KeysDaoSource;
import com.flowcrypt.email.database.dao.source.UserIdEmailsKeysDaoSource;
import com.flowcrypt.email.js.PgpKeyInfo;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.KeyStoreCryptoManager;
import com.flowcrypt.email.security.SecurityStorageConnector;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.flowcrypt.email.util.exception.NoPrivateKeysAvailableException;
import com.flowcrypt.email.util.exception.NodeException;
import com.google.android.gms.common.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;

/**
 * This loader can be used for changing a pass phrase of private keys of some account.
 *
 * @author Denis Bondarenko
 * Date: 06.08.2018
 * Time: 9:25
 * E-mail: DenBond7@gmail.com
 */
public class ChangePassPhraseAsyncTaskLoader extends AsyncTaskLoader<LoaderResult> {

  private final String newPassphrase;
  private final AccountDao account;
  private boolean isActionStarted;
  private LoaderResult data;

  public ChangePassPhraseAsyncTaskLoader(Context context, AccountDao account, String newPassphrase) {
    super(context);
    this.account = account;
    this.newPassphrase = newPassphrase;
  }

  @Override
  public void onStartLoading() {
    if (data != null) {
      deliverResult(data);
    } else {
      if (!isActionStarted) {
        forceLoad();
      }
    }
  }

  @Override
  public LoaderResult loadInBackground() {
    isActionStarted = true;
    try {
      List<String> longIds = new UserIdEmailsKeysDaoSource().getLongIdsByEmail(getContext(), account.getEmail());

      SecurityStorageConnector storageConnector = new SecurityStorageConnector(getContext());
      PgpKeyInfo[] pgpKeyInfoArray = storageConnector.getFilteredPgpPrivateKeys(longIds.toArray(new String[0]));

      if (pgpKeyInfoArray == null || pgpKeyInfoArray.length == 0) {
        throw new NoPrivateKeysAvailableException(getContext(), account.getEmail());
      }

      KeyStoreCryptoManager keyStoreCryptoManager = new KeyStoreCryptoManager(getContext());
      List<KeysDao> keysDaoList = new ArrayList<>();

      for (PgpKeyInfo pgpKeyInfo : pgpKeyInfoArray) {
        String passphrase = storageConnector.getPassphrase(pgpKeyInfo.getLongid());
        NodeKeyDetails modifiedNodeKeyDetails = getModifiedNodeKeyDetails(passphrase, pgpKeyInfo.getPrivate());
        keysDaoList.add(KeysDao.generateKeysDao(keyStoreCryptoManager, modifiedNodeKeyDetails, newPassphrase));
      }

      ContentProviderResult[] contentProviderResults = new KeysDaoSource().updateKeys(getContext(), keysDaoList);

      if (contentProviderResults == null || contentProviderResults.length == 0) {
        throw new IllegalArgumentException("An error occurred during changing passphrases");
      }

      for (ContentProviderResult contentProviderResult : contentProviderResults) {
        if (contentProviderResult.count < 1) {
          throw new IllegalArgumentException("An error occurred when we tried update " + contentProviderResult.uri);
        }
      }

      return new LoaderResult(true, null);
    } catch (Exception e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
      return new LoaderResult(null, e);
    }
  }

  @Override
  public void deliverResult(@Nullable LoaderResult data) {
    this.data = data;
    super.deliverResult(data);
  }

  private NodeKeyDetails getModifiedNodeKeyDetails(String passphrase, String originalPrivateKey)
      throws IOException, NodeException {
    List<NodeKeyDetails> keyDetailsList = NodeCallsExecutor.parseKeys(originalPrivateKey);
    if (CollectionUtils.isEmpty(keyDetailsList) || keyDetailsList.size() != 1) {
      throw new IllegalStateException("Parse keys error");
    }

    NodeKeyDetails nodeKeyDetails = keyDetailsList.get(0);
    String longId = nodeKeyDetails.getLongId();

    if (TextUtils.isEmpty(passphrase)) {
      throw new IllegalStateException("Passphrase for key with longid " + longId + " not found");
    }

    DecryptKeyResult decryptResult = NodeCallsExecutor.decryptKey(nodeKeyDetails.getPrivateKey(), passphrase);

    if (TextUtils.isEmpty(decryptResult.getDecryptedKey())) {
      throw new IllegalStateException("Can't decrypt key with longid " + longId);
    }

    EncryptKeyResult encryptResult = NodeCallsExecutor.encryptKey(decryptResult.getDecryptedKey(), newPassphrase);

    if (TextUtils.isEmpty(encryptResult.getEncryptedKey())) {
      throw new IllegalStateException("Can't encrypt key with longid " + longId);
    }

    List<NodeKeyDetails> modifiedKeyDetailsList = NodeCallsExecutor.parseKeys(encryptResult.getEncryptedKey());
    if (CollectionUtils.isEmpty(modifiedKeyDetailsList) || modifiedKeyDetailsList.size() != 1) {
      throw new IllegalStateException("Parse keys error");
    }

    return modifiedKeyDetailsList.get(0);
  }
}
