/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel;

import android.app.Application;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.retrofit.node.PgpApiRepository;
import com.flowcrypt.email.api.retrofit.request.node.ParseDecryptMsgRequest;
import com.flowcrypt.email.js.UiJsManager;
import com.flowcrypt.email.model.PgpKeyInfo;
import com.flowcrypt.email.security.SecurityStorageConnector;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

/**
 * This {@link ViewModel} implementation can be used to parse and decrypt (if needed) an incoming message.
 *
 * @author Denis Bondarenko
 * Date: 3/21/19
 * Time: 11:47 AM
 * E-mail: DenBond7@gmail.com
 */
public class DecryptMessageViewModel extends BaseNodeApiViewModel implements SecurityStorageConnector.OnRefreshListener {
  private SecurityStorageConnector connector;
  private PgpApiRepository apiRepository;
  private String rawMessage;

  public DecryptMessageViewModel(@NonNull Application application) {
    super(application);
  }

  @Override
  public void onRefresh() {
  }

  public void init(PgpApiRepository apiRepository) {
    this.apiRepository = apiRepository;
    this.connector = UiJsManager.getInstance(getApplication()).getStorageConnector();
    this.connector.attachOnRefreshListener(this);
  }

  public void decryptMessage(String rawMessage) {
    this.rawMessage = rawMessage;
    List<String> passphrases = new ArrayList<>();

    SecurityStorageConnector connector = UiJsManager.getInstance(getApplication()).getStorageConnector();

    PgpKeyInfo[] pgpKeyInfoArray = connector.getAllPgpPrivateKeys();

    for (PgpKeyInfo pgpKeyInfo : pgpKeyInfoArray) {
      passphrases.add(connector.getPassphrase(pgpKeyInfo.getLongid()));
    }

    apiRepository.parseDecryptMsg(R.id.live_data_id_parse_and_decrypt_msg, responsesLiveData,
        new ParseDecryptMsgRequest(rawMessage, pgpKeyInfoArray, passphrases.toArray(new String[0]), true));
  }
}
