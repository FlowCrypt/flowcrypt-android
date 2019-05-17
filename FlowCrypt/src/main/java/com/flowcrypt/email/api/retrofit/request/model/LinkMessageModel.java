/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * The request model for the https://flowcrypt.com/api/link/message API.
 *
 * @author Denis Bondarenko
 * Date: 13.07.2017
 * Time: 15:12
 * E-mail: DenBond7@gmail.com
 */

public class LinkMessageModel extends BaseRequestModel {

  @SerializedName("short")
  @Expose
  private String shortValue;

  public LinkMessageModel(String shortValue) {
    this.shortValue = shortValue;
  }

  public String getShortValue() {
    return shortValue;
  }
}
