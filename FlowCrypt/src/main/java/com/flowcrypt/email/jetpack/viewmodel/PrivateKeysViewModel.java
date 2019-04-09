/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel;

import android.app.Application;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.retrofit.node.PgpApiRepository;
import com.flowcrypt.email.model.PgpKeyInfo;
import com.flowcrypt.email.security.KeysStorageImpl;
import com.google.android.gms.common.util.CollectionUtils;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

/**
 * This {@link ViewModel} implementation can be used to fetch details about imported keys.
 *
 * @author Denis Bondarenko
 * Date: 2/14/19
 * Time: 10:50 AM
 * E-mail: DenBond7@gmail.com
 */
public class PrivateKeysViewModel extends BaseNodeApiViewModel implements KeysStorageImpl.OnRefreshListener {
  private KeysStorageImpl keysStorage;
  private PgpApiRepository apiRepository;

  public PrivateKeysViewModel(@NonNull Application application) {
    super(application);
  }

  @Override
  public void onRefresh() {
    checkAndFetchKeys();
  }

  public void init(PgpApiRepository apiRepository) {
    this.apiRepository = apiRepository;
    this.keysStorage = KeysStorageImpl.getInstance(getApplication());
    this.keysStorage.attachOnRefreshListener(this);
    checkAndFetchKeys();
  }

  private void fetchKeys(String rawKey) {
    apiRepository.fetchKeyDetails(R.id.live_data_id_fetch_keys, responsesLiveData, rawKey);
  }

  private void checkAndFetchKeys() {
    List<PgpKeyInfo> pgpKeyInfoList = keysStorage.getAllPgpPrivateKeys();
    if (!CollectionUtils.isEmpty(pgpKeyInfoList)) {
      StringBuilder builder = new StringBuilder();
      for (PgpKeyInfo pgpKeyInfo : pgpKeyInfoList) {
        builder.append(pgpKeyInfo.getPrivate()).append("\n");
      }

      fetchKeys(builder.toString());
    } else {
      fetchKeys(null);
    }
  }
}
