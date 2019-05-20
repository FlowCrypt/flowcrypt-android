/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model;

import android.text.TextUtils;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * This is a POJO object which used to make a request to the API "https://flowcrypt.com/attester/lookup/email"
 *
 * @author DenBond7
 * Date: 24.04.2017
 * Time: 13:27
 * E-mail: DenBond7@gmail.com
 */

public class PostLookUpEmailModel implements RequestModel {

  @SerializedName("email")
  @Expose
  private String email;

  public PostLookUpEmailModel(String email) {
    if (!TextUtils.isEmpty(email)) {
      this.email = email.toLowerCase();
    }
  }

  public String getEmail() {
    return email;
  }
}
