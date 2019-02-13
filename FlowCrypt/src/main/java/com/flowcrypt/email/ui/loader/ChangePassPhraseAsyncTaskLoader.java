/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.ContentProviderResult;
import android.content.Context;

import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor;
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails;
import com.flowcrypt.email.database.dao.KeysDao;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.KeysDaoSource;
import com.flowcrypt.email.database.dao.source.UserIdEmailsKeysDaoSource;
import com.flowcrypt.email.js.PgpKeyInfo;
import com.flowcrypt.email.js.UiJsManager;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.KeyStoreCryptoManager;
import com.flowcrypt.email.security.SecurityStorageConnector;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.flowcrypt.email.util.exception.NoPrivateKeysAvailableException;
import com.google.android.gms.common.util.CollectionUtils;

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

      SecurityStorageConnector storageConnector = UiJsManager.getInstance(getContext()).getSecurityStorageConnector();
      PgpKeyInfo[] pgpKeyInfoArray = storageConnector.getFilteredPgpPrivateKeys(longIds.toArray(new String[0]));

      if (pgpKeyInfoArray == null || pgpKeyInfoArray.length == 0) {
        throw new NoPrivateKeysAvailableException(getContext(), account.getEmail());
      }

      KeyStoreCryptoManager keyStoreCryptoManager = new KeyStoreCryptoManager(getContext());
      List<KeysDao> keysDaoList = new ArrayList<>();

      for (PgpKeyInfo pgpKeyInfo : pgpKeyInfoArray) {
        List<NodeKeyDetails> nodeKeyDetails = NodeCallsExecutor.parseKeys(pgpKeyInfo.getPrivate());
        if (CollectionUtils.isEmpty(nodeKeyDetails) || nodeKeyDetails.size() != 1) {
          throw new IllegalStateException("Parse keys error");
        }
        keysDaoList.add(KeysDao.generateKeysDao(keyStoreCryptoManager, nodeKeyDetails.get(0), newPassphrase));
      }

      ContentProviderResult[] contentProviderResults = new KeysDaoSource().updateKeys(getContext(), keysDaoList);

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
}
