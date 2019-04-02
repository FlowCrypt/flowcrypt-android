/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel;

import android.app.Application;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.retrofit.node.PgpApiRepository;
import com.flowcrypt.email.js.UiJsManager;
import com.flowcrypt.email.model.PgpKeyInfo;
import com.flowcrypt.email.security.SecurityStorageConnector;

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
public class PrivateKeysViewModel extends BaseNodeApiViewModel implements SecurityStorageConnector.OnRefreshListener {
  private SecurityStorageConnector connector;
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
    this.connector = UiJsManager.getInstance(getApplication()).getSecurityStorageConnector();
    this.connector.attachOnRefreshListener(this);
    checkAndFetchKeys();
  }

  private void fetchKeys(String rawKey) {
    apiRepository.fetchKeyDetails(R.id.live_data_id_fetch_keys, responsesLiveData, rawKey);
  }

  private void checkAndFetchKeys() {
    PgpKeyInfo[] data = connector.getAllPgpPrivateKeys();
    if (data != null && data.length > 0) {
      StringBuilder builder = new StringBuilder();
      for (PgpKeyInfo pgpKeyInfo : data) {
        builder.append(pgpKeyInfo.getPrivate()).append("\n");
      }

      fetchKeys(builder.toString());
    } else {
      fetchKeys(null);
    }
  }
}
