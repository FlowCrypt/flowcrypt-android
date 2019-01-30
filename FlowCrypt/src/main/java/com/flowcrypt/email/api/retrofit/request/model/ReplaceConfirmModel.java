/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model;

import android.os.Parcel;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * The request model for the https://attester.flowcrypt.com/replace/confirm API.
 *
 * @author Denis Bondarenko
 * Date: 13.07.2017
 * Time: 11:50
 * E-mail: DenBond7@gmail.com
 */

public class ReplaceConfirmModel extends BaseRequestModel {

  public static final Creator<ReplaceConfirmModel> CREATOR = new Creator<ReplaceConfirmModel>() {
    @Override
    public ReplaceConfirmModel createFromParcel(Parcel source) {
      return new ReplaceConfirmModel(source);
    }

    @Override
    public ReplaceConfirmModel[] newArray(int size) {
      return new ReplaceConfirmModel[size];
    }
  };

  @SerializedName("signed_message")
  @Expose
  private String signedMsg;

  public ReplaceConfirmModel(String signedMsg) {
    this.signedMsg = signedMsg;
  }


  protected ReplaceConfirmModel(Parcel in) {
    this.signedMsg = in.readString();
  }

  @Override
  public String toString() {
    return "ReplaceConfirmModel{" +
        "signedMsg='" + signedMsg + '\'' +
        "} " + super.toString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.signedMsg);
  }
}
