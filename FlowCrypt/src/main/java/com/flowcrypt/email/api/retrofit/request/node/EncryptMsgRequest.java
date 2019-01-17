package com.flowcrypt.email.api.retrofit.request.node;

import com.flowcrypt.email.api.retrofit.node.NodeService;
import com.flowcrypt.email.api.retrofit.response.node.EncryptedMsgResult;
import com.google.gson.annotations.Expose;

import java.io.IOException;
import java.util.List;

import retrofit2.Response;

/**
 * Using this class we can create a request to encrypt an input message using the given public keys.
 *
 * @author Denis Bondarenko
 * Date: 1/11/19
 * Time: 12:48 PM
 * E-mail: DenBond7@gmail.com
 */
public final class EncryptMsgRequest extends BaseNodeRequest {

  @Expose
  private List<String> pubKeys;

  private String msg;

  public EncryptMsgRequest(String msg, List<String> pubKeys) {
    this.msg = msg;
    this.pubKeys = pubKeys;
  }

  @Override
  public String getEndpoint() {
    return "encryptMsg";
  }

  @Override
  public byte[] getData() {
    return msg != null ? msg.getBytes() : new byte[]{};
  }

  @Override
  public Response getResponse(NodeService nodeService) throws IOException {
    if (nodeService != null) {
      return nodeService.encryptMsg(this).execute();
    } else return null;
  }

  @Override
  public Class getResponseClass() {
    return EncryptedMsgResult.class;
  }
}
