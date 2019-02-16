/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.node;

import android.text.TextUtils;

import com.flowcrypt.email.api.retrofit.node.NodeService;

import java.io.IOException;

import retrofit2.Response;

/**
 * Using this class we can create a request to parse the given string to find keys. It can take one key or many keys,
 * it can be private or public keys, it can be armored or binary.. doesn't matter.
 *
 * @author Denis Bondarenko
 * Date: 2/11/19
 * Time: 11:59 AM
 * E-mail: DenBond7@gmail.com
 */
public class ParseKeysRequest extends BaseNodeRequest {
  private String rawKey;

  public ParseKeysRequest(String rawKey) {
    this.rawKey = rawKey;
  }

  @Override
  public String getEndpoint() {
    return "parseKeys";
  }

  @Override
  public byte[] getData() {
    return TextUtils.isEmpty(rawKey) ? new byte[]{} : rawKey.getBytes();
  }

  @Override
  public Response getResponse(NodeService nodeService) throws IOException {
    if (nodeService != null) {
      return nodeService.parseKeys(this).execute();
    } else return null;
  }
}
