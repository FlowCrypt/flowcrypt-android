/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model.node;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * @author DenBond7
 */
public class PrivateKeyInfo {

  @SerializedName("private")
  @Expose
  private String privateKey;
  @Expose
  private String longid;


  public PrivateKeyInfo(String privateKey, String longid) {
    this.privateKey = privateKey;
    this.longid = longid;
  }

  public String getPrivateKey() {
    return privateKey;
  }

  public String getLongid() {
    return longid;
  }
}
