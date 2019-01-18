package com.flowcrypt.email.api.retrofit.request.node;

import com.flowcrypt.email.api.retrofit.node.NodeService;
import com.flowcrypt.email.api.retrofit.request.model.node.PrivateKeyInfo;
import com.flowcrypt.email.api.retrofit.response.node.DecryptedMsgResult;
import com.flowcrypt.email.model.PgpKeyInfo;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Response;

/**
 * This class will be used for the message decryption.
 *
 * @author Denis Bondarenko
 * Date: 1/11/19
 * Time: 03:29 PM
 * E-mail: DenBond7@gmail.com
 */
public final class DecryptMsgRequest extends BaseNodeRequest {

  @SerializedName("keys")
  @Expose
  private List<PrivateKeyInfo> privateKeyInfoList;

  @SerializedName("passphrases")
  @Expose
  private List<String> passphrases;

  private String encryptedMsg;

  public DecryptMsgRequest(String encryptedMsg, PgpKeyInfo[] prvKeys, String[] passphrases) {
    this.encryptedMsg = encryptedMsg;
    this.privateKeyInfoList = new ArrayList<>();

    for (PgpKeyInfo pgpKeyInfo : prvKeys) {
      privateKeyInfoList.add(new PrivateKeyInfo(pgpKeyInfo.getPrivate(), pgpKeyInfo.getLongid()));
    }

    this.passphrases = Arrays.asList(passphrases);
  }

  @Override
  public String getEndpoint() {
    return "decryptMsg";
  }

  @Override
  public byte[] getData() {
    return encryptedMsg.getBytes();
  }

  @Override
  public Response getResponse(NodeService nodeService) throws IOException {
    if (nodeService != null) {
      return nodeService.decryptMsg(this).execute();
    } else return null;
  }

  @Override
  public Class getResponseClass() {
    return DecryptedMsgResult.class;
  }
}
