/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.node;

import com.flowcrypt.email.api.retrofit.node.NodeService;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;

import retrofit2.Response;

/**
 * Using this class we can create a request to encrypt a private key using the given passphrase. The private key
 * should be armored.
 *
 * @author Denis Bondarenko
 * Date: 2/18/19
 * Time: 9:26 AM
 * E-mail: DenBond7@gmail.com
 */
public class EncryptKeyRequest extends BaseNodeRequest {

  @Expose
  @SerializedName("armored")
  private String armoredKey;

  @Expose
  private String passphrase;

  public EncryptKeyRequest(String armoredKey, String passphrase) {
    this.armoredKey = armoredKey;
    this.passphrase = passphrase;
  }

  @Override
  public String getEndpoint() {
    return "encryptKey";
  }

  @Override
  public Response getResponse(NodeService nodeService) throws IOException {
    if (nodeService != null) {
      return nodeService.encryptKey(this).execute();
    } else return null;
  }
}
