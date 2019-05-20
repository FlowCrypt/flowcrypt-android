/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * The request model for the https://flowcrypt.com/attester/replace/request API.
 *
 * @author Denis Bondarenko
 * Date: 13.07.2017
 * Time: 10:03
 * E-mail: DenBond7@gmail.com
 */

public class ReplaceRequestModel implements RequestModel {

  @SerializedName("signed_message")
  @Expose
  private String signedMsg;

  @SerializedName("new_pubkey")
  @Expose
  private String newPubKey;

  @SerializedName("email")
  @Expose
  private String email;

  public ReplaceRequestModel(String signedMsg, String newPubKey, String email) {
    this.signedMsg = signedMsg;
    this.newPubKey = newPubKey;
    this.email = email;
  }

  public String getSignedMsg() {
    return signedMsg;
  }

  public String getNewPubKey() {
    return newPubKey;
  }

  public String getEmail() {
    return email;
  }
}
