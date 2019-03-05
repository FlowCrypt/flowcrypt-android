/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.node;

import com.flowcrypt.email.api.retrofit.node.NodeService;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.List;

import retrofit2.Response;

/**
 * Using this class we can create a request to decrypt the given private key. The private key should be armored. We
 * can use one or more passphrases.
 *
 * @author Denis Bondarenko
 * Date: 2/12/19
 * Time: 4:34 PM
 * E-mail: DenBond7@gmail.com
 */
public class DecryptKeyRequest extends BaseNodeRequest {
  @Expose
  @SerializedName("armored")
  private String armoredKey;

  @Expose
  private List<String> passphrases;

  public DecryptKeyRequest(String armoredKey, List<String> passphrases) {
    this.armoredKey = armoredKey;
    this.passphrases = passphrases;
  }

  @Override
  public String getEndpoint() {
    return "decryptKey";
  }

  @Override
  public Response getResponse(NodeService nodeService) throws IOException {
    if (nodeService != null) {
      return nodeService.decryptKey(this).execute();
    } else return null;
  }
}
