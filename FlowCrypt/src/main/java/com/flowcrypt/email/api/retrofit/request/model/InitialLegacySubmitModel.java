/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * This is a POJO object which used to make a request
 * to the API "https://flowcrypt.com/api/initial/legacy_submit"
 *
 * @author DenBond7
 * Date: 15.01.2018
 * Time: 16:37
 * E-mail: DenBond7@gmail.com
 */

public class InitialLegacySubmitModel extends BaseRequestModel {

  @Expose
  private String email;

  @SerializedName("pubkey")
  @Expose
  private String pubKey;

  public InitialLegacySubmitModel(String email, String pubKey) {
    this.email = email;
    this.pubKey = pubKey;
  }

  public String getEmail() {
    return email;
  }

  public String getPubKey() {
    return pubKey;
  }
}
