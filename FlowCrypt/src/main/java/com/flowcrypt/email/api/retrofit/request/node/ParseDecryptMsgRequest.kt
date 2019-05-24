/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.node;

import android.text.TextUtils;

import com.flowcrypt.email.api.retrofit.node.NodeService;
import com.flowcrypt.email.api.retrofit.request.model.node.PrivateKeyInfo;
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
public final class ParseDecryptMsgRequest extends BaseNodeRequest {

  @SerializedName("keys")
  @Expose
  private List<PrivateKeyInfo> privateKeyInfoList;

  @SerializedName("passphrases")
  @Expose
  private List<String> passphrases;

  @Expose
  private boolean isEmail;

  private String msg;

  public ParseDecryptMsgRequest(String msg, List<PgpKeyInfo> prvKeys, String[] passphrases) {
    this(msg, prvKeys, passphrases, false);
  }

  public ParseDecryptMsgRequest(String msg, List<PgpKeyInfo> prvKeys, String[] passphrases, boolean isEmail) {
    this.msg = msg;
    this.isEmail = isEmail;
    this.privateKeyInfoList = new ArrayList<>();

    for (PgpKeyInfo pgpKeyInfo : prvKeys) {
      privateKeyInfoList.add(new PrivateKeyInfo(pgpKeyInfo.getPrivate(), pgpKeyInfo.getLongid()));
    }

    this.passphrases = Arrays.asList(passphrases);
  }

  @Override
  public String getEndpoint() {
    return "parseDecryptMsg";
  }

  @Override
  public byte[] getData() {
    return TextUtils.isEmpty(msg) ? new byte[]{} : msg.getBytes();
  }

  @Override
  public Response getResponse(NodeService nodeService) throws IOException {
    if (nodeService != null) {
      return nodeService.parseDecryptMsg(this).execute();
    } else return null;
  }
}
