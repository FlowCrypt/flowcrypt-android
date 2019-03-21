/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.node;

import com.flowcrypt.email.api.retrofit.request.node.DecryptMsgRequest;
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails;
import com.flowcrypt.email.api.retrofit.response.node.NodeResponseWrapper;

import androidx.lifecycle.MutableLiveData;

/**
 * It's an entry point of all requests to work with PGP actions.
 *
 * @author Denis Bondarenko
 * Date: 2/15/19
 * Time: 10:25 AM
 * E-mail: DenBond7@gmail.com
 */
public interface PgpApiRepository {
  /**
   * Parse the given raw string and fetch a list of {@link NodeKeyDetails}.
   *
   * @param requestCode The unique request code for identify the current action.
   * @param liveData    An instance of {@link MutableLiveData} which will be used for the result delivering.
   * @param raw         The raw string which can take one key or many keys,
   *                    it can be private or public keys, it can be armored or binary.. doesn't matter.
   */
  void fetchKeyDetails(int requestCode, MutableLiveData<NodeResponseWrapper> liveData, String raw);

  /**
   * Parse the given raw MIME message and decrypt some parts if needed.
   *
   * @param requestCode The unique request code for identify the current action.
   * @param liveData    An instance of {@link MutableLiveData} which will be used for the result delivering.
   */
  void parseAndDecryptMsg(int requestCode, MutableLiveData<NodeResponseWrapper> liveData,
                          DecryptMsgRequest decryptMsgRequest);
}
