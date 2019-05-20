/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * The request model for the https://flowcrypt.com/attester/initial/confirm API.
 *
 * @author Denis Bondarenko
 * Date: 12.07.2017
 * Time: 17:09
 * E-mail: DenBond7@gmail.com
 */

public class InitialConfirmModel implements RequestModel {

  @SerializedName("signed_message")
  @Expose
  private String signedMsg;

  public InitialConfirmModel(String signedMsg) {
    this.signedMsg = signedMsg;
  }

  public String getSignedMsg() {
    return signedMsg;
  }
}
