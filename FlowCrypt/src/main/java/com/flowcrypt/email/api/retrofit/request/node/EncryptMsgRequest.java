package com.flowcrypt.email.api.retrofit.request.node;

import com.google.gson.annotations.Expose;

import java.util.List;

/**
 * Using this class we can create a request to encrypt an input message using the given public keys.
 *
 * @author Denis Bondarenko
 * Date: 1/11/19
 * Time: 12:48 PM
 * E-mail: DenBond7@gmail.com
 */
public final class EncryptMsgRequest implements BaseNodeRequest {

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
}
