/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.node;

import com.flowcrypt.email.api.retrofit.node.NodeService;
import com.flowcrypt.email.api.retrofit.request.model.node.PrivateKeyInfo;
import com.flowcrypt.email.js.PgpKeyInfo;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Response;

/**
 * Using this class we can create a request to decrypt an encrypted file using the given private keys.
 *
 * @author Denis Bondarenko
 * Date: 1/15/19
 * Time: 4:32 PM
 * E-mail: DenBond7@gmail.com
 */
public class DecryptFileRequest extends BaseNodeRequest {

  @SerializedName("keys")
  @Expose
  private List<PrivateKeyInfo> privateKeyInfoList;

  @SerializedName("passphrases")
  @Expose
  private List<String> passphrases;

  private byte[] data;

  public DecryptFileRequest(byte[] data, PgpKeyInfo[] prvKeys, String[] passphrases) {
    this.data = data;
    this.privateKeyInfoList = new ArrayList<>();

    for (PgpKeyInfo pgpKeyInfo : prvKeys) {
      privateKeyInfoList.add(new PrivateKeyInfo(pgpKeyInfo.getPrivate(), pgpKeyInfo.getLongid()));
    }

    this.passphrases = Arrays.asList(passphrases);
  }

  @Override
  public String getEndpoint() {
    return "decryptFile";
  }

  @Override
  public byte[] getData() {
    return data;
  }

  @Override
  public Response getResponse(NodeService nodeService) throws IOException {
    if (nodeService != null) {
      return nodeService.decryptFile(this).execute();
    } else return null;
  }
}
