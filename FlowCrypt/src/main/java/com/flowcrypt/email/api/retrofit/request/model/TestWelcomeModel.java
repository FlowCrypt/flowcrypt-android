/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * The request model for the https://flowcrypt.com/attester/test/welcome API.
 *
 * @author Denis Bondarenko
 * Date: 12.07.2017
 * Time: 16:40
 * E-mail: DenBond7@gmail.com
 */

public class TestWelcomeModel extends BaseRequestModel {

  @SerializedName("email")
  @Expose
  private String email;

  @SerializedName("pubkey")
  @Expose
  private String pubKey;

  public TestWelcomeModel(String email, String pubKey) {
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
